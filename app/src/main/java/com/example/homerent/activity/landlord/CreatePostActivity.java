package com.example.homerent.activity.landlord;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.R;
import com.example.homerent.adapter.SelectedImageAdapter;
import com.example.homerent.model.Post;
import com.example.homerent.model.User; // Cần model User
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CreatePostActivity extends AppCompatActivity implements OnMapReadyCallback, SelectedImageAdapter.OnImageRemoveListener {

    private static final String TAG = "CreatePostActivity";
    private static final int MAX_IMAGES = 5; // Giới hạn số lượng ảnh
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Toolbar toolbar;
    private TextInputEditText etCity, etDistrict, etWard, etAddressDetail, etArea, etPrice,
            etBedrooms, etFloors, etContactName, etContactEmail, etContactPhone,
            etPostTitle, etPostDescription;
    private Button btnLocateAddress, btnAddImage, btnSavePost, btnStartDate, btnEndDate;
    private TextView tvSelectedDates;
    private RecyclerView rvSelectedImages;
    private ProgressBar progressBarCreatePost;

    private GoogleMap gMap;
    private SupportMapFragment mapFragment;
    private Geocoder geocoder;
    private Marker currentMarker;
    private LatLng currentLatLng; // Lưu tọa độ đã chọn

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private SelectedImageAdapter selectedImageAdapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private Timestamp startDateTimestamp;
    private Timestamp endDateTimestamp;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Firebase Init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đăng tin", Toast.LENGTH_SHORT).show();
            // Chuyển về màn hình đăng nhập hoặc finish
            finish();
            return;
        }

        // Bind Views
        bindViews();

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragmentContainer);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        geocoder = new Geocoder(this, Locale.getDefault());

        // Setup RecyclerView for images
        setupRecyclerView();

        // Setup Image Picker Launcher
        setupImagePicker();

        // Setup Button Listeners
        setupButtonClickListeners();

        // Pre-fill contact info if possible
        prefillContactInfo();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarCreatePost);
        etCity = findViewById(R.id.etCity);
        etDistrict = findViewById(R.id.etDistrict);
        etWard = findViewById(R.id.etWard);
        etAddressDetail = findViewById(R.id.etAddressDetail);
        etArea = findViewById(R.id.etArea);
        etPrice = findViewById(R.id.etPrice);
        etBedrooms = findViewById(R.id.etBedrooms);
        etFloors = findViewById(R.id.etFloors);
        etContactName = findViewById(R.id.etContactName);
        etContactEmail = findViewById(R.id.etContactEmail);
        etContactPhone = findViewById(R.id.etContactPhone);
        etPostTitle = findViewById(R.id.etPostTitle);
        etPostDescription = findViewById(R.id.etPostDescription);
        btnLocateAddress = findViewById(R.id.btnLocateAddress);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnSavePost = findViewById(R.id.btnSavePost);
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        progressBarCreatePost = findViewById(R.id.progressBarCreatePost);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        tvSelectedDates = findViewById(R.id.tvSelectedDates);
    }

    private void setupRecyclerView() {
        selectedImageAdapter = new SelectedImageAdapter(this, selectedImageUris, this);
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedImages.setAdapter(selectedImageAdapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            // Multiple images selected
                            int count = result.getData().getClipData().getItemCount();
                            int currentImageCount = selectedImageUris.size();
                            for (int i = 0; i < count && currentImageCount + i < MAX_IMAGES; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                selectedImageUris.add(imageUri);
                            }
                        } else if (result.getData().getData() != null) {
                            // Single image selected
                            if (selectedImageUris.size() < MAX_IMAGES) {
                                Uri imageUri = result.getData().getData();
                                selectedImageUris.add(imageUri);
                            }
                        }
                        selectedImageAdapter.notifyDataSetChanged();
                        checkImageLimit(); // Disable button if limit reached
                    }
                });
    }

    private void checkImageLimit() {
        btnAddImage.setEnabled(selectedImageUris.size() < MAX_IMAGES);
        if (!btnAddImage.isEnabled()) {
            Toast.makeText(this, "Đã đạt giới hạn " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageRemoved(int position) {
        if (position >= 0 && position < selectedImageUris.size()) {
            selectedImageUris.remove(position);
            selectedImageAdapter.notifyItemRemoved(position);
            selectedImageAdapter.notifyItemRangeChanged(position, selectedImageUris.size()); // Update indices
            checkImageLimit(); // Re-enable button if below limit
        }
    }

    private void setupButtonClickListeners() {
        btnLocateAddress.setOnClickListener(v -> geocodeAddress());
        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnSavePost.setOnClickListener(v -> attemptSavePost());
        btnStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        btnEndDate.setOnClickListener(v -> showDatePickerDialog(false));
    }

    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        // Use existing date if available, otherwise use today
        Timestamp initialTimestamp = isStartDate ? startDateTimestamp : endDateTimestamp;
        if(initialTimestamp != null) {
            calendar.setTime(initialTimestamp.toDate());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth,0,0,0); // Set time to start of day
                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                    Date selectedDate = selectedCalendar.getTime();

                    if (isStartDate) {
                        // Validate start date is not after end date
                        if (endDateTimestamp != null && selectedDate.after(endDateTimestamp.toDate())) {
                            Toast.makeText(this, "Ngày bắt đầu không thể sau ngày kết thúc", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startDateTimestamp = new Timestamp(selectedDate);
                    } else {
                        // Validate end date is not before start date
                        if (startDateTimestamp != null && selectedDate.before(startDateTimestamp.toDate())) {
                            Toast.makeText(this, "Ngày kết thúc không thể trước ngày bắt đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        endDateTimestamp = new Timestamp(selectedDate);
                    }
                    updateSelectedDatesText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        // Optional: Set min/max dates for the picker
        // if(isStartDate && endDateTimestamp != null) datePickerDialog.getDatePicker().setMaxDate(endDateTimestamp.toDate().getTime());
        // if(!isStartDate && startDateTimestamp != null) datePickerDialog.getDatePicker().setMinDate(startDateTimestamp.toDate().getTime());

        datePickerDialog.show();
    }

    private void updateSelectedDatesText() {
        String startStr = startDateTimestamp != null ? displayDateFormat.format(startDateTimestamp.toDate()) : "Chưa chọn";
        String endStr = endDateTimestamp != null ? displayDateFormat.format(endDateTimestamp.toDate()) : "Chưa chọn";
        tvSelectedDates.setText(String.format("%s - %s", startStr, endStr));
        tvSelectedDates.setVisibility(View.VISIBLE);
    }

    private void prefillContactInfo() {
        // Lấy thông tin từ profile user đã lưu trong Firestore
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (user.getName() != null) etContactName.setText(user.getName());
                            if (user.getEmail() != null) etContactEmail.setText(user.getEmail());
                            if (user.getPhoneNumber() != null) etContactPhone.setText(user.getPhoneNumber());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching user profile for prefill", e));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
        imagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        // Set default location (e.g., Hanoi)
        LatLng defaultLocation = new LatLng(21.028511, 105.804817);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        // Enable zoom controls
        gMap.getUiSettings().setZoomControlsEnabled(true);
        // Optional: Request location permission and show current location button
        // enableMyLocation();
    }

    private void geocodeAddress() {
        String city = etCity.getText().toString().trim();
        String district = etDistrict.getText().toString().trim();
        String ward = etWard.getText().toString().trim();
        String addressDetail = etAddressDetail.getText().toString().trim();

        String fullAddress = addressDetail + ", " + ward + ", " + district + ", " + city;
        fullAddress = fullAddress.replaceAll("(, )+", ", ").replaceAll("^, |, $", "").trim(); // Clean up commas

        if (TextUtils.isEmpty(fullAddress) || fullAddress.equals(",")) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Geocoding address: " + fullAddress);

        try {
            List<Address> addresses = geocoder.getFromLocationName(fullAddress, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Location found: " + currentLatLng.latitude + ", " + currentLatLng.longitude);

                // Update map
                if (gMap != null) {
                    if (currentMarker != null) {
                        currentMarker.remove();
                    }
                    currentMarker = gMap.addMarker(new MarkerOptions().position(currentLatLng).title(fullAddress));
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)); // Zoom closer
                }
                Toast.makeText(this, "Đã tìm thấy địa chỉ!", Toast.LENGTH_SHORT).show();

            } else {
                Log.d(TAG, "No location found for address: " + fullAddress);
                Toast.makeText(this, "Không tìm thấy địa chỉ trên bản đồ", Toast.LENGTH_SHORT).show();
                currentLatLng = null; // Reset if not found
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding failed", e);
            Toast.makeText(this, "Lỗi định vị địa chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            currentLatLng = null;
        }
    }

    // --- Saving Logic ---

    private void attemptSavePost() {
        // --- Simple Validation ---
        if (!validateInput()) {
            return;
        }

        // --- Start Saving Process ---
        progressBarCreatePost.setVisibility(View.VISIBLE);
        btnSavePost.setEnabled(false);

        if (!selectedImageUris.isEmpty()) {
            uploadImagesAndSavePost();
        } else {
            // Save post without images
            saveDataToFirestore(new ArrayList<>()); // Pass empty list
        }
    }

    private boolean validateInput() {
        // Add more specific validation as needed (e.g., check ranges)
        if (TextUtils.isEmpty(etCity.getText())) { showValidationError(etCity, "Vui lòng nhập Tỉnh/Thành phố"); return false; }
        if (TextUtils.isEmpty(etDistrict.getText())) { showValidationError(etDistrict, "Vui lòng nhập Quận/Huyện"); return false; }
        if (TextUtils.isEmpty(etWard.getText())) { showValidationError(etWard, "Vui lòng nhập Phường/Xã"); return false; }
        if (TextUtils.isEmpty(etAddressDetail.getText())) { showValidationError(etAddressDetail, "Vui lòng nhập địa chỉ chi tiết"); return false; }
        if (TextUtils.isEmpty(etArea.getText())) { showValidationError(etArea, "Vui lòng nhập diện tích"); return false; }
        if (TextUtils.isEmpty(etPrice.getText())) { showValidationError(etPrice, "Vui lòng nhập mức giá"); return false; }
        if (TextUtils.isEmpty(etBedrooms.getText())) { showValidationError(etBedrooms, "Vui lòng nhập số phòng ngủ"); return false; }
        if (TextUtils.isEmpty(etFloors.getText())) { showValidationError(etFloors, "Vui lòng nhập số tầng"); return false; }
        if (TextUtils.isEmpty(etPostTitle.getText())) { showValidationError(etPostTitle, "Vui lòng nhập tiêu đề"); return false; }
        if (TextUtils.isEmpty(etPostDescription.getText())) { showValidationError(etPostDescription, "Vui lòng nhập mô tả"); return false; }
        if (startDateTimestamp == null) { Toast.makeText(this, "Vui lòng chọn ngày bắt đầu", Toast.LENGTH_SHORT).show(); return false;}
        if (endDateTimestamp == null) { Toast.makeText(this, "Vui lòng chọn ngày kết thúc", Toast.LENGTH_SHORT).show(); return false;}
        // Optional validation for contact info
        // ...

        // Validate numbers
        try {
            Double.parseDouble(etArea.getText().toString());
            Long.parseLong(etPrice.getText().toString());
            Integer.parseInt(etBedrooms.getText().toString());
            Integer.parseInt(etFloors.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Diện tích, giá, số phòng, số tầng phải là số hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }


        return true;
    }

    private void showValidationError(TextInputEditText editText, String errorMsg) {
        editText.setError(errorMsg);
        editText.requestFocus();
    }


    private void uploadImagesAndSavePost() {
        List<String> downloadUrls = new ArrayList<>();
        AtomicInteger uploadCounter = new AtomicInteger(0);
        int totalImages = selectedImageUris.size();

        StorageReference storageRef = storage.getReference().child("post_images");

        for (Uri imageUri : selectedImageUris) {
            // Create unique filename
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageRef.child(filename);

            UploadTask uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return fileRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    downloadUrls.add(downloadUri.toString());
                    Log.d(TAG, "Image uploaded: " + downloadUri.toString());
                } else {
                    // Handle failures
                    Log.w(TAG, "Image upload failed: " + imageUri.toString(), task.getException());
                    // Optionally show a message for the failed image
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải lên ảnh: " + imageUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                }

                // Check if all uploads are complete (success or failure)
                if (uploadCounter.incrementAndGet() == totalImages) {
                    // All uploads finished, proceed to save data even if some failed
                    Log.d(TAG, "All image uploads finished. URLs obtained: " + downloadUrls.size());
                    if (downloadUrls.size() < totalImages) {
                        // Inform user that some images failed but we proceed
                        Toast.makeText(this, "Một số ảnh tải lên thất bại, bài đăng vẫn sẽ được lưu.", Toast.LENGTH_LONG).show();
                    }
                    saveDataToFirestore(downloadUrls);
                }
            });
        }
    }


    private void saveDataToFirestore(List<String> imageUrls) {
        if (currentUser == null) {
            showErrorAndReset("Lỗi: Người dùng không tồn tại.");
            return;
        }

        Post newPost = new Post();
        newPost.setUserId(currentUser.getUid());
        newPost.setTitle(etPostTitle.getText().toString().trim());
        newPost.setDescription(etPostDescription.getText().toString().trim());
        newPost.setCity(etCity.getText().toString().trim());
        newPost.setDistrict(etDistrict.getText().toString().trim());
        newPost.setWard(etWard.getText().toString().trim());
        newPost.setAddress(etAddressDetail.getText().toString().trim());

        // Parse numbers safely after validation
        try {
            newPost.setArea(Double.parseDouble(etArea.getText().toString()));
            newPost.setPrice(Long.parseLong(etPrice.getText().toString()));
            newPost.setBedrooms(Integer.parseInt(etBedrooms.getText().toString()));
            newPost.setFloors(Integer.parseInt(etFloors.getText().toString()));
        } catch (NumberFormatException e) {
            showErrorAndReset("Lỗi định dạng số.");
            Log.e(TAG, "Number parsing error during save", e);
            return;
        }

        newPost.setImageUrls(imageUrls);
        newPost.setTimestamp(Timestamp.now());
        newPost.setAvailable(true); // Default to available
        newPost.setViewCount(0); // Default view count
        newPost.setStartDate(startDateTimestamp); // Gán Timestamp đã chọn
        newPost.setEndDate(endDateTimestamp); // Gán Timestamp đã chọn

        if (currentLatLng != null) {
            newPost.setLatitude(currentLatLng.latitude);
            newPost.setLongitude(currentLatLng.longitude);
        } else {
            newPost.setLatitude(0); // Hoặc giá trị mặc định khác
            newPost.setLongitude(0);
        }

        // --- Save to Firestore ---
        db.collection("posts")
                .add(newPost) // Use add() to auto-generate ID
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Post created successfully with ID: " + documentReference.getId());
                    progressBarCreatePost.setVisibility(View.GONE);
                    Toast.makeText(CreatePostActivity.this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous activity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding post document", e);
                    showErrorAndReset("Lỗi lưu tin đăng: " + e.getMessage());
                });
    }

    private void showErrorAndReset(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        progressBarCreatePost.setVisibility(View.GONE);
        btnSavePost.setEnabled(true);
    }


    // Handle Toolbar back button
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Optional: Request location permission if needed for advanced map features
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, maybe enable 'my location' layer
                // if (gMap != null) {
                //     if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //         gMap.setMyLocationEnabled(true);
                //     }
                // }
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}