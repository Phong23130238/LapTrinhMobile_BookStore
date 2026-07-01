package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // IMPORT MỚI
import androidx.recyclerview.widget.GridLayoutManager; // IMPORT MỚI
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
    private SearchView searchViewBooks; // Khai báo thêm thanh tìm kiếm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_books);

        rvAdminBooks = findViewById(R.id.rvAdminBooks);
        FloatingActionButton fabAddBook = findViewById(R.id.fabAddBook);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageBooks);
        searchViewBooks = findViewById(R.id.searchViewBooks); // Ánh xạ SearchView

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 1. NÂNG CẤP: Chuyển sang giao diện lưới (Grid) 2 cột thay vì List 1 cột
        rvAdminBooks.setLayoutManager(new GridLayoutManager(this, 2));

        // Khởi tạo danh sách rỗng và gắn Adapter
        adminBookList = new ArrayList<>();
        adminBookAdapter = new BookAdapter(adminBookList);
        rvAdminBooks.setAdapter(adminBookAdapter);

        // Gọi hàm lấy dữ liệu từ Firebase
        loadBooksFromFirebase();

        // 2. NÂNG CẤP: Bắt sự kiện gõ phím trên thanh tìm kiếm
        searchViewBooks.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Xảy ra khi người dùng bấm nút "Enter" trên bàn phím
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Lọc dữ liệu Real-time ngay khi gõ từng chữ
                filterBooks(newText);
                return true;
            }
        });

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

    // 3. THUẬT TOÁN TÌM KIẾM
    private void filterBooks(String text) {
        List<Book> filteredList = new ArrayList<>();
        for (Book item : adminBookList) {
            // Kiểm tra tên sách (Chống lỗi Null và chuyển về chữ thường để tìm chính xác)
            if (item.getTitle() != null && item.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }

        // Cập nhật lại Adapter với danh sách đã lọc
        adminBookAdapter.filterList(filteredList);
    }

    private void loadBooksFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("books").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                adminBookList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    if (book.getBookId() == null) {
                        book.setBookId(document.getId());
                    }
                    adminBookList.add(book);
                }
                adminBookAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(ManageBooksActivity.this, "Lỗi khi tải danh sách Sách!", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting books: ", task.getException());
            }
        });
    }
}