package com.example.homerent;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.homerent.databinding.FragmentChangeAccountBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ChangeAccountActivity extends AppCompatActivity {
    private FragmentChangeAccountBinding binding;
    private Uri photoUrl;
    private ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentChangeAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.imgAvatar.setImageResource(R.drawable.ic_ava);
        progressDialog = new ProgressDialog(this);
        setUserInfo();
        initListener();
        initActivityResultLauncher();
    }

    private void initActivityResultLauncher() {
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        photoUrl = imageUri;
                        binding.imgAvatar.setImageURI(photoUrl);
                    }
                }
        );
    }

    private void setUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        binding.edtTen.setText(user.getDisplayName());
        binding.edtEmail.setText(user.getEmail());
        Glide.with(this).load(user.getPhotoUrl()).into(binding.imgAvatar);
    }

    private void initListener() {
        binding.imgAvatar.setOnClickListener(v -> pickImage());
        binding.btnLuu.setOnClickListener(v -> onClickUpdateProfile());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            openGallery();
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        mActivityResultLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
    }

    private void onClickUpdateProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressDialog.setMessage("Đang lưu thông tin...");
        progressDialog.show();

        String fullname = binding.edtTen.getText().toString();
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder().setDisplayName(fullname);

        if (photoUrl != null) builder.setPhotoUri(photoUrl);

        user.updateProfile(builder.build())
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        Log.e("UpdateProfile", "Lỗi: ", task.getException());
                    }
                });
    }

    public void setBitmapImage(Bitmap bitmap) {
        if (binding != null) binding.imgAvatar.setImageBitmap(bitmap);
    }

    public void setImageUri(Uri uri) {
        this.photoUrl = uri;
    }
}