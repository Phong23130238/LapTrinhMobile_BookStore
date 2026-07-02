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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.HashMap;
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

    // Nút User
    private MaterialButton btnCancelOrder;

    // Card & Nút Admin
    private MaterialCardView cardAdminUpdateStatus;
    private MaterialButton btnAdminConfirm;
    private MaterialButton btnAdminShipping;
    private MaterialButton btnAdminDelivered;
    private MaterialButton btnAdminCancel;
    private TextView tvAdminStatusLocked;

    private String currentOrderId;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Đọc flag IS_ADMIN từ Intent
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        initViews();

        currentOrderId = getIntent().getStringExtra("ORDER_ID");
        if (currentOrderId != null && !currentOrderId.isEmpty()) {
            loadOrderDetails(currentOrderId);
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarOrderDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Các TextView thông tin đơn hàng
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

        // Nút User: hủy đơn
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnCancelOrder.setOnClickListener(v -> showCancelDialog());

        // Card và nút Admin
        cardAdminUpdateStatus = findViewById(R.id.cardAdminUpdateStatus);
        btnAdminConfirm = findViewById(R.id.btnAdminConfirm);
        btnAdminShipping = findViewById(R.id.btnAdminShipping);
        btnAdminDelivered = findViewById(R.id.btnAdminDelivered);
        btnAdminCancel = findViewById(R.id.btnAdminCancel);
        tvAdminStatusLocked = findViewById(R.id.tvAdminStatusLocked);

        // Gán sự kiện nút Admin
        btnAdminConfirm.setOnClickListener(v ->
                showAdminUpdateDialog("Xác nhận đơn hàng?",
                        "Chuyển trạng thái sang 'Đã xác nhận'.", "confirmed"));

        btnAdminShipping.setOnClickListener(v ->
                showAdminUpdateDialog("Chuyển sang Đang giao?",
                        "Chuyển trạng thái sang 'Đang giao hàng'.", "shipping"));

        btnAdminDelivered.setOnClickListener(v ->
                showAdminUpdateDialog("Xác nhận đã giao?",
                        "Chuyển trạng thái sang 'Đã giao thành công'.", "delivered"));

        btnAdminCancel.setOnClickListener(v ->
                showAdminCancelDialog());

        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemList = new ArrayList<>();
        adapter = new OrderDetailAdapter(this, itemList);
        rvOrderItems.setAdapter(adapter);

        setupBottomNav();
    }

    private void setupBottomNav() {
        if (bottomNav != null) {
            // Nếu là Admin thì ẩn BottomNav (không dùng nav user)
            if (isAdmin) {
                bottomNav.setVisibility(View.GONE);
                return;
            }
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

    // =============================================
    // TẢI DỮ LIỆU ĐƠN HÀNG
    // =============================================
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

    // =============================================
    // HIỂN THỊ CHI TIẾT ĐƠN HÀNG
    // =============================================
    private void displayOrderDetails(Order order, String docId) {
        tvOrderId.setVisibility(View.VISIBLE);
        String displayId = (order.getDisplayId() != null && !order.getDisplayId().isEmpty())
                ? order.getDisplayId()
                : "Mã đơn đang cập nhật";

        tvOrderId.setText("Mã đơn: " + displayId);
        tvOrderId.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", displayId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã copy mã đơn hàng", Toast.LENGTH_SHORT).show();
        });
        tvOrderId.setTextColor(getResources().getColor(R.color.navy_dark));
        tvOrderId.setTextSize(18);

        // Chuyển đổi trạng thái sang text tiếng Việt
        String status = order.getStatus();
        String statusText = getStatusText(status);
        tvOrderStatus.setText("Trạng thái: " + statusText);
        tvOrderStatus.setTextColor(getStatusColor(status));

        // Ngày đặt
        String dateStr = order.getCreatedAt();
        if (dateStr != null && !dateStr.isEmpty()) {
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
                } catch (Exception e2) { /* ignore */ }
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

        // Hiển thị nút phù hợp
        if (isAdmin) {
            // Admin mode: hiện Card Admin, ẩn nút hủy của User
            btnCancelOrder.setVisibility(View.GONE);
            cardAdminUpdateStatus.setVisibility(View.VISIBLE);
            setupAdminButtons(status);
        } else {
            // User mode: ẩn Card Admin, chỉ hiện nút hủy khi pending
            cardAdminUpdateStatus.setVisibility(View.GONE);
            if ("pending".equals(status)) {
                btnCancelOrder.setVisibility(View.VISIBLE);
            } else {
                btnCancelOrder.setVisibility(View.GONE);
            }
        }
    }

    // =============================================
    // LOGIC NÚT ADMIN THEO TRẠNG THÁI
    // =============================================
    private void setupAdminButtons(String status) {
        // Ẩn tất cả trước
        btnAdminConfirm.setVisibility(View.GONE);
        btnAdminShipping.setVisibility(View.GONE);
        btnAdminDelivered.setVisibility(View.GONE);
        btnAdminCancel.setVisibility(View.GONE);
        tvAdminStatusLocked.setVisibility(View.GONE);

        if (status == null) status = "";
        switch (status.toLowerCase()) {
            case "pending":
                // pending → có thể Xác nhận hoặc Hủy
                btnAdminConfirm.setVisibility(View.VISIBLE);
                btnAdminCancel.setVisibility(View.VISIBLE);
                break;
            case "confirmed":
                // confirmed → có thể chuyển sang Đang giao hoặc Hủy
                btnAdminShipping.setVisibility(View.VISIBLE);
                btnAdminCancel.setVisibility(View.VISIBLE);
                break;
            case "shipping":
                // shipping → chỉ có thể Đã giao
                btnAdminDelivered.setVisibility(View.VISIBLE);
                break;
            case "delivered":
            case "cancelled":
                // Đã kết thúc → không cho sửa
                tvAdminStatusLocked.setVisibility(View.VISIBLE);
                break;
            default:
                tvAdminStatusLocked.setVisibility(View.VISIBLE);
                break;
        }
    }

    // =============================================
    // DIALOG XÁC NHẬN ADMIN (không hủy)
    // =============================================
    private void showAdminUpdateDialog(String title, String message, String newStatus) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> updateOrderStatus(newStatus, null))
                .setNegativeButton("Đóng", (dialog, which) -> dialog.cancel())
                .show();
    }

    // =============================================
    // DIALOG HỦY ĐƠN ADMIN (kèm lý do)
    // =============================================
    private void showAdminCancelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hủy đơn hàng");
        final EditText input = new EditText(this);
        input.setHint("Nhập lý do hủy đơn...");
        builder.setView(input);
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do hủy", Toast.LENGTH_SHORT).show();
            } else {
                updateOrderStatus("cancelled", reason);
            }
        });
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // =============================================
    // CẬP NHẬT TRẠNG THÁI LÊN FIREBASE
    // =============================================
    private void updateOrderStatus(String newStatus, String cancelReason) {
        if (currentOrderId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .format(new Date()));
        if (cancelReason != null && !cancelReason.isEmpty()) {
            updates.put("cancelReason", cancelReason);
        }

        FirebaseFirestore.getInstance()
                .collection("orders")
                .document(currentOrderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "✓ Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                    loadOrderDetails(currentOrderId); // Reload lại UI
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi khi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    Log.e("OrderDetail", "Lỗi cập nhật trạng thái: ", e);
                });
    }

    // =============================================
    // HỦY ĐƠN CHO USER (giữ nguyên chức năng cũ)
    // =============================================
    private void showCancelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hủy đơn hàng");
        final EditText input = new EditText(this);
        input.setHint("Nhập lý do hủy đơn...");
        builder.setView(input);
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do hủy", Toast.LENGTH_SHORT).show();
            } else {
                cancelOrder(reason);
            }
        });
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void cancelOrder(String reason) {
        if (currentOrderId == null) return;
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("orders").document(currentOrderId)
                .update("status", "cancelled", "cancelReason", reason)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    loadOrderDetails(currentOrderId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi khi hủy đơn", Toast.LENGTH_SHORT).show();
                    Log.e("OrderDetail", "Lỗi hủy đơn: ", e);
                });
    }

    // =============================================
    // HÀM TIỆN ÍCH: Text & màu trạng thái
    // =============================================
    private String getStatusText(String status) {
        if (status == null) return "Đang xử lý";
        switch (status.toLowerCase()) {
            case "pending":   return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "shipping":  return "Đang giao hàng";
            case "delivered": return "Đã giao thành công";
            case "cancelled": return "Đã hủy";
            default:          return "Đang xử lý";
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFFD97706;
        switch (status.toLowerCase()) {
            case "pending":   return 0xFFD97706; // Orange
            case "confirmed": return 0xFF0284C7; // Blue
            case "shipping":  return 0xFF7C3AED; // Purple
            case "delivered": return 0xFF047857; // Green
            case "cancelled": return 0xFFBE123C; // Rose
            default:          return 0xFFD97706;
        }
    }
}
