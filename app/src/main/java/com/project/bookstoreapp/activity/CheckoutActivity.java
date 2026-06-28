package com.project.bookstoreapp.activity;

import android.annotation.SuppressLint;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.CheckoutAdapter;
import com.project.bookstoreapp.model.CartItem;

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

    private static final Pattern VN_PHONE =
            Pattern.compile("^0[35789][0-9]{8}$");

    private static final long SHIPPING_FEE  = 25_000L;
    private static final long FREE_SHIP_MIN = 300_000L;

    private static final NumberFormat VND =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ---- UI ----
    private RecyclerView      rvCheckoutItems;
    private TextInputEditText etFullName, etPhone, etAddress, etNote, etVoucher;
    private TextInputLayout   tilFullName, tilPhone, tilAddress, tilVoucher;
    private RadioGroup        rgPaymentMethod;
    private TextView          tvSubtotal, tvShippingFee, tvVoucherDiscount, tvFinalTotal;
    private Button            btnApplyVoucher, btnPlaceOrder, btnBackToCart;
    private LinearLayout      layoutEmptyCheckout;
    private NestedScrollView  scrollCheckout;
    private View              bottomBar;

    // ---- Data ----
    private ArrayList<CartItem> selectedItems      = new ArrayList<>();
    private CheckoutAdapter     checkoutAdapter;
    private long                subtotal           = 0L;
    private long                discountAmount     = 0L;
    private String              appliedVoucherCode = ""; // ← field lưu mã voucher đã áp dụng
    private String              currentUserId;

    // ---- Firebase ----
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();

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
        bottomBar         = findViewById(R.id.bottomCheckoutBar);

        btnApplyVoucher.setOnClickListener(v -> applyVoucher());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        if (btnBackToCart != null) btnBackToCart.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void receiveDataFromCart() {
        ArrayList<CartItem> items =
                getIntent().getParcelableArrayListExtra(CartActivity.EXTRA_SELECTED_ITEMS);

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
        for (CartItem item : selectedItems) subtotal += item.getSubtotal();
    }

    private void updateOrderSummary() {
        long shipping = subtotal >= FREE_SHIP_MIN ? 0L : SHIPPING_FEE;
        long total    = Math.max(0, subtotal + shipping - discountAmount);

        tvSubtotal.setText(fmt(subtotal));
        tvShippingFee.setText(shipping == 0 ? "Miễn phí" : fmt(shipping));
        tvVoucherDiscount.setText("- " + fmt(discountAmount));
        tvFinalTotal.setText(fmt(total));
    }

    private void applyVoucher() {
        String code = getEditText(etVoucher).toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            tilVoucher.setError("Vui lòng nhập mã voucher");
            return;
        }
        tilVoucher.setError(null);
        btnApplyVoucher.setEnabled(false);

        db.collection("vouchers")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    btnApplyVoucher.setEnabled(true);

                    if (!doc.exists()) {
                        tilVoucher.setError("Mã voucher không hợp lệ");
                        resetDiscount();
                        return;
                    }

                    Boolean isActive = doc.getBoolean("isActive");
                    if (isActive == null || !isActive) {
                        tilVoucher.setError("Mã voucher đã hết hiệu lực");
                        resetDiscount();
                        return;
                    }

                    long minOrder    = doc.getLong("minOrderValue")   != null ? doc.getLong("minOrderValue")   : 0L;
                    long maxDisc     = doc.getLong("maxDiscount")     != null ? doc.getLong("maxDiscount")     : 0L;
                    long discPercent = doc.getLong("discountPercent") != null ? doc.getLong("discountPercent") : 0L;

                    if (subtotal < minOrder) {
                        tilVoucher.setError("Đơn tối thiểu " + fmt(minOrder));
                        resetDiscount();
                        return;
                    }

                    long discount = subtotal * discPercent / 100;
                    if (discount > maxDisc) discount = maxDisc;

                    discountAmount     = discount;
                    appliedVoucherCode = code; // lưu để ghi vào order
                    updateOrderSummary();
                    tilVoucher.setError(null);
                    Toast.makeText(this, "Giảm thành công " + fmt(discountAmount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnApplyVoucher.setEnabled(true);
                    Toast.makeText(this, "Lỗi kiểm tra voucher: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void resetDiscount() {
        discountAmount     = 0;
        appliedVoucherCode = "";
        updateOrderSummary();
    }

    private boolean validateForm() {
        boolean ok = true;

        if (getEditText(etFullName).isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên"); ok = false;
        } else tilFullName.setError(null);

        String phone = getEditText(etPhone);
        if (phone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại"); ok = false;
        } else if (!VN_PHONE.matcher(phone).matches()) {
            tilPhone.setError("SĐT không hợp lệ (VD: 0912345678)"); ok = false;
        } else tilPhone.setError(null);

        if (getEditText(etAddress).isEmpty()) {
            tilAddress.setError("Vui lòng nhập địa chỉ giao hàng"); ok = false;
        } else tilAddress.setError(null);

        return ok;
    }

    private void placeOrder() {
        if (!validateForm()) return;

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");

        String paymentMethod = (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbVnPay)
                ? "vnpay" : "cod";

        long shipping  = subtotal >= FREE_SHIP_MIN ? 0L : SHIPPING_FEE;
        long total     = Math.max(0, subtotal + shipping - discountAmount);
        String nowStr  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .format(new Date());

        // Build danh sách items
        List<Map<String, Object>> orderItems = new ArrayList<>();
        List<String> bookIds = new ArrayList<>();
        for (CartItem ci : selectedItems) {
            Map<String, Object> m = new HashMap<>();
            m.put("bookId",   ci.getBookId());
            m.put("title",    ci.getTitle());
            m.put("quantity", ci.getQuantity());
            m.put("price",    ci.getPrice());
            m.put("subtotal", ci.getSubtotal());
            orderItems.add(m);
            bookIds.add(ci.getBookId());
        }

        Map<String, Object> order = new HashMap<>();
        order.put("userId",          currentUserId != null ? currentUserId : "");
        order.put("receiverName",    getEditText(etFullName));
        order.put("receiverPhone",   getEditText(etPhone));
        order.put("shippingAddress", getEditText(etAddress));
        order.put("note",            getEditText(etNote));
        order.put("paymentMethod",   paymentMethod);
        order.put("status",          "pending");
        order.put("subtotal",        subtotal);
        order.put("shippingFee",     shipping);
        order.put("discountAmount",  discountAmount);
        order.put("voucherCode",     appliedVoucherCode); // ← dùng field đúng
        order.put("totalPrice",      total);
        order.put("items",           orderItems);
        order.put("bookIds",         bookIds.toString());
        order.put("createdAt",       nowStr);
        order.put("updatedAt",       nowStr);

        // Ghi vào Firestore collection "orders"
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(docRef -> {
                    removeItemsFromCart();
                    Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                    Toast.makeText(this, "Thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
    @SuppressLint("SetTextI18n")
    private void removeItemsFromCart() {
        if (currentUserId == null) return;
        for (CartItem ci : selectedItems) {
            db.collection("carts")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("bookId", ci.getBookId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                            doc.getReference().delete();
                        }
                    });
        }
    }

    private void showEmptyCheckout() {
        layoutEmptyCheckout.setVisibility(View.VISIBLE);
        scrollCheckout.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
    }

    private void showCheckoutContent() {
        layoutEmptyCheckout.setVisibility(View.GONE);
        scrollCheckout.setVisibility(View.VISIBLE);
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
    }

    private String getEditText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String fmt(long price) {
        return VND.format(price) + " đ";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}