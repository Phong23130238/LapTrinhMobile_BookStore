package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.model.Book;
import java.util.ArrayList;
import java.util.List;

public class ManageBooksActivity extends AppCompatActivity {

    private RecyclerView rvAdminBooks;
    private BookAdapter adminBookAdapter;
    private List<Book> adminBookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_books);

        rvAdminBooks = findViewById(R.id.rvAdminBooks);
        FloatingActionButton fabAddBook = findViewById(R.id.fabAddBook);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageBooks);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAdminBooks.setLayoutManager(new LinearLayoutManager(this));

        // 1. Khởi tạo danh sách rỗng và gắn Adapter TRƯỚC
        adminBookList = new ArrayList<>();
        adminBookAdapter = new BookAdapter(adminBookList);
        rvAdminBooks.setAdapter(adminBookAdapter);

        // 2. Gọi hàm lấy dữ liệu từ Firebase
        loadBooksFromFirebase();

        // Sự kiện sửa
        adminBookAdapter.setOnItemClickListener(selectedBook -> {
            Intent intent = new Intent(ManageBooksActivity.this, AddEditBookActivity.class);
            intent.putExtra("BOOK_DATA", selectedBook);
            startActivity(intent);
        });

        // Sự kiện thêm mới
        fabAddBook.setOnClickListener(v -> {
            startActivity(new Intent(ManageBooksActivity.this, AddEditBookActivity.class));
        });
    }

    private void loadBooksFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Truy vấn vào collection "books"
        db.collection("books").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                adminBookList.clear(); // Xóa dữ liệu cũ (nếu có)
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // Firebase tự động chuyển đổi Document thành Object Book
                    Book book = document.toObject(Book.class);

                    // (Tùy chọn) Nếu ID của sách nằm ở tên Document chứ không nằm trong field
                    if (book.getBookId() == null) {
                        book.setBookId(document.getId());
                    }

                    adminBookList.add(book);
                }
                // Báo cho Adapter biết dữ liệu đã thay đổi để vẽ lại giao diện
                adminBookAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi khi tải danh sách Sách!", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting books: ", task.getException());
            }
        });
    }
}