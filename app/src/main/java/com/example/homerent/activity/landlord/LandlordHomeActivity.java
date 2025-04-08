package com.example.homerent.activity.landlord; // Thay đổi package

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.homerent.R; // Thay R
import com.example.homerent.fragment.landlord.AccountFragment;
import com.example.homerent.fragment.landlord.PostManagementFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView; // Quan trọng: Import đúng

public class LandlordHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_home);

        EdgeToEdge.enable(this); // Bật chế độ Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_landlord_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bottomNavigationView = findViewById(R.id.bottomNavigationViewLandlord);

        // Load fragment mặc định khi Activity khởi tạo
        if (savedInstanceState == null) {
            loadFragment(new PostManagementFragment());
            // Optional: Set title cho toolbar nếu dùng
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Tin đăng");
            }
        }


        // Xử lý sự kiện khi chọn item trên BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String title = getString(R.string.app_name); // Title mặc định

                int itemId = item.getItemId();
                if (itemId == R.id.navigation_posts) {
                    selectedFragment = new PostManagementFragment();
                    title = "Tin đăng";
                } else if (itemId == R.id.navigation_account) {
                    selectedFragment = new AccountFragment();
                    title = "Tài khoản";
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    // Optional: Cập nhật title Toolbar
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(title);
                    }
                    return true; // Return true để hiển thị item được chọn
                }
                return false;
            }
        });

        // Optional: Đặt item được chọn mặc định (nếu cần)
        // bottomNavigationView.setSelectedItemId(R.id.navigation_posts);
    }

    // Hàm để thay thế Fragment trong container
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerLandlord, fragment);
        // fragmentTransaction.addToBackStack(null); // Optional: Thêm vào back stack nếu muốn nút back quay lại fragment trước đó
        fragmentTransaction.commit();
    }

    // Optional: Hàm để điều hướng ra màn hình Login nếu cần (gọi từ fragment)
    // public void navigateToLogin() {
    //     Intent intent = new Intent(this, LoginActivity.class);
    //     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    //     startActivity(intent);
    //     finish();
    // }
}