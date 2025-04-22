package com.example.homerent; // Hoặc package phù hợp

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.homerent.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FullScreenMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "FullScreenMapActivity";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private GoogleMap fullMap;
    private SupportMapFragment fullMapFragment;
    private FloatingActionButton fabOpenInMaps;
    private ImageButton btnBack;

    private LatLng locationLatLng;
    private String locationAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this); // Bật Edge-to-Edge
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_map);

        // Xử lý WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_full_screen_map), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Áp margin cho các nút thay vì padding cho root
            // Nút Back
            ImageButton backButton = findViewById(R.id.btnBackFullScreenMap);
            ViewGroup.MarginLayoutParams backParams = (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
            backParams.topMargin = systemBars.top + (int) (16 * getResources().getDisplayMetrics().density); // 16dp margin top + status bar
            backParams.leftMargin = systemBars.left + (int) (16 * getResources().getDisplayMetrics().density); // 16dp margin left + nav bar (nếu ở bên trái)
            backButton.setLayoutParams(backParams);

            // Nút FAB
            FloatingActionButton fab = findViewById(R.id.fabOpenInMaps);
            ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabParams.bottomMargin = systemBars.bottom + (int) (16 * getResources().getDisplayMetrics().density);
            fabParams.rightMargin = systemBars.right + (int) (16 * getResources().getDisplayMetrics().density);
            fab.setLayoutParams(fabParams);

            // Trả về insets gốc để hệ thống xử lý padding cho các thành phần khác nếu cần
            return insets;
        });


        // Lấy dữ liệu từ Intent
        double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
        locationAddress = getIntent().getStringExtra(EXTRA_ADDRESS);

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this, "Tọa độ không hợp lệ.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Received invalid coordinates (0,0).");
            finish();
            return;
        }
        locationLatLng = new LatLng(latitude, longitude);
        if (locationAddress == null) {
            locationAddress = "Vị trí đã chọn"; // Default title
        }

        // Ánh xạ Views
        fabOpenInMaps = findViewById(R.id.fabOpenInMaps);
        btnBack = findViewById(R.id.btnBackFullScreenMap);

        // Lấy Map Fragment
        fullMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fullScreenMapFragmentContainer);
        if (fullMapFragment != null) {
            fullMapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Full screen map fragment not found!");
            finish();
        }

        // Set Listeners
        btnBack.setOnClickListener(v -> onBackPressed()); // Hoặc finish()
        fabOpenInMaps.setOnClickListener(v -> openInGoogleMapsApp());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Full Screen Map Ready");
        fullMap = googleMap;

        // Cho phép tương tác đầy đủ
        fullMap.getUiSettings().setZoomControlsEnabled(true);
        fullMap.getUiSettings().setCompassEnabled(true);
        fullMap.getUiSettings().setMyLocationButtonEnabled(true); // Cần xin quyền vị trí
        fullMap.getUiSettings().setMapToolbarEnabled(true); // Cho phép nút mặc định

        // Thêm marker và di chuyển camera
        if (locationLatLng != null) {
            fullMap.addMarker(new MarkerOptions().position(locationLatLng).title(locationAddress));
            fullMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 16f)); // Zoom gần hơn
        }
    }

    private void openInGoogleMapsApp() {
        if (locationLatLng == null) return;

        // Tạo Uri geo: lat,lng ? q=address (q=address giúp hiển thị marker đúng hơn)
        Uri gmmIntentUri = Uri.parse("geo:" + locationLatLng.latitude + "," + locationLatLng.longitude + "?q=" + Uri.encode(locationAddress));
        // Hoặc chỉ dùng tọa độ: Uri.parse("geo:" + locationLatLng.latitude + "," + locationLatLng.longitude)

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Chỉ định mở bằng Google Maps

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Ứng dụng Google Maps chưa được cài đặt.", Toast.LENGTH_SHORT).show();
            // Có thể thử mở bằng trình duyệt nếu không có Google Maps
            // Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + locationLatLng.latitude + "," + locationLatLng.longitude));
            // startActivity(webIntent);
        }
    }
}