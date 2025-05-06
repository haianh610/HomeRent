package com.example.homerent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Thêm import View
import android.view.inputmethod.EditorInfo; // Thêm import EditorInfo
import android.widget.Button;
// import android.widget.EditText; // Không cần EditText thường nữa
import android.widget.ProgressBar; // Thêm import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerent.activity.landlord.LandlordHomeActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.google.android.material.textfield.TextInputEditText; // Import TextInputEditText
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    // Sử dụng TextInputEditText
    private TextInputEditText emailEditText, passwordEditText;
    private Button btnLogin;
    private TextView registerHere;
    private ProgressBar progressBarLogin; // Thêm ProgressBar
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Bật nếu muốn tràn viền
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ TextInputEditText và ProgressBar
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        registerHere = findViewById(R.id.registerHere);
        progressBarLogin = findViewById(R.id.progressBarLogin); // Ánh xạ ProgressBar

        btnLogin.setOnClickListener(v -> loginUser());
        registerHere.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // --- Xử lý sự kiện Enter trên ô mật khẩu ---
        passwordEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Người dùng nhấn nút Done/Enter trên bàn phím
                loginUser(); // Gọi hàm đăng nhập
                return true; // Đã xử lý sự kiện
            }
            return false; // Chưa xử lý, để hệ thống xử lý tiếp (nếu có)
        });
        // ------------------------------------------
    }

    private void loginUser() {
        String emailInput = emailEditText.getText().toString().trim();
        String passwordInput = passwordEditText.getText().toString().trim();

        if (emailInput.isEmpty()) {
            // Hiển thị lỗi trên TextInputLayout nếu có
            com.google.android.material.textfield.TextInputLayout tilEmail = findViewById(R.id.tilEmail);
            if (tilEmail != null) tilEmail.setError("Vui lòng nhập tài khoản");
            else emailEditText.setError("Vui lòng nhập tài khoản"); // Fallback
            emailEditText.requestFocus();
            return;
        } else {
            com.google.android.material.textfield.TextInputLayout tilEmail = findViewById(R.id.tilEmail);
            if (tilEmail != null) tilEmail.setError(null); // Xóa lỗi
            else emailEditText.setError(null);
        }

        if (passwordInput.isEmpty()) {
            com.google.android.material.textfield.TextInputLayout tilPassword = findViewById(R.id.tilPassword);
            if (tilPassword != null) tilPassword.setError("Vui lòng nhập mật khẩu");
            else passwordEditText.setError("Vui lòng nhập mật khẩu");
            passwordEditText.requestFocus();
            return;
        } else {
            com.google.android.material.textfield.TextInputLayout tilPassword = findViewById(R.id.tilPassword);
            if (tilPassword != null) tilPassword.setError(null);
            else passwordEditText.setError(null);
        }

        // --- Hiển thị ProgressBar và vô hiệu hóa nút ---
        showLoading(true);
        // ------------------------------------------

        mAuth.signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener(this, task -> {
                    // --- Luôn ẩn ProgressBar và bật lại nút khi hoàn tất ---
                    showLoading(false);
                    // -------------------------------------------------
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            // Hiển thị loading lần nữa khi lấy role (tùy chọn)
                            // showLoading(true);
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        // showLoading(false); // Ẩn nếu đã bật ở trên
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");
                                            Intent intent;
                                            if ("landlord".equals(role)) {
                                                intent = new Intent(LoginActivity.this, LandlordHomeActivity.class);
                                            } else { // Mặc định là tenant nếu role khác landlord hoặc null
                                                intent = new Intent(LoginActivity.this, TenantHomeActivity.class);
                                            }
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                            // Toast thành công có thể không cần vì đã chuyển màn hình
                                            // Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                                            mAuth.signOut(); // Đăng xuất nếu không có profile
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // showLoading(false); // Ẩn nếu đã bật ở trên
                                        Toast.makeText(LoginActivity.this, "Lỗi khi lấy thông tin vai trò: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        mAuth.signOut(); // Đăng xuất nếu lỗi lấy role
                                    });
                        } else {
                            // Trường hợp hiếm: user null sau khi đăng nhập thành công
                            Toast.makeText(LoginActivity.this, "Lỗi đăng nhập, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm helper để quản lý trạng thái loading
    private void showLoading(boolean isLoading) {
        if (progressBarLogin != null) {
            progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading); // Vô hiệu hóa nút khi đang load
        }
        if (emailEditText != null) {
            emailEditText.setEnabled(!isLoading);
        }
        if (passwordEditText != null) {
            passwordEditText.setEnabled(!isLoading);
        }
    }
}