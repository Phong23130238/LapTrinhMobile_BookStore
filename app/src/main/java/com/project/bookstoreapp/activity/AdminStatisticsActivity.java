package com.project.bookstoreapp.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.BookStatAdapter;
import com.project.bookstoreapp.model.AdminStatsResponse;
import com.project.bookstoreapp.model.BookStat;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatisticsActivity extends AppCompatActivity {
    private static final String TAG = "AdminStats";
    
    private MaterialButton btnFromDate, btnToDate, btnFilterStats;
    private TextView tvTotalSoldQty, tvTotalInventory, tvTotalRevenue;
    private TextView tvEmptyTopBooks, tvEmptyUnsoldBooks;
    private RecyclerView rvTopBooks, rvUnsoldBooks;

    private BookStatAdapter topBooksAdapter;
    private BookStatAdapter unsoldBooksAdapter;
    
    private List<BookStat> topBooksList = new ArrayList<>();
    private List<BookStat> unsoldBooksList = new ArrayList<>();

    private final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private Calendar calendarFrom, calendarTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        initViews();
        setupToolbar();
        setupDatePickers();
        setupRecyclerViews();

        // Load data for current month by default
        loadStatistics();
    }

    private void initViews() {
        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);
        btnFilterStats = findViewById(R.id.btnFilterStats);
        
        tvTotalSoldQty = findViewById(R.id.tvTotalSoldQty);
        tvTotalInventory = findViewById(R.id.tvTotalInventory);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvEmptyTopBooks = findViewById(R.id.tvEmptyTopBooks);
        tvEmptyUnsoldBooks = findViewById(R.id.tvEmptyUnsoldBooks);
        rvTopBooks = findViewById(R.id.rvTopBooks);
        rvUnsoldBooks = findViewById(R.id.rvUnsoldBooks);

        btnFilterStats.setOnClickListener(v -> loadStatistics());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAdminStats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupDatePickers() {
        calendarFrom = Calendar.getInstance();
        calendarFrom.set(Calendar.DAY_OF_MONTH, 1); // Set to 1st of current month
        
        calendarTo = Calendar.getInstance();

        updateDateButtons();

        btnFromDate.setOnClickListener(v -> showDatePicker(calendarFrom, true));
        btnToDate.setOnClickListener(v -> showDatePicker(calendarTo, false));
    }

    private void showDatePicker(Calendar calendar, boolean isFromDate) {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateDateButtons() {
        btnFromDate.setText(displayFormat.format(calendarFrom.getTime()));
        btnToDate.setText(displayFormat.format(calendarTo.getTime()));
    }

    private void setupRecyclerViews() {
        rvTopBooks.setLayoutManager(new LinearLayoutManager(this));
        topBooksAdapter = new BookStatAdapter(this, topBooksList);
        rvTopBooks.setAdapter(topBooksAdapter);

        rvUnsoldBooks.setLayoutManager(new LinearLayoutManager(this));
        unsoldBooksAdapter = new BookStatAdapter(this, unsoldBooksList);
        rvUnsoldBooks.setAdapter(unsoldBooksAdapter);
    }

    private void loadStatistics() {
        if (calendarFrom.getTimeInMillis() > calendarTo.getTimeInMillis()) {
            Toast.makeText(this, "Từ ngày không được lớn hơn Đến ngày", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromDateStr = dateFormat.format(calendarFrom.getTime());
        String toDateStr = dateFormat.format(calendarTo.getTime());

        btnFilterStats.setEnabled(false);
        btnFilterStats.setText("Đang tải...");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getAdminStats(fromDateStr, toDateStr).enqueue(new Callback<ApiResponse<AdminStatsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AdminStatsResponse>> call, Response<ApiResponse<AdminStatsResponse>> response) {
                btnFilterStats.setEnabled(true);
                btnFilterStats.setText("Xem");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AdminStatsResponse stats = response.body().getData();
                    updateUI(stats);
                } else {
                    Toast.makeText(AdminStatisticsActivity.this, "Lỗi lấy dữ liệu từ server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AdminStatsResponse>> call, Throwable t) {
                btnFilterStats.setEnabled(true);
                btnFilterStats.setText("Xem");
                Log.e(TAG, "API call failed", t);
                Toast.makeText(AdminStatisticsActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(AdminStatsResponse stats) {
        tvTotalSoldQty.setText(String.valueOf(stats.getTotalSoldQty()));
        tvTotalInventory.setText(String.valueOf(stats.getTotalInventory()));
        tvTotalRevenue.setText(VND.format(stats.getTotalRevenue()) + " đ");

        topBooksList.clear();
        if (stats.getTopBooks() != null && !stats.getTopBooks().isEmpty()) {
            topBooksList.addAll(stats.getTopBooks());
            rvTopBooks.setVisibility(View.VISIBLE);
            tvEmptyTopBooks.setVisibility(View.GONE);
        } else {
            rvTopBooks.setVisibility(View.GONE);
            tvEmptyTopBooks.setVisibility(View.VISIBLE);
        }
        topBooksAdapter.notifyDataSetChanged();

        unsoldBooksList.clear();
        if (stats.getUnsoldBooks() != null && !stats.getUnsoldBooks().isEmpty()) {
            unsoldBooksList.addAll(stats.getUnsoldBooks());
            rvUnsoldBooks.setVisibility(View.VISIBLE);
            tvEmptyUnsoldBooks.setVisibility(View.GONE);
        } else {
            rvUnsoldBooks.setVisibility(View.GONE);
            tvEmptyUnsoldBooks.setVisibility(View.VISIBLE);
        }
        unsoldBooksAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
