package com.example.homerent;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerent.databinding.FragmentChangePasswordBinding;
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
        String password = binding.edtPassword.getText().toString().trim();

        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đổi mật khẩu")
                .setMessage("Bạn có chắc chắn muốn đổi mật khẩu?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    progressDialog.setMessage("Đang đổi mật khẩu...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.updatePassword(password)
                                .addOnCompleteListener(task -> {
                                    progressDialog.dismiss();
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
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
