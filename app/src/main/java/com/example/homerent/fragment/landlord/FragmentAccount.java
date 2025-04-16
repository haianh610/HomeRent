package com.example.homerent.fragment.landlord;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.homerent.MainActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.homerent.ChangeAccountActivity;
import com.example.homerent.ChangePasswordActivity;
import com.example.homerent.LoginActivity;

import com.example.homerent.R;
import com.example.homerent.databinding.FragmentAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FragmentAccount extends Fragment {
    private static final String TAG = "FragmentAccount";
    private FirebaseAuth auth;
    FragmentAccountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();

        binding.tvTtTK.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangeAccountActivity.class);
            startActivity(intent);
        });
        binding.tvDoiMk.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        binding.tvDangXuat.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        auth.signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xoá backstack
                        startActivity(intent);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        showUserInformation();

        return binding.getRoot();
    }

    public void showUserInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Lấy thông tin cơ bản
        String email = user.getEmail();
        Uri photoUrl = user.getPhotoUrl();
        String uid = user.getUid();

        // Dùng hàm helper từ MainActivity để lấy tên người dùng
        MainActivity.getUserName(uid, new MainActivity.UserNameCallback() {
            @Override
            public void onUserNameLoaded(String name) {
                if (isAdded() && getActivity() != null) { // Kiểm tra fragment còn attached
                    binding.tvName.setText(name);
                    Log.d(TAG, "Tên người dùng đã được tải: " + name);
                }
            }
        });

        binding.tvEmail.setText(email != null ? email : "Không có email");

        if (photoUrl != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_ava)
                    .into(binding.imgAvatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.ic_ava);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showUserInformation();
    }
}