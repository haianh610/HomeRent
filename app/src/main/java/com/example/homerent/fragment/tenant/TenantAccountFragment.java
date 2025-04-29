package com.example.homerent.fragment.tenant;

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
import com.example.homerent.activity.landlord.LandlordHomeActivity; // Import LandlordHome
import com.example.homerent.databinding.FragmentAccountBinding;
import com.example.homerent.fragment.BaseAccountFragment; // Import Base Fragment
import com.google.android.material.button.MaterialButton;


public class TenantAccountFragment extends BaseAccountFragment<FragmentAccountBinding> {

    public static final String TAG = "Tài khoản"; // Dùng cho title
    private static final String LOG_TAG = "TenantAccountFragment";

    public static TenantAccountFragment newInstance() {
        return new TenantAccountFragment();
    }

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

    // Định nghĩa hành động nút chuyển đổi cho Người thuê - LUÔN CHUYỂN SANG LANDLORD
    @Override
    protected void setupSwitchModeButton(MaterialButton switchButton) {
        switchButton.setText("Chuyển sang đăng tin");
        switchButton.setIconResource(R.drawable.ic_switch_account);
        switchButton.setVisibility(View.VISIBLE); // Đảm bảo nút hiện

        switchButton.setOnClickListener(v -> {
            // Gọi hàm của lớp cha để cập nhật role thành "landlord" và chuyển activity
            updateUserRoleAndSwitch("landlord");
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