package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.model.Book;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvBooks;
    private BookAdapter bookAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- CODE MỚI THÊM VÀO ---
        rvBooks = findViewById(R.id.rvBooks);

        // Tạo giao diện lưới (Grid) 2 cột giống các app bán hàng
        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));

        // Tạo danh sách dữ liệu ảo (Mock Data)
        List<Book> mockList = new ArrayList<>();
// Khách hàng chỉ thấy các sách có isHidden = false
        mockList.add(new Book(1, "Đắc Nhân Tâm", "Dale Carnegie", 85000, 0, false));
        mockList.add(new Book(2, "Nhà Giả Kim", "Paulo Coelho", 79000, 0, false));
        mockList.add(new Book(3, "Tôi Thấy Hoa Vàng Trên Cỏ Xanh", "Nguyễn Nhật Ánh", 120000, 0, false));
        mockList.add(new Book(5, "Tuổi Trẻ Đáng Giá Bao Nhiêu", "Rosie Nguyễn", 95000, 0, false));
        mockList.add(new Book(6, "Cây Cam Ngọt Của Tôi", "José Mauro de Vasconcelos", 108000, 0, false));

        // Nạp dữ liệu vào Adapter và gắn vào RecyclerView
        bookAdapter = new BookAdapter(mockList);
        // Gắn sự kiện lắng nghe để mở trang Chi tiết
        bookAdapter.setOnItemClickListener(new BookAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Book book) {
                // Đây chính là đoạn code Intent của nhánh main được dời ra ngoài
                Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
                intent.putExtra("BOOK_ID", book.getId());
                startActivity(intent);
            }
        });
        rvBooks.setAdapter(bookAdapter);
        // ------------------------

        // Ánh xạ thanh Bottom Navigation (Giữ nguyên code cũ của bạn)
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
}