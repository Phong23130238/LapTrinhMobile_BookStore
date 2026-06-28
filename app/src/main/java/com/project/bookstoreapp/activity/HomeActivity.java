package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText; // Thêm import này
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private List<Book> originalList; // BIẾN TẠM: Lưu trữ danh sách gốc không bị thay đổi
    
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvBooks = findViewById(R.id.rvBooks);
        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));

        bookList = new ArrayList<>();
        originalList = new ArrayList<>(); // Khởi tạo danh sách gốc rỗng
        bookAdapter = new BookAdapter(bookList);

        bookAdapter.setOnItemClickListener(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            // Sửa book.getId() thành book.getBookId() nếu class Book của bạn dùng định dạng Firebase document ID
            intent.putExtra("BOOK_ID", book.getBookId() != null ? book.getBookId() : book.getId());
            startActivity(intent);
        });

        rvBooks.setAdapter(bookAdapter);

        // Gọi hàm lấy dữ liệu thật từ Firebase
        loadBooksFromFirebase();

        // --- XỬ LÝ THANH TÌM KIẾM ĐÚNG VỚI MATERIAL DESIGN XML ---
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Không cần xử lý
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> filterBooks(s.toString());
                    searchHandler.postDelayed(searchRunnable, 300); // 300ms debounce
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Không cần xử lý
                }
            });
        }

        // Ánh xạ thanh Bottom Navigation
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("books").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bookList.clear();
                originalList.clear(); // Xóa sạch dữ liệu cũ ở danh sách sao lưu

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);

                    if (book.getBookId() == null) {
                        book.setBookId(document.getId());
                    }

                    if (!book.isHidden()) {
                        bookList.add(book);
                        originalList.add(book); // Sao lưu một bản vào danh sách gốc ổn định
                    }
                }

                bookAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(HomeActivity.this, "Lỗi khi tải dữ liệu sách", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Lỗi tải sách HomeActivity: ", task.getException());
            }
        });
    }

    /**
     * Hàm xử lý bộ lọc tìm kiếm sách theo tên
     */
    private void filterBooks(String text) {
        List<Book> filteredList = new ArrayList<>();

        // Nếu ô tìm kiếm trống, quay về hiển thị toàn bộ danh sách gốc ban đầu
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            // Duyệt từ danh sách gốc ra để tránh bị mất dữ liệu khi xóa chữ
            for (Book item : originalList) {
                // LƯU Ý: Nếu trong file Book.java bạn đặt tên hàm lấy tên sách khác (Ví dụ: getName(), getTenSach()...)
                // thì hãy thay thế .getTitle() bằng hàm đó.
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }

        // Cập nhật danh sách mới đã lọc vào adapter để làm mới RecyclerView
        if (bookAdapter != null) {
            bookAdapter.setFilteredList(filteredList);
        }
    }
}