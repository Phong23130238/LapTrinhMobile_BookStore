package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvBooks = findViewById(R.id.rvBooks);

        // Tạo giao diện lưới (Grid) 2 cột giống các app bán hàng
        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));

        // 1. Khởi tạo danh sách rỗng và gắn Adapter NGAY LẬP TỨC để tránh lỗi văng app
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(bookList);

        // Bắt sự kiện lắng nghe để mở trang Chi tiết
        bookAdapter.setOnItemClickListener(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            intent.putExtra("BOOK_ID", book.getId()); // Truyền String bookId
            startActivity(intent);
        });

        rvBooks.setAdapter(bookAdapter);

        // 2. Gọi hàm lấy dữ liệu thật từ Firebase
        loadBooksFromFirebase();

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
                bookList.clear(); // Xóa sạch dữ liệu cũ mỗi lần tải lại

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);

                    // Dự phòng trường hợp ID không nằm trong field mà nằm ở tên Document
                    if (book.getBookId() == null) {
                        book.setBookId(document.getId());
                    }

                    // LỌC DỮ LIỆU: Khách hàng chỉ thấy các sách đang mở bán (isHidden = false)
                    if (!book.isHidden()) {
                        bookList.add(book);
                    }
                }

                // Báo cho Adapter biết dữ liệu đã tải xong để hiển thị lên màn hình
                bookAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(HomeActivity.this, "Lỗi khi tải dữ liệu sách", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Lỗi tải sách HomeActivity: ", task.getException());
            }
        });
    }
}