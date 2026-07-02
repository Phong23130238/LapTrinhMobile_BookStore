package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.OrderAdapter;
import com.project.bookstoreapp.model.Order;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView rvAdminOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;        // Danh sách gốc từ Firebase
    private List<Order> sortedList;       // Danh sách sau khi sort (trước khi filter)

    // Tìm kiếm, lọc và sắp xếp
    private SearchView searchViewOrders;
    private ChipGroup chipGroupOrderStatus;
    private Spinner spinnerSort;
    private TextView tvOrderCount;

    private String currentSearchQuery = "";
    private String currentStatusFilter = "All";
    private boolean sortNewest = true;   // true = Mới nhất → Cũ nhất

    private static final String[] SORT_OPTIONS = {"Mới nhất trước", "Cũ nhất trước"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        // Ánh xạ View
        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageOrders);
        searchViewOrders = findViewById(R.id.searchViewOrders);
        chipGroupOrderStatus = findViewById(R.id.chipGroupOrderStatus);
        spinnerSort = findViewById(R.id.spinnerSort);
        tvOrderCount = findViewById(R.id.tvOrderCount);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter
        orderList = new ArrayList<>();
        sortedList = new ArrayList<>();
        orderAdapter = new OrderAdapter(sortedList, clickedOrder -> {
            Intent intent = new Intent(ManageOrdersActivity.this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", clickedOrder.getOrderId());
            intent.putExtra("IS_ADMIN", true); // Truyền flag Admin
            startActivity(intent);
        });
        rvAdminOrders.setAdapter(orderAdapter);

        // Setup Spinner sort
        setupSortSpinner();

        // Lắng nghe thanh tìm kiếm
        searchViewOrders.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText.trim().toLowerCase();
                filterOrders();
                return true;
            }
        });

        // Lắng nghe Chip lọc trạng thái
        chipGroupOrderStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentStatusFilter = "All";
            } else if (checkedId == R.id.chipPending) {
                currentStatusFilter = "pending";
            } else if (checkedId == R.id.chipConfirmed) {
                currentStatusFilter = "confirmed";
            } else if (checkedId == R.id.chipShipping) {
                currentStatusFilter = "shipping";
            } else if (checkedId == R.id.chipDelivered) {
                currentStatusFilter = "delivered";
            } else if (checkedId == R.id.chipCancelled) {
                currentStatusFilter = "cancelled";
            }
            filterOrders();
        });

        // Tải dữ liệu từ Firebase
        loadOrdersFromFirebase();
    }

    // --- Setup Spinner ---
    private void setupSortSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                SORT_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortNewest = (position == 0); // 0 = Mới nhất, 1 = Cũ nhất
                applySortAndFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // --- Sắp xếp danh sách gốc rồi mới lọc ---
    private void applySortAndFilter() {
        // Sao chép danh sách gốc ra để sort
        sortedList.clear();
        sortedList.addAll(orderList);

        // Sort theo createdAt
        final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        Collections.sort(sortedList, (o1, o2) -> {
            Date d1 = parseDate(o1.getCreatedAt(), sdf1, sdf2);
            Date d2 = parseDate(o2.getCreatedAt(), sdf1, sdf2);
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return sortNewest ? d2.compareTo(d1) : d1.compareTo(d2);
        });

        filterOrders();
    }

    private Date parseDate(String dateStr, SimpleDateFormat sdf1, SimpleDateFormat sdf2) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try { return sdf1.parse(dateStr); } catch (ParseException e1) {
            try { return sdf2.parse(dateStr); } catch (ParseException e2) { return null; }
        }
    }

    // --- Lọc kép: tìm kiếm + trạng thái ---
    private void filterOrders() {
        List<Order> filteredList = new ArrayList<>();

        for (Order order : sortedList) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;

            if (!currentSearchQuery.isEmpty()) {
                String displayId = (order.getDisplayId() != null) ? order.getDisplayId().toLowerCase() : "";
                if (!displayId.contains(currentSearchQuery)) {
                    matchesSearch = false;
                }
            }

            if (!currentStatusFilter.equals("All")) {
                if (order.getStatus() == null || !order.getStatus().equalsIgnoreCase(currentStatusFilter)) {
                    matchesStatus = false;
                }
            }

            if (matchesSearch && matchesStatus) {
                filteredList.add(order);
            }
        }

        // Cập nhật số lượng đơn
        tvOrderCount.setText(filteredList.size() + " đơn");

        // Cập nhật giao diện
        orderAdapter.filterList(filteredList);
    }

    // --- Tải dữ liệu từ Firebase ---
    private void loadOrdersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                orderList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Order order = document.toObject(Order.class);
                        if (order.getOrderId() == null) {
                            order.setOrderId(document.getId());
                        }
                        if (order.getDisplayId() == null) {
                            order.setDisplayId(document.getId());
                        }
                        orderList.add(order);
                    } catch (Exception e) {
                        Log.e("Firebase_Error", "Lỗi dữ liệu đơn hàng admin: " + document.getId(), e);
                    }
                }
                // Sort + filter sau khi load xong
                applySortAndFilter();
            } else {
                Toast.makeText(ManageOrdersActivity.this, "Lỗi tải Đơn hàng", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting orders: ", task.getException());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload khi quay lại (sau khi Admin cập nhật trạng thái)
        loadOrdersFromFirebase();
    }
}