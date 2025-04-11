package com.example.homerent.activity.tenant;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
// Import Toolbar
import androidx.appcompat.widget.Toolbar;
// Import SearchView
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Thêm import này
import androidx.fragment.app.FragmentTransaction; // Thêm import này

import com.example.homerent.R;
import com.example.homerent.fragment.tenant.PostViewFragment;
import com.example.homerent.fragment.tenant.SavedPostsFragment; // Giả sử bạn sẽ tạo Fragment này
import com.example.homerent.fragment.tenant.TenantAccountFragment; // Giả sử bạn sẽ tạo Fragment này
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TenantHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private String currentFragmentTag = ""; // Để biết Fragment nào đang hiển thị

    // Interface để Fragment lắng nghe sự kiện tìm kiếm
    public interface SearchableFragment {
        void onSearchQuery(String query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        toolbar = findViewById(R.id.toolbarTenantHome);
        setSupportActionBar(toolbar); // Đặt Toolbar làm ActionBar

        bottomNavigationView = findViewById(R.id.bottomNavigationViewTenant);

        if (savedInstanceState == null) {
            loadFragment(PostViewFragment.newInstance(), PostViewFragment.TAG); // Load fragment mặc định với tag
            toolbar.setTitle("Trang chủ"); // Set title ban đầu
        } else {
            // Khôi phục fragment nếu activity bị destroy và recreate
            currentFragmentTag = savedInstanceState.getString("CURRENT_FRAGMENT_TAG", PostViewFragment.TAG);
            Fragment restoredFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
            if (restoredFragment != null) {
                // Nếu fragment đã tồn tại, chỉ cần cập nhật title
                updateToolbarTitle(currentFragmentTag);
            } else {
                // Nếu không tìm thấy, load lại fragment mặc định
                loadFragment(PostViewFragment.newInstance(), PostViewFragment.TAG);
                toolbar.setTitle("Trang chủ");
            }
        }


        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String selectedTag = "";
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                if (!currentFragmentTag.equals(PostViewFragment.TAG)) {
                    selectedFragment = PostViewFragment.newInstance();
                    selectedTag = PostViewFragment.TAG;
                }
            } else if (itemId == R.id.navigation_saved_posts) {
                if (!currentFragmentTag.equals(SavedPostsFragment.TAG)) {
                    selectedFragment = SavedPostsFragment.newInstance();
                    selectedTag = SavedPostsFragment.TAG;
                }
            } else if (itemId == R.id.navigation_account) {
                if (!currentFragmentTag.equals(TenantAccountFragment.TAG)) {
                    // --- Sửa chỗ này ---
                    selectedFragment = TenantAccountFragment.newInstance(); // Tạo fragment tài khoản
                    selectedTag = TenantAccountFragment.TAG;
                    // --- Hết sửa ---
                    // Không cần Toast nữa
                    // Toast.makeText(this, "Chức năng Tài khoản chưa có", Toast.LENGTH_SHORT).show();
                    // return false; // Bỏ return false ở đây
                }
            }

            // --- Phần code xử lý transaction giữ nguyên như trước ---
            if (selectedFragment != null && !selectedTag.isEmpty()) {
                Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(selectedTag);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                Fragment currentFrag = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                if (currentFrag != null && currentFrag.isAdded()) { // Kiểm tra isAdded
                    transaction.hide(currentFrag);
                }

                if (existingFragment == null) {
                    transaction.add(R.id.fragmentContainerTenant, selectedFragment, selectedTag);
                } else {
                    transaction.show(existingFragment);
                }
                // Sử dụng commitAllowingStateLoss() nếu gặp lỗi IllegalStateException khi commit sau onSaveInstanceState
                // transaction.commit();
                transaction.commitAllowingStateLoss();

                currentFragmentTag = selectedTag;
                updateToolbarTitle(currentFragmentTag);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        // Kiểm tra nếu fragment đã tồn tại thì không load lại
        if (getSupportFragmentManager().findFragmentByTag(tag) != null) {
            getSupportFragmentManager().beginTransaction()
                    .show(getSupportFragmentManager().findFragmentByTag(tag))
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerTenant, fragment, tag)
                    .commit();
        }
        // Ẩn các fragment khác (nếu có)
        hideOtherFragments(tag);
        currentFragmentTag = tag; // Cập nhật tag của fragment hiện tại
    }

    // Hàm ẩn các fragment không phải là fragment đang được hiển thị
    private void hideOtherFragments(String visibleTag) {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment frag : fm.getFragments()) {
            if (frag != null && !frag.getTag().equals(visibleTag)) {
                fm.beginTransaction().hide(frag).commit();
            }
        }
    }

    // Cập nhật title toolbar dựa trên tag
    public void updateToolbarTitle(String tag) {
        if (PostViewFragment.TAG.equals(tag)) {
            toolbar.setTitle("Trang chủ");
        } else if (SavedPostsFragment.TAG.equals(tag)) {
            toolbar.setTitle("Tin đã lưu");
        } else if (TenantAccountFragment.TAG.equals(tag)) {
            toolbar.setTitle("Tài khoản"); // Đảm bảo có dòng này
        } else {
            toolbar.setTitle(getString(R.string.app_name)); // Title mặc định
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu tag của fragment hiện tại
        outState.putString("CURRENT_FRAGMENT_TAG", currentFragmentTag);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tenant_home_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setQueryHint("Tìm kiếm tin đăng...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // Xử lý khi người dùng nhấn enter/search trên bàn phím
                    performSearch(query);
                    searchView.clearFocus(); // Ẩn bàn phím
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Xử lý khi nội dung text thay đổi (tìm kiếm real-time)
                    performSearch(newText);
                    return true;
                }
            });
        } else {
            Log.e("TenantHomeActivity", "SearchView is null!");
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            // Đã xử lý bằng SearchView, không cần làm gì thêm ở đây
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Hàm gửi query tìm kiếm đến Fragment hiện tại
    private void performSearch(String query) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        // Chỉ gửi query đến fragment nếu nó implement SearchableFragment
        if (currentFragment instanceof SearchableFragment) {
            ((SearchableFragment) currentFragment).onSearchQuery(query);
        } else {
            // Nếu fragment hiện tại không tìm kiếm được (vd: Account), có thể thông báo
            // Toast.makeText(this, "Tìm kiếm không áp dụng cho màn hình này", Toast.LENGTH_SHORT).show();
        }
    }
}