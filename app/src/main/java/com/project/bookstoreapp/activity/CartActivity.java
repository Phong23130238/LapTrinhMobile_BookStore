package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.CartAdapter;
import com.project.bookstoreapp.model.CartItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartActionListener {

    // ---- Firebase ----
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private String currentUserId;

    // ---- UI ----
    private RecyclerView rvCartItems;
    private TextView tvTotalPrice;
    private Button btnCheckout, btnShopNow;
    private LinearLayout layoutEmptyCart;

    // ---- Data ----
    private final List<CartItem> cartItemList = new ArrayList<>();
    private CartAdapter cartAdapter;
    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // Key dùng để truyền dữ liệu sang CheckoutActivity
    public static final String EXTRA_SELECTED_ITEMS = "selected_items";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadCartFromFirebase();
    }

    private void initViews() {
        rvCartItems      = findViewById(R.id.rvCartItems);
        tvTotalPrice     = findViewById(R.id.tvTotalPrice);
        btnCheckout      = findViewById(R.id.btnCheckout);
        btnShopNow       = findViewById(R.id.btnShopNow);
        layoutEmptyCart  = findViewById(R.id.layoutEmptyCart);

        btnCheckout.setOnClickListener(v -> proceedToCheckout());
        btnShopNow.setOnClickListener(v -> finish());   // quay lại màn hình mua sắm
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarCart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, cartItemList, this);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);
    }

    private void loadCartFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "test_user_123";

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("CARTS")
                .document(currentUserId)
                .collection("items")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cartItemList.clear();
                    if (snapshot == null || snapshot.isEmpty()) {
                        showEmptyCart();
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        CartItem item = new CartItem(
                                doc.getString("bookId"),
                                doc.getString("title"),
                                doc.getString("author"),
                                doc.getString("imageUrl"),
                                doc.getLong("price") != null ? doc.getLong("price") : 0L,
                                doc.getLong("originalPrice") != null ? doc.getLong("originalPrice") : 0L,
                                doc.getLong("quantity") != null ? doc.getLong("quantity").intValue() : 1,
                                doc.getString("addedAt")
                        );
                        cartItemList.add(item);
                    }
                    showCartContent();
                    cartAdapter.notifyDataSetChanged();
                    updateTotalPrice();
                });
    }

    @Override
    public void onCartChanged() {
        updateTotalPrice();
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (item.getBookId() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "test_user_123";

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("CARTS")
                .document(uid)
                .collection("items")
                .document("bookId_" + item.getBookId())
                .update("quantity", newQuantity)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật số lượng", Toast.LENGTH_SHORT).show());
    }

    private void updateTotalPrice() {
        long total = cartAdapter.getSelectedTotal();
        tvTotalPrice.setText(VND_FORMAT.format(total) + " đ");
    }

    private void proceedToCheckout() {
        ArrayList<CartItem> selectedItems = cartAdapter.getSelectedItems();

        if (selectedItems.isEmpty()) {
            Toast.makeText(this,
                    "Vui lòng chọn ít nhất một sản phẩm để thanh toán",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Đóng gói dữ liệu và chuyển sang CheckoutActivity
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_SELECTED_ITEMS, selectedItems);
        startActivity(intent);
    }

    private void showEmptyCart() {
        layoutEmptyCart.setVisibility(View.VISIBLE);
        rvCartItems.setVisibility(View.GONE);
        tvTotalPrice.setText("0 đ");
    }

    private void showCartContent() {
        layoutEmptyCart.setVisibility(View.GONE);
        rvCartItems.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy listener tránh memory leak
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
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