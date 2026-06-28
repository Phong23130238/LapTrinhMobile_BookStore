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
import com.google.firebase.firestore.FirebaseFirestore;
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
    private SessionManager sessionManager;

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

        btnShopNow.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
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
                        orderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
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
                            orderList.add(order);
                        }
                        
                        if (orderList.isEmpty()) {
                            rvOrders.setVisibility(View.GONE);
                            layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
                            rvOrders.setVisibility(View.VISIBLE);
                            layoutEmpty.setVisibility(View.GONE);
                            orderAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(OrdersActivity.this, "Lỗi tải Đơn hàng", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase_Error", "Error getting orders: ", task.getException());
                    }
                });
    }
}