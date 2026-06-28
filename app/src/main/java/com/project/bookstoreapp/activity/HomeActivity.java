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

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvBooks;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private List<Book> originalList; // BIẾN TẠM: Lưu trữ danh sách gốc không bị thay đổi

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
                    // Người dùng gõ đến đâu, gọi hàm lọc sách thời gian thực đến đó
                    filterBooks(s.toString());
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