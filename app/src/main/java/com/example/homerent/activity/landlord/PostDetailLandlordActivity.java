package com.example.homerent.activity.landlord;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.homerent.R; // Thay R
//import com.example.homerent.activity.landlord.EditPostActivity;
import com.example.homerent.adapter.ImageSliderAdapter;
import com.example.homerent.model.Post;
//import com.example.homerent.model.User; // Model User nếu có
import com.example.homerent.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailLandlordActivity extends AppCompatActivity {

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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String postId;
    private Post currentPost;
    private ImageSliderAdapter imageSliderAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_landlord);

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
        scrollViewContent = findViewById(R.id.LandlorđetailscrollView);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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

        // Setup ViewPager2
        List<String> images = currentPost.getImageUrls() != null ? currentPost.getImageUrls() : Collections.emptyList();
        if (images.isEmpty()) {
            // Có thể ẩn ViewPager hoặc hiển thị ảnh mặc định
            viewPagerImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        } else {
            viewPagerImages.setVisibility(View.VISIBLE);
            tvImageCounter.setVisibility(View.VISIBLE);
            imageSliderAdapter = new ImageSliderAdapter(this, images);
            viewPagerImages.setAdapter(imageSliderAdapter);
            updateImageCounter(images.size()); // Cập nhật counter ban đầu
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateImageCounter(images.size());
                }
            });
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
            onBackPressed(); // Xử lý nút back trên toolbar
            return true;
        } else if (id == R.id.action_edit_post) {
            navigateToEdit();
            return true;
        }
        // Thêm xử lý cho item "more options" nếu có
        return super.onOptionsItemSelected(item);
    }

    // --- Navigation ---
    private void navigateToEdit() {
        if (currentPost != null) {
//            Intent intent = new Intent(PostDetailLandlordActivity.this, EditPostActivity.class);
//            intent.putExtra("POST_ID", currentPost.getPostId());
//            startActivity(intent);
        } else {
            Toast.makeText(this, "Không thể lấy thông tin tin đăng để sửa.", Toast.LENGTH_SHORT).show();
        }
    }

    // Model User (Ví dụ - bạn cần tạo model này nếu chưa có)
    // package com.example.homerent.model;
    // public class User {
    //     private String name;
    //     private String avatarUrl;
    //     // Các trường khác...
    //     public User() {}
    //     public String getName() { return name; }
    //     public void setName(String name) { this.name = name; }
    //     public String getAvatarUrl() { return avatarUrl; }
    //     public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    // }
}