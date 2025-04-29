package com.example.homerent.activity.tenant; // Đảm bảo đúng package

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.homerent.R;
import com.example.homerent.adapter.ImageSliderAdapter;
import com.example.homerent.model.Post;
import com.example.homerent.model.User; // Cần model User
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Để lưu post đã save

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.homerent.FullScreenImageViewerActivity; // Import FullScreenImageViewerActivity
import com.example.homerent.FullScreenMapActivity; // Import FullScreenMapActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Import M3 Dialog
import android.content.DialogInterface;
import android.content.pm.PackageManager; // Import PackageManager

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton; // Import MaterialButton

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailTenantActivity extends AppCompatActivity
        implements ImageSliderAdapter.OnItemClickListener, OnMapReadyCallback {

    private static final String TAG = "PostDetailTenantAct";

    private Toolbar toolbar;
    private ViewPager2 viewPagerImages;
    private TextView tvImageCounter;
    private TextView tvPostTitleDetail;
    private TextView tvPostPriceDetail;
    private TextView tvPostAreaDetail;
    private TextView tvFullAddress; // TextView địa chỉ đầy đủ
    private TextView tvPostDescriptionDetail;
    private TextView tvPriceValue;
    private TextView tvAreaValue;
    private TextView tvBedroomsValue;
    private TextView tvFloorsValue;
    private TextView tvPostingDateDetail;
    private CircleImageView ivLandlordAvatar;
    private TextView tvLandlordName;
    private MaterialButton btnContactOptions; // Đổi thành MaterialButton
    private ProgressBar progressBarDetail;
    private NestedScrollView scrollViewContent;
    private Button btnViewFullMap; // Nút xem map lớn
    private SupportMapFragment detailMapFragment;
    private GoogleMap detailMap;
    private LatLng postLatLng;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String postId;
    private Post currentPost;
    private User landlordInfo; // Lưu thông tin chủ nhà
    private ImageSliderAdapter imageSliderAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private boolean isSaved = false; // Trạng thái bài đăng đã được lưu chưa
    private MenuItem saveMenuItem; // Để thay đổi icon lưu

    private List<String> postImageUrls = new ArrayList<>(); // Để dùng trong listener


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- EdgeToEdge ---
        EdgeToEdge.enable(this);
        //--------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_tenant); // Đảm bảo đúng layout
        // --- WindowInsets Listener ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_post_detail_tenant), (v, insets) -> { // ID root layout
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom()); // Apply padding for status/nav bars
            // If using CoordinatorLayout, the button might adjust automatically with insetEdge="bottom"
            // Otherwise, adjust bottom margin/padding for btnContactOptions here if needed
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currencyFormat.setMaximumFractionDigits(0);

        postId = getIntent().getStringExtra("POST_ID");
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "ID tin đăng không hợp lệ!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid Post ID received.");
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupMap(); // Setup map riêng
        loadPostDetails();
        setupButtonClickListeners();

        if (currentUser != null) {
            checkIfPostIsSaved();
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarPostDetailTenant);
        viewPagerImages = findViewById(R.id.viewPagerImagesTenant);
        tvImageCounter = findViewById(R.id.tvImageCounterTenant);
        tvPostTitleDetail = findViewById(R.id.tvPostTitleDetailTenant);
        tvPostPriceDetail = findViewById(R.id.tvPostPriceDetailTenant);
        tvPostAreaDetail = findViewById(R.id.tvPostAreaDetailTenant);
        tvFullAddress = findViewById(R.id.tvFullAddressTenant);
        tvPostDescriptionDetail = findViewById(R.id.tvPostDescriptionDetailTenant);
        tvPriceValue = findViewById(R.id.tvPriceValueTenant);
        tvAreaValue = findViewById(R.id.tvAreaValueTenant);
        tvBedroomsValue = findViewById(R.id.tvBedroomsValueTenant);
        tvFloorsValue = findViewById(R.id.tvFloorsValueTenant);
        tvPostingDateDetail = findViewById(R.id.tvPostingDateDetailTenant);
        ivLandlordAvatar = findViewById(R.id.ivLandlordAvatarTenant);
        tvLandlordName = findViewById(R.id.tvLandlordNameTenant);
        btnContactOptions = findViewById(R.id.btnContactOptions); // ID nút liên hệ mới
        progressBarDetail = findViewById(R.id.progressBarDetailTenant);
        scrollViewContent = findViewById(R.id.tenantDetailScrollView);
        btnViewFullMap = findViewById(R.id.btnViewFullMapTenant); // ID nút xem map mới
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Chi tiết tin");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupMap() {
        detailMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDetailFragmentContainerTenant); // ID map mới
        if (detailMapFragment != null) {
            detailMapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found!");
            if(btnViewFullMap != null) btnViewFullMap.setVisibility(View.GONE); // Ẩn nút nếu lỗi map
        }
    }

    private void setupButtonClickListeners() {
        // Nút xem bản đồ lớn
        btnViewFullMap.setOnClickListener(v -> openFullScreenMap());

        // Nút liên hệ
        btnContactOptions.setOnClickListener(v -> {
            if (landlordInfo != null && landlordInfo.getPhoneNumber() != null && !landlordInfo.getPhoneNumber().isEmpty()) {
                showContactOptionsDialog(landlordInfo.getPhoneNumber());
            } else {
                Toast.makeText(this, "Không có thông tin liên hệ của chủ nhà.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFullScreenMap() {
        if (postLatLng != null && currentPost != null) {
            Intent intent = new Intent(this, FullScreenMapActivity.class);
            intent.putExtra(FullScreenMapActivity.EXTRA_LATITUDE, postLatLng.latitude);
            intent.putExtra(FullScreenMapActivity.EXTRA_LONGITUDE, postLatLng.longitude);
            // Tạo địa chỉ đầy đủ để hiển thị trên map và tìm kiếm
            String fullAddress = (currentPost.getAddress() != null ? currentPost.getAddress() + ", " : "")
                    + currentPost.getWard() + ", "
                    + currentPost.getDistrict() + ", "
                    + currentPost.getCity();
            intent.putExtra(FullScreenMapActivity.EXTRA_ADDRESS, fullAddress.replaceAll("^, ", "").trim());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không có thông tin vị trí để hiển thị.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDetailMapMarker(LatLng latLng, String title) {
        if (detailMap == null || latLng == null) return;
        Log.d(TAG, "Updating detail map marker at: " + latLng.latitude + "," + latLng.longitude);
        detailMap.clear(); // Xóa marker cũ (nếu có)
        detailMap.addMarker(new MarkerOptions().position(latLng).title(title));
        detailMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)); // Zoom vừa phải
    }

    // onMapReady giữ nguyên như PostDetailLandlordActivity
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Detail Map Ready");
        detailMap = googleMap;
        detailMap.getUiSettings().setAllGesturesEnabled(false); // Tắt hết tương tác cho map nhỏ
        detailMap.getUiSettings().setMapToolbarEnabled(false);

        if (postLatLng != null) {
            updateDetailMapMarker(postLatLng, currentPost != null ? currentPost.getTitle() : "Vị trí");
        }
    }

    private void loadPostDetails() {
        Log.d(TAG, "Loading details for post: " + postId);
        progressBarDetail.setVisibility(View.VISIBLE);
        scrollViewContent.setVisibility(View.GONE); // Ẩn nội dung khi load
        btnContactOptions.setVisibility(View.GONE);

        DocumentReference postRef = db.collection("posts").document(postId);
        postRef.get().addOnCompleteListener(task -> {
            if (!isDestroyed() && !isFinishing()) { // Kiểm tra activity còn tồn tại
                progressBarDetail.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        try {
                            currentPost = document.toObject(Post.class);
                            if (currentPost != null) {
                                currentPost.setPostId(document.getId());
                                populateUI();
                                scrollViewContent.setVisibility(View.VISIBLE); // Hiện nội dung
                                // Load thông tin người đăng (Landlord)
                                if (currentPost.getUserId() != null) {
                                    loadLandlordInfo(currentPost.getUserId());
                                } else {
                                    setDefaultLandlordInfo();
                                    btnContactOptions.setVisibility(View.GONE); // Không có chủ thì ẩn nút gọi
                                }
                                // Tăng view count
                                incrementViewCount(postId);

                            } else {
                                handleLoadError("Không thể chuyển đổi dữ liệu tin đăng.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting post data", e);
                            handleLoadError("Lỗi định dạng dữ liệu tin đăng.");
                        }
                    } else {
                        Log.d(TAG, "No such document");
                        handleLoadError("Tin đăng không tồn tại.");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    handleLoadError("Lỗi tải dữ liệu: " + Objects.requireNonNull(task.getException()).getMessage());
                }
            }
        });
    }

    private void populateUI() {
        if (currentPost == null) return;

        tvPostTitleDetail.setText(currentPost.getTitle());
        tvPostPriceDetail.setText(currencyFormat.format(currentPost.getPrice()) + "/tháng");
        tvPostAreaDetail.setText(String.format(Locale.US, "%.1fm²", currentPost.getArea()));

        // Hiển thị địa chỉ đầy đủ
        String fullAddressStr = String.join(", ",
                notNull(currentPost.getAddress()),
                notNull(currentPost.getWard()),
                notNull(currentPost.getDistrict()),
                notNull(currentPost.getCity())
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
        tvFullAddress.setText(fullAddressStr.isEmpty() ? "Không có địa chỉ" : fullAddressStr);


        tvPostDescriptionDetail.setText(notNull(currentPost.getDescription()));

        tvPriceValue.setText(currencyFormat.format(currentPost.getPrice()) + "/tháng");
        tvAreaValue.setText(String.format(Locale.US, "%.1fm²", currentPost.getArea()));
        tvBedroomsValue.setText(String.valueOf(currentPost.getBedrooms()));
        tvFloorsValue.setText(String.valueOf(currentPost.getFloors())); // Hiển thị số tầng



        if (currentPost.getTimestamp() != null) {
            tvPostingDateDetail.setText("Tin đăng ngày: " + dateFormat.format(currentPost.getTimestamp().toDate()));
        } else {
            tvPostingDateDetail.setText("Tin đăng ngày: N/A");
        }

        // Setup ViewPager2
        // --- Setup ViewPager2 với listener ---
        postImageUrls = currentPost.getImageUrls() != null ? currentPost.getImageUrls() : Collections.emptyList();
        if (postImageUrls.isEmpty()) {
            viewPagerImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        } else {
            viewPagerImages.setVisibility(View.VISIBLE);
            tvImageCounter.setVisibility(View.VISIBLE);
            // *** Khởi tạo adapter với listener là this ***
            imageSliderAdapter = new ImageSliderAdapter(this, postImageUrls, this);
            viewPagerImages.setAdapter(imageSliderAdapter);
            updateImageCounter(postImageUrls.size());
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateImageCounter(postImageUrls.size());
                }
            });
        }

        // --- Xử lý Map ---
        View mapFragmentView = findViewById(R.id.mapDetailFragmentContainerTenant); // Lấy view của fragment
        if (currentPost.getLatitude() != 0 || currentPost.getLongitude() != 0) {
            postLatLng = new LatLng(currentPost.getLatitude(), currentPost.getLongitude());
            btnViewFullMap.setVisibility(View.VISIBLE);
            if(mapFragmentView != null) mapFragmentView.setVisibility(View.VISIBLE); // Hiện fragment map
            if (detailMap != null) {
                updateDetailMapMarker(postLatLng, currentPost.getTitle());
            }
        } else {
            postLatLng = null;
            btnViewFullMap.setVisibility(View.GONE);
            if(mapFragmentView != null) mapFragmentView.setVisibility(View.GONE); // Ẩn fragment map
            Log.w(TAG,"Post has no valid coordinates (0,0). Hiding map.");
        }
    }

    // Hàm helper để tránh lỗi NullPointerException khi ghép chuỗi địa chỉ
    private String notNull(String value) {
        return value != null ? value : "";
    }

    // *** Implement OnItemClickListener cho ImageSliderAdapter ***
    @Override
    public void onItemClick(int position) {
        if (postImageUrls != null && !postImageUrls.isEmpty()) {
            Intent intent = new Intent(this, FullScreenImageViewerActivity.class);
            intent.putStringArrayListExtra(FullScreenImageViewerActivity.EXTRA_IMAGE_URLS, new ArrayList<>(postImageUrls));
            intent.putExtra(FullScreenImageViewerActivity.EXTRA_START_POSITION, position);
            startActivity(intent);
        }
    }

    private void updateImageCounter(int totalImages) {
        if (totalImages > 0) {
            int currentItem = viewPagerImages.getCurrentItem() + 1;
            tvImageCounter.setText(currentItem + "/" + totalImages);
            tvImageCounter.setVisibility(View.VISIBLE);
        } else {
            tvImageCounter.setVisibility(View.GONE);
        }
    }

    private void loadLandlordInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isDestroyed() && !isFinishing()){
                        if (documentSnapshot.exists()) {
                            landlordInfo = documentSnapshot.toObject(User.class);
                            if (landlordInfo != null) {
                                tvLandlordName.setText(landlordInfo.getName() != null ? landlordInfo.getName() : "Chủ nhà");
                                // Load ảnh đại diện
                                if (landlordInfo.getAvatarUrl() != null && !landlordInfo.getAvatarUrl().isEmpty()) {
                                    Glide.with(this)
                                            .load(landlordInfo.getAvatarUrl())
                                            .placeholder(R.drawable.person_24px)
                                            .error(R.drawable.person_24px)
                                            .into(ivLandlordAvatar);
                                } else {
                                    ivLandlordAvatar.setImageResource(R.drawable.person_24px);
                                }
                                // *** CẬP NHẬT NÚT LIÊN HỆ ***
                                if (landlordInfo.getPhoneNumber() != null && !landlordInfo.getPhoneNumber().trim().isEmpty()){
                                    btnContactOptions.setText("Liên hệ: " + landlordInfo.getPhoneNumber()); // Hiện SĐT
                                    btnContactOptions.setVisibility(View.VISIBLE); // Hiển thị nút
                                } else {
                                    btnContactOptions.setVisibility(View.GONE); // Ẩn nút nếu không có SĐT
                                }
                                // **************************

                            } else {
                                setDefaultLandlordInfo();
                                btnContactOptions.setVisibility(View.GONE);
                            }
                        } else {
                            // ... (setDefaultLandlordInfo, ẩn nút)
                            btnContactOptions.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed() && !isFinishing()){
                        // ... (setDefaultLandlordInfo, ẩn nút)
                        btnContactOptions.setVisibility(View.GONE);
                    }
                });
    }

    private void showContactOptionsDialog(final String phoneNumber) {
        final CharSequence[] options = {"Gọi điện", "Nhắn tin SMS", "Mở Zalo"};
        final boolean isZaloInstalled = isAppInstalled("com.zing.zalo"); // Kiểm tra Zalo

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Liên hệ với chủ nhà");

        // Tạo danh sách lựa chọn, chỉ thêm Zalo nếu đã cài
        ArrayList<CharSequence> availableOptions = new ArrayList<>();
        availableOptions.add(options[0]); // Gọi điện
        availableOptions.add(options[1]); // SMS
        if (isZaloInstalled) {
            availableOptions.add(options[2]); // Zalo
        }

        builder.setItems(availableOptions.toArray(new CharSequence[0]), (dialog, which) -> {
            String selectedOption = availableOptions.get(which).toString();
            switch (selectedOption) {
                case "Gọi điện":
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                    startActivitySafely(dialIntent, "Không thể mở ứng dụng gọi điện.");
                    break;
                case "Nhắn tin SMS":
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
                    // intent.putExtra("sms_body", "Xin chào, tôi quan tâm đến phòng trọ bạn đăng trên HomeRent."); // Thêm nội dung SMS mặc định (tùy chọn)
                    startActivitySafely(smsIntent, "Không thể mở ứng dụng nhắn tin.");
                    break;
                case "Mở Zalo":
                    // Đảm bảo kiểm tra lại isZaloInstalled nếu cần
                    // Uri zaloUri = Uri.parse("zalo://qr/p/" + phoneNumber); // Thử URI Zalo qua SĐT (có thể không ổn định)
                    // Hoặc dùng link web zalo.me
                    Uri zaloUri = Uri.parse("https://zalo.me/" + phoneNumber);
                    Intent zaloIntent = new Intent(Intent.ACTION_VIEW, zaloUri);
                    // Không cần setPackage vì sẽ mở qua trình duyệt nếu Zalo không xử lý được link web
                    startActivitySafely(zaloIntent, "Không thể mở Zalo hoặc trình duyệt.");
                    break;
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Hàm helper để kiểm tra ứng dụng đã cài đặt chưa
    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Hàm helper để khởi chạy Intent an toàn
    private void startActivitySafely(Intent intent, String errorMessage) {
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Activity not found for intent: " + intent.toString(), ex);
        }
    }

    private void setDefaultLandlordInfo() {
        tvLandlordName.setText("Chủ nhà (Ẩn danh)");
        ivLandlordAvatar.setImageResource(R.drawable.person_24px);
    }

    private void handleLoadError(String message) {
        if (!isDestroyed() && !isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            scrollViewContent.setVisibility(View.GONE);
            btnContactOptions.setVisibility(View.GONE);
            // Có thể finish activity hoặc hiển thị lỗi trên màn hình
            // finish();
        }
    }

    // --- Tăng View Count ---
    private void incrementViewCount(String postIdToUpdate) {
        if (postIdToUpdate == null || postIdToUpdate.isEmpty()) return;
        DocumentReference postRef = db.collection("posts").document(postIdToUpdate);
        postRef.update("viewCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "View count incremented successfully for " + postIdToUpdate))
                .addOnFailureListener(e -> Log.w(TAG, "Error incrementing view count for " + postIdToUpdate, e));
    }


    // --- Menu trên Toolbar (Lưu tin) ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_detail_tenant_menu, menu);
        saveMenuItem = menu.findItem(R.id.action_save_post);
        updateSaveIcon(); // Cập nhật icon ban đầu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_save_post) {
            toggleSavePost();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Logic Lưu/Bỏ lưu tin ---
    private void checkIfPostIsSaved() {
        if (currentUser == null || postId == null) return;
        DocumentReference savedPostRef = db.collection("users").document(currentUser.getUid())
                .collection("savedPosts").document(postId);

        savedPostRef.get().addOnCompleteListener(task -> {
            if (!isDestroyed() && !isFinishing()) {
                if (task.isSuccessful()) {
                    isSaved = task.getResult() != null && task.getResult().exists();
                    Log.d(TAG, "Post " + postId + " is saved: " + isSaved);
                } else {
                    Log.w(TAG, "Error checking saved status", task.getException());
                    isSaved = false; // Giả định là chưa lưu nếu có lỗi
                }
                updateSaveIcon(); // Cập nhật icon sau khi kiểm tra xong
            }
        });
    }

    private void toggleSavePost() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu tin", Toast.LENGTH_SHORT).show();
            // Có thể chuyển đến màn hình đăng nhập
            return;
        }
        if (postId == null || currentPost == null) return;

        DocumentReference savedPostRef = db.collection("users").document(currentUser.getUid())
                .collection("savedPosts").document(postId);

        if (isSaved) {
            // Bỏ lưu
            savedPostRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        if (!isDestroyed() && !isFinishing()) {
                            isSaved = false;
                            updateSaveIcon();
                            Toast.makeText(this, "Đã bỏ lưu tin", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Post unsaved: " + postId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isDestroyed() && !isFinishing()){
                            Toast.makeText(this, "Lỗi khi bỏ lưu tin", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error unsaving post", e);
                        }
                    });
        } else {
            // Lưu tin (có thể lưu thêm thông tin cơ bản của post để hiển thị nhanh)
            Map<String, Object> savedPostData = new HashMap<>();
            savedPostData.put("title", currentPost.getTitle());
            // savedPostData.put("price", currentPost.getPrice()); // Lưu thêm nếu cần
            // savedPostData.put("thumbnailUrl", currentPost.getImageUrls() != null && !currentPost.getImageUrls().isEmpty() ? currentPost.getImageUrls().get(0) : null);
            savedPostData.put("savedAt", FieldValue.serverTimestamp()); // Lưu thời gian lưu

            savedPostRef.set(savedPostData, SetOptions.merge()) // Dùng merge để không ghi đè nếu đã tồn tại
                    .addOnSuccessListener(aVoid -> {
                        if (!isDestroyed() && !isFinishing()) {
                            isSaved = true;
                            updateSaveIcon();
                            Toast.makeText(this, "Đã lưu tin", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Post saved: " + postId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isDestroyed() && !isFinishing()) {
                            Toast.makeText(this, "Lỗi khi lưu tin", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error saving post", e);
                        }
                    });
        }
    }

    private void updateSaveIcon() {
        if (saveMenuItem != null && ContextCompat.getDrawable(this, R.drawable.bookmark_filled_24px) != null && ContextCompat.getDrawable(this, R.drawable.bookmark_24px) != null ) {
            if (isSaved) {
                saveMenuItem.setIcon(R.drawable.bookmark_filled_24px); // Icon đã lưu
                saveMenuItem.setTitle("Bỏ lưu"); // Đổi title nếu muốn
            } else {
                saveMenuItem.setIcon(R.drawable.bookmark_24px); // Icon chưa lưu
                saveMenuItem.setTitle("Lưu tin");
            }
        }
    }
}