package com.example.homerent.activity.landlord;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.homerent.ChangeAccountActivity;
import com.example.homerent.R;
import com.example.homerent.fragment.landlord.FragmentAccount;
import com.example.homerent.fragment.landlord.PostManagementFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.IOException;

public class LandlordHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    public static final int MY_REQUEST_CODE = 10;

    // Khởi tạo FragmentChangeAccount để truy cập được setImageUri() và setBitmapImage()
    private ChangeAccountActivity fragmentChangeAccount;

    // Xử lý chọn ảnh từ gallery
    private final ActivityResultLauncher<Intent> mActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (fragmentChangeAccount != null) {
                            fragmentChangeAccount.setImageUri(uri);
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                                fragmentChangeAccount.setBitmapImage(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_home);

        // Edge to edge layout
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_landlord_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationViewLandlord);

        // Load fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new PostManagementFragment());
        }

        // Xử lý chuyển tab
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_posts) {
                    selectedFragment = new PostManagementFragment();
                } else if (itemId == R.id.navigation_account) {
                    selectedFragment = new FragmentAccount();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    // Hàm load fragment
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainerLandlord, fragment);
        transaction.commit();
    }

    // Mở gallery chọn ảnh
    public void openGallery(ChangeAccountActivity fragment) {
        this.fragmentChangeAccount = fragment; // Lưu lại instance đang dùng
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        mActivityResultLauncher.launch(Intent.createChooser(intent, "Chọn ảnh đại diện"));
    }

    // Cấp quyền READ_EXTERNAL_STORAGE
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (fragmentChangeAccount != null) {
                    openGallery(fragmentChangeAccount);
                }
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hàm để FragmentAccount cập nhật lại thông tin sau khi chỉnh sửa
    public void reloadUserInfo() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerLandlord);
        if (currentFragment instanceof FragmentAccount) {
            ((FragmentAccount) currentFragment).showUserInformation();
        }
    }
}
