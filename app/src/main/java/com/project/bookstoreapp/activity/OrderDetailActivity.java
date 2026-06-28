package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.OrderDetailAdapter;
import com.project.bookstoreapp.model.Order;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvOrderId, tvOrderStatus, tvOrderDate, tvShippingAddress;
    private TextView tvSubtotal, tvShippingFee, tvTotal, tvPaymentMethod;
    private RecyclerView rvOrderItems;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;

    private OrderDetailAdapter adapter;
    private List<Map<String, Object>> itemList;
    private ApiService apiService;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        initViews();

        String orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId != null && !orderId.isEmpty()) {
            loadOrderDetails(orderId);
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarOrderDetail);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderStatus = findViewById(R.id.tvOrderStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvShippingAddress = findViewById(R.id.tvShippingAddress);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotal = findViewById(R.id.tvTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        progressBar = findViewById(R.id.progressBarOrder);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        bottomNav = findViewById(R.id.bottomNav);

        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemList = new ArrayList<>();
        adapter = new OrderDetailAdapter(this, itemList);
        rvOrderItems.setAdapter(adapter);

        setupBottomNav();
    }

    private void setupBottomNav() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_orders);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    android.content.Intent intent = new android.content.Intent(OrderDetailActivity.this, HomeActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_cart) {
                    android.content.Intent intent = new android.content.Intent(OrderDetailActivity.this, CartActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    android.content.Intent intent = new android.content.Intent(OrderDetailActivity.this, ProfileActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadOrderDetails(String orderId) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getOrderDetails(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess() && response.body().getData() != null) {
                        displayOrderDetails(response.body().getData(), orderId);
                    } else {
                        Toast.makeText(OrderDetailActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Lỗi tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("OrderDetail", "onFailure: ", t);
                Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrderDetails(Order order, String docId) {
        tvOrderId.setText("Mã ĐH: #" + (order.getOrderId() != null ? order.getOrderId() : docId));
        
        String statusText = "Đang xử lý";
        String status = order.getStatus();
        if ("delivered".equals(status)) statusText = "Đã giao thành công";
        else if ("shipping".equals(status)) statusText = "Đang giao hàng";
        else if ("cancelled".equals(status)) statusText = "Đã hủy";
        
        tvOrderStatus.setText("Trạng thái: " + statusText);
        
        String dateStr = order.getCreatedAt();
        if (dateStr != null && !dateStr.isEmpty()) {
            // Payment Method
            if (tvPaymentMethod != null) {
                String paymentMethod = order.getPaymentMethod();
                if ("banking".equalsIgnoreCase(paymentMethod)) {
                    tvPaymentMethod.setText("Chuyển khoản (VNPay)");
                } else {
                    tvPaymentMethod.setText("Trả tiền mặt (COD)");
                }
            }

            Date date = null;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateStr);
            } catch (Exception e) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateStr);
                } catch (Exception e2) {
                }
            }
            if (date != null) {
                tvOrderDate.setText("Ngày đặt: " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(date));
            } else {
                tvOrderDate.setText("Ngày đặt: " + dateStr);
            }
        }

        tvShippingAddress.setText(order.getShippingAddress());

        double total = order.getTotalPrice();
        double shipping = order.getShippingFee();
        double sub = total - shipping;

        tvSubtotal.setText(formatter.format(sub) + " đ");
        tvShippingFee.setText(formatter.format(shipping) + " đ");
        tvTotal.setText(formatter.format(total) + " đ");

        if (order.getItems() != null) {
            itemList.clear();
            itemList.addAll(order.getItems());
            adapter.notifyDataSetChanged();
        }
    }
}
