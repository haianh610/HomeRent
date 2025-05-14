package com.example.homerent.fragment; // Tạo package common nếu chưa có

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding; // Import ViewBinding chung

import com.bumptech.glide.Glide;
import com.example.homerent.ChangeAccountActivity;
import com.example.homerent.ChangePasswordActivity;
import com.example.homerent.LoginActivity;
import com.example.homerent.R;
import com.example.homerent.activity.landlord.LandlordHomeActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.example.homerent.model.User; // Import User model
import com.google.android.material.button.MaterialButton; // Import MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Dùng M3 Dialog
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public abstract class BaseAccountFragment<T extends ViewBinding> extends Fragment {

    protected T binding;
    protected FirebaseAuth auth;
    protected FirebaseFirestore db;
    protected FirebaseUser currentUser;
    protected User currentUserProfile;
    protected ProgressBar switchModeProgressBar; // Optional: for loading state

    protected abstract T inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    // ** Thay đổi: setupSwitchModeButton giờ chỉ cần biết role ĐÍCH **
    protected abstract void setupSwitchModeButton(MaterialButton switchButton);
    protected abstract String getFragmentTag();

    // Thêm ProgressBar vào các hàm abstract để lớp con cung cấp (nếu dùng)
    protected abstract ProgressBar getSwitchModeProgressBar(); // Optional

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();
        // Lấy ProgressBar (nếu có)
        switchModeProgressBar = getSwitchModeProgressBar();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
        loadUserProfile();
    }

    protected void setupListeners() {
        // Lấy các LinearLayout từ binding (giả sử FragmentAccountBinding chứa chúng)
        LinearLayout llChangeAccount = getChangeAccountLayout();
        LinearLayout llChangePassword = getChangePasswordLayout();
        LinearLayout llLogout = getLogoutLayout();

        if (llChangeAccount != null) {
            llChangeAccount.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChangeAccountActivity.class)));
        }
        if (llChangePassword != null) {
            llChangePassword.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChangePasswordActivity.class)));
        }
        if (llLogout != null) {
            llLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        MaterialButton btnSwitchMode = getSwitchModeButton();
        if (btnSwitchMode != null) {
            setupSwitchModeButton(btnSwitchMode); // Lớp con sẽ gán OnClickListener gọi updateUserRoleAndSwitch
        }
    }

    // --- HÀM MỚI ĐỂ CẬP NHẬT ROLE VÀ CHUYỂN ACTIVITY ---
    protected void updateUserRoleAndSwitch(final String newRole) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Lỗi: Chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        String targetActivityName = "landlord".equals(newRole) ? "Đăng tin" : "Tìm tin";
        Log.d(getLogTag(), "Attempting to switch role to: " + newRole + " and navigate to " + targetActivityName);


        // Hiển thị loading và vô hiệu hóa nút (nếu có ProgressBar)
        MaterialButton switchButton = getSwitchModeButton(); // Lấy lại nút
        if (switchButton != null) switchButton.setEnabled(false);
        if (switchModeProgressBar != null) switchModeProgressBar.setVisibility(View.VISIBLE);


        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .update("role", newRole) // Chỉ cập nhật trường role
                .addOnSuccessListener(aVoid -> {
                    Log.i(getLogTag(), "User role successfully updated to: " + newRole);
                    Toast.makeText(getContext(), "Đã chuyển sang chế độ " + targetActivityName, Toast.LENGTH_SHORT).show();

                    // Cập nhật profile cục bộ (tùy chọn)
                    if(currentUserProfile != null) {
                        currentUserProfile.setRole(newRole);
                    }

                    // Chuyển Activity SAU KHI cập nhật thành công
                    Intent intent;
                    if ("landlord".equals(newRole)) {
                        intent = new Intent(getActivity(), LandlordHomeActivity.class);
                    } else { // tenant
                        intent = new Intent(getActivity(), TenantHomeActivity.class);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finishAffinity();
                    }
                    // Không cần bật lại nút hay ẩn progress vì activity sẽ đóng
                })
                .addOnFailureListener(e -> {
                    Log.e(getLogTag(), "Error updating user role to: " + newRole, e);
                    Toast.makeText(getContext(), "Lỗi khi chuyển đổi vai trò. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    // Ẩn loading và bật lại nút nếu thất bại
                    if (switchButton != null) switchButton.setEnabled(true);
                    if (switchModeProgressBar != null) switchModeProgressBar.setVisibility(View.GONE);
                });
    }

    protected void showLogoutConfirmationDialog() {
        // Sử dụng MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finishAffinity(); // Đóng tất cả activity của app
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    protected void loadUserProfile() {
        if (currentUser == null) {
            Log.e(getLogTag(), "Current user is null, cannot load profile.");
            // Có thể hiển thị trạng thái lỗi trên UI
            displayUserInfo(null); // Hiển thị trạng thái mặc định/lỗi
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && getActivity() != null) {
                        if (documentSnapshot.exists()) {
                            currentUserProfile = documentSnapshot.toObject(User.class);
                            Log.d(getLogTag(), "User profile loaded: " + (currentUserProfile != null ? currentUserProfile.getName() : "null"));
                        } else {
                            Log.w(getLogTag(), "User document does not exist in Firestore for UID: " + uid);
                            currentUserProfile = null; // Đặt là null nếu không tìm thấy
                        }
                        displayUserInfo(currentUserProfile); // Hiển thị thông tin (kể cả khi null)
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getActivity() != null) {
                        Log.e(getLogTag(), "Error loading user profile", e);
                        Toast.makeText(getContext(), "Lỗi tải thông tin tài khoản", Toast.LENGTH_SHORT).show();
                        currentUserProfile = null;
                        displayUserInfo(null); // Hiển thị trạng thái mặc định/lỗi
                    }
                });
    }

    // Hiển thị thông tin lên UI
    protected void displayUserInfo(@Nullable User userProfile) {
        // Lấy các TextView và ImageView từ binding
        ImageView imgAvatar = getAvatarImageView();
        TextView tvName = getNameTextView();
        TextView tvEmail = getEmailTextView();

        if (imgAvatar == null || tvName == null || tvEmail == null) {
            Log.e(getLogTag(), "One or more UI elements for user info are null in binding.");
            return;
        }


        if (userProfile != null) {
            tvName.setText(userProfile.getName() != null ? userProfile.getName() : "Chưa cập nhật tên");
            tvEmail.setText(userProfile.getEmail() != null ? userProfile.getEmail() : "Không có email");

            if (userProfile.getAvatarUrl() != null && !userProfile.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(userProfile.getAvatarUrl())
                        .placeholder(R.drawable.person_24px) // Placeholder M3
                        .error(R.drawable.person_24px)     // Error placeholder M3
                        .into(imgAvatar);
            } else if (currentUser != null && currentUser.getPhotoUrl() != null) {
                // Fallback to Firebase Auth photo URL if Firestore URL is missing
                Glide.with(this).load(currentUser.getPhotoUrl()).into(imgAvatar);
            }
            else {
                imgAvatar.setImageResource(R.drawable.person_24px); // Default M3 avatar
            }
        } else {
            // Trường hợp userProfile null hoặc currentUser null
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser(); // Lấy lại để chắc chắn
            tvName.setText("Khách");
            tvEmail.setText(authUser != null ? authUser.getEmail() : "Không thể tải email");
            imgAvatar.setImageResource(R.drawable.person_24px);
        }
        // Cập nhật lại text nút chuyển đổi dựa trên role HIỆN TẠI (sau khi load)
        MaterialButton switchButton = getSwitchModeButton();
        if (switchButton != null) {
            switchButton.setVisibility(View.VISIBLE); // Đảm bảo luôn hiện
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại profile khi quay lại fragment
        loadUserProfile();
        // Cập nhật title (nếu cần - Activity sẽ gọi)
        // if (getActivity() instanceof TenantHomeActivity) { ((TenantHomeActivity) getActivity()).updateToolbarTitle(getFragmentTag()); }
        // else if (getActivity() instanceof LandlordHomeActivity) { ((LandlordHomeActivity) getActivity()).updateToolbarTitle(getFragmentTag()); }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Quan trọng: clear binding để tránh memory leak
    }

    // --- Các phương thức abstract để lớp con cung cấp View từ Binding ---
    // Sử dụng FragmentAccountBinding làm kiểu trả về chung
    protected abstract ImageView getAvatarImageView();
    protected abstract TextView getNameTextView();
    protected abstract TextView getEmailTextView();
    protected abstract LinearLayout getChangeAccountLayout();
    protected abstract LinearLayout getChangePasswordLayout();
    protected abstract LinearLayout getLogoutLayout();
    protected abstract MaterialButton getSwitchModeButton();
    protected abstract String getLogTag(); // Lấy TAG cho logging


}