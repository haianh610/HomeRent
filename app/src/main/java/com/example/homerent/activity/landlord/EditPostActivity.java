package com.example.homerent.activity.landlord;

import android.Manifest;
import android.app.Activity;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.ImageTouchHelperCallback;
import com.example.homerent.R;
import com.example.homerent.adapter.SelectedImageAdapter;
import com.example.homerent.model.AddressData;
import com.example.homerent.model.Commune;
import com.example.homerent.model.District;
import com.example.homerent.model.Post;
import com.example.homerent.model.Province;
import com.example.homerent.model.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EditPostActivity extends AppCompatActivity implements OnMapReadyCallback, SelectedImageAdapter.OnImageRemoveListener {

    private static final String TAG = "EditPostActivity";
    private static final int MAX_IMAGES = 5; // Giới hạn số lượng ảnh
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // --- UI Elements (Use same IDs as in the XML) ---
    private Toolbar toolbar;
    private AutoCompleteTextView actProvince, actDistrict, actCommune;
    private TextInputLayout tilProvince, tilDistrict, tilCommune;
    private TextInputEditText etAddressDetail, etArea, etPrice,
            etBedrooms, etFloors, etContactName, etContactEmail, etContactPhone,
            etPostTitle, etPostDescription;
    private RadioGroup rgPostDuration; // Keep this
    private Button btnLocateAddress, btnAddImage, btnUpdatePost; // Renamed button
    private RecyclerView rvSelectedImages;
    private ProgressBar progressBarEditPost; // Renamed progress bar

    private GoogleMap gMap;
    private SupportMapFragment mapFragment;
    private Geocoder geocoder;
    private Marker currentMarker;
    private LatLng currentLatLng; // Lưu tọa độ đã chọn

    // --- Firebase & Data ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private String postId; // To store the ID of the post being edited
    private Post existingPost; // To store the loaded post data

    private ArrayList<Object> displayedImageList = new ArrayList<>(); // Holds both String (URL) and Uri (new)
    private ArrayList<String> initialImageUrls = new ArrayList<>(); // Store original URLs to check for removals
    private SelectedImageAdapter selectedImageAdapter; // Use the same adapter type
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // --- Address Data ---
    private ArrayAdapter<Province> provinceArrayAdapter;
    private ArrayAdapter<District> districtArrayAdapter;
    private ArrayAdapter<Commune> communeArrayAdapter;
    private List<Province> allProvinces = new ArrayList<>();
    private List<District> allDistricts = new ArrayList<>();
    private List<Commune> allCommunes = new ArrayList<>();
    private List<District> filteredDistricts = new ArrayList<>();
    private List<Commune> filteredCommunes = new ArrayList<>();
    private Province selectedProvince;
    private District selectedDistrict;
    private Commune selectedCommune;

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_edit_post), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        // Get postId from Intent
        postId = getIntent().getStringExtra("POST_ID");
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "ID tin đăng không hợp lệ!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid Post ID received.");
            finish();
            return;
        }

        // Firebase Init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Should ideally not happen if coming from a logged-in state, but check anyway
            Toast.makeText(this, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
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

        // Load address data FIRST
        setInputsEnabled(false);
        loadAddressData(); // This will call setupAutoCompleteTextViews upon completion

        // Setup Button Listeners
        setupButtonClickListeners();


    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarEditPost); // Changed ID
        actProvince = findViewById(R.id.actProvince);
        actDistrict = findViewById(R.id.actDistrict);
        actCommune = findViewById(R.id.actCommune);
        tilProvince = findViewById(R.id.tilProvince);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilCommune = findViewById(R.id.tilCommune);
        etAddressDetail = findViewById(R.id.etAddressDetail);
        etArea = findViewById(R.id.etArea);
        etPrice = findViewById(R.id.etPrice);
        etBedrooms = findViewById(R.id.etBedrooms);
        etFloors = findViewById(R.id.etFloors);
        rgPostDuration = findViewById(R.id.rgPostDuration);
        etContactName = findViewById(R.id.etContactName);
        etContactEmail = findViewById(R.id.etContactEmail);
        etContactPhone = findViewById(R.id.etContactPhone);
        etPostTitle = findViewById(R.id.etPostTitle);
        etPostDescription = findViewById(R.id.etPostDescription);
        btnLocateAddress = findViewById(R.id.btnLocateAddress);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnUpdatePost = findViewById(R.id.btnUpdatePost); // Changed ID
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        progressBarEditPost = findViewById(R.id.progressBarEditPost); // Changed ID
    }

    private void setupRecyclerView() {
        // Initialize adapter with the list that holds both Strings and Uris
        selectedImageAdapter = new SelectedImageAdapter(this, displayedImageList, this);
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)); // Vertical layout
        rvSelectedImages.setAdapter(selectedImageAdapter);

        // ItemTouchHelper setup remains the same, passing the adapter and the displayed list
        ItemTouchHelper.Callback callback = new ImageTouchHelperCallback(selectedImageAdapter, displayedImageList);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvSelectedImages);
    }

    private void setupButtonClickListeners() {
        btnLocateAddress.setOnClickListener(v -> geocodeAddress());
        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnUpdatePost.setOnClickListener(v -> attemptUpdatePost()); // Call update method
        // Removed listeners for date buttons
    }

    // loadAddressData - Load existing post data *after* address data is ready
    private void loadAddressData() {
        progressBarEditPost.setVisibility(View.VISIBLE); // Hiển thị loading cho cả việc load address data
        setInputsEnabled(false); // Vô hiệu hóa input
        try {
            InputStream is = getAssets().open("donvihanhchinh.json"); // Tên file JSON
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Gson gson = new Gson();

            // *** KHAI BÁO VÀ GÁN GIÁ TRỊ CHO BIẾN 'data' ***
            AddressData data = gson.fromJson(reader, AddressData.class);
            // *********************************************

            reader.close(); // Đóng reader sau khi parse xong

            if (data != null) {
                // Gán dữ liệu vào các list toàn cục
                allProvinces = data.getProvinces() != null ? data.getProvinces() : new ArrayList<>();
                allDistricts = data.getDistricts() != null ? data.getDistricts() : new ArrayList<>();
                allCommunes = data.getCommunes() != null ? data.getCommunes() : new ArrayList<>();

                Log.d(TAG, "Loaded " + allProvinces.size() + " provinces, " + allDistricts.size() + " districts, " + allCommunes.size() + " communes.");

                // Sắp xếp Tỉnh/Thành phố
                allProvinces.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));

                // Setup các AutoCompleteTextView
                setupAutoCompleteTextViews();

                // Load dữ liệu bài đăng hiện có (sau khi dropdowns đã sẵn sàng)
                loadExistingPostData(postId);

            } else {
                Log.e(TAG, "Failed to parse AddressData from JSON. Data object is null.");
                Toast.makeText(this, "Lỗi đọc dữ liệu địa chỉ (null).", Toast.LENGTH_SHORT).show();
                finish(); // Không thể tiếp tục nếu lỗi nghiêm trọng
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading address JSON file", e);
            Toast.makeText(this, "Lỗi đọc file dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
            progressBarEditPost.setVisibility(View.GONE); // Ẩn PB nếu lỗi address data
            setInputsEnabled(true); // Bật lại input
            finish(); // Không thể tiếp tục nếu lỗi nghiêm trọng
        } catch (com.google.gson.JsonSyntaxException e) {
            Log.e(TAG, "Error parsing address JSON file", e);
            Toast.makeText(this, "Lỗi định dạng file dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
            progressBarEditPost.setVisibility(View.GONE); // Ẩn PB nếu lỗi address data
            setInputsEnabled(true); // Bật lại input
            finish(); // Không thể tiếp tục nếu lỗi nghiêm trọng
        }
    }

    // setupAutoCompleteTextViews - Remains mostly the same as CreatePostActivity
    private void setupAutoCompleteTextViews() {
        // --- Province ---
        provinceArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allProvinces);
        actProvince.setAdapter(provinceArrayAdapter);
        actProvince.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvince = (Province) parent.getItemAtPosition(position);
            actDistrict.setText("", false); selectedDistrict = null;
            updateDistrictDropdown(selectedProvince.getIdProvince());
            actCommune.setText("", false); selectedCommune = null;
            updateCommuneDropdown(null); tilCommune.setEnabled(false);
            updateMapLocation();
        });

        // --- District ---
        districtArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actDistrict.setAdapter(districtArrayAdapter);
        tilDistrict.setEnabled(false); // Initially disabled
        actDistrict.setOnItemClickListener((parent, view, position, id) -> {
            selectedDistrict = (District) parent.getItemAtPosition(position);
            actCommune.setText("", false); selectedCommune = null;
            updateCommuneDropdown(selectedDistrict.getIdDistrict());
            updateMapLocation();
        });

        // --- Commune ---
        communeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actCommune.setAdapter(communeArrayAdapter);
        tilCommune.setEnabled(false); // Initially disabled
        actCommune.setOnItemClickListener((parent, view, position, id) -> {
            selectedCommune = (Commune) parent.getItemAtPosition(position);
            updateMapLocation();
        });
    }

    private void updateMapLocation() {
        // Tự động geocode khi chọn xong Phường/Xã hoặc có đủ thông tin
        if (selectedProvince != null && selectedDistrict != null && selectedCommune != null && !TextUtils.isEmpty(etAddressDetail.getText().toString().trim())) {
            geocodeAddress();
        } else if (selectedProvince != null && selectedDistrict != null && !TextUtils.isEmpty(etAddressDetail.getText().toString().trim())){
            // Hoặc geocode khi chỉ có Tỉnh/Huyện/Chi tiết để lấy vị trí tương đối
            geocodeAddress();
        }
        // Có thể thêm logic chỉ zoom về Tỉnh/Huyện nếu chỉ chọn đến đó
    }

    private void updateDistrictDropdown(String provinceId) {
        filteredDistricts.clear();
        boolean hasData = false;
        if (provinceId != null) {
            filteredDistricts = allDistricts.stream()
                    .filter(d -> provinceId.equals(d.getIdProvince()))
                    .sorted((d1, d2) -> d1.getName().compareTo(d2.getName()))
                    .collect(Collectors.toList());
            hasData = !filteredDistricts.isEmpty();
            Log.d(TAG, "Found " + filteredDistricts.size() + " districts for province ID: " + provinceId);
        }

        // Cập nhật Adapter
        districtArrayAdapter.clear();
        if (hasData) {
            districtArrayAdapter.addAll(filteredDistricts);
        }
        districtArrayAdapter.notifyDataSetChanged();

        // Bật/tắt TextInputLayout
        tilDistrict.setEnabled(hasData);
        // Đảm bảo text được xóa nếu không có data
        if (!hasData) {
            actDistrict.setText("", false);
            selectedDistrict = null;
        }
    }

    // Đổi tên hàm updateCommuneSpinner thành updateCommuneDropdown
    private void updateCommuneDropdown(String districtId) {
        filteredCommunes.clear();
        boolean hasData = false;
        if (districtId != null) {
            filteredCommunes = allCommunes.stream()
                    .filter(c -> districtId.equals(c.getIdDistrict()))
                    .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                    .collect(Collectors.toList());
            hasData = !filteredCommunes.isEmpty();
            Log.d(TAG, "Found " + filteredCommunes.size() + " communes for district ID: " + districtId);
        }

        // Cập nhật Adapter
        communeArrayAdapter.clear();
        if(hasData){
            communeArrayAdapter.addAll(filteredCommunes);
        }
        communeArrayAdapter.notifyDataSetChanged();

        // Bật/tắt TextInputLayout
        tilCommune.setEnabled(hasData);
        // Đảm bảo text được xóa nếu không có data
        if (!hasData) {
            actCommune.setText("", false);
            selectedCommune = null;
        }
    }

    // loadExistingPostData: Quản lý ProgressBar và setInputsEnabled
    private void loadExistingPostData(String postIdToLoad) {
        Log.d(TAG, "Loading existing data for post: " + postIdToLoad);
        // ProgressBar đã được bật từ loadAddressData
        // setInputsEnabled(false) cũng đã được gọi

        DocumentReference postRef = db.collection("posts").document(postIdToLoad);
        postRef.get().addOnCompleteListener(task -> {
            if (!isDestroyed()) {
                // --- ẨN PROGRESS BAR VÀ BẬT INPUT KHI LOAD XONG (CẢ SUCCESS VÀ FAILURE) ---
                progressBarEditPost.setVisibility(View.GONE);
                // Chỉ bật lại input nếu không gặp lỗi nghiêm trọng khiến activity finish
                boolean shouldFinishOnError = false;
                // -----------------------------------------------------------------------

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        existingPost = document.toObject(Post.class);
                        if (existingPost != null) {
                            existingPost.setPostId(document.getId());

                            // *** Ownership Check ***
                            if (!currentUser.getUid().equals(existingPost.getUserId())) {
                                Toast.makeText(this, "Bạn không có quyền chỉnh sửa tin này.", Toast.LENGTH_LONG).show();
                                shouldFinishOnError = true; // Đánh dấu để finish
                            } else {
                                // Populate UI with loaded data
                                populateUiWithPostData(existingPost);
                                // Bật input sau khi populate thành công
                                setInputsEnabled(true);
                            }
                        } else {
                            handleLoadError("Không thể đọc dữ liệu tin đăng.");
                            shouldFinishOnError = true;
                        }
                    } else {
                        Log.d(TAG, "No such document for postId: " + postIdToLoad);
                        handleLoadError("Tin đăng không tồn tại.");
                        shouldFinishOnError = true;
                    }
                } else {
                    Log.w(TAG, "Error getting existing post data.", task.getException());
                    handleLoadError("Lỗi tải dữ liệu: " + Objects.requireNonNull(task.getException()).getMessage());
                    shouldFinishOnError = true;
                }

                // Kết thúc activity nếu có lỗi nghiêm trọng
                if (shouldFinishOnError) {
                    finish();
                } else if(!task.isSuccessful()){
                    // Nếu chỉ là lỗi tải nhưng không finish, vẫn bật lại input
                    setInputsEnabled(true);
                }
            }
        });
    }

    private void setInputsEnabled(boolean enabled) {
        tilProvince.setEnabled(enabled);
        tilDistrict.setEnabled(enabled && selectedProvince != null); // Only enable if province is selected
        tilCommune.setEnabled(enabled && selectedDistrict != null); // Only enable if district is selected
        etAddressDetail.setEnabled(enabled);
        etArea.setEnabled(enabled);
        etPrice.setEnabled(enabled);
        etBedrooms.setEnabled(enabled);
        etFloors.setEnabled(enabled);
        rgPostDuration.setEnabled(enabled); // Need to enable/disable children too
        for (int i = 0; i < rgPostDuration.getChildCount(); i++) {
            rgPostDuration.getChildAt(i).setEnabled(enabled);
        }
        etContactName.setEnabled(enabled);
        etContactEmail.setEnabled(enabled);
        etContactPhone.setEnabled(enabled);
        etPostTitle.setEnabled(enabled);
        etPostDescription.setEnabled(enabled);
        btnLocateAddress.setEnabled(enabled);
        btnAddImage.setEnabled(enabled && displayedImageList.size() < MAX_IMAGES);
        btnUpdatePost.setEnabled(enabled);
        // Map interaction is usually handled by the fragment itself
    }

    private void handleLoadError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Finish activity on critical load errors
    }
    // --- Populate UI ---
    private void populateUiWithPostData(Post post) {
        etPostTitle.setText(post.getTitle());
        etPostDescription.setText(post.getDescription());
        etAddressDetail.setText(post.getAddress()); // Detail address
        etArea.setText(String.valueOf(post.getArea()));
        etPrice.setText(String.valueOf(post.getPrice()));
        etBedrooms.setText(String.valueOf(post.getBedrooms()));
        etFloors.setText(String.valueOf(post.getFloors()));

        // Pre-select Address Dropdowns (Requires finding the object by name)
        // Province
        Province provinceToSelect = null;
        for (Province p : allProvinces) {
            if (p.getName().equals(post.getCity())) {
                provinceToSelect = p;
                break;
            }
        }
        if (provinceToSelect != null) {
            actProvince.setText(provinceToSelect.getName(), false); // Set text, don't filter
            selectedProvince = provinceToSelect; // Set the selected object
            updateDistrictDropdown(provinceToSelect.getIdProvince()); // Trigger update for district

            // District (after province is set and districts are loaded)
            District districtToSelect = null;
            for (District d : filteredDistricts) { // Use the filtered list
                if (d.getName().equals(post.getDistrict())) {
                    districtToSelect = d;
                    break;
                }
            }
            if (districtToSelect != null) {
                actDistrict.setText(districtToSelect.getName(), false);
                selectedDistrict = districtToSelect;
                updateCommuneDropdown(districtToSelect.getIdDistrict()); // Trigger update for commune

                // Commune (after district is set and communes are loaded)
                Commune communeToSelect = null;
                for (Commune c : filteredCommunes) { // Use the filtered list
                    if (c.getName().equals(post.getWard())) {
                        communeToSelect = c;
                        break;
                    }
                }
                if (communeToSelect != null) {
                    actCommune.setText(communeToSelect.getName(), false);
                    selectedCommune = communeToSelect;
                } else {
                    Log.w(TAG, "Could not find matching commune: " + post.getWard());
                    tilCommune.setEnabled(true); // Keep enabled if district exists but commune doesn't match (maybe data changed)
                }
            } else {
                Log.w(TAG, "Could not find matching district: " + post.getDistrict());
                tilDistrict.setEnabled(true); // Keep enabled if province exists but district doesn't match
            }
        } else {
            Log.w(TAG, "Could not find matching province: " + post.getCity());
        }


        // Pre-select Duration RadioButton
        long durationMillis = post.getEndDate().toDate().getTime() - post.getStartDate().toDate().getTime();
        long durationDays = TimeUnit.MILLISECONDS.toDays(durationMillis);
        if (durationDays <= 10) { // Treat anything <= 10 as 10 for simplicity or use exact match
            rgPostDuration.check(R.id.rbDuration10);
        } else if (durationDays <= 20) {
            rgPostDuration.check(R.id.rbDuration20);
        } else {
            rgPostDuration.check(R.id.rbDuration30);
        }


        // Pre-fill Contact Info (can be overwritten by user profile loading later if desired)
        // Assuming Post model doesn't store separate contact info, prefill from user profile
        prefillContactInfo(); // Call this again or rely on initial call

        // Load existing Images
        if (post.getImageUrls() != null) {
            initialImageUrls.addAll(post.getImageUrls()); // Store original URLs
            displayedImageList.addAll(post.getImageUrls()); // Add URLs to the list displayed by adapter
            selectedImageAdapter.notifyDataSetChanged();
            checkImageLimit();
        }

        // Set Map Location
        if (post.getLatitude() != 0 || post.getLongitude() != 0) {
            currentLatLng = new LatLng(post.getLatitude(), post.getLongitude());
            // Wait for map to be ready before adding marker
            if (gMap != null) {
                updateMapMarker(currentLatLng, post.getAddress() + ", " + post.getWard() + ", ..."); // Create full address string
            } else {
                mapFragment.getMapAsync(googleMap -> { // Get map again if not ready initially
                    gMap = googleMap;
                    updateMapMarker(currentLatLng, post.getAddress() + ", " + post.getWard() + ", ...");
                });
            }
        }
    }
    // Helper to update map marker
    private void updateMapMarker(LatLng latLng, String title) {
        if(gMap == null || latLng == null) return;
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = gMap.addMarker(new MarkerOptions().position(latLng).title(title));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    // onMapReady - Update marker if lat/lng is already loaded
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setZoomControlsEnabled(true);
        // If location data was loaded *before* map was ready, set marker now
        if (currentLatLng != null && existingPost != null) {
            String fullAddress = existingPost.getAddress() + ", " + existingPost.getWard() + ", " + existingPost.getDistrict() + ", " + existingPost.getCity();
            updateMapMarker(currentLatLng, fullAddress.replaceAll("^, ", "").trim());
        } else {
            // Default location if no existing data or lat/lng is 0,0
            LatLng defaultLocation = new LatLng(21.028511, 105.804817);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        }
        // enableMyLocation(); // Optional
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        int currentImageCount = displayedImageList.size(); // Check combined list size
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count && currentImageCount + i < MAX_IMAGES; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                if (!isUriAlreadyAdded(imageUri)) { // Prevent adding duplicates
                                    displayedImageList.add(imageUri); // Add Uri directly
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            if (currentImageCount < MAX_IMAGES) {
                                Uri imageUri = result.getData().getData();
                                if (!isUriAlreadyAdded(imageUri)) {
                                    displayedImageList.add(imageUri);
                                }
                            }
                        }
                        selectedImageAdapter.notifyDataSetChanged();
                        checkImageLimit();
                    }
                });
    }

    private boolean isUriAlreadyAdded(Uri uri) {
        for (Object item : displayedImageList) {
            if (item instanceof Uri && item.equals(uri)) {
                return true;
            }
        }
        return false;
    }


    private void checkImageLimit() {
        btnAddImage.setEnabled(displayedImageList.size() < MAX_IMAGES);
        // ... (Toast message remains the same)
    }

    // onImageRemoved - Use displayedImageList
    @Override
    public void onImageRemoved(int position) {
        if (position >= 0 && position < displayedImageList.size()) {
            displayedImageList.remove(position);
            selectedImageAdapter.notifyItemRemoved(position);
            selectedImageAdapter.notifyItemRangeChanged(position, displayedImageList.size());
            checkImageLimit();
        }
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



    private void geocodeAddress() {
        String city = actProvince.getText().toString().trim(); // Lấy text trực tiếp
        String district = actDistrict.getText().toString().trim();
        String ward = actCommune.getText().toString().trim();
        String addressDetail = etAddressDetail.getText().toString().trim();

        // Tạo địa chỉ đầy đủ hơn để geocode (thứ tự có thể ảnh hưởng kết quả)
        String fullAddress = addressDetail;
        if (!TextUtils.isEmpty(ward)) fullAddress = (!TextUtils.isEmpty(fullAddress)? fullAddress + ", " : "") + ward;
        if (!TextUtils.isEmpty(district)) fullAddress = (!TextUtils.isEmpty(fullAddress)? fullAddress + ", " : "") + district;
        if (!TextUtils.isEmpty(city)) fullAddress = (!TextUtils.isEmpty(fullAddress)? fullAddress + ", " : "") + city;
        fullAddress += ", Việt Nam";

        fullAddress = fullAddress.replaceAll("^, ", "").trim(); // Bỏ dấu phẩy đầu

        // Chỉ geocode nếu có ít nhất Tỉnh/TP
        if (TextUtils.isEmpty(city)) {
            // Toast.makeText(this, "Vui lòng chọn ít nhất Tỉnh/Thành phố", Toast.LENGTH_SHORT).show();
            return; // Không geocode nếu chưa chọn tỉnh
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

    private void attemptUpdatePost() {
        if (!validateInput()) {
            return;
        }

        progressBarEditPost.setVisibility(View.VISIBLE); // *** HIỂN THỊ LOADING KHI LƯU ***
        btnUpdatePost.setEnabled(false); // Vô hiệu hóa nút lưu
        setInputsEnabled(false); // Vô hiệu hóa các input khác

        uploadNewImagesAndUpdatePost(); // Bắt đầu quá trình upload/lưu
    }

    private void uploadNewImagesAndUpdatePost() {
        final List<String> finalImageUrls = new ArrayList<>();
        final List<Uri> newImageUris = new ArrayList<>();
        final List<String> removedImageUrls = new ArrayList<>();

        // 1. Identify new Uris and existing Urls to keep
        for (Object item : displayedImageList) {
            if (item instanceof Uri) {
                newImageUris.add((Uri) item);
            } else if (item instanceof String) {
                finalImageUrls.add((String) item); // Add existing URLs that are still in the list
            }
        }

        // 2. Identify removed URLs
        for (String initialUrl : initialImageUrls) {
            if (!finalImageUrls.contains(initialUrl)) { // If an initial URL is no longer in the displayed list (as a String)
                removedImageUrls.add(initialUrl);
            }
        }

        Log.d(TAG, "New images to upload: " + newImageUris.size());
        Log.d(TAG, "Existing images to keep: " + finalImageUrls.size());
        Log.d(TAG, "Images to remove from storage: " + removedImageUrls.size());


        // 3. Delete removed images from Storage (Run concurrently with uploads)
        deleteRemovedImagesFromStorage(removedImageUrls);


        // 4. Upload new images (if any)
        if (newImageUris.isEmpty()) {
            // No new images, just update Firestore with kept URLs
            saveDataToFirestore(finalImageUrls);
        } else {
            // Upload new images and then save
            uploadNewImages(newImageUris, finalImageUrls); // Pass kept URLs to combine later
        }
    }
    private void uploadNewImages(List<Uri> urisToUpload, List<String> keptUrls) {
        AtomicInteger uploadCounter = new AtomicInteger(0);
        int totalImagesToUpload = urisToUpload.size();
        List<String> newlyUploadedUrls = new ArrayList<>();

        StorageReference storageRef = storage.getReference().child("post_images");

        for (Uri imageUri : urisToUpload) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageRef.child(filename);
            UploadTask uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) { throw task.getException(); }
                return fileRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    newlyUploadedUrls.add(downloadUri.toString());
                    Log.d(TAG, "New image uploaded: " + downloadUri.toString());
                } else {
                    Log.w(TAG, "New image upload failed: " + imageUri.toString(), task.getException());
                    Toast.makeText(EditPostActivity.this, "Lỗi tải lên ảnh mới: " + imageUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                }

                if (uploadCounter.incrementAndGet() == totalImagesToUpload) {
                    Log.d(TAG, "All new image uploads finished. Newly uploaded URLs: " + newlyUploadedUrls.size());
                    // Combine kept URLs and newly uploaded URLs
                    List<String> finalUrls = new ArrayList<>(keptUrls);
                    finalUrls.addAll(newlyUploadedUrls);
                    saveDataToFirestore(finalUrls); // Save with the combined list
                }
            });
        }
    }


    private void deleteRemovedImagesFromStorage(List<String> urlsToRemove) {
        if (urlsToRemove.isEmpty()) {
            Log.d(TAG, "No images to remove from storage.");
            return;
        }

        for (String imageUrl : urlsToRemove) {
            try {
                StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                photoRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully deleted removed image: " + imageUrl))
                        .addOnFailureListener(exception -> Log.w(TAG, "Error deleting removed image: " + imageUrl, exception));
            } catch (IllegalArgumentException e) {
                Log.e(TAG,"Invalid URL format for deletion: " + imageUrl, e);
            } catch (Exception e) {
                Log.e(TAG,"Error getting reference for deletion: " + imageUrl, e);
            }
        }
    }



    // validateInput: Kiểm tra text của AutoCompleteTextView
    private boolean validateInput() {
        if (TextUtils.isEmpty(actProvince.getText())) {
            tilProvince.setError("Vui lòng chọn Tỉnh/Thành phố");
            actProvince.requestFocus();
            return false;
        } else {
            tilProvince.setError(null); // Clear error
        }

        if (TextUtils.isEmpty(actDistrict.getText())) {
            tilDistrict.setError("Vui lòng chọn Quận/Huyện");
            actDistrict.requestFocus();
            return false;
        } else {
            tilDistrict.setError(null);
        }

        if (TextUtils.isEmpty(actCommune.getText())) {
            tilCommune.setError("Vui lòng chọn Phường/Xã");
            actCommune.requestFocus();
            return false;
        } else {
            tilCommune.setError(null);
        }

        // Bỏ kiểm tra bắt buộc cho etAddressDetail
        // if (TextUtils.isEmpty(etAddressDetail.getText())) { showValidationError(etAddressDetail, "Vui lòng nhập địa chỉ chi tiết"); return false; }

        // ... (Các kiểm tra khác giữ nguyên: area, price, bedrooms, floors, title, description, dates, parse số)
        if (TextUtils.isEmpty(etArea.getText())) { showValidationError(etArea, "Vui lòng nhập diện tích"); return false; } else {etArea.setError(null);}
        // ... thêm clear error cho các trường khác

        // *** Kiểm tra RadioGroup Thời hạn ***
        if (rgPostDuration.getCheckedRadioButtonId() == -1) { // -1 nghĩa là chưa có cái nào được chọn
            Toast.makeText(this, "Vui lòng chọn thời hạn đăng tin", Toast.LENGTH_SHORT).show();
            rgPostDuration.requestFocus(); // Focus vào RadioGroup
            return false;
        }
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





    private void saveDataToFirestore(List<String> finalImageUrls) {
        if (currentUser == null || postId == null ) {
            showErrorAndReset("Lỗi: Không thể cập nhật tin đăng.");
            return;
        }

        // Create a new Post object or update the existing one
        // It's often easier to create a new object with all current values
        Post updatedPost = new Post();
        updatedPost.setUserId(currentUser.getUid()); // Keep original owner
        updatedPost.setPostId(postId); // Keep original ID

        updatedPost.setTitle(etPostTitle.getText().toString().trim());
        updatedPost.setDescription(etPostDescription.getText().toString().trim());
        updatedPost.setCity(actProvince.getText().toString().trim());
        updatedPost.setDistrict(actDistrict.getText().toString().trim());
        updatedPost.setWard(actCommune.getText().toString().trim());
        updatedPost.setAddress(etAddressDetail.getText().toString().trim());

        // --- Recalculate Start/End Dates based on selected duration ---
        Timestamp startDate = Timestamp.now(); // Or keep existingPost.getStartDate() if you don't want it to reset? Let's reset for simplicity now.
        int selectedDurationId = rgPostDuration.getCheckedRadioButtonId();
        int durationDays = 0;
        if (selectedDurationId == R.id.rbDuration10) durationDays = 10;
        else if (selectedDurationId == R.id.rbDuration20) durationDays = 20;
        else if (selectedDurationId == R.id.rbDuration30) durationDays = 30;
        else { showErrorAndReset("Lỗi thời hạn đăng."); return; }
        long startTimeMillis = startDate.toDate().getTime();
        long durationMillis = TimeUnit.DAYS.toMillis(durationDays);
        Timestamp endDate = new Timestamp(new Date(startTimeMillis + durationMillis));
        updatedPost.setStartDate(startDate);
        updatedPost.setEndDate(endDate);
        // --------------------------------------------------------------


        try {
            updatedPost.setArea(Double.parseDouble(etArea.getText().toString()));
            updatedPost.setPrice(Long.parseLong(etPrice.getText().toString()));
            updatedPost.setBedrooms(Integer.parseInt(etBedrooms.getText().toString()));
            updatedPost.setFloors(Integer.parseInt(etFloors.getText().toString()));
        } catch (NumberFormatException e) {
            showErrorAndReset("Lỗi định dạng số."); return;
        }

        updatedPost.setImageUrls(finalImageUrls); // Use the final list
        updatedPost.setTimestamp(Timestamp.now()); // Update timestamp to edit time
        updatedPost.setAvailable(existingPost.isAvailable()); // Keep original availability status? Or add a switch? Let's keep it for now.
        updatedPost.setViewCount(existingPost.getViewCount()); // Keep original view count

        if (currentLatLng != null) {
            updatedPost.setLatitude(currentLatLng.latitude);
            updatedPost.setLongitude(currentLatLng.longitude);
        } else {
            // Keep original lat/lng if geocoding wasn't performed or failed in edit?
            updatedPost.setLatitude(existingPost.getLatitude());
            updatedPost.setLongitude(existingPost.getLongitude());
        }

        // --- Update in Firestore using .set() ---
        db.collection("posts").document(postId)
                .set(updatedPost) // Use set() to overwrite the document with new data
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post updated successfully: " + postId);
                    Toast.makeText(EditPostActivity.this, "Cập nhật tin thành công!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);

                    finish(); // Go back
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating post document: " + postId, e);
                    showErrorAndReset("Lỗi cập nhật tin đăng: " + e.getMessage());
                });
    }

    // showErrorAndReset: Đảm bảo ẩn ProgressBar và bật lại nút CẬP NHẬT
    private void showErrorAndReset(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        progressBarEditPost.setVisibility(View.GONE); // *** ẨN PROGRESS BAR ***
        btnUpdatePost.setEnabled(true); // *** BẬT LẠI NÚT CẬP NHẬT ***
        setInputsEnabled(true); // Bật lại các input khác
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