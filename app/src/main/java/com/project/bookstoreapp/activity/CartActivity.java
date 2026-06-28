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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.CartAdapter;
import com.project.bookstoreapp.model.CartItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartActionListener {

    public static final String EXTRA_SELECTED_ITEMS = "selected_items";
    public static final String EXTRA_AUTO_SELECT_BOOK_ID = "auto_select_book_id"; // bookId được truyền từ BookDetail

    // Firestore
    private FirebaseFirestore db;
    private ListenerRegistration cartListener;  // dùng để hủy snapshot listener
    private String currentUserId;

    // UI
    private RecyclerView  rvCartItems;
    private TextView      tvTotalPrice;
    private Button        btnCheckout, btnShopNow;
    private LinearLayout  layoutEmptyCart;

    // Data
    private final List<CartItem>   cartItemList   = new ArrayList<>();
    private final List<String>     docIdList      = new ArrayList<>(); // lưu Firestore doc ID để update/delete
    private CartAdapter cartAdapter;
    private String autoSelectBookId = null; // bookId cần auto-select khi mở từ BookDetail

    private static final NumberFormat VND =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        db = FirebaseFirestore.getInstance();

        // Nhận bookId cần auto-select (khi đến từ BookDetailActivity)
        autoSelectBookId = getIntent().getStringExtra(EXTRA_AUTO_SELECT_BOOK_ID);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadCartFromFirestore();
    }

    private void initViews() {
        rvCartItems    = findViewById(R.id.rvCartItems);
        tvTotalPrice   = findViewById(R.id.tvTotalPrice);
        btnCheckout    = findViewById(R.id.btnCheckout);
        btnShopNow     = findViewById(R.id.btnShopNow);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);

        btnCheckout.setOnClickListener(v -> proceedToCheckout());
        btnShopNow.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, cartItemList, this);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);
    }

    private void loadCartFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyCart();
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUserId = user.getUid();

        // Lắng nghe realtime — collection "carts", filter theo userId
        cartListener = db.collection("carts")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải giỏ hàng: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    cartItemList.clear();
                    docIdList.clear();

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyCart();
                        cartAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        // Map thủ công — tránh lỗi kiểu Timestamp vs String cho addedAt
                        String bookId       = doc.getString("bookId");
                        String title        = doc.getString("title");
                        String author       = doc.getString("author");
                        String imageUrl     = doc.getString("imageUrl");
                        long   price        = doc.getLong("price")         != null ? doc.getLong("price")         : 0L;
                        long   origPrice    = doc.getLong("originalPrice") != null ? doc.getLong("originalPrice") : 0L;
                        int    quantity     = doc.getLong("quantity")      != null ? doc.getLong("quantity").intValue() : 1;

                        // addedAt có thể là Timestamp hoặc String — xử lý cả 2
                        String addedAt = "";
                        Object addedAtObj = doc.get("addedAt");
                        if (addedAtObj instanceof com.google.firebase.Timestamp) {
                            addedAt = ((com.google.firebase.Timestamp) addedAtObj)
                                    .toDate().toString();
                        } else if (addedAtObj instanceof String) {
                            addedAt = (String) addedAtObj;
                        }

                        CartItem item = new CartItem(
                                bookId, title, author, imageUrl,
                                price, origPrice, quantity, addedAt
                        );
                        cartItemList.add(item);
                        docIdList.add(doc.getId()); // lưu doc ID để update sau
                    }

                    showCartContent();
                    cartAdapter.notifyDataSetChanged();

                    // Auto-select item nếu đến từ BookDetailActivity
                    if (autoSelectBookId != null && !autoSelectBookId.isEmpty()) {
                        for (CartItem item : cartItemList) {
                            if (autoSelectBookId.equals(item.getBookId())) {
                                item.setSelected(true);
                                break;
                            }
                        }
                        cartAdapter.notifyDataSetChanged();
                    }

                    updateTotalPrice();
                });
    }

    @Override
    public void onCartChanged() {
        updateTotalPrice();
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        // Tìm doc ID tương ứng với item
        int idx = cartItemList.indexOf(item);
        if (idx < 0 || idx >= docIdList.size()) return;

        String docId = docIdList.get(idx);
        db.collection("carts").document(docId)
                .update("quantity", newQuantity)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật số lượng", Toast.LENGTH_SHORT).show());
    }

    private void updateTotalPrice() {
        long total = cartAdapter.getSelectedTotal();
        tvTotalPrice.setText(VND.format(total) + " đ");
    }

    private void proceedToCheckout() {
        ArrayList<CartItem> selected = cartAdapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_SELECTED_ITEMS, selected);
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
        if (cartListener != null) cartListener.remove(); // hủy Firestore listener
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}