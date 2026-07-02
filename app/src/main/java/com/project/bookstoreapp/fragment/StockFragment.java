package com.project.bookstoreapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.InventoryAdapter;
import com.project.bookstoreapp.model.Book;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StockFragment extends Fragment {

    private RecyclerView rvStock;
    private ProgressBar pbStock;
    private TextView tvTotalBooks, tvLowStock;
    private SearchView svStock;
    private Spinner spinnerSortStock;

    private InventoryAdapter adapter;
    private List<Book> originalList;
    private List<Book> displayList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_inventory_stock, container, false);

        rvStock = v.findViewById(R.id.rvInventoryStock);
        pbStock = v.findViewById(R.id.pbStock);
        tvTotalBooks = v.findViewById(R.id.tvTotalBooks);
        tvLowStock = v.findViewById(R.id.tvLowStock);
        svStock = v.findViewById(R.id.svStock);
        spinnerSortStock = v.findViewById(R.id.spinnerSortStock);

        rvStock.setLayoutManager(new LinearLayoutManager(getContext()));
        originalList = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new InventoryAdapter(displayList);
        rvStock.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        setupSearchAndSort();

        return v;
    }

    private void setupSearchAndSort() {
        // Setup Spinner
        String[] sortOptions = {"Mặc định", "Tồn kho tăng dần", "Tồn kho giảm dần"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortStock.setAdapter(spinnerAdapter);

        spinnerSortStock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilterAndSort(svStock.getQuery().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup SearchView
        svStock.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilterAndSort(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilterAndSort(newText);
                return true;
            }
        });
    }

    private void applyFilterAndSort(String query) {
        displayList.clear();
        String q = query.toLowerCase().trim();

        // Lọc
        for (Book b : originalList) {
            if (b.getTitle() != null && b.getTitle().toLowerCase().contains(q)) {
                displayList.add(b);
            }
        }

        // Sắp xếp
        int sortMode = spinnerSortStock.getSelectedItemPosition();
        if (sortMode == 1) { // Tăng dần
            Collections.sort(displayList, (b1, b2) -> Integer.compare(b1.getStock(), b2.getStock()));
        } else if (sortMode == 2) { // Giảm dần
            Collections.sort(displayList, (b1, b2) -> Integer.compare(b2.getStock(), b1.getStock()));
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStock(); // Tự động reload khi quay lại tab này
    }

    private void loadStock() {
        pbStock.setVisibility(View.VISIBLE);
        db.collection("books").get().addOnCompleteListener(task -> {
            pbStock.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                originalList.clear();
                int totalBooks = 0;
                int lowStock = 0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Book book = doc.toObject(Book.class);
                    if (book.getBookId() == null) book.setBookId(doc.getId());
                    originalList.add(book);

                    totalBooks++;
                    if (book.getStock() < 5) {
                        lowStock++;
                    }
                }

                tvTotalBooks.setText(String.valueOf(totalBooks));
                tvLowStock.setText(String.valueOf(lowStock));
                
                applyFilterAndSort(svStock.getQuery().toString());
            } else {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu kho!", Toast.LENGTH_SHORT).show();
                Log.e("StockFragment", "Error getting documents: ", task.getException());
            }
        });
    }
}
