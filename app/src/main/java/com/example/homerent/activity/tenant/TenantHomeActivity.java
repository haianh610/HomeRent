package com.example.homerent.activity.tenant;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.homerent.R;
import com.example.homerent.fragment.tenant.PostViewFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TenantHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationViewTenant);

        // Load fragment mặc định (Trang chủ)
        if (savedInstanceState == null) {
            loadFragment(new PostViewFragment());
        }

        // Xử lý sự kiện chuyển tab
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = new PostViewFragment();
            } else if (itemId == R.id.navigation_saved_posts) {
                // Placeholder cho SavedPostsFragment (chưa triển khai)
                // Tạm thời giữ liên kết, bạn sẽ thêm sau
                return false;
            } else if (itemId == R.id.navigation_account) {
                // Placeholder cho TenantAccountFragment (chưa triển khai)
                // Tạm thời giữ liên kết, bạn sẽ thêm sau
                return false;
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerTenant, fragment)
                .commit();
    }
}