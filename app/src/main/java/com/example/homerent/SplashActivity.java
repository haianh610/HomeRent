package com.example.homerent;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerent.activity.landlord.LandlordHomeActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Sử dụng Handler để trì hoãn việc kiểm tra một chút (tùy chọn, để user thấy splash)
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, 1500); // 1.5 giây delay
    }

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