package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.Collections;
import java.util.Comparator;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.OrderAdapter;
import com.project.bookstoreapp.model.Order;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private MaterialButton btnShopNow;
    private BottomNavigationView bottomNav;

    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private List<Order> originalOrderList;
    private SessionManager sessionManager;
    
    private ChipGroup chipGroupStatus;
    private ImageButton btnSortTime;
    private TextView tvEmptyTitle, tvEmptyDesc;
    private boolean isSortDesc = true;
    private String currentStatusFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        sessionManager = new SessionManager(this);
        User currentUser = sessionManager.getUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupBottomNav();

        orderList = new ArrayList<>();
        originalOrderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, clickedOrder -> {
            Intent intent = new Intent(OrdersActivity.this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", clickedOrder.getOrderId());
            startActivity(intent);
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        loadOrdersFromFirebase(currentUser.getUid());
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rvOrders);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnShopNow = findViewById(R.id.btnShopNow);
        bottomNav = findViewById(R.id.bottomNav);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);

        btnShopNow.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        btnSortTime = findViewById(R.id.btnSortTime);
        
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentStatusFilter = "all";
            else if (checkedId == R.id.chipPending) currentStatusFilter = "pending";
            else if (checkedId == R.id.chipProcessing) currentStatusFilter = "processing";
            else if (checkedId == R.id.chipShipping) currentStatusFilter = "shipping";
            else if (checkedId == R.id.chipDelivered) currentStatusFilter = "delivered";
            else if (checkedId == R.id.chipCancelled) currentStatusFilter = "cancelled";
            applyFilterAndSort();
        });
        
        btnSortTime.setOnClickListener(v -> {
            isSortDesc = !isSortDesc;
            Toast.makeText(this, isSortDesc ? "Sắp xếp: Mới nhất" : "Sắp xếp: Cũ nhất", Toast.LENGTH_SHORT).show();
            applyFilterAndSort();
        });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_orders);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(OrdersActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(OrdersActivity.this, CartActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_orders) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(OrdersActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadOrdersFromFirebase(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        originalOrderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Order order = document.toObject(Order.class);
                                if (order.getOrderId() == null) {
                                    try {
                                        java.lang.reflect.Field field = Order.class.getDeclaredField("orderId");
                                        field.setAccessible(true);
                                        field.set(order, document.getId());
                                    } catch (Exception e) {
                                        Log.e("Order", "Error setting orderId", e);
                                    }
                                }
                                originalOrderList.add(order);
                            } catch (Exception e) {
                                Log.e("Firebase_Error", "Lỗi dữ liệu đơn hàng: " + document.getId(), e);
                            }
                        }
                        applyFilterAndSort();
                    } else {
                        Toast.makeText(OrdersActivity.this, "Lỗi tải Đơn hàng", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase_Error", "Error getting orders: ", task.getException());
                    }
                });
    }

    private void applyFilterAndSort() {
        orderList.clear();
        for (Order order : originalOrderList) {
            if ("all".equals(currentStatusFilter) || currentStatusFilter.equals(order.getStatus())) {
                orderList.add(order);
            }
        }
        
        Collections.sort(orderList, new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                String d1 = o1.getCreatedAt() != null ? o1.getCreatedAt() : "";
                String d2 = o2.getCreatedAt() != null ? o2.getCreatedAt() : "";
                if (isSortDesc) {
                    return d2.compareTo(d1);
                } else {
                    return d1.compareTo(d2);
                }
            }
        });
        
        if (orderList.isEmpty()) {
            rvOrders.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            
            if ("all".equals(currentStatusFilter)) {
                tvEmptyTitle.setText("Bạn chưa có đơn hàng nào");
                tvEmptyDesc.setVisibility(View.VISIBLE);
                btnShopNow.setVisibility(View.VISIBLE);
            } else {
                String statusName = "";
                if ("pending".equals(currentStatusFilter)) statusName = "đang chờ xác nhận";
                else if ("processing".equals(currentStatusFilter)) statusName = "đang xử lý";
                else if ("shipping".equals(currentStatusFilter)) statusName = "đang giao";
                else if ("delivered".equals(currentStatusFilter)) statusName = "đã giao";
                else if ("cancelled".equals(currentStatusFilter)) statusName = "đã hủy";
                
                tvEmptyTitle.setText("Bạn chưa có đơn nào " + statusName);
                tvEmptyDesc.setVisibility(View.GONE);
                btnShopNow.setVisibility(View.GONE);
            }
        } else {
            rvOrders.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            orderAdapter.notifyDataSetChanged();
        }
    }
}