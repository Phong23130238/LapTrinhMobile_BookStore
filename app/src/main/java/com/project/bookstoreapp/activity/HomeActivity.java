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
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.model.Book;

import java.util.ArrayList;
import java.util.List;

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
    }

    private void loadBooksFromFirebase() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("BOOKS")
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