package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.OrderAdapter;
import com.project.bookstoreapp.model.Order;
import java.util.ArrayList;
import java.util.List;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView rvAdminOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageOrders);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));

        // Dữ liệu ảo (Mock Data) chuẩn bị trước cho VNPay và GHN
        orderList = new ArrayList<>();
        orderList.add(new Order("#DH5001", "Trần Minh Quân", 170000, "25/06/2026", "Đã TT (VNPay)", "GHN: Đang giao", "GHN998822"));
        orderList.add(new Order("#DH5002", "Lê Thùy Dương", 95000, "24/06/2026", "Chưa thanh toán", "Chờ xác nhận", ""));
        orderList.add(new Order("#DH5003", "Phạm Hoàng Nam", 328000, "23/06/2026", "Đã TT (VNPay)", "GHN: Đã giao", "GHN112233"));

        orderAdapter = new OrderAdapter(orderList, clickedOrder -> {
            // Tương lai: Mở màn hình chi tiết đơn hàng (OrderDetailActivity) để xem cụ thể địa chỉ định vị Google Maps hoặc cập nhật tiến độ GHN
            Toast.makeText(ManageOrdersActivity.this, "Xem đơn hàng: " + clickedOrder.getOrderId(), Toast.LENGTH_SHORT).show();
        });
        rvAdminOrders.setAdapter(orderAdapter);
    }
}