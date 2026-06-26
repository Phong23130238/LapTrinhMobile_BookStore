package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;

public class AddEditBookActivity extends AppCompatActivity {

    private TextInputEditText edtTitle, edtAuthor, edtPrice;
    private SwitchMaterial switchHideBook;
    private MaterialButton btnSaveBook;
    private Book currentBook = null; // Biến lưu sách nếu đang ở chế độ SỬA

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_book);

        // Ánh xạ
        MaterialToolbar toolbar = findViewById(R.id.toolbarAddEdit);
        edtTitle = findViewById(R.id.edtBookTitle);
        edtAuthor = findViewById(R.id.edtBookAuthor);
        edtPrice = findViewById(R.id.edtBookPrice);
        switchHideBook = findViewById(R.id.switchHideBook);
        btnSaveBook = findViewById(R.id.btnSaveBook);

        // Nút Back
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Kiểm tra xem là chế độ SỬA hay THÊM
        if (getIntent() != null && getIntent().hasExtra("BOOK_DATA")) {
            // Đây là chế độ SỬA
            currentBook = (Book) getIntent().getSerializableExtra("BOOK_DATA");
            toolbar.setTitle("Cập nhật Sản Phẩm");

            // Đổ dữ liệu cũ lên giao diện
            edtTitle.setText(currentBook.getTitle());
            edtAuthor.setText(currentBook.getAuthor());
            // Loại bỏ phần thập phân để hiển thị đẹp hơn (.0)
            edtPrice.setText(String.valueOf((long) currentBook.getPrice()));
            switchHideBook.setChecked(currentBook.isHidden());
        } else {
            // Đây là chế độ THÊM
            toolbar.setTitle("Thêm Sản Phẩm Mới");
        }

        // Bắt sự kiện bấm nút LƯU
        btnSaveBook.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String author = edtAuthor.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            boolean isHidden = switchHideBook.isChecked();

            if (title.isEmpty() || author.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);

            if (currentBook == null) {
                // Tương lai: Gọi API Node.js / DAO SQLite để THÊM mới
                Toast.makeText(this, "Đã thêm mới: " + title + " (Ẩn: " + isHidden + ")", Toast.LENGTH_LONG).show();
            } else {
                // Tương lai: Gọi API Node.js / DAO SQLite để CẬP NHẬT theo ID
                Toast.makeText(this, "Đã cập nhật: " + title + " (Ẩn: " + isHidden + ")", Toast.LENGTH_LONG).show();
            }

            // Đóng màn hình quay về trang quản lý
            finish();
        });
    }
}