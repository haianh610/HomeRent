package com.example.homerent.fragment.tenant;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.homerent.MainActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.homerent.ChangeAccountActivity;

import com.example.homerent.ChangePasswordActivity;

import com.example.homerent.LoginActivity;
import com.example.homerent.R;
import com.example.homerent.activity.tenant.TenantHomeActivity; // Import để cập nhật title
import com.example.homerent.databinding.FragmentAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TenantAccountFragment extends Fragment {

    public static final String TAG = "Thông tin tài khoản";
    private static final String LOG_TAG = "TenantAccountFragment";

    private FragmentAccountBinding binding;
    private FirebaseAuth auth;

    public TenantAccountFragment() {
        // Required empty public constructor
    }

    public static TenantAccountFragment newInstance() {
        return new TenantAccountFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();

        setupListeners();
        showUserInfo();

        return binding.getRoot();
    }

    private void setupListeners() {
        // Chuyển đến trang đổi thông tin
        binding.tvTtTK.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ChangeAccountActivity.class))
        );

        // Chuyển đến trang đổi mật khẩu
        binding.tvDoiMk.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ChangePasswordActivity.class))
        );

        // Xử lý đăng xuất
        binding.tvDangXuat.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        auth.signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void showUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Lấy thông tin cơ bản
        String email = user.getEmail();
        Uri photoUrl = user.getPhotoUrl();
        String uid = user.getUid();

        // Sử dụng phương thức từ MainActivity để lấy tên người dùng
        MainActivity.getUserName(uid, new MainActivity.UserNameCallback() {
            @Override
            public void onUserNameLoaded(String name) {
                if (isAdded() && getActivity() != null) { // Kiểm tra fragment còn attached
                    binding.tvName.setText(name);
                    Log.d(LOG_TAG, "Tên người dùng đã được tải: " + name);
                }
            }
        });

        binding.tvEmail.setText(email != null ? email : "Không có email");

        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).into(binding.imgAvatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.ic_ava);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật title toolbar
        if (getActivity() instanceof TenantHomeActivity) {
        }
        // Cập nhật lại thông tin nếu có thay đổi
        showUserInfo();
    }
}