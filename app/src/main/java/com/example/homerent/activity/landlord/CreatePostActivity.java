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
import com.example.homerent.model.Post;
import com.example.homerent.model.User; // Cần model User
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.widget.AdapterView; // Import AdapterView
import android.widget.ArrayAdapter; // Import ArrayAdapter
import android.widget.AutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import com.example.homerent.model.AddressData;
import com.example.homerent.model.Commune;
import com.example.homerent.model.District;
import com.example.homerent.model.Province;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors; // Cần Java 8+

import android.Manifest; // Import Manifest
import android.annotation.SuppressLint; // Import SuppressLint
import android.content.pm.PackageManager;
import android.location.Location; // Import Location
import com.google.android.gms.location.FusedLocationProviderClient; // Import FusedLocationProviderClient
import com.google.android.gms.location.LocationServices; // Import LocationServices
import com.google.android.gms.location.Priority; // Import Priority
import com.google.android.gms.tasks.CancellationTokenSource; // Import CancellationTokenSource

public class CreatePostActivity extends AppCompatActivity implements OnMapReadyCallback, SelectedImageAdapter.OnImageRemoveListener {

    private static final String TAG = "CreatePostActivity";
    private static final int MAX_IMAGES = 5; // Giới hạn số lượng ảnh
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Toolbar toolbar;
    private TextInputEditText etAddressDetail, etArea, etPrice,
            etBedrooms, etFloors, etContactName, etContactEmail, etContactPhone,
            etPostTitle, etPostDescription;

    private RadioGroup rgPostDuration;

    private AutoCompleteTextView actProvince, actDistrict, actCommune;
    private TextInputLayout tilProvince, tilDistrict, tilCommune; // Để bật/tắt dễ hơn

    private ArrayAdapter<Province> provinceArrayAdapter;
    private ArrayAdapter<District> districtArrayAdapter;
    private ArrayAdapter<Commune> communeArrayAdapter;

    private List<Province> allProvinces = new ArrayList<>();
    private List<District> allDistricts = new ArrayList<>();
    private List<Commune> allCommunes = new ArrayList<>();

    private List<District> filteredDistricts = new ArrayList<>(); // List quận/huyện cho tỉnh đã chọn
    private List<Commune> filteredCommunes = new ArrayList<>(); // List phường/xã cho quận đã chọn

    // Lưu trữ lựa chọn hiện tại
    private Province selectedProvince;
    private District selectedDistrict;
    private Commune selectedCommune;

    private Button btnLocateAddress, btnAddImage, btnSavePost;
    private TextView tvSelectedDates;
    private RecyclerView rvSelectedImages;
    private ProgressBar progressBarCreatePost;
    private Button btnGetCurrentLocation; // Nút mới
    private FusedLocationProviderClient fusedLocationClient;

