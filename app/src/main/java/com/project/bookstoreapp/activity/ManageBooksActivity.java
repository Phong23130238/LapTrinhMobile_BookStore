package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

        // Ánh xạ View
        rvAdminBooks = findViewById(R.id.rvAdminBooks);
        FloatingActionButton fabAddBook = findViewById(R.id.fabAddBook);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageBooks);

        // Nút Back trên Toolbar
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Cài đặt danh sách dạng dọc (1 cột)
        rvAdminBooks.setLayoutManager(new LinearLayoutManager(this));

        // QUAN TRỌNG: Phải khởi tạo danh sách trước khi add dữ liệu để tránh lỗi văng app
        adminBookList = new ArrayList<>();

        // Các cuốn sách đang hiển thị bình thường (isHidden = false)
        adminBookList.add(new Book(1, "Đắc Nhân Tâm", "Dale Carnegie", 85000, 0, false));
        adminBookList.add(new Book(2, "Nhà Giả Kim", "Paulo Coelho", 79000, 0, false));
        adminBookList.add(new Book(3, "Tôi Thấy Hoa Vàng Trên Cỏ Xanh", "Nguyễn Nhật Ánh", 120000, 0, false));
        // Giả sử cuốn này đang tạm hết hàng, Admin chọn Ẩn (isHidden = true)
        adminBookList.add(new Book(4, "Muôn Kiếp Nhân Sinh", "Nguyên Phong", 168000, 0, true));

        adminBookAdapter = new BookAdapter(adminBookList);
        rvAdminBooks.setAdapter(adminBookAdapter);

        // --- CODE MỚI THÊM VÀO ---
        // Bắt sự kiện khi Admin bấm vào một cuốn sách bất kỳ trong danh sách (Chế độ SỬA)
        adminBookAdapter.setOnItemClickListener(new BookAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Book selectedBook) {
                // Khởi tạo chiếc xe Intent
                Intent intent = new Intent(ManageBooksActivity.this, AddEditBookActivity.class);

                // Đóng gói cuốn sách được chọn và dán nhãn "BOOK_DATA"
                intent.putExtra("BOOK_DATA", selectedBook);

                // Chuyển trang
                startActivity(intent);
            }
        });
        // ------------------------

        // Sự kiện khi bấm nút dấu CỘNG (Chế độ THÊM MỚI)
        fabAddBook.setOnClickListener(v -> {
            // Không có putExtra vì đây là sách mới hoàn toàn
            startActivity(new Intent(ManageBooksActivity.this, AddEditBookActivity.class));
        });
    }
}