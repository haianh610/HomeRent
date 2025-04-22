package com.example.homerent.activity.landlord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.homerent.FullScreenImageViewerActivity;
import com.example.homerent.R; // Thay R
//import com.example.homerent.activity.landlord.EditPostActivity;
import com.example.homerent.adapter.ImageSliderAdapter;
import com.example.homerent.model.Post;
import com.example.homerent.FullScreenMapActivity;
//import com.example.homerent.model.User; // Model User nếu có
import com.example.homerent.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.FrameLayout; // Import FrameLayout

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailLandlordActivity extends AppCompatActivity implements ImageSliderAdapter.OnItemClickListener {

    private static final String TAG = "PostDetailLandlordActivity";

    private Toolbar toolbar;
    private ViewPager2 viewPagerImages;
    private TextView tvImageCounter;
    private TextView tvPostTitleDetail;
    private TextView tvPostPriceDetail;
    private TextView tvPostAreaDetail;
    private TextView tvViewCount;
    private TextView tvPostDescriptionDetail;
    private TextView tvPriceValue;
    private TextView tvAreaValue;
    private TextView tvBedroomsValue;
    private TextView tvPostingDateDetail;
    private CircleImageView ivLandlordAvatar;
    private TextView tvLandlordName;
    private Button btnEditPostBottom;
    private ProgressBar progressBarDetail;
    private NestedScrollView scrollViewContent; // Để ẩn/hiện toàn bộ nội dung
    private Button btnViewFullMap; // Thêm biến cho nút mới
    private SupportMapFragment detailMapFragment;
    private GoogleMap detailMap;
    private LatLng postLatLng; // Lưu tọa độ của bài đăng

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String postId;
    private Post currentPost;
    private ImageSliderAdapter imageSliderAdapter;
    private List<String> postImageUrls = new ArrayList<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private ActivityResultLauncher<Intent> editPostLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        //-----------------------------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_landlord);
        // --- Xử lý WindowInsets cho PostDetailActivity ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_post_detail_landlord), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Chỉ áp padding top cho root layout để AppBar đẩy xuống
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            // NestedScrollView và Button đã có cơ chế riêng để tránh bottom bar
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currencyFormat.setMaximumFractionDigits(0); // Bỏ phần thập phân tiền tệ

        // Lấy postId từ Intent
        postId = getIntent().getStringExtra("POST_ID");
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "ID tin đăng không hợp lệ!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid Post ID received.");
            finish();
            return;
        }

        // Ánh xạ Views
        toolbar = findViewById(R.id.toolbarPostDetail);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        tvPostTitleDetail = findViewById(R.id.tvPostTitleDetail);
        tvPostPriceDetail = findViewById(R.id.tvPostPriceDetail);
        tvPostAreaDetail = findViewById(R.id.tvPostAreaDetail);
        tvViewCount = findViewById(R.id.tvViewCount);
        tvPostDescriptionDetail = findViewById(R.id.tvPostDescriptionDetail);
        tvPriceValue = findViewById(R.id.tvPriceValue);
        tvAreaValue = findViewById(R.id.tvAreaValue);
        tvBedroomsValue = findViewById(R.id.tvBedroomsValue);
        tvPostingDateDetail = findViewById(R.id.tvPostingDateDetail);
        ivLandlordAvatar = findViewById(R.id.ivLandlordAvatar);
        tvLandlordName = findViewById(R.id.tvLandlordName);
        btnEditPostBottom = findViewById(R.id.btnEditPostBottom);
        progressBarDetail = findViewById(R.id.progressBarDetail);
        scrollViewContent = findViewById(R.id.LandlordDetailScrollView);
        btnViewFullMap = findViewById(R.id.btnViewFullMap); // Thêm dòng này


        editPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Kiểm tra xem kết quả trả về có phải là RESULT_OK không
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Chỉnh sửa thành công, load lại dữ liệu!
                        Log.d(TAG, "Returned from EditPostActivity with RESULT_OK. Refreshing data.");
                        // Gọi lại hàm load dữ liệu chi tiết
                        if (postId != null && !postId.isEmpty()) {
                            loadPostDetails();
                        } else {
                            Log.e(TAG, "Post ID is null or empty after returning from edit. Cannot refresh.");
                            Toast.makeText(this, "Lỗi không thể tải lại dữ liệu sau khi sửa.", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng màn hình chi tiết nếu không có ID
                        }
                    } else {
                        // Người dùng hủy hoặc có lỗi bên EditPostActivity
                        Log.d(TAG, "Returned from EditPostActivity without RESULT_OK (Result Code: " + result.getResultCode() + ")");
                        // Không cần làm gì ở đây, dữ liệu cũ vẫn hiển thị
                    }
                }
        );

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Lấy SupportMapFragment
        detailMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDetailFragmentContainer);
        if (detailMapFragment != null) {
            detailMapFragment.getMapAsync(this::onMapReady); // Lấy map bất đồng bộ
        } else {
            Log.e(TAG, "Map fragment not found!");
            btnViewFullMap = findViewById(R.id.btnViewFullMap); // Cần ánh xạ để ẩn
            if(btnViewFullMap != null) btnViewFullMap.setVisibility(View.GONE);
        }

        // Ánh xạ và gán Listener cho nút xem bản đồ
        btnViewFullMap = findViewById(R.id.btnViewFullMap);
        btnViewFullMap.setOnClickListener(v -> openFullScreenMap());

        // Load dữ liệu
        loadPostDetails();

        // Nút chỉnh sửa dưới cùng
        btnEditPostBottom.setOnClickListener(v -> navigateToEdit());
    }

    private void loadPostDetails() {
        Log.d(TAG, "Loading details for post: " + postId);
        progressBarDetail.setVisibility(View.VISIBLE);
        // Ẩn các thành phần UI chính thay vì cả ScrollView
        viewPagerImages.setVisibility(View.GONE);
        //... ẩn các TextView, CardView khác ...
        btnEditPostBottom.setVisibility(View.GONE);


        DocumentReference postRef = db.collection("posts").document(postId);
        postRef.get().addOnCompleteListener(task -> {
            progressBarDetail.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    try {
                        currentPost = document.toObject(Post.class);
                        if (currentPost != null) {
                            currentPost.setPostId(document.getId()); // Đảm bảo postId được gán
                            populateUI();
                            // Hiện lại các thành phần UI
                            viewPagerImages.setVisibility(View.VISIBLE);
                            //... hiện các TextView, CardView khác ...
                            // Kiểm tra quyền sở hữu trước khi hiện nút Edit
                            if (currentUser != null && currentPost.getUserId() != null && currentUser.getUid().equals(currentPost.getUserId())) {
                                btnEditPostBottom.setVisibility(View.VISIBLE);
                            } else {
                                btnEditPostBottom.setVisibility(View.GONE); // Ẩn nếu không phải chủ sở hữu
                            }

                            // Load thông tin người đăng (Landlord)
                            if(currentPost.getUserId() != null) {
                                loadLandlordInfo(currentPost.getUserId());
                            } else {
                                tvLandlordName.setText("Người dùng ẩn danh");
                                ivLandlordAvatar.setImageResource(R.drawable.person_24px); // Ảnh mặc định
                            }

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
        });
    }

    private void populateUI() {
        if (currentPost == null) return;

        tvPostTitleDetail.setText(currentPost.getTitle());
        tvPostPriceDetail.setText(currencyFormat.format(currentPost.getPrice()) + "/tháng");
        tvPostAreaDetail.setText(String.format(Locale.US, "%.1fm²", currentPost.getArea())); // Format diện tích
        tvViewCount.setText(String.valueOf(currentPost.getViewCount()));
        tvPostDescriptionDetail.setText(currentPost.getDescription());

        tvPriceValue.setText(currencyFormat.format(currentPost.getPrice()) + "/tháng");
        tvAreaValue.setText(String.format(Locale.US, "%.1fm²", currentPost.getArea()));
        tvBedroomsValue.setText(String.valueOf(currentPost.getBedrooms()));

        if (currentPost.getTimestamp() != null) {
            tvPostingDateDetail.setText("Tin đăng ngày: " + dateFormat.format(currentPost.getTimestamp().toDate()));
        } else {
            tvPostingDateDetail.setText("Tin đăng ngày: N/A");
        }

        postImageUrls = currentPost.getImageUrls() != null ? currentPost.getImageUrls() : Collections.emptyList(); // Gán vào biến class
        if (postImageUrls.isEmpty()) {
            viewPagerImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        } else {
            viewPagerImages.setVisibility(View.VISIBLE);
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
        // Lưu tọa độ
        if (currentPost.getLatitude() != 0 || currentPost.getLongitude() != 0) {
            postLatLng = new LatLng(currentPost.getLatitude(), currentPost.getLongitude());
            btnViewFullMap.setVisibility(View.VISIBLE);
            findViewById(R.id.mapDetailFragmentContainer).setVisibility(View.VISIBLE); // Đảm bảo fragment hiển thị

            // Nếu map đã sẵn sàng, cập nhật ngay
            if (detailMap != null) {
                updateDetailMapMarker(postLatLng, currentPost.getTitle());
            }
            // Nếu map chưa sẵn sàng, onMapReady sẽ tự cập nhật sau
        } else {
            postLatLng = null;
            btnViewFullMap.setVisibility(View.GONE);
            Log.w(TAG,"Post has no valid coordinates (0,0). Hiding map.");
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

    // *** Implement phương thức của OnItemClickListener ***
    @Override
    public void onItemClick(int position) {
        if (postImageUrls != null && !postImageUrls.isEmpty()) {
            Intent intent = new Intent(this, FullScreenImageViewerActivity.class);
            // Chú ý: Truyền ArrayList<String>
            intent.putStringArrayListExtra(FullScreenImageViewerActivity.EXTRA_IMAGE_URLS, new ArrayList<>(postImageUrls));
            intent.putExtra(FullScreenImageViewerActivity.EXTRA_START_POSITION, position);
            startActivity(intent);
        }
    }

    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Detail Map Ready");
        detailMap = googleMap;
        // Cấu hình UI cho bản đồ nhỏ (ít tương tác)
        detailMap.getUiSettings().setZoomControlsEnabled(false);
        detailMap.getUiSettings().setScrollGesturesEnabled(false);
        detailMap.getUiSettings().setZoomGesturesEnabled(false);
        detailMap.getUiSettings().setMapToolbarEnabled(false); // Tắt nút mở Google Maps mặc định

        // Nếu tọa độ đã có khi map sẵn sàng, cập nhật marker
        if (postLatLng != null) {
            updateDetailMapMarker(postLatLng, currentPost != null ? currentPost.getTitle() : "Vị trí");
        }
    }

    // Hàm cập nhật marker và camera cho bản đồ nhỏ
    private void updateDetailMapMarker(LatLng latLng, String title) {
        if (detailMap == null || latLng == null) return;
        Log.d(TAG, "Updating detail map marker at: " + latLng.latitude + "," + latLng.longitude);
        detailMap.clear(); // Xóa marker cũ (nếu có)
        detailMap.addMarker(new MarkerOptions().position(latLng).title(title));
        detailMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)); // Zoom vừa phải
    }

    // Hàm mở Activity bản đồ toàn màn hình
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

    private void loadLandlordInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Giả sử bạn có model User với trường 'name' và 'avatarUrl'
                        User landlord = documentSnapshot.toObject(User.class);
                        if (landlord != null) {
                            tvLandlordName.setText(landlord.getName() != null ? landlord.getName() : "Chủ nhà");
                            // Load ảnh đại diện bằng Glide
                            if (landlord.getAvatarUrl() != null && !landlord.getAvatarUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(landlord.getAvatarUrl())
                                        .placeholder(R.drawable.person_24px)
                                        .error(R.drawable.person_24px)
                                        .into(ivLandlordAvatar);
                            } else {
                                ivLandlordAvatar.setImageResource(R.drawable.person_24px);
                            }
                        } else {
                            setDefaultLandlordInfo();
                        }
                    } else {
                        setDefaultLandlordInfo();
                        Log.d(TAG, "Landlord user document not found: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    setDefaultLandlordInfo();
                    Log.e(TAG, "Error loading landlord info for user: " + userId, e);
                });
    }

    private void setDefaultLandlordInfo(){
        tvLandlordName.setText("Chủ nhà");
        ivLandlordAvatar.setImageResource(R.drawable.person_24px);
    }

    private void handleLoadError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Có thể finish activity nếu lỗi nghiêm trọng
        // finish();
    }

    // --- Menu trên Toolbar ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Chỉ hiển thị menu sửa nếu là chủ sở hữu tin
        if (currentUser != null && currentPost != null && currentPost.getUserId() != null && currentUser.getUid().equals(currentPost.getUserId())) {
            getMenuInflater().inflate(R.menu.post_detail_landlord_menu, menu);
            //ẩn nút sửa bottom
            btnEditPostBottom.setVisibility(View.VISIBLE);
            return true;
        }
        btnEditPostBottom.setVisibility(View.GONE); // Ẩn nút dưới nếu ko phải chủ
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit_post) {
            navigateToEdit(); // Gọi hàm đã được sửa đổi
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Navigation ---
    private void navigateToEdit() {
        if (currentPost != null && postId != null && !postId.isEmpty()) { // Thêm kiểm tra postId nữa cho chắc
            Intent intent = new Intent(PostDetailLandlordActivity.this, EditPostActivity.class);
            intent.putExtra("POST_ID", postId); // Truyền postId

            // *** SỬ DỤNG LAUNCHER ĐỂ BẮT KẾT QUẢ ***
            Log.d(TAG, "Launching EditPostActivity for postId: " + postId);
            editPostLauncher.launch(intent);
            // startActivity(intent); // Bỏ dòng này
            // ***************************************

        } else {
            Toast.makeText(this, "Không thể lấy thông tin tin đăng để sửa.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot navigate to edit: currentPost or postId is null/empty.");
        }
    }

}