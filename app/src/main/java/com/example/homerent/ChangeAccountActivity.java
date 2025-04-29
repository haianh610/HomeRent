package com.example.homerent;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem; // For Toolbar back button
import android.view.View;
import android.widget.FrameLayout; // For progress overlay
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.core.content.ContextCompat; // Import ContextCompat

import com.bumptech.glide.Glide;
import com.example.homerent.databinding.ActivityChangeAccountBinding; // Import correct binding
import com.example.homerent.model.User; // Import User model
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Use M3 Dialog
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

public class ChangeAccountActivity extends AppCompatActivity {

    private static final String TAG = "ChangeAccountActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;

    private ActivityChangeAccountBinding binding; // Use generated binding class
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private User currentUserProfile; // Store loaded Firestore profile

    private Uri newAvatarUri = null; // Store URI from picker/camera
    private Uri cameraImageUri = null; // Store URI for image taken by camera

    private FrameLayout progressOverlay;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangeAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind Progress Overlay
        progressOverlay = binding.progressOverlay; // Assuming ID is progressOverlay

        // Setup Toolbar
        setSupportActionBar(binding.toolbarChangeAccount);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Launchers
        initializeLaunchers();

        // Load user info from Firestore
        loadUserProfile();

        // Setup Listeners
        setupListeners();
    }

    private void initializeLaunchers() {
        // Gallery Launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        newAvatarUri = result.getData().getData();
                        if (newAvatarUri != null) {
                            Glide.with(this).load(newAvatarUri).into(binding.imgAvatarChange);
                        }
                    }
                });

        // Camera Launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        newAvatarUri = cameraImageUri; // Use the URI saved before launching camera
                        Glide.with(this).load(newAvatarUri).into(binding.imgAvatarChange);
                    } else {
                        // Handle potential error or cancellation
                        if (cameraImageUri != null) {
                            // Delete the empty file created for the camera if capture failed/cancelled
                            getContentResolver().delete(cameraImageUri, null, null);
                            cameraImageUri = null;
                        }
                    }
                });

        // Storage Permission Launcher
        requestStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Cần cấp quyền truy cập bộ nhớ để chọn ảnh.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Camera Permission Launcher
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Cần cấp quyền sử dụng máy ảnh.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserProfile() {
        showLoading(true);
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        currentUserProfile = documentSnapshot.toObject(User.class);
                        if (currentUserProfile != null) {
                            displayUserInfo();
                        } else {
                            Log.e(TAG, "Failed to parse User object from Firestore.");
                            // Fallback to Auth info
                            displayAuthInfo();
                        }
                    } else {
                        Log.w(TAG, "User document not found in Firestore, using Auth info.");
                        // Fallback to Auth info
                        displayAuthInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading user profile from Firestore", e);
                    Toast.makeText(this, "Lỗi tải thông tin tài khoản.", Toast.LENGTH_SHORT).show();
                    // Fallback to Auth info
                    displayAuthInfo();
                });
    }

    // Display info primarily from Firestore profile, fallback to Auth
    private void displayUserInfo() {
        binding.etEmailChange.setText(currentUser.getEmail()); // Email is from Auth usually

        if (currentUserProfile != null) {
            binding.etNameChange.setText(currentUserProfile.getName());
            binding.etPhoneChange.setText(currentUserProfile.getPhoneNumber()); // Load phone number

            // Load avatar: prioritize Firestore URL, then Auth URL, then default
            String avatarUrlToLoad = currentUserProfile.getAvatarUrl();
            if (TextUtils.isEmpty(avatarUrlToLoad) && currentUser.getPhotoUrl() != null) {
                avatarUrlToLoad = currentUser.getPhotoUrl().toString();
            }

            Glide.with(this)
                    .load(avatarUrlToLoad) // Load Firestore or Auth URL
                    .placeholder(R.drawable.person_24px)
                    .error(R.drawable.person_24px)
                    .into(binding.imgAvatarChange);
        } else {
            // Case where Firestore data failed but Auth exists
            displayAuthInfo();
        }
    }

    // Display info ONLY from FirebaseAuth (Fallback)
    private void displayAuthInfo() {
        binding.etNameChange.setText(currentUser.getDisplayName());
        binding.etEmailChange.setText(currentUser.getEmail());
        binding.etPhoneChange.setText(""); // No phone in Auth by default
        Glide.with(this)
                .load(currentUser.getPhotoUrl())
                .placeholder(R.drawable.person_24px)
                .error(R.drawable.person_24px)
                .into(binding.imgAvatarChange);
    }


    private void setupListeners() {
        // Toolbar back navigation
        binding.toolbarChangeAccount.setNavigationOnClickListener(v -> finish());

        // Change avatar FAB
        binding.fabChangeAvatar.setOnClickListener(v -> showImageSourceDialog());

        // Save button
        binding.btnSaveChanges.setOnClickListener(v -> attemptSaveChanges());
    }

    private void showImageSourceDialog() {
        CharSequence[] options = {"Chụp ảnh mới", "Chọn từ thư viện", "Hủy"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Chụp ảnh mới
                            checkCameraPermissionAndOpenCamera();
                            break;
                        case 1: // Chọn từ thư viện
                            checkStoragePermissionAndOpenGallery();
                            break;
                        case 2: // Hủy
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void checkStoragePermissionAndOpenGallery() {
        // For Android 13+ (API 33), READ_MEDIA_IMAGES is needed.
        // For older versions, READ_EXTERNAL_STORAGE is needed.
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            // Request permission
            requestStoragePermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // intent.setType("image/*"); // Setting type might cause issues with ACTION_PICK on some devices
        galleryLauncher.launch(intent);
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        // Create a file Uri for the camera to save the image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Avatar " + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DESCRIPTION, "From HomeRent App");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(this, "Không thể tạo file ảnh.", Toast.LENGTH_SHORT).show();
        }
    }


    private void attemptSaveChanges() {
        String newName = binding.etNameChange.getText().toString().trim();
        String newPhone = binding.etPhoneChange.getText().toString().trim(); // Get phone number

        if (TextUtils.isEmpty(newName)) {
            binding.tilNameChange.setError("Vui lòng nhập tên");
            return;
        } else {
            binding.tilNameChange.setError(null);
        }
        // Optional: Add phone number validation (e.g., length, pattern)
        // if (!isValidPhoneNumber(newPhone)) { ... }

        showLoading(true);

        // Decide whether to update avatar
        if (newAvatarUri != null) {
            // Upload new avatar first, then update profile
            uploadAvatarAndUpdateProfile(newName, newPhone, newAvatarUri);
        } else {
            // Only update name and phone (if changed) in Firestore and Auth name
            updateProfileData(newName, newPhone, currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        }
    }

    private void uploadAvatarAndUpdateProfile(String name, String phone, Uri avatarUri) {
        StorageReference storageRef = storage.getReference()
                .child("user_avatars")
                .child(currentUser.getUid() + ".jpg"); // Unique path for user avatar

        UploadTask uploadTask = storageRef.putFile(avatarUri);

        uploadTask.addOnProgressListener(snapshot -> {
            // Optional: Update progress dialog
            // double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            // progressDialog.setMessage("Đang tải ảnh lên... " + (int) progress + "%");
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            // Continue with the task to get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                Log.d(TAG, "Avatar uploaded successfully: " + downloadUri);
                updateProfileData(name, phone, downloadUri.toString()); // Update with new URL
            } else {
                showLoading(false);
                Log.e(TAG, "Avatar upload failed.", task.getException());
                Toast.makeText(this, "Lỗi tải ảnh đại diện lên.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Updates Firestore document and Firebase Auth profile
    private void updateProfileData(String newName, String newPhone, @Nullable String newAvatarUrl) {
        // --- 1. Update Firestore Document ---
        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        userDocRef.update(
                "name", newName,
                "phoneNumber", newPhone, // Update phone number
                "avatarUrl", newAvatarUrl // Update avatar URL (can be null if no change or error)
        ).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Firestore profile updated successfully.");
            // --- 2. Update Firebase Auth Profile (Name and Photo URL only) ---
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName);
            if (newAvatarUrl != null) {
                profileUpdates.setPhotoUri(Uri.parse(newAvatarUrl));
            } // No update needed if newAvatarUrl is null (means keep old or no avatar)

            currentUser.updateProfile(profileUpdates.build())
                    .addOnCompleteListener(authTask -> {
                        showLoading(false);
                        if (authTask.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth profile updated.");
                            Toast.makeText(ChangeAccountActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            setResult(Activity.RESULT_OK); // Signal success to previous activity if needed
                            finish();
                        } else {
                            // Firestore updated, but Auth failed (rare, but possible)
                            Log.w(TAG, "Firestore updated, but Auth profile update failed.", authTask.getException());
                            Toast.makeText(ChangeAccountActivity.this, "Cập nhật tên/ảnh Auth thất bại.", Toast.LENGTH_SHORT).show();
                            // Still finish as main data is saved
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    });

        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Error updating Firestore profile", e);
            Toast.makeText(ChangeAccountActivity.this, "Lỗi cập nhật thông tin Firestore.", Toast.LENGTH_SHORT).show();
        });
    }


    private void showLoading(boolean isLoading) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Disable/Enable save button
        binding.btnSaveChanges.setEnabled(!isLoading);
    }

    // Handle Toolbar back button press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Deprecated - Removed ProgressDialog usage
    // private void onClickUpdateProfile() { ... }

    // Deprecated - Bitmap handling not needed with URI approach
    // public void setBitmapImage(Bitmap bitmap) { ... }
    // public void setImageUri(Uri uri) { ... }
}