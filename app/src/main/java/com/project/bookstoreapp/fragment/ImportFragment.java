package com.project.bookstoreapp.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;
import com.project.bookstoreapp.model.InventoryReceipt;
import com.project.bookstoreapp.model.StockLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImportFragment extends Fragment {

    private AutoCompleteTextView actvBookSelect;
    private TextInputEditText etQuantity, etCost, etDate, etNote;
    private MaterialButton btnSave;
    private ProgressBar pbImport;

    private FirebaseFirestore db;
    private List<Book> allBooks = new ArrayList<>();
    private List<String> bookTitles = new ArrayList<>();
    private Book selectedBook = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_inventory_import, container, false);

        actvBookSelect = v.findViewById(R.id.actvBookSelect);
        etQuantity = v.findViewById(R.id.etImportQuantity);
        etCost = v.findViewById(R.id.etImportCost);
        etDate = v.findViewById(R.id.etImportDate);
        etNote = v.findViewById(R.id.etImportNote);
        btnSave = v.findViewById(R.id.btnSaveImport);
        pbImport = v.findViewById(R.id.pbImport);

        db = FirebaseFirestore.getInstance();

        // Gán ngày hiện tại làm mặc định
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        loadBooksForDropdown();

        actvBookSelect.setOnItemClickListener((parent, view, position, id) -> {
            String title = (String) parent.getItemAtPosition(position);
            for (Book b : allBooks) {
                if (b.getTitle().equals(title)) {
                    selectedBook = b;
                    break;
                }
            }
        });

        btnSave.setOnClickListener(view -> saveImportReceipt());

        return v;
    }

    private void loadBooksForDropdown() {
        db.collection("books").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allBooks.clear();
            bookTitles.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Book book = doc.toObject(Book.class);
                if (book.getBookId() == null) book.setBookId(doc.getId());
                allBooks.add(book);
                if (book.getTitle() != null) {
                    bookTitles.add(book.getTitle());
                }
            }
            if (getContext() != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, bookTitles);
                actvBookSelect.setAdapter(adapter);
            }
        });
    }

    private void saveImportReceipt() {
        if (selectedBook == null) {
            Toast.makeText(getContext(), "Vui lòng chọn sách", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQuantity.getText().toString().trim();
        String costStr = etCost.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();

        if (TextUtils.isEmpty(qtyStr) || TextUtils.isEmpty(costStr) || TextUtils.isEmpty(dateStr)) {
            Toast.makeText(getContext(), "Vui lòng điền đủ Số lượng, Giá nhập và Ngày", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(qtyStr);
        long costPrice = Long.parseLong(costStr);

        if (quantity <= 0) {
            Toast.makeText(getContext(), "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }

        pbImport.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // 1. Tạo Phiếu nhập kho (InventoryReceipt)
        InventoryReceipt receipt = new InventoryReceipt(
                selectedBook.getBookId(),
                selectedBook.getTitle(),
                selectedBook.getImageUrl(),
                quantity,
                costPrice,
                dateStr,
                noteStr
        );

        db.collection("inventory_receipts").add(receipt).addOnSuccessListener(docRef -> {
            String receiptId = docRef.getId();
            String displayId = "PN-" + receiptId.substring(0, Math.min(5, receiptId.length())).toUpperCase();

            // Cập nhật lại ID cho phiếu
            docRef.update("receiptId", receiptId, "receiptDisplayId", displayId);

            // 2. Ghi Log vào stock_logs
            StockLog log = new StockLog(
                    selectedBook.getBookId(),
                    selectedBook.getTitle(),
                    "import",
                    quantity,
                    "receipt",
                    receiptId,
                    "Phiếu nhập kho #" + displayId
            );
            db.collection("stock_logs").add(log);

            // 3. Cập nhật số lượng tồn vào collection `books`
            int newStock = selectedBook.getStock() + quantity;
            db.collection("books").document(selectedBook.getBookId())
                    .update("stock", newStock)
                    .addOnSuccessListener(aVoid -> {
                        pbImport.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Nhập kho thành công!", Toast.LENGTH_LONG).show();

                        // Reset form
                        selectedBook = null;
                        actvBookSelect.setText("");
                        etQuantity.setText("");
                        etCost.setText("");
                        etNote.setText("");
                    })
                    .addOnFailureListener(e -> {
                        pbImport.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi cập nhật stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            pbImport.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(getContext(), "Lỗi tạo phiếu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
