package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.CheckoutAdapter;
import com.project.bookstoreapp.model.CartItem;
import com.project.bookstoreapp.model.Voucher;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class CheckoutActivity extends AppCompatActivity {

    private static final Pattern VN_PHONE_PATTERN =
            Pattern.compile("^(0[35789][0-9]{8})$");

    private static final long SHIPPING_FEE = 25000L;
    private static final long FREE_SHIP_THRESHOLD = 300000L;

    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private RecyclerView rvCheckoutItems;
    private TextInputEditText etFullName, etPhone, etAddress, etNote, etVoucher;
    private TextInputLayout tilFullName, tilPhone, tilAddress, tilVoucher;
    private RadioGroup rgPaymentMethod;
    private TextView tvSubtotal, tvShippingFee, tvVoucherDiscount, tvFinalTotal;
    private Button btnApplyVoucher, btnPlaceOrder, btnBackToCart;
    private LinearLayout layoutEmptyCheckout;
    private NestedScrollView scrollCheckout;


    private ArrayList<CartItem> selectedItems = new ArrayList<>();
    private CheckoutAdapter checkoutAdapter;
    private Voucher appliedVoucher = null;
    private long subtotal = 0L;
    private long discountAmount = 0L;
    private String currentUserId;

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        dbRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getUid();

        initViews();
        setupToolbar();
        receiveDataFromCart();
    }

    private void initViews() {
        rvCheckoutItems   = findViewById(R.id.rvCheckoutItems);
        etFullName        = findViewById(R.id.etFullName);
        etPhone           = findViewById(R.id.etPhone);
        etAddress         = findViewById(R.id.etAddress);
        etNote            = findViewById(R.id.etNote);
        etVoucher         = findViewById(R.id.etVoucher);
        tilFullName       = findViewById(R.id.tilFullName);
        tilPhone          = findViewById(R.id.tilPhone);
        tilAddress        = findViewById(R.id.tilAddress);
        tilVoucher        = findViewById(R.id.tilVoucher);
        rgPaymentMethod   = findViewById(R.id.rgPaymentMethod);
        tvSubtotal        = findViewById(R.id.tvSubtotal);
        tvShippingFee     = findViewById(R.id.tvShippingFee);
        tvVoucherDiscount = findViewById(R.id.tvVoucherDiscount);
        tvFinalTotal      = findViewById(R.id.tvFinalTotal);
        btnApplyVoucher   = findViewById(R.id.btnApplyVoucher);
        btnPlaceOrder     = findViewById(R.id.btnPlaceOrder);
        btnBackToCart     = findViewById(R.id.btnBackToCart);
        layoutEmptyCheckout = findViewById(R.id.layoutEmptyCheckout);
        scrollCheckout    = findViewById(R.id.scrollCheckout);

        btnApplyVoucher.setOnClickListener(v -> applyVoucher());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        btnBackToCart.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void receiveDataFromCart() {
        ArrayList<CartItem> items = getIntent().getParcelableArrayListExtra(CartActivity.EXTRA_SELECTED_ITEMS);

        if (items == null || items.isEmpty()) {
            showEmptyCheckout();
            return;
        }

        selectedItems = items;
        setupCheckoutItems();
        computeSubtotal();
        updateOrderSummary();
    }

    private void setupCheckoutItems() {
        checkoutAdapter = new CheckoutAdapter(this, selectedItems);
        rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        rvCheckoutItems.setNestedScrollingEnabled(false);
        rvCheckoutItems.setAdapter(checkoutAdapter);
        showCheckoutContent();
    }

    private void computeSubtotal() {
        subtotal = 0L;
        for (CartItem item : selectedItems) {
            subtotal += item.getSubtotal();
        }
    }

    private void updateOrderSummary() {
        long shipping = subtotal >= FREE_SHIP_THRESHOLD ? 0L : SHIPPING_FEE;
        long total = subtotal + shipping - discountAmount;
        if (total < 0) total = 0;

        tvSubtotal.setText(formatPrice(subtotal));
        tvShippingFee.setText(shipping == 0 ? "Miễn phí" : formatPrice(shipping));
        tvVoucherDiscount.setText("-" + formatPrice(discountAmount));
        tvFinalTotal.setText(formatPrice(total));
    }

    private void applyVoucher() {
        String code = etVoucher.getText() != null
                ? etVoucher.getText().toString().trim().toUpperCase()
                : "";

        if (code.isEmpty()) {
            tilVoucher.setError("Vui lòng nhập mã voucher");
            return;
        }
        tilVoucher.setError(null);

        // Truy vấn Firebase: VOUCHERS/{code}
        dbRef.child("VOUCHERS").child(code)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tilVoucher.setError("Mã voucher không hợp lệ");
                            appliedVoucher = null;
                            discountAmount = 0;
                            updateOrderSummary();
                            return;
                        }

                        Voucher voucher = snapshot.getValue(Voucher.class);
                        if (voucher == null || !voucher.isActive()) {
                            tilVoucher.setError("Mã voucher đã hết hiệu lực");
                            return;
                        }

                        if (subtotal < voucher.getMinOrderValue()) {
                            tilVoucher.setError("Đơn hàng tối thiểu "
                                    + formatPrice(voucher.getMinOrderValue())
                                    + " để áp dụng voucher này");
                            return;
                        }

                        // Áp dụng thành công
                        appliedVoucher = voucher;
                        discountAmount = voucher.calculateDiscount(subtotal);
                        updateOrderSummary();
                        tilVoucher.setError(null);
                        Toast.makeText(CheckoutActivity.this,
                                "Áp dụng voucher thành công! Giảm " + formatPrice(discountAmount),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CheckoutActivity.this,
                                "Lỗi kiểm tra voucher", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            valid = false;
        } else {
            tilFullName.setError(null);
        }

        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            valid = false;
        } else if (!VN_PHONE_PATTERN.matcher(phone).matches()) {
            tilPhone.setError("Số điện thoại không hợp lệ (VD: 0912345678)");
            valid = false;
        } else {
            tilPhone.setError(null);
        }

        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        if (TextUtils.isEmpty(address)) {
            tilAddress.setError("Vui lòng nhập địa chỉ giao hàng");
            valid = false;
        } else {
            tilAddress.setError(null);
        }

        return valid;
    }

    private void placeOrder() {
        if (!validateForm()) return;

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");

        String name    = etFullName.getText().toString().trim();
        String phone   = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String note    = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        // Xác định phương thức thanh toán
        String paymentMethod = "cod";
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        if (checkedId == R.id.rbVnPay) {
            paymentMethod = "vnpay";
        }

        long shipping = subtotal >= FREE_SHIP_THRESHOLD ? 0L : SHIPPING_FEE;
        long finalTotal = Math.max(0, subtotal + shipping - discountAmount);

        // Tạo danh sách items cho đơn hàng
        List<Map<String, Object>> orderItems = new ArrayList<>();
        List<String> bookIds = new ArrayList<>();
        for (CartItem item : selectedItems) {
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("bookId", item.getBookId());
            orderItem.put("title", item.getTitle());
            orderItem.put("quantity", item.getQuantity());
            orderItem.put("price", item.getPrice());
            orderItem.put("subtotal", item.getSubtotal());
            orderItems.add(orderItem);
            bookIds.add(item.getBookId());
        }

        // Build đối tượng Order
        String orderId = dbRef.child("ORDERS").push().getKey();
        String createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .format(new Date());

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("userId", currentUserId != null ? currentUserId : "");
        order.put("receiverName", name);
        order.put("receiverPhone", phone);
        order.put("shippingAddress", address);
        order.put("note", note);
        order.put("paymentMethod", paymentMethod);
        order.put("status", "pending");
        order.put("subtotal", subtotal);
        order.put("shippingFee", shipping);
        order.put("discountAmount", discountAmount);
        order.put("voucherCode", appliedVoucher != null ? appliedVoucher.getCode() : "");
        order.put("totalPrice", finalTotal);
        order.put("items", orderItems);
        order.put("bookIds", bookIds.toString());
        order.put("createdAt", createdAt);
        order.put("updatedAt", createdAt);

        // Ghi đơn hàng lên Firebase
        dbRef.child("ORDERS").child(orderId).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    // Xóa các item đã mua khỏi giỏ hàng
                    removeItemsFromCart();

                    Toast.makeText(this,
                            "Đặt hàng thành công! Mã đơn: " + orderId,
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                    Toast.makeText(this,
                            "Đặt hàng thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void removeItemsFromCart() {
        if (currentUserId == null) return;

        DatabaseReference cartItemsRef = dbRef
                .child("CARTS")
                .child("userId_" + currentUserId)
                .child("items");

        for (CartItem item : selectedItems) {
            cartItemsRef.child("bookId_" + item.getBookId()).removeValue();
        }
    }

    private void showEmptyCheckout() {
        layoutEmptyCheckout.setVisibility(View.VISIBLE);
        scrollCheckout.setVisibility(View.GONE);
        findViewById(R.id.bottomCheckoutBar).setVisibility(View.GONE);
    }

    private void showCheckoutContent() {
        layoutEmptyCheckout.setVisibility(View.GONE);
        scrollCheckout.setVisibility(View.VISIBLE);
        findViewById(R.id.bottomCheckoutBar).setVisibility(View.VISIBLE);
    }

    private String formatPrice(long price) {
        return VND_FORMAT.format(price) + " đ";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}