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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.adapter.CategoryAdapter;
import com.project.bookstoreapp.model.Book;
import com.project.bookstoreapp.model.Category;

import java.util.ArrayList;
import java.util.List;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageButton;
import android.view.View;
import com.bumptech.glide.Glide;
import com.project.bookstoreapp.utils.SessionManager;
import com.project.bookstoreapp.model.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvBooks;
    private RecyclerView rvCategories;

    private BookAdapter bookAdapter;
    private CategoryAdapter categoryAdapter;

    private List<Book> bookList;
    private List<Book> originalList;
    private List<Category> categoryList;

    private FirebaseFirestore db;
    
    private com.google.firebase.firestore.ListenerRegistration userListener;
    private com.google.firebase.firestore.ListenerRegistration cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        // Ánh xạ View
        rvBooks = findViewById(R.id.rvBooks);
        rvCategories = findViewById(R.id.rvCategories); // Bạn cần thêm id này cho RecyclerView ngang trong activity_home.xml
        TextView tvViewAll = findViewById(R.id.tvViewAll); // ID của nút "Xem tất cả"

        // Khởi tạo List
        bookList = new ArrayList<>();
        originalList = new ArrayList<>();
        categoryList = new ArrayList<>();

        // Setup RecyclerView Sách (Lưới 2 cột)
        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));
        bookAdapter = new BookAdapter(bookList);
        bookAdapter.setOnItemClickListener(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            intent.putExtra("BOOK_ID", book.getBookId());
            startActivity(intent);
        });
        rvBooks.setAdapter(bookAdapter);

        // Setup RecyclerView Thể loại (Ngang)
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Tải dữ liệu từ Firebase
        loadCategoriesFromFirebase();
        loadBooks("ALL"); // Mặc định tải tất cả sách bán chạy

        // Xử lý nút Xem tất cả
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AllBooksActivity.class);
                startActivity(intent);
            });
        }

        // Xử lý thanh tìm kiếm thời gian thực (Lọc trên danh sách 10 cuốn hiện tại)
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupHeader();
    }

    private void loadCategoriesFromFirebase() {
        // LƯU Ý: Hãy chắc chắn "categories" viết đúng hoa/thường như trên Firebase
        db.collection("categories").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                // Thêm mục mặc định "Tất cả"
                categoryList.add(new Category("ALL", "Tất cả", 0));

                // Đổ dữ liệu từ Firebase vào
                if (task.getResult() != null) {
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Category category = doc.toObject(Category.class);
                        if (category != null) {
                            category.setCategoryId(doc.getId());
                            categoryList.add(category);
                        }
                    }

                    // Hiện thông báo để test xem lấy được bao nhiêu cái
                    Toast.makeText(this, "Tải được: " + (categoryList.size() - 1) + " thể loại", Toast.LENGTH_SHORT).show();
                }

                // Cập nhật lại RecyclerView
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(categoryList, category -> {
                        loadBooks(category.getCategoryId());
                    });
                    rvCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.notifyDataSetChanged();
                }

            } else {
                Log.e("HomeActivity", "Lỗi tải thể loại", task.getException());
                Toast.makeText(this, "Lỗi kết nối khi tải thể loại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Tải 10 sách bán chạy nhất, có thể theo Category
    private void loadBooks(String categoryId) {
        Query query = db.collection("books");

        if (!categoryId.equals("ALL")) {
            query = query.whereEqualTo("categoryId", categoryId);
        }

        // Sắp xếp theo số lượng bán giảm dần, giới hạn 10 cuốn
        // Lưu ý: Cần có trường "sold" trong DB.
        query.orderBy("sold", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();
                    originalList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book.getBookId() == null || book.getBookId().isEmpty()) {
                            book.setBookId(doc.getId());
                        }

                        if (!book.isHidden()) {
                            bookList.add(book);
                        }
                    }

                    // Sắp xếp thẩm mỹ: ưu tiên sách có ảnh lên trước
                    java.util.Collections.sort(bookList, (b1, b2) -> {
                        boolean hasImage1 = b1.getImageUrl() != null && b1.getImageUrl().trim().startsWith("http");
                        boolean hasImage2 = b2.getImageUrl() != null && b2.getImageUrl().trim().startsWith("http");
                        if (hasImage1 && !hasImage2) return -1;
                        if (!hasImage1 && hasImage2) return 1;
                        return 0;
                    });

                    originalList.addAll(bookList);
                    bookAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeActivity", "Lỗi tải sách bán chạy: " + e.getMessage());
                    // Nếu lỗi do thiếu Index Firebase, log sẽ hiển thị một đường link
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
            if (user != null && user.getUid() != null) {
                if (userListener != null) userListener.remove();
                userListener = db.collection("users")
                        .document(user.getUid())
                        .addSnapshotListener(HomeActivity.this, (snapshot, error) -> {
                            if (error != null) return;
                            if (snapshot != null && snapshot.exists() && snapshot.getString("avatarUrl") != null) {
                                if (!isDestroyed() && !isFinishing()) {
                                    Glide.with(HomeActivity.this).load(snapshot.getString("avatarUrl")).into(ivAvatar);
                                }
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

        if (tvBadge != null && user != null && user.getUid() != null) {
            if (cartListener != null) cartListener.remove();
            cartListener = db.collection("carts")
                    .whereEqualTo("userId", user.getUid())
                    .addSnapshotListener(HomeActivity.this, (value, error) -> {
                        if (error != null) return;
                        if (value != null && !value.isEmpty()) {
                            tvBadge.setText(String.valueOf(value.size()));
                            tvBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvBadge.setVisibility(View.GONE);
                        }
                    });
        }
    }
}