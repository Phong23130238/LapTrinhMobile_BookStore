package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//import com.bumptech.glide.Glide;
import com.project.bookstoreapp.R;
public class BookDetailActivity extends AppCompatActivity {

    private ImageView ivCover;
    private TextView tvTitle, tvAuthor, tvPrice, tvDescription;
    private Button btnAddToCart, btnWriteReview;

    private String currentBookId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        initViews();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
//Lấy ID từ home
        currentBookId = getIntent().getStringExtra("BOOK_ID");

        if (currentBookId != null && !currentBookId.isEmpty()) {
            // gọi Firebase tải dữ liệu
            loadBookData(currentBookId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sách!", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnWriteReview = findViewById(R.id.btnWriteReview);
    }

    private void loadBookData(String bookId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books")
                .document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy dữ liệu từ Firebase ra
                        String title = documentSnapshot.getString("title");
                        String author = documentSnapshot.getString("author");
                        Double price = documentSnapshot.getDouble("price");
                        String description = documentSnapshot.getString("description");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        // Đẩy dữ liệu lên màn hình dt
                        tvTitle.setText(title);
                        tvAuthor.setText(author);
                        tvDescription.setText(description);

                        // Format giá tiền
                        if (price != null) {
                            java.text.DecimalFormat formatter = new java.text.DecimalFormat("###,###,###");
                            tvPrice.setText(formatter.format(price) + " đ");
                        }

                        // Tải ảnh bìa (Bạn cần cài thư viện Glide trong build.gradle)
//                         Glide.with(this).load(imageUrl).into(ivCover);

                    } else {
                        Toast.makeText(this, "Cuốn sách này không còn tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}