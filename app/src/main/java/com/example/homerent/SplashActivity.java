package com.example.homerent;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerent.activity.landlord.LandlordHomeActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.android.libraries.places.api.Places; // Thêm import này
import com.google.android.libraries.places.api.net.PlacesClient; // Có thể dùng nếu cần client ngay

@SuppressLint("CustomSplashScreen") // Chỉ dùng nếu bạn muốn bỏ cảnh báo mặc định
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // --- KHỞI TẠO PLACES SDK ---
        initializePlacesSDK();
        // -------------------------

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Sử dụng Handler để trì hoãn việc kiểm tra một chút
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, 500);
    }

    // --- HÀM KHỞI TẠO PLACES SDK ---
    private void initializePlacesSDK() {
        // Lấy API Key từ manifest placeholders (được định nghĩa trong build.gradle)
        // Hoặc từ strings.xml nếu bạn lưu ở đó
        String apiKey = "";
        try {
            // Cách 1: Lấy từ Manifest Placeholders
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            apiKey = bundle.getString("com.google.android.geo.API_KEY"); // Tên key trong manifest

            // Cách 2: Lấy từ strings.xml (nếu bạn dùng cách này)
            // apiKey = getString(R.string.MAPS_API_KEY);

        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }


        if (TextUtils.isEmpty(apiKey) || apiKey.equals("YOUR_API_KEY_HERE") || apiKey.equals("")) { // Kiểm tra kỹ giá trị rỗng hoặc placeholder
            Log.e(TAG, "*** Places API Key is missing or invalid in AndroidManifest.xml or resources! ***");
            // Bạn có thể hiển thị lỗi cho người dùng hoặc vô hiệu hóa tính năng Places
            Toast.makeText(this, "Lỗi cấu hình API Key cho dịch vụ vị trí.", Toast.LENGTH_LONG).show();
            // Không return ở đây, có thể các phần khác của app vẫn chạy được
        } else {
            // Initialize the SDK
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
                // placesClient = Places.createClient(this); // Khởi tạo client nếu cần dùng ngay
                Log.i(TAG, "Places SDK Initialized successfully.");
            } else {
                Log.i(TAG, "Places SDK already initialized.");
            }
        }
    }
    // -------------------------------

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Người dùng đã đăng nhập, kiểm tra role và chuyển hướng
            Log.d(TAG, "User already logged in: " + currentUser.getUid());
            redirectToHome(currentUser.getUid());
        } else {
            // Người dùng chưa đăng nhập, chuyển đến LoginActivity
            Log.d(TAG, "No user logged in, redirecting to LoginActivity.");
            goToLoginActivity();
        }
    }

    private void redirectToHome(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Intent intent;
                        if ("landlord".equals(role)) {
                            Log.d(TAG, "User is landlord, redirecting to LandlordHomeActivity.");
                            intent = new Intent(SplashActivity.this, LandlordHomeActivity.class);
                        } else if ("tenant".equals(role)){
                            Log.d(TAG, "User is tenant, redirecting to TenantHomeActivity.");
                            intent = new Intent(SplashActivity.this, TenantHomeActivity.class);
                        } else {
                            // Role không xác định hoặc null, xử lý như lỗi hoặc mặc định
                            Log.w(TAG, "User role not found or invalid for userId: " + userId + ". Redirecting to Login.");
                            // Có thể đăng xuất user ở đây nếu muốn: mAuth.signOut();
                            goToLoginActivity();
                            return; // Quan trọng: thoát khỏi đây
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Đóng SplashActivity
                    } else {
                        // User đã đăng nhập trên Auth nhưng không có document trong Firestore (lỗi)
                        Log.e(TAG, "User document not found in Firestore for logged-in user: " + userId + ". Logging out and redirecting to Login.");
                        Toast.makeText(SplashActivity.this, "Lỗi dữ liệu người dùng, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        mAuth.signOut(); // Đăng xuất user bị lỗi
                        goToLoginActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi truy cập Firestore
                    Log.e(TAG, "Error fetching user role for userId: " + userId, e);
                    Toast.makeText(SplashActivity.this, "Lỗi kết nối, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    // Có thể cho phép ở lại Splash hoặc chuyển về Login tùy UX
                    // mAuth.signOut(); // Cân nhắc đăng xuất nếu không lấy được role
                    goToLoginActivity();
                });
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng SplashActivity
    }
}