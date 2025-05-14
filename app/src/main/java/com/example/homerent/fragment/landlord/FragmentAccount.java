package com.example.homerent.fragment.landlord;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.homerent.R;
import com.example.homerent.activity.tenant.TenantHomeActivity; // Import TenantHome
import com.example.homerent.databinding.FragmentAccountBinding;
import com.example.homerent.fragment.BaseAccountFragment; // Import Base Fragment
import com.example.homerent.model.User;
import com.google.android.material.button.MaterialButton;


public class FragmentAccount extends BaseAccountFragment<FragmentAccountBinding> {

    public static final String TAG = "Tài khoản";
    private static final String LOG_TAG = "LandlordAccountFragment";

    public static FragmentAccount newInstance() { return new FragmentAccount(); }

    @Override
    protected FragmentAccountBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAccountBinding.inflate(inflater, container, false);
    }

    @Override
    protected String getFragmentTag() { return TAG; }

    @Override
    protected String getLogTag() { return LOG_TAG; }

    // Cung cấp Views từ Binding (giữ nguyên)
    @Override protected ImageView getAvatarImageView() { return binding.imgAvatar; }
    @Override protected TextView getNameTextView() { return binding.tvName; }
    @Override protected TextView getEmailTextView() { return binding.tvEmail; }
    @Override protected LinearLayout getChangeAccountLayout() { return binding.llChangeAccount; }
    @Override protected LinearLayout getChangePasswordLayout() { return binding.llChangePassword; }
    @Override protected LinearLayout getLogoutLayout() { return binding.llLogout; }
    @Override protected MaterialButton getSwitchModeButton() { return binding.btnSwitchMode; }


    // Hành động nút chuyển đổi cho Chủ nhà - LUÔN CHUYỂN SANG TENANT (Giữ nguyên)
    @Override
    protected void setupSwitchModeButton(MaterialButton switchButton) {
        switchButton.setText("Chuyển sang tìm tin");
        switchButton.setIconResource(R.drawable.ic_switch_account);
        switchButton.setVisibility(View.VISIBLE); // Đảm bảo nút hiện

        switchButton.setOnClickListener(v -> {
            // Gọi hàm của lớp cha để cập nhật role thành "tenant" và chuyển activity
            updateUserRoleAndSwitch("tenant");
        });
    }
    // Cung cấp ProgressBar (nếu có trong layout binding) hoặc return null
    @Override
    protected ProgressBar getSwitchModeProgressBar() {
        // Giả sử không có ProgressBar riêng cho nút này trong layout FragmentAccountBinding
        return null;
        // Nếu có, ví dụ: return binding.progressBarSwitch;
    }

}