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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.StockLogAdapter;
import com.project.bookstoreapp.model.StockLog;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private ProgressBar pbHistory;
    private TextView tvEmpty;
    private SearchView svHistory;
    private Spinner spinnerSortHistory;

    private StockLogAdapter adapter;
    private List<StockLog> originalLogList;
    private List<StockLog> displayLogList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_inventory_history, container, false);

        rvHistory = v.findViewById(R.id.rvInventoryHistory);
        pbHistory = v.findViewById(R.id.pbHistory);
        tvEmpty = v.findViewById(R.id.tvHistoryEmpty);
        svHistory = v.findViewById(R.id.svHistory);
        spinnerSortHistory = v.findViewById(R.id.spinnerSortHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        originalLogList = new ArrayList<>();
        displayLogList = new ArrayList<>();
        adapter = new StockLogAdapter(displayLogList);
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        setupSearchAndSort();

        return v;
    }

    private void setupSearchAndSort() {
        // Setup Spinner
        String[] sortOptions = {"Mới nhất", "Cũ nhất"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortHistory.setAdapter(spinnerAdapter);

        spinnerSortHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilterAndSort(svHistory.getQuery().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup SearchView
        svHistory.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        displayLogList.clear();
        String q = query.toLowerCase().trim();

        // Lọc
        for (StockLog log : originalLogList) {
            if (log.getBookTitle() != null && log.getBookTitle().toLowerCase().contains(q)) {
                displayLogList.add(log);
            }
        }

        // Sắp xếp
        int sortMode = spinnerSortHistory.getSelectedItemPosition();
        if (sortMode == 0) { // Mới nhất (Giảm dần theo thời gian)
            Collections.sort(displayLogList, (l1, l2) -> l2.getCreatedAt().compareTo(l1.getCreatedAt()));
        } else if (sortMode == 1) { // Cũ nhất (Tăng dần theo thời gian)
            Collections.sort(displayLogList, (l1, l2) -> l1.getCreatedAt().compareTo(l2.getCreatedAt()));
        }

        adapter.notifyDataSetChanged();
        
        if (displayLogList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        pbHistory.setVisibility(View.VISIBLE);
        // Sắp xếp theo ngày tạo mới nhất lên trên
        db.collection("stock_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    pbHistory.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        originalLogList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            StockLog log = doc.toObject(StockLog.class);
                            if (log.getLogId() == null) log.setLogId(doc.getId());
                            originalLogList.add(log);
                        }

                        applyFilterAndSort(svHistory.getQuery().toString());

                    } else {
                        Toast.makeText(getContext(), "Lỗi tải lịch sử!", Toast.LENGTH_SHORT).show();
                        Log.e("HistoryFragment", "Error getting documents: ", task.getException());
                    }
                });
    }
}
