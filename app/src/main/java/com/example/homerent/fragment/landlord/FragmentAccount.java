package com.example.homerent.fragment.landlord;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            binding.tvName.setText(name != null ? name : "Không có tên");
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

        return binding.getRoot();
    }
    public void showUserInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        String email = user.getEmail();
        Uri photoUrl = user.getPhotoUrl();

        if (name == null || name.isEmpty()) {
            binding.tvName.setVisibility(View.GONE);
        } else {
            binding.tvName.setVisibility(View.VISIBLE);
            binding.tvName.setText(name);
        }
        binding.tvEmail.setText(email != null ? email : "No email available");

        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).into(binding.imgAvatar);
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
