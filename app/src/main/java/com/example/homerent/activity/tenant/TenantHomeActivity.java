package com.example.homerent.activity.tenant;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
// Import Toolbar
import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
// Import SearchView
import com.google.android.material.search.SearchView; // Import M3 SearchView
// Import SearchBar
import com.google.android.material.search.SearchBar; // Import M3 SearchBar

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Thêm import này
import androidx.fragment.app.FragmentTransaction; // Thêm import này

import com.example.homerent.AddressUtils;
import com.example.homerent.R;
import com.example.homerent.fragment.tenant.PostViewFragment;
import com.example.homerent.fragment.tenant.SavedPostsFragment; // Giả sử bạn sẽ tạo Fragment này
import com.example.homerent.fragment.tenant.TenantAccountFragment; // Giả sử bạn sẽ tạo Fragment này
import com.example.homerent.model.Province;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TenantHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String currentFragmentTag = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_tenant_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationViewTenant);

        if (savedInstanceState == null) {
            loadFragment(PostViewFragment.newInstance(), PostViewFragment.TAG);
        } else {
            // Restore logic - simpler now, just restore tag
            currentFragmentTag = savedInstanceState.getString("CURRENT_FRAGMENT_TAG", PostViewFragment.TAG);
            // FragmentManager handles restoring the actual fragment instance if possible
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String selectedTag = "";
            int itemId = item.getItemId();
            boolean shouldLoad = false; // Flag to check if we need to perform transaction

            if (itemId == R.id.navigation_home) {
                if (!currentFragmentTag.equals(PostViewFragment.TAG)) {
                    selectedTag = PostViewFragment.TAG;
                    shouldLoad = true;
                }
            } else if (itemId == R.id.navigation_saved_posts) {
                if (!currentFragmentTag.equals(SavedPostsFragment.TAG)) {
                    selectedTag = SavedPostsFragment.TAG;
                    shouldLoad = true;
                }
            } else if (itemId == R.id.navigation_account) {
                if (!currentFragmentTag.equals(TenantAccountFragment.TAG)) {
                    selectedTag = TenantAccountFragment.TAG;
                    shouldLoad = true;
                }
            }

            if (shouldLoad) {
                Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(selectedTag);
                if (existingFragment == null) {
                    // Create new instance based on tag
                    if (PostViewFragment.TAG.equals(selectedTag)) selectedFragment = PostViewFragment.newInstance();
                    else if (SavedPostsFragment.TAG.equals(selectedTag)) selectedFragment = SavedPostsFragment.newInstance();
                    else if (TenantAccountFragment.TAG.equals(selectedTag)) selectedFragment = TenantAccountFragment.newInstance();
                } else {
                    selectedFragment = existingFragment; // Use existing if found
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, selectedTag);
                    return true;
                }
            }
            return false; // Return false if the same item is tapped again
        });
    }

    // Add this public getter method
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    // Modified loadFragment to handle hide/show/add correctly
    private void loadFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // Hide the current fragment if it exists and is added
        Fragment currentFrag = fm.findFragmentByTag(currentFragmentTag);
        if (currentFrag != null && currentFrag.isAdded()) {
            transaction.hide(currentFrag);
        }

        // Check if the target fragment already exists
        Fragment targetFragment = fm.findFragmentByTag(tag);
        if (targetFragment == null) {
            // Add the fragment if it doesn't exist
            transaction.add(R.id.fragmentContainerTenant, fragment, tag);
            Log.d("TenantHome", "Adding fragment with tag: " + tag);
        } else {
            // Show the fragment if it exists
            transaction.show(targetFragment);
            Log.d("TenantHome", "Showing fragment with tag: " + tag);
        }

        transaction.setReorderingAllowed(true); // Best practice
        transaction.commitAllowingStateLoss(); // Use if needed
        currentFragmentTag = tag; // Update the current tag
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("CURRENT_FRAGMENT_TAG", currentFragmentTag);
    }

    // Remove menu methods as Toolbar is gone
    // Remove performSearch, updateSearchBarText
}