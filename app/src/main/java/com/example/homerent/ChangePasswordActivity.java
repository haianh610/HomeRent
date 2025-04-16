package com.example.homerent;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerent.databinding.FragmentChangePasswordBinding;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {
    private FragmentChangePasswordBinding binding;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);

        binding.btnChangePassword.setOnClickListener(v -> onClickChangePassword());
        binding.back.setOnClickListener(v -> finish());
    }

    private void onClickChangePassword() {
        String oldPassword = binding.edtOldPassword.getText().toString().trim();
        String newPassword = binding.edtPassword.getText().toString().trim();
        String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

        // Kiểm tra các trường nhập mật khẩu
        if (oldPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu cũ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lại mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra mật khẩu mới và mật khẩu xác nhận có khớp nhau không
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị hộp thoại xác nhận
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đổi mật khẩu")
                .setMessage("Bạn có chắc chắn muốn đổi mật khẩu?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    progressDialog.setMessage("Đang đổi mật khẩu...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        // Xác thực lại người dùng với mật khẩu cũ
                        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldPassword))
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Nếu mật khẩu cũ đúng, tiến hành thay đổi mật khẩu mới
                                        user.updatePassword(newPassword)
                                                .addOnCompleteListener(task1 -> {
                                                    progressDialog.dismiss();
                                                    if (task1.isSuccessful()) {
                                                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(this, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(this, "Mật khẩu cũ không chính xác", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }
}