    private GoogleMap gMap;
    private SupportMapFragment mapFragment;
    private Geocoder geocoder;
    private Marker currentMarker;
    private LatLng currentLatLng; // Lưu tọa độ đã chọn

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private ArrayList<Object> selectedImageItems = new ArrayList<>();
    private SelectedImageAdapter selectedImageAdapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_create_post), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Bind Views
        bindViews();

        // Load dữ liệu địa chỉ từ JSON
        loadAddressData();

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
        // Ánh xạ AutoCompleteTextView và TextInputLayout
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
        etContactName = findViewById(R.id.etContactName);
        etContactEmail = findViewById(R.id.etContactEmail);
        etContactPhone = findViewById(R.id.etContactPhone);
        etPostTitle = findViewById(R.id.etPostTitle);
        etPostDescription = findViewById(R.id.etPostDescription);
        btnLocateAddress = findViewById(R.id.btnLocateAddress);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnSavePost = findViewById(R.id.btnSavePost);
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation); // Bind nút mới
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        progressBarCreatePost = findViewById(R.id.progressBarCreatePost);
        rgPostDuration = findViewById(R.id.rgPostDuration);
    }

    private void setupRecyclerView() {
        selectedImageAdapter = new SelectedImageAdapter(this, selectedImageItems, this);
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)); // Đổi thành VERTICAL
        rvSelectedImages.setAdapter(selectedImageAdapter);

        // *** THÊM PHẦN NÀY ĐỂ KÍCH HOẠT KÉO THẢ ***
        // 1. Tạo Callback, truyền adapter và danh sách ảnh vào
        ItemTouchHelper.Callback callback = new ImageTouchHelperCallback(selectedImageAdapter, selectedImageItems);
        // 2. Tạo ItemTouchHelper từ Callback
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        // 3. Gắn ItemTouchHelper vào RecyclerView
        itemTouchHelper.attachToRecyclerView(rvSelectedImages);
        // ******************************************
    }

    private void loadAddressData() {
        try {
            InputStream is = getAssets().open("donvihanhchinh.json"); // Tên file JSON của bạn
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            AddressData data = gson.fromJson(reader, AddressData.class);
            reader.close();

            if (data != null) {
                allProvinces = data.getProvinces() != null ? data.getProvinces() : new ArrayList<>();
                allDistricts = data.getDistricts() != null ? data.getDistricts() : new ArrayList<>();
                allCommunes = data.getCommunes() != null ? data.getCommunes() : new ArrayList<>();

                Log.d(TAG, "Loaded " + allProvinces.size() + " provinces, " + allDistricts.size() + " districts, " + allCommunes.size() + " communes.");

                // Sắp xếp theo tên (tùy chọn)
                Collections.sort(allProvinces, (p1, p2) -> p1.getName().compareTo(p2.getName()));
                // Không cần sắp xếp district/commune ban đầu vì sẽ lọc động

                setupAutoCompleteTextViews(); // Setup spinner sau khi có dữ liệu
            } else {
                Log.e(TAG, "Failed to parse AddressData from JSON.");
                Toast.makeText(this, "Lỗi đọc dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading address JSON file", e);
            Toast.makeText(this, "Lỗi đọc file dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
        } catch (com.google.gson.JsonSyntaxException e) {
            Log.e(TAG, "Error parsing address JSON file", e);
            Toast.makeText(this, "Lỗi định dạng file dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAutoCompleteTextViews() {
        // --- Province AutoCompleteTextView ---
        // Không cần thêm item hint vì TextInputLayout đã có hint
        provinceArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allProvinces);
        actProvince.setAdapter(provinceArrayAdapter);
        actProvince.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvince = (Province) parent.getItemAtPosition(position);
            Log.d(TAG, "Province Selected: " + selectedProvince.getName() + " (ID: " + selectedProvince.getIdProvince() + ")");

            // Clear và cập nhật District
            actDistrict.setText("", false); // Clear text, false để không trigger listener
            selectedDistrict = null;
            updateDistrictDropdown(selectedProvince.getIdProvince());

            // Clear và vô hiệu hóa Commune
            actCommune.setText("", false);
            selectedCommune = null;
            updateCommuneDropdown(null); // Gọi với null để clear và disable
            tilCommune.setEnabled(false);

            updateMapLocation();
        });

        // --- District AutoCompleteTextView ---
        // Khởi tạo adapter rỗng ban đầu
        districtArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actDistrict.setAdapter(districtArrayAdapter);
        // tilDistrict đã được disable trong XML, sẽ enable khi có dữ liệu

        actDistrict.setOnItemClickListener((parent, view, position, id) -> {
            selectedDistrict = (District) parent.getItemAtPosition(position);
            Log.d(TAG, "District Selected: " + selectedDistrict.getName() + " (ID: " + selectedDistrict.getIdDistrict() + ")");

            // Clear và cập nhật Commune
            actCommune.setText("", false);
            selectedCommune = null;
            updateCommuneDropdown(selectedDistrict.getIdDistrict());

            updateMapLocation();
        });

        // --- Commune AutoCompleteTextView ---
        // Khởi tạo adapter rỗng ban đầu
        communeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actCommune.setAdapter(communeArrayAdapter);
        // tilCommune đã được disable trong XML

        actCommune.setOnItemClickListener((parent, view, position, id) -> {
            selectedCommune = (Commune) parent.getItemAtPosition(position);
            Log.d(TAG, "Commune Selected: " + selectedCommune.getName() + " (ID: " + selectedCommune.getIdCommune() + ")");
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



    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // *** SỬ DỤNG selectedImageItems ***
                        int currentImageCount = selectedImageItems.size();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count && currentImageCount + i < MAX_IMAGES; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                // Thêm Uri vào List<Object>
                                if (!isUriAlreadyAdded(imageUri)) { // Kiểm tra trùng lặp nếu cần
                                    selectedImageItems.add(imageUri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            if (currentImageCount < MAX_IMAGES) {
                                Uri imageUri = result.getData().getData();
                                if (!isUriAlreadyAdded(imageUri)) {
                                    selectedImageItems.add(imageUri);
                                }
                            }
                        }
                        selectedImageAdapter.notifyDataSetChanged();
                        checkImageLimit();
                    }
                });
    }

    // Hàm này dùng để kiểm tra trùng lặp nếu cần (giữ nguyên)
    private boolean isUriAlreadyAdded(Uri uri) {
        for (Object item : selectedImageItems) {
            if (item instanceof Uri && item.equals(uri)) {
                return true;
            }
        }
        return false;
    }


    private void checkImageLimit() {
        btnAddImage.setEnabled(selectedImageItems.size() < MAX_IMAGES);
        if (!btnAddImage.isEnabled()) {
            Toast.makeText(this, "Đã đạt giới hạn " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageRemoved(int position) {
        if (position >= 0 && position < selectedImageItems.size()) {
            selectedImageItems.remove(position);
            selectedImageAdapter.notifyItemRemoved(position);
            selectedImageAdapter.notifyItemRangeChanged(position, selectedImageItems.size()); // Update indices
            checkImageLimit(); // Re-enable button if below limit
        }
    }

    private void setupButtonClickListeners() {
        btnGetCurrentLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation()); // Gọi hàm kiểm tra quyền
        btnLocateAddress.setOnClickListener(v -> geocodeAddress());
        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnSavePost.setOnClickListener(v -> attemptSavePost());
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


    // --- Location Logic ---

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Quyền đã được cấp, lấy vị trí
            Log.d(TAG, "Location permission already granted. Getting location...");
            getCurrentDeviceLocation();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Hiển thị giải thích tại sao cần quyền (ví dụ: dùng Dialog)
            Log.d(TAG, "Showing rationale for location permission.");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Yêu cầu quyền vị trí")
                    .setMessage("Ứng dụng cần truy cập vị trí của bạn để tự động điền thông tin địa chỉ.")
                    .setPositiveButton("Đồng ý", (dialog, which) -> requestLocationPermission())
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            // Yêu cầu quyền lần đầu hoặc khi user chọn "Don't ask again"
            Log.d(TAG, "Requesting location permission.");
            requestLocationPermission();
        }
    }

    @SuppressLint("MissingPermission") // Cảnh báo sẽ được xử lý bởi check trước đó
    private void getCurrentDeviceLocation() {
        showLoading(true); // Hiển thị loading
        btnGetCurrentLocation.setEnabled(false); // Vô hiệu hóa nút

        // Sử dụng getCurrentLocation để lấy vị trí mới nhất
        // Priority.PRIORITY_HIGH_ACCURACY: Độ chính xác cao (GPS), tốn pin hơn
        // Priority.PRIORITY_BALANCED_POWER_ACCURACY: Cân bằng (Wi-Fi, Mạng di động)
        // Priority.PRIORITY_LOW_POWER: Độ chính xác thấp, tiết kiệm pin
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Location obtained: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        // Có vị trí, tiến hành reverse geocode
                        reverseGeocodeLocation(location);
                    } else {
                        // Không lấy được vị trí (có thể do location services bị tắt)
                        Log.w(TAG, "Failed to get current location (location is null).");
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại. Vui lòng kiểm tra cài đặt vị trí.", Toast.LENGTH_LONG).show();
                        showLoading(false); // Ẩn loading
                        btnGetCurrentLocation.setEnabled(true); // Bật lại nút
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting current location", e);
                    Toast.makeText(this, "Lỗi khi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false); // Ẩn loading
                    btnGetCurrentLocation.setEnabled(true); // Bật lại nút
                });

    }
    private void reverseGeocodeLocation(Location location) {
        if (!Geocoder.isPresent()) {
            Log.e(TAG, "Geocoder backend service is not present.");
            Toast.makeText(this, "Dịch vụ địa chỉ không khả dụng.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            btnGetCurrentLocation.setEnabled(true);
            return;
        }

        // Cập nhật biến tọa độ và bản đồ ngay lập tức
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        updateMapMarker(currentLatLng, "Vị trí hiện tại"); // Update map

        // Geocoding có thể chậm, nên chạy trên background thread nếu cần
        // Nhưng với 1 lần gọi, thử trên main thread trước kèm try-catch
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            showLoading(false); // Ẩn loading sau khi geocode xong
            btnGetCurrentLocation.setEnabled(true); // Bật lại nút

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                populateAddressFields(address); // Điền thông tin vào các ô
                Log.i(TAG, "Reverse geocoding successful: " + address.getAddressLine(0));

            } else {
                Log.w(TAG, "No address found for the current location.");
                Toast.makeText(this, "Không tìm thấy địa chỉ cho vị trí hiện tại.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            showLoading(false);
            btnGetCurrentLocation.setEnabled(true);
            Log.e(TAG, "Reverse geocoding failed", e);
            if ("Service not Available".equalsIgnoreCase(e.getMessage())) {
                Toast.makeText(this, "Dịch vụ địa chỉ không khả dụng, kiểm tra mạng.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lỗi khi lấy địa chỉ.", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            // Handle invalid lat/lng if necessary (should not happen with fusedLocationClient)
            showLoading(false);
            btnGetCurrentLocation.setEnabled(true);
            Log.e(TAG, "Invalid coordinates for reverse geocoding", e);
            Toast.makeText(this, "Tọa độ không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Helper cho Loading ---
    private void showLoading(boolean isLoading) {
        if (progressBarCreatePost != null) { // ID ProgressBar của bạn
            progressBarCreatePost.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Có thể vô hiệu hóa các nút khác nếu cần
        btnGetCurrentLocation.setEnabled(!isLoading);
        btnLocateAddress.setEnabled(!isLoading);
        btnSavePost.setEnabled(!isLoading); // Hoặc btnSavePost
    }

    private void updateMapMarker(LatLng latLng, String title) {
        if(gMap == null || latLng == null) return;
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = gMap.addMarker(new MarkerOptions().position(latLng).title(title));
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f)); // Zoom gần hơn
    }

    private void populateAddressFields(Address address) {
        String provinceName = address.getAdminArea(); // Tỉnh/TP
        String districtName = address.getSubAdminArea(); // Quận/Huyện
        // Ward/Commune có thể là locality hoặc subLocality tùy dữ liệu trả về
        String wardName = address.getSubLocality() != null ? address.getSubLocality() : address.getLocality();
        String streetName = address.getThoroughfare(); // Tên đường
        String streetNumber = address.getSubThoroughfare(); // Số nhà

        Log.d(TAG, "Geocoded Address Components: Province=" + provinceName + ", District=" + districtName + ", Ward=" + wardName + ", Street=" + streetName + ", Number=" + streetNumber);

        // 1. Tìm và chọn Tỉnh/Thành phố
        Province matchedProvince = null;
        if (provinceName != null) {
            for (int i = 0; i < provinceArrayAdapter.getCount(); i++) {
                // So sánh tên không phân biệt hoa thường và bỏ "Tỉnh ", "Thành phố "
                String cleanedProvinceName = provinceArrayAdapter.getItem(i).getName()
                        .replace("Tỉnh ", "").replace("Thành phố ", "");
                String cleanedGeoProvince = provinceName.replace("Tỉnh ", "").replace("Thành phố ", "");
                if (cleanedGeoProvince.equalsIgnoreCase(cleanedProvinceName)) {
                    matchedProvince = provinceArrayAdapter.getItem(i);
                    actProvince.setText(matchedProvince.toString(), false); // Set text và không filter
                    selectedProvince = matchedProvince; // Cập nhật biến selected
                    updateDistrictDropdown(matchedProvince.getIdProvince()); // Kích hoạt load Quận/Huyện
                    Log.d(TAG, "Matched Province: " + matchedProvince.getName());
                    break;
                }
            }
            if (matchedProvince == null) Log.w(TAG, "Could not match Province: " + provinceName);
        } else { Log.w(TAG, "Geocoder did not return Province name."); }


        // 2. Tìm và chọn Quận/Huyện (Chờ sau khi updateDistrictDropdown chạy xong nếu nó bất đồng bộ)
        // Do updateDistrictDropdown là đồng bộ, có thể tìm ngay trong filteredDistricts
        District matchedDistrict = null;
        if (districtName != null && !filteredDistricts.isEmpty()) { // Phải có filteredDistricts trước
            for (District d : filteredDistricts) {
                // So sánh tên không phân biệt hoa thường và bỏ "Quận ", "Huyện ", "Thị xã "
                String cleanedDistrictName = d.getName()
                        .replace("Quận ","").replace("Huyện ","").replace("Thị xã ","");
                String cleanedGeoDistrict = districtName.replace("Quận ","").replace("Huyện ","").replace("Thị xã ","");
                if (cleanedGeoDistrict.equalsIgnoreCase(cleanedDistrictName)) {
                    matchedDistrict = d;
                    actDistrict.setText(matchedDistrict.toString(), false);
                    selectedDistrict = matchedDistrict;
                    updateCommuneDropdown(matchedDistrict.getIdDistrict());
                    Log.d(TAG, "Matched District: " + matchedDistrict.getName());
                    break;
                }
            }
            if (matchedDistrict == null) Log.w(TAG, "Could not match District: " + districtName);
        } else {Log.w(TAG, "Geocoder did not return District name or filtered list is empty.");}

        // 3. Tìm và chọn Phường/Xã
        Commune matchedCommune = null;
        if (wardName != null && !filteredCommunes.isEmpty()) {
            for (Commune c : filteredCommunes) {
                // So sánh tên không phân biệt hoa thường và bỏ "Phường ", "Xã ", "Thị trấn "
                String cleanedCommuneName = c.getName()
                        .replace("Phường ","").replace("Xã ","").replace("Thị trấn ","");
                String cleanedGeoWard = wardName.replace("Phường ","").replace("Xã ","").replace("Thị trấn ","");
                if (cleanedGeoWard.equalsIgnoreCase(cleanedCommuneName)) {
                    matchedCommune = c;
                    actCommune.setText(matchedCommune.toString(), false);
                    selectedCommune = matchedCommune;
                    Log.d(TAG, "Matched Commune: " + matchedCommune.getName());
                    break;
                }
            }
            if (matchedCommune == null) Log.w(TAG, "Could not match Commune: " + wardName);
        } else {Log.w(TAG, "Geocoder did not return Ward/Commune name or filtered list is empty.");}


        // 4. Điền địa chỉ chi tiết
        String detailAddress = "";
        if (streetNumber != null) detailAddress += streetNumber + " ";
        if (streetName != null) detailAddress += streetName;
        etAddressDetail.setText(detailAddress.trim());
        Log.d(TAG, "Set Detailed Address: " + detailAddress.trim());

    }

        // --- Saving Logic ---

    private void attemptSavePost() {
        if (!validateInput()) {
            return;
        }
        progressBarCreatePost.setVisibility(View.VISIBLE);
        btnSavePost.setEnabled(false);

        // *** LỌC RA CÁC URI TỪ LIST OBJECT TRƯỚC KHI UPLOAD ***
        List<Uri> urisToUpload = selectedImageItems.stream()
                .filter(item -> item instanceof Uri)
                .map(item -> (Uri) item)
                .collect(Collectors.toList());

        if (!urisToUpload.isEmpty()) {
            uploadImagesAndSavePost(urisToUpload); // Truyền List<Uri> đã lọc
        } else {
            saveDataToFirestore(new ArrayList<>()); // Lưu không có ảnh
        }
    }

    // *** THAY ĐỔI THAM SỐ CỦA HÀM NÀY ĐỂ NHẬN List<Uri> ***
    private void uploadImagesAndSavePost(List<Uri> urisToUpload) {
        List<String> downloadUrls = new ArrayList<>();
        AtomicInteger uploadCounter = new AtomicInteger(0);
        int totalImages = urisToUpload.size(); // Dùng size của list đã lọc

        StorageReference storageRef = storage.getReference().child("post_images");

        for (Uri imageUri : urisToUpload) { // Lặp qua list Uri đã lọc
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageRef.child(filename);
            UploadTask uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) { throw task.getException(); }
                return fileRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    downloadUrls.add(downloadUri.toString());
                } else {
                    Log.w(TAG, "Image upload failed: " + imageUri.toString(), task.getException());
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải lên ảnh: " + imageUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                }

                if (uploadCounter.incrementAndGet() == totalImages) {
                    saveDataToFirestore(downloadUrls);
                }
            });
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


    private void saveDataToFirestore(List<String> imageUrls) {
        if (currentUser == null || !validateInput()) {
            showErrorAndReset("Lỗi: Dữ liệu không hợp lệ hoặc người dùng không tồn tại.");
            return;
        }

        // --- Tính toán Ngày bắt đầu và Kết thúc ---
        Timestamp startDate = Timestamp.now(); // Ngày bắt đầu là hiện tại
        int selectedDurationId = rgPostDuration.getCheckedRadioButtonId();
        int durationDays = 0;

        if (selectedDurationId == R.id.rbDuration10) {
            durationDays = 10;
        } else if (selectedDurationId == R.id.rbDuration20) {
            durationDays = 20;
        } else if (selectedDurationId == R.id.rbDuration30) {
            durationDays = 30;
        } else {
            // Lỗi này không nên xảy ra nếu validation hoạt động đúng
            showErrorAndReset("Lỗi không xác định được thời hạn đăng.");
            return;
        }

        // Tính ngày kết thúc
        long startTimeMillis = startDate.toDate().getTime();

        long durationMillis = TimeUnit.DAYS.toMillis(durationDays);

        long endTimeMillis = startTimeMillis + durationMillis;
        Timestamp endDate = new Timestamp(new Date(endTimeMillis));
        // ------------------------------------------

        Post newPost = new Post();
        newPost.setUserId(currentUser.getUid());
        newPost.setTitle(etPostTitle.getText().toString().trim());
        newPost.setDescription(etPostDescription.getText().toString().trim());

        // Lấy tên từ AutoCompleteTextView
        newPost.setCity(actProvince.getText().toString().trim());
        newPost.setDistrict(actDistrict.getText().toString().trim());
        newPost.setWard(actCommune.getText().toString().trim());
        newPost.setAddress(etAddressDetail.getText().toString().trim()); // Có thể rỗng

        // ... (Gán các giá trị khác như cũ)
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
        newPost.setTimestamp(Timestamp.now()); // Thời gian tạo/cập nhật post
        newPost.setAvailable(true);
        newPost.setViewCount(0);
        // *** Gán ngày bắt đầu và kết thúc đã tính toán ***
        newPost.setStartDate(startDate);
        newPost.setEndDate(endDate);
        // ***------------------------------------***
        if (currentLatLng != null) {
            newPost.setLatitude(currentLatLng.latitude);
            newPost.setLongitude(currentLatLng.longitude);
        } else {
            newPost.setLatitude(0);
            newPost.setLongitude(0);
        }

        // --- Save to Firestore ---
        db.collection("posts")
                .add(newPost)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Post created successfully with ID: " + documentReference.getId());
                    progressBarCreatePost.setVisibility(View.GONE);
                    Toast.makeText(CreatePostActivity.this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                    finish();
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