package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

        // Khởi tạo Adapter với danh sách rỗng
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, clickedOrder -> {
            Toast.makeText(ManageOrdersActivity.this, "Xem đơn hàng: " + clickedOrder.getOrderId(), Toast.LENGTH_SHORT).show();
        });
        rvAdminOrders.setAdapter(orderAdapter);

        // Tải dữ liệu thật từ Firebase
        loadOrdersFromFirebase();
    }

    private void loadOrdersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                orderList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Order order = document.toObject(Order.class);
                    if (order.getOrderId() == null) {
                        // Tùy biến vì Seeder của bạn put orderId thẳng vào map,
                        // nhưng phòng hờ thì vẫn set thêm từ document.getId()
                    }
                    orderList.add(order);
                }
                orderAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(ManageOrdersActivity.this, "Lỗi tải Đơn hàng", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting orders: ", task.getException());
            }
        });
    }
}