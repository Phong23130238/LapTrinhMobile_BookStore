package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookAdapter;
import com.project.bookstoreapp.model.Book;
import com.project.bookstoreapp.model.Category;

import java.util.ArrayList;
import java.util.List;

public class AllBooksActivity extends AppCompatActivity {

    private RecyclerView rvAllBooks;
    private SearchView searchViewBooks;
    private Spinner spinCategoryFilter, spinPriceFilter;

    private BookAdapter bookAdapter;
    private List<Book> allBooksList; // Chứa TOÀN BỘ sách tải từ Firebase
    private List<Book> filteredBooksList; // Chứa sách sau khi đã lọc

    private List<Category> categoryList;
    private List<String> categoryNames;

    private FirebaseFirestore db;

    // Biến lưu trạng thái bộ lọc hiện tại
    private String currentSearchText = "";
    private String currentCategoryId = "ALL";
    private int currentPriceFilterIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_books);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupPriceSpinner();

        // Tải dữ liệu
        loadCategoriesFromFirebase();
        loadAllBooksFromFirebase();

        // Xử lý sự kiện tìm kiếm
        searchViewBooks.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchText = query;
                applyFilters();
                searchViewBooks.clearFocus(); // Ẩn bàn phím khi ấn Enter
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText;
                applyFilters();
                return true;
            }
        });
    }

    private void initViews() {
        rvAllBooks = findViewById(R.id.rvAllBooks);
        searchViewBooks = findViewById(R.id.searchViewBooks);
        spinCategoryFilter = findViewById(R.id.spinCategoryFilter);
        spinPriceFilter = findViewById(R.id.spinPriceFilter);

        allBooksList = new ArrayList<>();
        filteredBooksList = new ArrayList<>();
        categoryList = new ArrayList<>();
        categoryNames = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAllBooks);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        rvAllBooks.setLayoutManager(new GridLayoutManager(this, 2));
        bookAdapter = new BookAdapter(filteredBooksList);

        // Xử lý khi bấm vào 1 quyển sách
        bookAdapter.setOnItemClickListener(book -> {
            Intent intent = new Intent(AllBooksActivity.this, BookDetailActivity.class);
            intent.putExtra("BOOK_ID", book.getBookId());
            startActivity(intent);
        });

        rvAllBooks.setAdapter(bookAdapter);
    }

    // Thiết lập các mức giá mặc định
    private void setupPriceSpinner() {
        String[] priceRanges = {
                "Tất cả mức giá",
                "Dưới 100.000 đ",
                "100.000 đ - 300.000 đ",
                "Trên 300.000 đ"
        };

        ArrayAdapter<String> priceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priceRanges);
        priceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinPriceFilter.setAdapter(priceAdapter);

        spinPriceFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPriceFilterIndex = position;
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCategoriesFromFirebase() {
        db.collection("categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryList.clear();
            categoryNames.clear();

            // Thêm mục mặc định
            categoryList.add(new Category("ALL", "Tất cả thể loại", 0));
            categoryNames.add("Tất cả thể loại");

            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Category category = doc.toObject(Category.class);
                if (category != null) {
                    category.setCategoryId(doc.getId());
                    categoryList.add(category);
                    categoryNames.add(category.getName());
                }
            }

            // Đưa dữ liệu vào Spinner
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinCategoryFilter.setAdapter(catAdapter);

            spinCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentCategoryId = categoryList.get(position).getCategoryId();
                    applyFilters();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        }).addOnFailureListener(e -> Log.e("AllBooksActivity", "Lỗi tải thể loại", e));
    }

    private void loadAllBooksFromFirebase() {
        db.collection("books").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allBooksList.clear();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Book book = doc.toObject(Book.class);
                if (book.getBookId() == null || book.getBookId().isEmpty()) {
                    book.setBookId(doc.getId());
                }

                if (!book.isHidden()) {
                    allBooksList.add(book);
                }
            }

            // Sắp xếp ưu tiên sách có ảnh lên đầu
            java.util.Collections.sort(allBooksList, (b1, b2) -> {
                boolean hasImage1 = b1.getImageUrl() != null && b1.getImageUrl().trim().startsWith("http");
                boolean hasImage2 = b2.getImageUrl() != null && b2.getImageUrl().trim().startsWith("http");
                if (hasImage1 && !hasImage2) return -1;
                if (!hasImage1 && hasImage2) return 1;
                return 0;
            });

            applyFilters(); // Lọc và hiển thị dữ liệu ngay khi tải xong

        }).addOnFailureListener(e -> Log.e("AllBooksActivity", "Lỗi tải sách", e));
    }

    // Hàm quan trọng nhất: Xử lý gộp cả 3 bộ lọc
    private void applyFilters() {
        filteredBooksList.clear();
        String searchText = currentSearchText.toLowerCase().trim();

        for (Book book : allBooksList) {
            boolean matchName = true;
            boolean matchCategory = true;
            boolean matchPrice = true;

            // 1. Kiểm tra Tìm kiếm theo tên
            if (!searchText.isEmpty()) {
                if (book.getTitle() == null || !book.getTitle().toLowerCase().contains(searchText)) {
                    matchName = false;
                }
            }

            // 2. Kiểm tra Thể loại
            if (!currentCategoryId.equals("ALL")) {
                if (book.getCategoryId() == null || !book.getCategoryId().equals(currentCategoryId)) {
                    matchCategory = false;
                }
            }

            // 3. Kiểm tra Giá tiền
            long price = (long) book.getPrice();
            if (currentPriceFilterIndex == 1) { // Dưới 100k
                if (price >= 100000) matchPrice = false;
            } else if (currentPriceFilterIndex == 2) { // 100k - 300k
                if (price < 100000 || price > 300000) matchPrice = false;
            } else if (currentPriceFilterIndex == 3) { // Trên 300k
                if (price <= 300000) matchPrice = false;
            }

            // Nếu thỏa mãn cả 3 điều kiện thì mới thêm vào danh sách hiển thị
            if (matchName && matchCategory && matchPrice) {
                filteredBooksList.add(book);
            }
        }

        // Nếu adapter của bạn có hàm setFilteredList thì dùng nó,
        // ở đây mình dùng notifyDataSetChanged() trực tiếp lên list truyền vào adapter
        if (bookAdapter != null) {
            bookAdapter.setFilteredList(filteredBooksList);
        }
    }

    // Xử lý nút Back trên Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}