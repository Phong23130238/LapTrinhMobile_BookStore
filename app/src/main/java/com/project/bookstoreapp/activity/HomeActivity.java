package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.model.Book;

import java.util.ArrayList;
import java.util.List;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageButton;
import android.view.View;
import android.view.MenuItem;
import com.bumptech.glide.Glide;
import com.project.bookstoreapp.utils.SessionManager;
import com.project.bookstoreapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;
import android.os.Handler;
import android.os.Looper;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvBooks;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private List<Book> originalList; // Danh sách gốc lưu dữ liệu Firebase để tìm kiếm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvBooks = findViewById(R.id.rvBooks);
        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));

        bookList = new ArrayList<>();
        originalList = new ArrayList<>();
        bookAdapter = new BookAdapter(bookList);

        bookAdapter.setOnItemClickListener(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            intent.putExtra("BOOK_ID", book.getBookId());
            startActivity(intent);
        });

        rvBooks.setAdapter(bookAdapter);

        // Tải dữ liệu từ Firebase
        loadBooksFromFirebase();

        // Xử lý thanh tìm kiếm thời gian thực
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterBooks(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Cấu hình Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(HomeActivity.this, CartActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(HomeActivity.this, OrdersActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        setupHeader();
    }

    private void setupHeader() {
        CircleImageView ivAvatar = findViewById(R.id.ivAvatarHeader);
        TextView tvBadge = findViewById(R.id.tvCartBadge);
        ImageButton btnCart = findViewById(R.id.btnCartHeader);

        SessionManager sessionManager = new SessionManager(this);
        User user = sessionManager.getUser();

        if (ivAvatar != null) {
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this).load(user.getAvatarUrl()).into(ivAvatar);
            }
            // Realtime update from Firestore
            if (user != null && user.getUid() != null) {
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot != null && snapshot.exists() && snapshot.getString("avatarUrl") != null) {
                            Glide.with(this).load(snapshot.getString("avatarUrl")).into(ivAvatar);
                        }
                    });
            }

            ivAvatar.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(HomeActivity.this, v);
                popup.getMenu().add("Thông tin tài khoản");
                popup.getMenu().add("Đăng xuất");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Thông tin tài khoản")) {
                        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    } else if (item.getTitle().equals("Đăng xuất")) {
                        sessionManager.logoutUser();
                        // Firebase logout is removed since we only rely on SessionManager
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        finishAffinity();
                    }
                    return true;
                });
                popup.show();
                return true;
            });
        }

        if (btnCart != null) {
            btnCart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));
        }

        // Cart Badge Logic updated to use SessionManager and the correct whereEqualTo query
        if (tvBadge != null && user != null && user.getUid() != null) {
            FirebaseFirestore.getInstance().collection("carts")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        tvBadge.setText(String.valueOf(value.size()));
                        tvBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvBadge.setVisibility(View.GONE);
                    }
                });
        }
    }

    private void loadBooksFromFirebase() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore_Error", "Lỗi lấy dữ liệu: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        bookList.clear();
                        originalList.clear();

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Book book = doc.toObject(Book.class);

                            // Đồng bộ ID từ document key nếu bookId trong object bị null
                            if (book.getBookId() == null || book.getBookId().isEmpty()) {
                                book.setBookId(doc.getId());
                            }

                            if (!book.isHidden()) {
                                bookList.add(book);
                                originalList.add(book);
                            }
                        }
                        bookAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void filterBooks(String text) {
        List<Book> filteredList = new ArrayList<>();

        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (Book item : originalList) {
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(text.toLowerCase().trim())) {
                    filteredList.add(item);
                }
            }
        }

        if (bookAdapter != null) {
            bookAdapter.setFilteredList(filteredList);
        }
    }
}