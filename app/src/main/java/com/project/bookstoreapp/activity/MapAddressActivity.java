package com.project.bookstoreapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.project.bookstoreapp.R;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvSelectedAddress;
    private MaterialButton btnConfirmAddress;
    private CardView cvSearchBar;
    private String currentAddress = "";

    // Vị trí hiện tại
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Laucher cho Places Autocomplete
    private ActivityResultLauncher<Intent> autocompleteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_address);

        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnConfirmAddress = findViewById(R.id.btnConfirmAddress);
        cvSearchBar = findViewById(R.id.cvSearchBar);
        MaterialToolbar toolbar = findViewById(R.id.toolbarMap);

        toolbar.setNavigationOnClickListener(v -> finish());

        // 1. Khởi tạo Places SDK cho phần Gợi ý tìm kiếm
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyApfnG8yX_JdqmnkUV4ycnxBxJV-2auQZY");
        }

        // 2. Khởi tạo bộ định vị vị trí hiện tại
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Bắt sự kiện bấm vào thanh Tìm kiếm
        setupAutocomplete();

        // Sự kiện nút Xác nhận
        btnConfirmAddress.setOnClickListener(v -> {
            if (currentAddress.isEmpty() || currentAddress.equals("Đang tải vị trí...") || currentAddress.equals("Không tìm thấy địa chỉ")) {
                Toast.makeText(this, "Vui lòng chọn một địa chỉ hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_ADDRESS", currentAddress);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupAutocomplete() {
        autocompleteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        // Nhảy bản đồ đến vị trí người dùng vừa chọn từ gợi ý
                        if (place.getLatLng() != null && mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
                        }
                    }
                }
        );

        cvSearchBar.setOnClickListener(v -> {
            // Yêu cầu Google trả về ID, Tên, Tọa độ và Địa chỉ cụ thể
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
            // Mở giao diện Gợi ý tìm kiếm (Overlay phủ lên màn hình)
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("VN") // Giới hạn gợi ý ở Việt Nam
                    .build(this);
            autocompleteLauncher.launch(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Xin quyền vị trí hiện tại
        enableUserLocation();

        // Bắt sự kiện khi người dùng DỪNG kéo bản đồ -> Dịch tọa độ thành Tên đường
        mMap.setOnCameraIdleListener(() -> {
            LatLng centerLatLng = mMap.getCameraPosition().target;
            getAddressFromLatLng(centerLatLng);
        });
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Hiển thị chấm xanh và nút "My Location" góc phải trên
            mMap.setMyLocationEnabled(true);

            // Lấy vị trí hiện tại và zoom tới đó
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                } else {
                    // Mặc định TP.HCM nếu không lấy được
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.762622, 106.660172), 15f));
                }
            });
        } else {
            // Chưa có quyền -> Yêu cầu cấp quyền
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            // Mặc định TP.HCM trong lúc chờ cấp quyền
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.762622, 106.660172), 15f));
        }
    }

    // Lắng nghe kết quả khi người dùng bấm "Cho phép" hoặc "Từ chối" quyền Vị trí
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Không có quyền vị trí, sẽ hiển thị mặc định.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hàm dịch Tọa độ -> Chuỗi văn bản địa chỉ
    private void getAddressFromLatLng(LatLng latLng) {
        tvSelectedAddress.setText("Đang tải vị trí...");
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                currentAddress = addresses.get(0).getAddressLine(0);
                tvSelectedAddress.setText(currentAddress);
            } else {
                currentAddress = "Không tìm thấy địa chỉ";
                tvSelectedAddress.setText(currentAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
            currentAddress = "Lỗi kết nối dịch vụ bản đồ";
            tvSelectedAddress.setText(currentAddress);
        }
    }
}