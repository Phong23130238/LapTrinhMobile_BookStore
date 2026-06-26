package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.project.bookstoreapp.R;

public class BookDetailActivity extends AppCompatActivity {

    private ImageView ivCover, ivCoverBg;
    private TextView tvTitle, tvAuthor, tvPrice, tvDescription;
    private TextView tvOriginalPrice, tvRating, tvReviewCount, tvSold, tvCategory, tvStock, tvPublisher;
    private Button btnAddToCart, btnWriteReview;

    private String currentBookId = "7";
    private String loadedImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        initViews();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Lấy ID từ trang chủ
        currentBookId = getIntent().getStringExtra("BOOK_ID");

        if (currentBookId != null && !currentBookId.isEmpty()) {
            loadBookData(currentBookId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sách!", Toast.LENGTH_SHORT).show();
        }
        
        ivCover.setOnClickListener(v -> {
            if (loadedImageUrl != null && !loadedImageUrl.isEmpty()) {
                showImageDialog();
            }
        });
    }

    private void initViews() {
        ivCover = findViewById(R.id.ivCover);
        ivCoverBg = findViewById(R.id.ivCoverBg);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvSold = findViewById(R.id.tvSold);
        tvCategory = findViewById(R.id.tvCategory);
        tvStock = findViewById(R.id.tvStock);
        tvPublisher = findViewById(R.id.tvPublisher);

        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnWriteReview = findViewById(R.id.btnWriteReview);
        
        if (tvOriginalPrice != null) {
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void loadBookData(String bookId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books")
                .document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String author = documentSnapshot.getString("author");
                        Double price = documentSnapshot.getDouble("price");
                        Double originalPrice = documentSnapshot.getDouble("originalPrice");
                        String description = documentSnapshot.getString("description");
                        loadedImageUrl = documentSnapshot.getString("imageUrl");
                        
                        String category = documentSnapshot.getString("category");
                        Double rating = documentSnapshot.getDouble("rating");
                        Double reviewCount = documentSnapshot.getDouble("reviewCount");
                        Double sold = documentSnapshot.getDouble("sold");
                        Double stock = documentSnapshot.getDouble("stock");
                        String publisher = documentSnapshot.getString("publisher");
                        Double publishedYear = documentSnapshot.getDouble("publishedYear");

                        if (title != null) tvTitle.setText(title);
                        if (author != null) tvAuthor.setText(author);
                        if (description != null) tvDescription.setText(description);
                        if (category != null) tvCategory.setText(" " + category + " ");
                        
                        java.text.DecimalFormat formatter = new java.text.DecimalFormat("###,###,###");

                        if (price != null) {
                            tvPrice.setText(formatter.format(price) + " đ");
                        }
                        
                        if (originalPrice != null && originalPrice > 0 && originalPrice > (price != null ? price : 0)) {
                            tvOriginalPrice.setText(formatter.format(originalPrice) + " đ");
                            tvOriginalPrice.setVisibility(android.view.View.VISIBLE);
                        } else {
                            tvOriginalPrice.setVisibility(android.view.View.GONE);
                        }
                        
                        double rat = rating != null ? rating : 0;
                        int rCount = reviewCount != null ? reviewCount.intValue() : 0;
                        tvRating.setText(String.valueOf(rat));
                        if(tvReviewCount != null) {
                            tvReviewCount.setText(rCount + " đánh giá");
                        }
                        
                        int soldCount = sold != null ? sold.intValue() : 0;
                        tvSold.setText(String.valueOf(soldCount));

                        int stockCount = stock != null ? stock.intValue() : 0;
                        tvStock.setText(String.valueOf(stockCount));
                        
                        String pub = publisher != null && !publisher.trim().isEmpty() ? publisher.trim() : "Đang cập nhật";
                        String year = publishedYear != null ? String.valueOf(publishedYear.intValue()) : "";
                        if (!year.isEmpty()) pub += " (" + year + ")";
                        tvPublisher.setText("Nhà xuất bản: " + pub);

                        if (loadedImageUrl != null && !loadedImageUrl.isEmpty()) {
                            Glide.with(this).load(loadedImageUrl).into(ivCover);
                            if (ivCoverBg != null) {
                                Glide.with(this).load(loadedImageUrl).into(ivCoverBg);
                            }
                        }

                    } else {
                        Toast.makeText(this, "Cuốn sách này không còn tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showImageDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_viewer);
        
        ImageView ivFull = dialog.findViewById(R.id.ivFull);
        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        
        Glide.with(this).load(loadedImageUrl).into(ivFull);
        
        btnClose.setOnClickListener(view -> dialog.dismiss());
        ivFull.setOnClickListener(view -> dialog.dismiss());
        
        dialog.show();
    }
}