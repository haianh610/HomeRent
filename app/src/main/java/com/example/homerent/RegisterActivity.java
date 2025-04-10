package com.example.homerent;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils
import android.util.Log; // Import Log
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatRadioButton; // Sử dụng AppCompatRadioButton nếu theme của bạn là AppCompat


import androidx.appcompat.app.AppCompatActivity;

// Import model User
import com.example.homerent.activity.landlord.LandlordHomeActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.example.homerent.model.User;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Xóa import Map và HashMap nếu không dùng ở đâu khác
// import java.util.HashMap;
// import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // Thêm EditText cho tên
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private RadioGroup radioGroupUserType;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity"; // Thêm TAG để log

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ EditText tên
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        // ... (Lấy name, email, password, validation như cũ) ...
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        // ... (Validation code) ...

        int selectedId = radioGroupUserType.getCheckedRadioButtonId();
        // ... (Kiểm tra selectedId và xác định role như cũ) ...
        String role;
        if (selectedId == R.id.radioLandlord) { // Thay ID thực tế
            role = "landlord";
        } else if (selectedId == R.id.radioTenant) { // Thay ID thực tế
            role = "tenant";
        } else {
            role = "";
            Toast.makeText(this, "Vui lòng chọn loại người dùng", Toast.LENGTH_SHORT).show();
            return;
        }


        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // --- Tạo đối tượng User ---
                            User newUser = new User();
                            newUser.setUserId(userId);
                            newUser.setName(name);
                            newUser.setEmail(email);
                            newUser.setRole(role);
                            // *** THÊM DÒNG NÀY ĐỂ GÁN CREATEDAT ***
                            newUser.setCreatedAt(Timestamp.now());
                            // newUser.setAvatarUrl(null); // Hoặc URL mặc định nếu có

                            // --- Lưu đối tượng User vào Firestore ---
                            db.collection("users")
                                    .document(userId)
                                    .set(newUser) // Lưu object User
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User profile created successfully for: " + userId);
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                                        // Trong hàm registerUser(), sau khi lưu user vào Firestore
                                        Intent intent;
                                        if ("landlord".equals(role)) {
                                            intent = new Intent(RegisterActivity.this, LandlordHomeActivity.class);
                                        } else {
                                            intent = new Intent(RegisterActivity.this, TenantHomeActivity.class);
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();

                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error writing user document", e);
                                        Toast.makeText(RegisterActivity.this, "Đăng ký Auth thành công nhưng lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        btnRegister.setEnabled(true);
                                    });
                        } else {
                            Log.e(TAG, "FirebaseUser is null after successful registration.");
                            Toast.makeText(RegisterActivity.this, "Lỗi không xác định, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                            btnRegister.setEnabled(true);
                        }

                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }
}