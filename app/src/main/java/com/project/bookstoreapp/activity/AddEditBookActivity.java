package com.project.bookstoreapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;

import com.project.bookstoreapp.api.ApiClient;
import com.project.bookstoreapp.model.UploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditBookActivity extends AppCompatActivity {

    private TextInputEditText edtTitle, edtAuthor, edtPrice, edtOriginalPrice, edtStock, edtPublisher, edtDescription;
    private Spinner spinnerCategory;
    private SwitchMaterial switchHideBook;
    private MaterialButton btnSaveBook, btnChooseImage;
    private ImageView ivBookCover;

    private Book currentBook = null;
    private boolean isEditMode = false;

    private FirebaseFirestore db;
    private List<String> categoryIds = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String selectedCategoryId = "";

    // Biến xử lý ảnh
    private Uri selectedImageUri = null;
    private String finalImageUrl = "";

    // Mở thư viện ảnh
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(ivBookCover);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_book);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupSpinner();

        if (getIntent() != null && getIntent().hasExtra("BOOK_DATA")) {
            isEditMode = true;
            currentBook = (Book) getIntent().getSerializableExtra("BOOK_DATA");
            fillDataForEdit();
        }

        // Bắt sự kiện chọn ảnh
        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Bắt sự kiện lưu sách
        btnSaveBook.setOnClickListener(v -> saveBookData());
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAddEdit);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitle = findViewById(R.id.edtBookTitle);
        edtAuthor = findViewById(R.id.edtBookAuthor);
        edtPrice = findViewById(R.id.edtBookPrice);
        edtOriginalPrice = findViewById(R.id.edtOriginalPrice);
        edtStock = findViewById(R.id.edtStock);
        edtPublisher = findViewById(R.id.edtPublisher);
        edtDescription = findViewById(R.id.edtDescription);

        ivBookCover = findViewById(R.id.ivBookCover);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchHideBook = findViewById(R.id.switchHideBook);
        btnSaveBook = findViewById(R.id.btnSaveBook);
    }

    private void setupSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spinnerCategory.setAdapter(spinnerAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!categoryIds.isEmpty()) {
                    selectedCategoryId = categoryIds.get(position);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategoryId = "";
            }
        });

        loadCategoriesFromFirebase();
    }

    private void loadCategoriesFromFirebase() {
        db.collection("categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryIds.clear();
            categoryNames.clear();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                categoryIds.add(doc.getId());
                categoryNames.add(doc.getString("name") != null ? doc.getString("name") : "Không tên");
            }

            spinnerAdapter.notifyDataSetChanged();

            if (isEditMode && currentBook != null && currentBook.getCategoryId() != null) {
                int position = categoryIds.indexOf(currentBook.getCategoryId());
                if (position >= 0) {
                    spinnerCategory.setSelection(position);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show());
    }

    private void fillDataForEdit() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAddEdit);
        toolbar.setTitle("Cập nhật Sản Phẩm");

        edtTitle.setText(currentBook.getTitle());
        edtAuthor.setText(currentBook.getAuthor());
        edtPrice.setText(String.valueOf(currentBook.getPrice()));
        edtOriginalPrice.setText(String.valueOf(currentBook.getOriginalPrice()));
        edtStock.setText(String.valueOf(currentBook.getStock()));
        edtPublisher.setText(currentBook.getPublisher());
        edtDescription.setText(currentBook.getDescription());
        switchHideBook.setChecked(currentBook.isHidden());

        // Tải ảnh cũ
        finalImageUrl = currentBook.getImageUrl();
        if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
            Glide.with(this).load(finalImageUrl).into(ivBookCover);
        }
    }

    private void saveBookData() {
        String title = edtTitle.getText().toString().trim();
        String author = edtAuthor.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String stockStr = edtStock.getText().toString().trim();

        if (title.isEmpty() || author.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ các thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn thể loại!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đổi trạng thái nút
        btnSaveBook.setText("ĐANG XỬ LÝ...");
        btnSaveBook.setEnabled(false);

        // Upload ảnh trước, sau đó mới lưu data
        if (selectedImageUri != null) {
            uploadImageToServer();
        } else {
            if (isEditMode) {
                updateBookInFirebase(); // Giữ nguyên ảnh cũ
            } else {
                showError("Vui lòng chọn ảnh cho sách mới!");
            }
        }
    }

    private void uploadImageToServer() {
        try {
            File file = getFileFromUri(selectedImageUri);
            if (file == null) {
                showError("Không thể đọc file ảnh");
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("bookCover", file.getName(), requestFile);

            String bookIdForUpload = isEditMode ? currentBook.getBookId() : "";
            RequestBody idBody = RequestBody.create(MediaType.parse("text/plain"), bookIdForUpload);

            // GỌI API Retrofit
            ApiClient.getApiService().uploadBookCover(body, idBody).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        finalImageUrl = response.body().getImageUrl();

                        if (isEditMode) {
                            updateBookInFirebase();
                        } else {
                            uploadBookToFirebase();
                        }
                    } else {
                        showError("Upload ảnh thất bại từ Server");
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    showError("Lỗi kết nối máy chủ: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            showError("Lỗi xử lý file ảnh: " + e.getMessage());
        }
    }

    private File getFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = new File(getCacheDir(), "temp_image.jpg");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private void uploadBookToFirebase() {
        DocumentReference newBookRef = db.collection("books").document();

        Book newBook = new Book();
        newBook.setBookId(newBookRef.getId());
        newBook.setTitle(edtTitle.getText().toString().trim());
        newBook.setAuthor(edtAuthor.getText().toString().trim());
        newBook.setPublisher(edtPublisher.getText().toString().trim());
        newBook.setDescription(edtDescription.getText().toString().trim());
        newBook.setImageUrl(finalImageUrl); // Lưu link ảnh mới
        newBook.setCategoryId(selectedCategoryId);
        newBook.setHidden(switchHideBook.isChecked());

        try {
            newBook.setPrice(Long.parseLong(edtPrice.getText().toString().trim()));
            String origPriceStr = edtOriginalPrice.getText().toString().trim();
            newBook.setOriginalPrice(origPriceStr.isEmpty() ? 0 : Long.parseLong(origPriceStr));
            newBook.setStock(Integer.parseInt(edtStock.getText().toString().trim()));
        } catch (NumberFormatException e) {
            showError("Định dạng số không hợp lệ");
            return;
        }

        // Giá trị mặc định
        newBook.setSold(0);
        newBook.setRating(0.0);
        newBook.setReviewCount(0);

        newBookRef.set(newBook)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thêm sách thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> showError("Lỗi Firebase: " + e.getMessage()));
    }

    private void updateBookInFirebase() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", edtTitle.getText().toString().trim());
        updates.put("author", edtAuthor.getText().toString().trim());
        updates.put("publisher", edtPublisher.getText().toString().trim());
        updates.put("description", edtDescription.getText().toString().trim());
        updates.put("imageUrl", finalImageUrl); // Lưu link ảnh mới (hoặc giữ nguyên ảnh cũ)
        updates.put("categoryId", selectedCategoryId);
        updates.put("isHidden", switchHideBook.isChecked());

        try {
            updates.put("price", Long.parseLong(edtPrice.getText().toString().trim()));
            String origPriceStr = edtOriginalPrice.getText().toString().trim();
            updates.put("originalPrice", origPriceStr.isEmpty() ? 0 : Long.parseLong(origPriceStr));
            updates.put("stock", Integer.parseInt(edtStock.getText().toString().trim()));
        } catch (NumberFormatException e) {
            showError("Định dạng số không hợp lệ");
            return;
        }

        db.collection("books").document(currentBook.getBookId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật sách thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> showError("Lỗi Firebase: " + e.getMessage()));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        btnSaveBook.setText("LƯU THÔNG TIN");
        btnSaveBook.setEnabled(true);
    }
}