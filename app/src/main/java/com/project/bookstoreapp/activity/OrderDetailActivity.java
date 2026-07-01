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

import android.app.AlertDialog;
import android.widget.EditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
    private MaterialButton btnCancelOrder;
    private String currentOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // 1.1 Khởi tạo và thiết lập (onCreate & initViews)
        apiService = RetrofitClient.getClient().create(ApiService.class);
        initViews();

        // 1.1.2 Lấy tham số ORDER_ID từ Intent.getStringExtra("ORDER_ID") do màn hình trước truyền sang.
        currentOrderId = getIntent().getStringExtra("ORDER_ID");
        if (currentOrderId != null && !currentOrderId.isEmpty()) {
            loadOrderDetails(currentOrderId);
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 1.1.1 Khởi tạo các View, Toolbar, BottomNav và gán sự kiện click trong hàm initViews.
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarOrderDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
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
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        btnCancelOrder.setOnClickListener(v -> showCancelDialog());

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

    // 1.2 Tải dữ liệu từ backend (loadOrderDetails)
    private void loadOrderDetails(String orderId) {
        // 1.2.1 Hiển thị ProgressBar, gọi apiService.getOrderDetails(orderId).enqueue để gửi HTTP GET đến Node.js.
        progressBar.setVisibility(View.VISIBLE);
        apiService.getOrderDetails(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                progressBar.setVisibility(View.GONE);
                // 1.2.2 Xử lý Response: Nếu isSuccessful, gọi hàm displayOrderDetails truyền data.
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

    // 1.4 Xử lý hủy đơn (showCancelDialog)
    private void showCancelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hủy đơn hàng");
        
        // 1.4.1 Tạo AlertDialog chứa một EditText (input) để người dùng nhập lý do hủy đơn.
        final EditText input = new EditText(this);
        input.setHint("Nhập lý do hủy đơn...");
        builder.setView(input);

        // 1.4.2 Bắt sự kiện PositiveButton (Xác nhận), kiểm tra chuỗi rỗng và gọi hàm cancelOrder(reason).
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(OrderDetailActivity.this, "Vui lòng nhập lý do hủy", Toast.LENGTH_SHORT).show();
            } else {
                cancelOrder(reason);
            }
        });
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 1.5 Cập nhật Firestore (cancelOrder)
    private void cancelOrder(String reason) {
        if (currentOrderId == null) return;
        progressBar.setVisibility(View.VISIBLE);
        
        // 1.5.1 Gọi FirebaseFirestore.getInstance().collection("orders").document(currentOrderId).update() để đổi trạng thái (cancelled) và lý do (cancelReason).
        FirebaseFirestore.getInstance().collection("orders").document(currentOrderId)
            .update("status", "cancelled", "cancelReason", reason)
            // 1.5.2 Lắng nghe addOnSuccessListener để Toast thông báo thành công và gọi lại loadOrderDetails() cập nhật UI.
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                loadOrderDetails(currentOrderId); 
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OrderDetailActivity.this, "Lỗi khi hủy đơn", Toast.LENGTH_SHORT).show();
                Log.e("OrderDetail", "Lỗi hủy đơn: ", e);
            });
    }

    // 1.3 Hiển thị giao diện (displayOrderDetails)
    private void displayOrderDetails(Order order, String docId) {
        tvOrderId.setVisibility(View.VISIBLE);
        // Chỉ lấy displayId, nếu không có thì để "Chưa có mã" thay vì lấy ID ngẫu nhiên của Firebase
        String displayId = (order.getDisplayId() != null && !order.getDisplayId().isEmpty())
                ? order.getDisplayId()
                : "Mã đơn đang cập nhật";

        tvOrderId.setText("Mã đơn: " + displayId);
        // Thêm tính năng bấm vào mã đơn để copy
        tvOrderId.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", displayId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã copy mã đơn hàng", Toast.LENGTH_SHORT).show();
        });
        tvOrderId.setTextColor(getResources().getColor(R.color.navy_dark)); // Bạn có thể tùy chỉnh màu cho nổi bật
        tvOrderId.setTextSize(18); // Làm to hơn một chút cho dễ nhìn
        
        String statusText = "Đang xử lý";
        // 1.3.1 Dựa vào trường order.getStatus() để chuyển đổi thành text tiếng Việt (Đang xử lý, Đang giao...).
        String status = order.getStatus();
        if ("delivered".equals(status)) statusText = "Đã giao thành công";
        else if ("shipping".equals(status)) statusText = "Đang giao hàng";
        else if ("cancelled".equals(status)) statusText = "Đã hủy";
        
        tvOrderStatus.setText("Trạng thái: " + statusText);
        
        // 1.3.2 Xử lý logic ẩn/hiện nút btnCancelOrder (chỉ hiện khi status là 'pending').
        if ("pending".equals(status)) {
            btnCancelOrder.setVisibility(View.VISIBLE);
        } else {
            btnCancelOrder.setVisibility(View.GONE);
        }
        
        // 1.3.3 Format (SimpleDateFormat) và hiển thị ngày đặt, phương thức thanh toán, địa chỉ, các loại phí (DecimalFormat).
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

        // 1.3.4 Làm mới danh sách itemList (clear), thêm item mới và gọi adapter.notifyDataSetChanged() để render RecyclerView.
        if (order.getItems() != null) {
            itemList.clear();
            itemList.addAll(order.getItems());
            adapter.notifyDataSetChanged();
        }
    }
}
