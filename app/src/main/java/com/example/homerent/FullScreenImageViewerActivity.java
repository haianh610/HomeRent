package com.example.homerent; // Hoặc package phù hợp

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.homerent.R;
import com.example.homerent.adapter.FullScreenImageAdapter;

import java.util.ArrayList;
import java.util.List;

public class FullScreenImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS = "image_urls";
    public static final String EXTRA_START_POSITION = "start_position";

    private ViewPager2 viewPager;
    private TextView tvCounter;
    private ImageButton btnClose;
    private List<String> imageUrls;
    private int startPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- Edge to Edge ---
        // Bỏ Action Bar/Title Bar mặc định
        // requestWindowFeature(Window.FEATURE_NO_TITLE); // Có thể không cần nếu dùng Theme.NoActionBar
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // Làm full screen cũ
        EdgeToEdge.enable(this); // Bật Edge-to-Edge mới
        // -------------------

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image_viewer);

        // --- Xử lý WindowInsets cho Edge-to-Edge ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fullscreen_content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Áp padding cho container chính để nội dung không bị che lấp hoàn toàn
            // Hoặc chỉ áp padding cho các nút điều khiển như close, counter
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Nếu muốn ViewPager tràn hoàn toàn thì không set padding cho v mà set cho counter/button
            // FrameLayout.LayoutParams counterParams = (FrameLayout.LayoutParams) tvCounter.getLayoutParams();
            // counterParams.bottomMargin = systemBars.bottom + initialMarginBottom; // Điều chỉnh margin
            // tvCounter.setLayoutParams(counterParams);
            // Tương tự cho button close với top và right margin/padding
            return insets;
        });
        // -----------------------------------------

        viewPager = findViewById(R.id.viewPagerFullScreen);
        tvCounter = findViewById(R.id.tvImageCounterFullScreen);
        btnClose = findViewById(R.id.buttonCloseFullScreen);

        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            Toast.makeText(this, "Không có ảnh để hiển thị", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FullScreenImageAdapter adapter = new FullScreenImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false); // Chuyển đến ảnh được click ban đầu

        updateCounter(startPosition + 1, imageUrls.size()); // Cập nhật counter ban đầu

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position + 1, imageUrls.size());
            }
        });

        btnClose.setOnClickListener(v -> finish()); // Đóng activity khi nhấn nút close
    }

    private void updateCounter(int current, int total) {
        if (total > 0) {
            tvCounter.setText(String.format("%d / %d", current, total));
            tvCounter.setVisibility(View.VISIBLE);
        } else {
            tvCounter.setVisibility(View.GONE);
        }
    }
}