package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.project.bookstoreapp.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvSelectedAddress;
    private MaterialButton btnConfirmAddress;
    private String currentAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_address);

        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnConfirmAddress = findViewById(R.id.btnConfirmAddress);
        MaterialToolbar toolbar = findViewById(R.id.toolbarMap);

        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Sự kiện nút Xác nhận
        btnConfirmAddress.setOnClickListener(v -> {
            if (currentAddress.isEmpty() || currentAddress.equals("Đang tải vị trí...") || currentAddress.equals("Không tìm thấy địa chỉ")) {
                Toast.makeText(this, "Vui lòng chọn một địa chỉ hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Trả kết quả về cho Activity trước đó
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_ADDRESS", currentAddress);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Vị trí mặc định (Ví dụ: Trung tâm TP.HCM)
        LatLng defaultLocation = new LatLng(10.762622, 106.660172);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

        // Bắt sự kiện khi người dùng DỪNG kéo bản đồ
        mMap.setOnCameraIdleListener(() -> {
            // Lấy tọa độ ở chính giữa màn hình
            LatLng centerLatLng = mMap.getCameraPosition().target;
            getAddressFromLatLng(centerLatLng);
        });
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