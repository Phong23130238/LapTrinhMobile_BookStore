package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.ReviewAdapter;
import com.project.bookstoreapp.model.Review;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookDetailActivity extends AppCompatActivity {

    private ImageView ivCover, ivCoverBg;
    private TextView tvTitle, tvAuthor, tvPrice, tvDescription;
    private TextView tvOriginalPrice, tvRating, tvReviewCount, tvSold, tvCategory, tvStock, tvPublisher;
    private Button btnAddToCart, btnWriteReview;
    private RecyclerView rvReviews;
    private TextView tvNoReview;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    private String currentBookId = "";
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
        // currentBookId = "1"; // Để test tĩnh, hãy comment dòng này nếu dùng thật

        if (currentBookId != null && !currentBookId.isEmpty()) {
            loadBookData(currentBookId);
            loadReviews(currentBookId); // Đã chuyển qua dùng Node.js API
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sách!", Toast.LENGTH_SHORT).show();
        }
        
        ivCover.setOnClickListener(v -> {
            if (loadedImageUrl != null && !loadedImageUrl.isEmpty()) {
                showImageDialog();
            }
        });
        
        if (btnWriteReview != null) {
            btnWriteReview.setOnClickListener(v -> checkPurchaseAndReview());
        }
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
        
        // Review views
        rvReviews = findViewById(R.id.rvReviews);
        tvNoReview = findViewById(R.id.tvNoReview);
        
        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            reviewList = new ArrayList<>();
            reviewAdapter = new ReviewAdapter(this, reviewList);
            rvReviews.setAdapter(reviewAdapter);
        }

        if (tvOriginalPrice != null) {
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void loadBookData(String bookId) {
        FirebaseFirestore.getInstance()
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
                        if (category != null && tvCategory != null) tvCategory.setText(" " + category + " ");
                        
                        java.text.DecimalFormat formatter = new java.text.DecimalFormat("###,###,###");

                        if (price != null && tvPrice != null) {
                            tvPrice.setText(formatter.format(price) + " đ");
                        }
                        
                        if (originalPrice != null && originalPrice > 0 && originalPrice > (price != null ? price : 0) && tvOriginalPrice != null) {
                            tvOriginalPrice.setText(formatter.format(originalPrice) + " đ");
                            tvOriginalPrice.setVisibility(android.view.View.VISIBLE);
                        } else if (tvOriginalPrice != null) {
                            tvOriginalPrice.setVisibility(android.view.View.GONE);
                        }
                        
                        double rat = rating != null ? rating : 0;
                        int rCount = reviewCount != null ? reviewCount.intValue() : 0;
                        if (tvRating != null) tvRating.setText(String.valueOf(rat));
                        if(tvReviewCount != null) {
                            tvReviewCount.setText(rCount + " đánh giá");
                        }
                        
                        int soldCount = sold != null ? sold.intValue() : 0;
                        if (tvSold != null) tvSold.setText(String.valueOf(soldCount));

                        int stockCount = stock != null ? stock.intValue() : 0;
                        if (tvStock != null) tvStock.setText(String.valueOf(stockCount));
                        
                        String pub = publisher != null && !publisher.trim().isEmpty() ? publisher.trim() : "Đang cập nhật";
                        String year = publishedYear != null ? String.valueOf(publishedYear.intValue()) : "";
                        if (!year.isEmpty()) pub += " (" + year + ")";
                        if (tvPublisher != null) tvPublisher.setText("Nhà xuất bản: " + pub);

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

    private void loadReviews(String bookId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getReviews(bookId).enqueue(new Callback<ApiResponse<List<Review>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Review>>> call, Response<ApiResponse<List<Review>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    reviewList.clear();
                    if (response.body().getData() != null) {
                        reviewList.addAll(response.body().getData());
                    }
                    if (reviewAdapter != null) {
                        reviewAdapter.notifyDataSetChanged();
                    }
                    if (reviewList.isEmpty()) {
                        tvNoReview.setVisibility(android.view.View.VISIBLE);
                        rvReviews.setVisibility(android.view.View.GONE);
                    } else {
                        tvNoReview.setVisibility(android.view.View.GONE);
                        rvReviews.setVisibility(android.view.View.VISIBLE);
                    }
                } else {
                    Toast.makeText(BookDetailActivity.this, "Lỗi API tải đánh giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Review>>> call, Throwable t) {
                android.util.Log.e("API_ERROR", "Lỗi mạng", t);
                Toast.makeText(BookDetailActivity.this, "Lỗi kết nối Node.js Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPurchaseAndReview() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();
        
        HashMap<String, Object> body = new HashMap<>();
        body.put("userId", uid);
        body.put("bookId", currentBookId);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.checkPurchase(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if (response.body().isCanReview()) {
                        showWriteReviewDialog(uid, response.body().getOrderId());
                    } else {
                        Toast.makeText(BookDetailActivity.this, "Bạn cần mua và nhận hàng thành công cuốn sách này để có thể đánh giá!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    String err = "Lỗi: ";
                    if (!response.isSuccessful()) err += "HTTP " + response.code();
                    else if (response.body() == null) err += "Body null";
                    else if (!response.body().isSuccess()) err += response.body().getMessage();
                    Toast.makeText(BookDetailActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookDetailActivity.this, "Lỗi kết nối Node.js Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWriteReviewDialog(String uid, String orderId) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        dialog.setContentView(R.layout.dialog_write_review);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        android.widget.RatingBar ratingBar = dialog.findViewById(R.id.ratingBarReview);
        android.widget.EditText etComment = dialog.findViewById(R.id.etComment);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitReview);
        Button btnCancel = dialog.findViewById(R.id.btnCancelReview);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText().toString().trim();
            
            if (comment.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }
            
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String userName = "Khách hàng";
                        if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                            userName = documentSnapshot.getString("name");
                        }
                        submitReview(uid, userName, orderId, rating, comment, dialog);
                    })
                    .addOnFailureListener(e -> {
                        submitReview(uid, "Khách hàng", orderId, rating, comment, dialog);
                    });
        });
        
        dialog.show();
    }

    private void submitReview(String uid, String userName, String orderId, float rating, String comment, android.app.Dialog dialog) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("bookId", currentBookId);
        body.put("userId", uid);
        body.put("orderId", orderId);
        body.put("userName", userName);
        body.put("rating", rating);
        body.put("comment", comment);
        
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.submitReview(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(BookDetailActivity.this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadReviews(currentBookId);
                    // Tải lại thông tin sách để cập nhật rating mới hiển thị
                    loadBookData(currentBookId);
                } else {
                    Toast.makeText(BookDetailActivity.this, "Lỗi khi gửi đánh giá từ server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookDetailActivity.this, "Lỗi kết nối Node.js Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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