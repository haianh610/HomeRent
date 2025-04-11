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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailTenantActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailTenantAct";

    private Toolbar toolbar;
    private ViewPager2 viewPagerImages;
    private TextView tvImageCounter;
    private TextView tvPostTitleDetail;
    private TextView tvPostPriceDetail;
    private TextView tvPostAreaDetail;
    private TextView tvFullAddress; // Thay cho view count card landlord
    private TextView tvPostDescriptionDetail;
    private TextView tvPriceValue;
    private TextView tvAreaValue;
    private TextView tvBedroomsValue;
    private TextView tvFloorsValue; // Thêm nếu có
    private TextView tvPostingDateDetail;
    private CircleImageView ivLandlordAvatar;
    private TextView tvLandlordName;
    private Button btnCallLandlord;
    private ProgressBar progressBarDetail;
    private NestedScrollView scrollViewContent;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_tenant);

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
        tvFullAddress = findViewById(R.id.tvFullAddressTenant); // TextView mới cho địa chỉ
        tvPostDescriptionDetail = findViewById(R.id.tvPostDescriptionDetailTenant);
        tvPriceValue = findViewById(R.id.tvPriceValueTenant);
        tvAreaValue = findViewById(R.id.tvAreaValueTenant);
        tvBedroomsValue = findViewById(R.id.tvBedroomsValueTenant);
        tvFloorsValue = findViewById(R.id.tvFloorsValueTenant); // Bind nếu có
        tvPostingDateDetail = findViewById(R.id.tvPostingDateDetailTenant);
        ivLandlordAvatar = findViewById(R.id.ivLandlordAvatarTenant);
        tvLandlordName = findViewById(R.id.tvLandlordNameTenant);
        btnCallLandlord = findViewById(R.id.btnCallLandlord);
        progressBarDetail = findViewById(R.id.progressBarDetailTenant);
        scrollViewContent = findViewById(R.id.tenantDetailScrollView);
        // Không cần bind btnEditPostBottom
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Chi tiết tin"); // Set title nếu muốn
        }
        // Listener cho nút back trên toolbar
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupButtonClickListeners() {
        btnCallLandlord.setOnClickListener(v -> {
            if (landlordInfo != null && landlordInfo.getPhoneNumber() != null && !landlordInfo.getPhoneNumber().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL); // Mở màn hình gọi điện
                intent.setData(Uri.parse("tel:" + landlordInfo.getPhoneNumber()));
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(PostDetailTenantActivity.this, "Không thể mở ứng dụng gọi điện.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Không có thông tin số điện thoại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostDetails() {
        Log.d(TAG, "Loading details for post: " + postId);
        progressBarDetail.setVisibility(View.VISIBLE);
        scrollViewContent.setVisibility(View.GONE); // Ẩn nội dung khi load
        btnCallLandlord.setVisibility(View.GONE);

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
                                    btnCallLandlord.setVisibility(View.GONE); // Không có chủ thì ẩn nút gọi
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
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", ""); // Dọn dẹp dấu phẩy thừa
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
        List<String> images = currentPost.getImageUrls() != null ? currentPost.getImageUrls() : Collections.emptyList();
        if (images.isEmpty()) {
            viewPagerImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        } else {
            viewPagerImages.setVisibility(View.VISIBLE);
            tvImageCounter.setVisibility(View.VISIBLE);
            imageSliderAdapter = new ImageSliderAdapter(this, images);
            viewPagerImages.setAdapter(imageSliderAdapter);
            updateImageCounter(images.size());
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateImageCounter(images.size());
                }
            });
        }
    }

    // Hàm helper để tránh lỗi NullPointerException khi ghép chuỗi địa chỉ
    private String notNull(String value) {
        return value != null ? value : "";
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
                                // Hiển thị nút gọi nếu có SĐT
                                if (landlordInfo.getPhoneNumber() != null && !landlordInfo.getPhoneNumber().trim().isEmpty()){
                                    btnCallLandlord.setText("Gọi: " + landlordInfo.getPhoneNumber()); // Hiện SĐT trên nút
                                    btnCallLandlord.setVisibility(View.VISIBLE);
                                } else {
                                    btnCallLandlord.setVisibility(View.GONE);
                                }
                            } else {
                                setDefaultLandlordInfo();
                                btnCallLandlord.setVisibility(View.GONE);
                            }
                        } else {
                            setDefaultLandlordInfo();
                            btnCallLandlord.setVisibility(View.GONE);
                            Log.d(TAG, "Landlord user document not found: " + userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed() && !isFinishing()){
                        setDefaultLandlordInfo();
                        btnCallLandlord.setVisibility(View.GONE);
                        Log.e(TAG, "Error loading landlord info for user: " + userId, e);
                    }
                });
    }

    private void setDefaultLandlordInfo() {
        tvLandlordName.setText("Chủ nhà (Ẩn danh)");
        ivLandlordAvatar.setImageResource(R.drawable.person_24px);
    }

    private void handleLoadError(String message) {
        if (!isDestroyed() && !isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            scrollViewContent.setVisibility(View.GONE);
            btnCallLandlord.setVisibility(View.GONE);
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