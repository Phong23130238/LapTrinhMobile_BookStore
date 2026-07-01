package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // IMPORT MỚI
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup; // IMPORT MỚI
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

    // Các biến phục vụ tìm kiếm và lọc
    private SearchView searchViewOrders;
    private ChipGroup chipGroupOrderStatus;
    private String currentSearchQuery = "";
    private String currentStatusFilter = "All"; // Mặc định là "Tất cả"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageOrders);
        searchViewOrders = findViewById(R.id.searchViewOrders); // Ánh xạ
        chipGroupOrderStatus = findViewById(R.id.chipGroupOrderStatus); // Ánh xạ

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter với danh sách rỗng
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, clickedOrder -> {
            Intent intent = new Intent(ManageOrdersActivity.this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", clickedOrder.getOrderId());
            startActivity(intent);
        });
        rvAdminOrders.setAdapter(orderAdapter);

        // NÂNG CẤP: Lắng nghe sự kiện thanh tìm kiếm
        searchViewOrders.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText.trim().toLowerCase();
                filterOrders(); // Gọi hàm lọc tổng hợp
                return true;
            }
        });

        // NÂNG CẤP: Lắng nghe sự kiện chọn Chip lọc trạng thái
        chipGroupOrderStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentStatusFilter = "All";
            } else if (checkedId == R.id.chipPending) {
                // LƯU Ý: Sửa chữ "Pending" thành đúng chuỗi trạng thái bạn lưu trên Firebase
                currentStatusFilter = "pending";
            } else if (checkedId == R.id.chipShipping) {
                currentStatusFilter = "shipping";
            } else if (checkedId == R.id.chipDelivered) {
                currentStatusFilter = "delivered";
            } else if (checkedId == R.id.chipCancelled) {
                currentStatusFilter = "cancelled";
            }
            filterOrders(); // Gọi hàm lọc tổng hợp
        });

        // Tải dữ liệu thật từ Firebase
        loadOrdersFromFirebase();
    }

    // THUẬT TOÁN LỌC KÉP (Tìm kiếm + Trạng thái)
    private void filterOrders() {
        List<Order> filteredList = new ArrayList<>();

        for (Order order : orderList) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;

            if (!currentSearchQuery.isEmpty()) {
                // Tìm theo displayId (dễ nhớ) hoặc orderId (dự phòng)
                String displayId = (order.getDisplayId() != null) ? order.getDisplayId().toLowerCase() : "";

                if (!displayId.contains(currentSearchQuery)) {
                    matchesSearch = false;
                }
            }

            // 2. Kiểm tra điều kiện trạng thái
            if (!currentStatusFilter.equals("All")) {
                if (order.getStatus() == null || !order.getStatus().equalsIgnoreCase(currentStatusFilter)) {
                    matchesStatus = false;
                }
            }

            // Nếu thỏa mãn cả 2 điều kiện thì thêm vào danh sách hiển thị
            if (matchesSearch && matchesStatus) {
                filteredList.add(order);
            }
        }

        // Cập nhật giao diện
        orderAdapter.filterList(filteredList);
    }

    private void loadOrdersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                orderList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Order order = document.toObject(Order.class);

                        // 1. Luôn gán orderId nếu nó null
                        if (order.getOrderId() == null) {
                            order.setOrderId(document.getId());
                        }

                        // 2. XỬ LÝ QUAN TRỌNG: Nếu đơn hàng cũ không có displayId,
                        // ta gán tạm bằng orderId để không bị crash giao diện
                        if (order.getDisplayId() == null) {
                            order.setDisplayId(document.getId()); // Hoặc một chuỗi mặc định như "N/A"
                        }

                        orderList.add(order);
                    } catch (Exception e) {
                        Log.e("Firebase_Error", "Lỗi dữ liệu đơn hàng admin: " + document.getId(), e);
                    }
                }
                // Gọi filter sau khi đã chuẩn hóa xong dữ liệu
                filterOrders();
            } else {
                Toast.makeText(ManageOrdersActivity.this, "Lỗi tải Đơn hàng", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting orders: ", task.getException());
            }
        });
    }
}