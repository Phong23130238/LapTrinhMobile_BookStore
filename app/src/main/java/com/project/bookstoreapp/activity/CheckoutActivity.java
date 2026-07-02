package com.project.bookstoreapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.CheckoutAdapter;
import com.project.bookstoreapp.ghn.District;
import com.project.bookstoreapp.ghn.GHNItem;
import com.project.bookstoreapp.ghn.GHNOrderData;
import com.project.bookstoreapp.ghn.GHNOrderRequest;
import com.project.bookstoreapp.ghn.GHNResponse;
import com.project.bookstoreapp.ghn.Province;
import com.project.bookstoreapp.ghn.RetrofitClientGHN;
import com.project.bookstoreapp.ghn.Ward;
import com.project.bookstoreapp.model.CartItem;
import com.project.bookstoreapp.model.PaymentResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private static final Pattern VN_PHONE =
            Pattern.compile("^0[35789][0-9]{8}$");

    private long dynamicShippingFee = 0L;
    private static final long FREE_SHIP_MIN = 300_000L;

    private static final NumberFormat VND =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ---- UI ----
    private RecyclerView      rvCheckoutItems;
    private TextInputEditText etFullName, etPhone, etAddress, etNote, etVoucher;
    private TextInputLayout   tilFullName, tilPhone, tilAddress, tilVoucher;
    private RadioGroup        rgPaymentMethod;
    private TextView          tvSubtotal, tvShippingFee, tvVoucherDiscount, tvFinalTotal;
    private Button            btnApplyVoucher, btnPlaceOrder, btnBackToCart;
    private LinearLayout      layoutEmptyCheckout;
    private NestedScrollView  scrollCheckout;
    private View              bottomBar;

    private AutoCompleteTextView spinProvince, spinDistrict, spinWard;
    private List<Province> provinceList = new ArrayList<>();
    private List<District> districtList = new ArrayList<>();
    private List<Ward> wardList = new ArrayList<>();
    private Province selectedProvince = null;
    private District selectedDistrict = null;
    private Ward selectedWard = null;
    private final String GHN_TOKEN = "dffec2e1-6725-11f1-a973-aee5264794df";

    // ---- Data ----
    private ArrayList<CartItem> selectedItems      = new ArrayList<>();
    private CheckoutAdapter     checkoutAdapter;
    private long                subtotal           = 0L;
    private long                discountAmount     = 0L;
    private String              appliedVoucherCode = ""; 
    private String              currentUserId;

    // ---- Firebase ----
    private FirebaseFirestore db;

    // ---- Orders Logic ----
    private ActivityResultLauncher<Intent> vnpayLauncher;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Map<String, Object> pendingOrder = null;
    private GHNOrderRequest pendingGhnRequest = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        vnpayLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Thành công
                        if (pendingOrder != null) {
                            pendingOrder.put("paymentStatus", "paid");
                            saveOrderToFirestoreAndShowPopup("Thanh toán VNPAY thành công!");
                        }
                    } else {
                        // Thất bại
                        btnPlaceOrder.setEnabled(true);
                        btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                        Toast.makeText(this, "Thanh toán VNPAY thất bại hoặc bị hủy!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        db = FirebaseFirestore.getInstance();

        com.project.bookstoreapp.utils.SessionManager sessionManager = new com.project.bookstoreapp.utils.SessionManager(this);
        if (!sessionManager.isLoggedIn() || sessionManager.getUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = sessionManager.getUser().getUid();

        initViews();
        setupToolbar();
        receiveDataFromCart();
        
        com.project.bookstoreapp.model.User user = sessionManager.getUser();
        if (user != null) {
            if (user.getName() != null && !user.getName().isEmpty()) etFullName.setText(user.getName());
            if (user.getPhone() != null && !user.getPhone().isEmpty()) etPhone.setText(user.getPhone());
            if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                parseAndAutoFillAddress(user.getAddress());
            }
        }
    }

    private void initViews() {
        rvCheckoutItems   = findViewById(R.id.rvCheckoutItems);
        etFullName        = findViewById(R.id.etFullName);
        etPhone           = findViewById(R.id.etPhone);
        etAddress         = findViewById(R.id.etAddress);
        spinProvince      = findViewById(R.id.spinProvince);
        spinDistrict      = findViewById(R.id.spinDistrict);
        spinWard          = findViewById(R.id.spinWard);
        etNote            = findViewById(R.id.etNote);
        etVoucher         = findViewById(R.id.etVoucher);
        tilFullName       = findViewById(R.id.tilFullName);
        tilPhone          = findViewById(R.id.tilPhone);
        tilAddress        = findViewById(R.id.tilAddress);
        tilVoucher        = findViewById(R.id.tilVoucher);
        rgPaymentMethod   = findViewById(R.id.rgPaymentMethod);
        tvSubtotal        = findViewById(R.id.tvSubtotal);
        tvShippingFee     = findViewById(R.id.tvShippingFee);
        tvVoucherDiscount = findViewById(R.id.tvVoucherDiscount);
        tvFinalTotal      = findViewById(R.id.tvFinalTotal);
        btnApplyVoucher   = findViewById(R.id.btnApplyVoucher);
        btnPlaceOrder     = findViewById(R.id.btnPlaceOrder);
        btnBackToCart     = findViewById(R.id.btnBackToCart);
        layoutEmptyCheckout = findViewById(R.id.layoutEmptyCheckout);
        scrollCheckout    = findViewById(R.id.scrollCheckout);
        bottomBar         = findViewById(R.id.bottomCheckoutBar);

        btnApplyVoucher.setOnClickListener(v -> applyVoucher());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        if (btnBackToCart != null) btnBackToCart.setOnClickListener(v -> finish());

        spinProvince.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvince = provinceList.get(position);
            spinDistrict.setText("");
            spinWard.setText("");
            selectedDistrict = null;
            selectedWard = null;
            loadDistricts(selectedProvince.ProvinceID);
        });

        spinDistrict.setOnItemClickListener((parent, view, position, id) -> {
            selectedDistrict = districtList.get(position);
            spinWard.setText("");
            selectedWard = null;
            loadWards(selectedDistrict.DistrictID);
        });

        spinWard.setOnItemClickListener((parent, view, position, id) -> {
            selectedWard = wardList.get(position);
            calculateShippingFee();
        });

        loadProvinces();
    }

    private void loadProvinces() {
        RetrofitClientGHN.getApiService().getProvinces(GHN_TOKEN).enqueue(new Callback<GHNResponse<List<Province>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<Province>>> call, Response<GHNResponse<List<Province>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    provinceList = response.body().data;
                    ArrayAdapter<Province> adapter = new ArrayAdapter<>(CheckoutActivity.this, android.R.layout.simple_list_item_1, provinceList);
                    spinProvince.setAdapter(adapter);
                    
                    if (targetProvinceName != null && !targetProvinceName.isEmpty()) {
                        for (Province p : provinceList) {
                            if (p.ProvinceName != null && p.ProvinceName.equalsIgnoreCase(targetProvinceName)) {
                                selectedProvince = p;
                                spinProvince.setText(p.ProvinceName, false);
                                loadDistricts(p.ProvinceID);
                                break;
                            }
                        }
                        targetProvinceName = null;
                    }
                } else {
                    Toast.makeText(CheckoutActivity.this, "Lỗi tải Tỉnh: API trả về thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<Province>>> call, Throwable t) {
                Log.e("CheckoutActivity", "Lỗi tải Tỉnh: ", t);
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối GHN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDistricts(int provinceId) {
        RetrofitClientGHN.getApiService().getDistricts(GHN_TOKEN, provinceId).enqueue(new Callback<GHNResponse<List<District>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<District>>> call, Response<GHNResponse<List<District>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    districtList = response.body().data;
                    ArrayAdapter<District> adapter = new ArrayAdapter<>(CheckoutActivity.this, android.R.layout.simple_list_item_1, districtList);
                    spinDistrict.setAdapter(adapter);
                    
                    if (targetDistrictName != null && !targetDistrictName.isEmpty()) {
                        for (District d : districtList) {
                            if (d.DistrictName != null && d.DistrictName.equalsIgnoreCase(targetDistrictName)) {
                                selectedDistrict = d;
                                spinDistrict.setText(d.DistrictName, false);
                                loadWards(d.DistrictID);
                                break;
                            }
                        }
                        targetDistrictName = null;
                    }
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<District>>> call, Throwable t) {
                Log.e("CheckoutActivity", "Lỗi tải Quận/Huyện: ", t);
            }
        });
    }

    private void loadWards(int districtId) {
        RetrofitClientGHN.getApiService().getWards(GHN_TOKEN, districtId).enqueue(new Callback<GHNResponse<List<Ward>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<Ward>>> call, Response<GHNResponse<List<Ward>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    wardList = response.body().data;
                    ArrayAdapter<Ward> adapter = new ArrayAdapter<>(CheckoutActivity.this, android.R.layout.simple_list_item_1, wardList);
                    spinWard.setAdapter(adapter);
                    
                    if (targetWardName != null && !targetWardName.isEmpty()) {
                        for (Ward w : wardList) {
                            if (w.WardName != null && w.WardName.equalsIgnoreCase(targetWardName)) {
                                selectedWard = w;
                                spinWard.setText(w.WardName, false);
                                calculateShippingFee();
                                break;
                            }
                        }
                        targetWardName = null;
                    }
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<Ward>>> call, Throwable t) {
                Log.e("CheckoutActivity", "Lỗi tải Phường/Xã: ", t);
            }
        });
    }

    private void calculateShippingFee() {
        if (selectedDistrict == null || selectedWard == null) return;
        
        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang tính phí ship...");
        
        int toDistrictId = selectedDistrict.DistrictID;
        String toWardCode = selectedWard.WardCode;
        
        com.project.bookstoreapp.ghn.GHNFeeRequest request = new com.project.bookstoreapp.ghn.GHNFeeRequest(1452, "21211", toDistrictId, toWardCode);
        int SHOP_ID = 200948;
        
        RetrofitClientGHN.getApiService().calculateFee(GHN_TOKEN, SHOP_ID, request).enqueue(new Callback<GHNResponse<com.project.bookstoreapp.ghn.GHNFeeData>>() {
            @Override
            public void onResponse(Call<GHNResponse<com.project.bookstoreapp.ghn.GHNFeeData>> call, Response<GHNResponse<com.project.bookstoreapp.ghn.GHNFeeData>> response) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    dynamicShippingFee = response.body().data.total;
                    updateOrderSummary();
                } else {
                    Toast.makeText(CheckoutActivity.this, "Không thể tính phí vận chuyển", Toast.LENGTH_SHORT).show();
                    dynamicShippingFee = 0;
                    updateOrderSummary();
                }
            }

            @Override
            public void onFailure(Call<GHNResponse<com.project.bookstoreapp.ghn.GHNFeeData>> call, Throwable t) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối khi tính phí", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String targetProvinceName = null;
    private String targetDistrictName = null;
    private String targetWardName = null;
    
    private void parseAndAutoFillAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isEmpty()) return;
        String[] parts = fullAddress.split(", ");
        if (parts.length >= 4) {
            targetProvinceName = parts[parts.length - 1].trim();
            targetDistrictName = parts[parts.length - 2].trim();
            targetWardName = parts[parts.length - 3].trim();
            
            StringBuilder street = new StringBuilder();
            for (int i = 0; i < parts.length - 3; i++) {
                street.append(parts[i]);
                if (i < parts.length - 4) street.append(", ");
            }
            etAddress.setText(street.toString());
        } else {
            etAddress.setText(fullAddress);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void receiveDataFromCart() {
        ArrayList<CartItem> items =
                getIntent().getParcelableArrayListExtra(CartActivity.EXTRA_SELECTED_ITEMS);

        if (items == null || items.isEmpty()) {
            showEmptyCheckout();
            return;
        }

        selectedItems = items;
        setupCheckoutItems();
        computeSubtotal();
        updateOrderSummary();
    }

    private void setupCheckoutItems() {
        checkoutAdapter = new CheckoutAdapter(this, selectedItems);
        rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        rvCheckoutItems.setNestedScrollingEnabled(false);
        rvCheckoutItems.setAdapter(checkoutAdapter);
        showCheckoutContent();
    }

    private void computeSubtotal() {
        subtotal = 0L;
        for (CartItem item : selectedItems) subtotal += item.getSubtotal();
    }

    private void updateOrderSummary() {
        long shipping = subtotal >= FREE_SHIP_MIN ? 0L : dynamicShippingFee;
        long total    = Math.max(0, subtotal + shipping - discountAmount);

        tvSubtotal.setText(fmt(subtotal));
        tvShippingFee.setText(shipping == 0 ? "Miễn phí" : fmt(shipping));
        tvVoucherDiscount.setText("- " + fmt(discountAmount));
        tvFinalTotal.setText(fmt(total));
    }

    private void applyVoucher() {
        String code = getEditText(etVoucher).toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            tilVoucher.setError("Vui lòng nhập mã voucher");
            return;
        }
        tilVoucher.setError(null);
        btnApplyVoucher.setEnabled(false);

        db.collection("vouchers")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    btnApplyVoucher.setEnabled(true);

                    if (!doc.exists()) {
                        tilVoucher.setError("Mã voucher không hợp lệ");
                        resetDiscount();
                        return;
                    }

                    Boolean isActive = doc.getBoolean("isActive");
                    if (isActive == null || !isActive) {
                        tilVoucher.setError("Mã voucher đã hết hiệu lực");
                        resetDiscount();
                        return;
                    }

                    long minOrder    = doc.getLong("minOrderValue")   != null ? doc.getLong("minOrderValue")   : 0L;
                    long maxDisc     = doc.getLong("maxDiscount")     != null ? doc.getLong("maxDiscount")     : 0L;
                    long discPercent = doc.getLong("discountPercent") != null ? doc.getLong("discountPercent") : 0L;

                    if (subtotal < minOrder) {
                        tilVoucher.setError("Đơn tối thiểu " + fmt(minOrder));
                        resetDiscount();
                        return;
                    }

                    long discount = subtotal * discPercent / 100;
                    if (discount > maxDisc) discount = maxDisc;

                    discountAmount     = discount;
                    appliedVoucherCode = code; 
                    updateOrderSummary();
                    tilVoucher.setError(null);
                    Toast.makeText(this, "Giảm thành công " + fmt(discountAmount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnApplyVoucher.setEnabled(true);
                    Toast.makeText(this, "Lỗi kiểm tra voucher: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void resetDiscount() {
        discountAmount     = 0;
        appliedVoucherCode = "";
        updateOrderSummary();
    }

    private boolean validateForm() {
        boolean ok = true;

        if (getEditText(etFullName).isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên"); ok = false;
        } else tilFullName.setError(null);

        String phone = getEditText(etPhone);
        if (phone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại"); ok = false;
        } else if (!VN_PHONE.matcher(phone).matches()) {
            tilPhone.setError("SĐT không hợp lệ (VD: 0912345678)"); ok = false;
        } else tilPhone.setError(null);

        if (selectedProvince == null || selectedDistrict == null || selectedWard == null || getEditText(etAddress).isEmpty()) {
            tilAddress.setError("Vui lòng chọn Tỉnh, Quận, Phường và nhập số nhà"); ok = false;
        } else tilAddress.setError(null);

        return ok;
    }

    private void placeOrder() {
        if (!validateForm()) return;

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");

        String paymentMethod = (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbVnPay)
                ? "vnpay" : "cod";

        long shipping  = subtotal >= FREE_SHIP_MIN ? 0L : dynamicShippingFee;
        long total     = Math.max(0, subtotal + shipping - discountAmount);
        String nowStr  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .format(new Date());

        List<Map<String, Object>> orderItems = new ArrayList<>();
        List<String> bookIds = new ArrayList<>();
        List<GHNItem> ghnItems = new ArrayList<>();
        int totalWeight = 0;

        for (CartItem ci : selectedItems) {
            Map<String, Object> m = new HashMap<>();
            m.put("bookId",   ci.getBookId());
            m.put("title",    ci.getTitle());
            m.put("quantity", ci.getQuantity());
            m.put("price",    ci.getPrice());
            m.put("imageUrl", ci.getImageUrl());
            m.put("subtotal", ci.getSubtotal());
            orderItems.add(m);
            bookIds.add(ci.getBookId());

            int itemWeight = 200; 
            totalWeight += (itemWeight * ci.getQuantity());
            ghnItems.add(new GHNItem(ci.getTitle(), ci.getBookId(), ci.getQuantity(), (int)ci.getPrice(), itemWeight));
        }

        Map<String, Object> order = new HashMap<>();
        order.put("userId",          currentUserId != null ? currentUserId : "");
        order.put("receiverName",    getEditText(etFullName));
        order.put("receiverPhone",   getEditText(etPhone));
        String fullAddress = getEditText(etAddress) + ", " + selectedWard.WardName + ", " + selectedDistrict.DistrictName + ", " + selectedProvince.ProvinceName;
        order.put("shippingAddress", fullAddress);
        order.put("note",            getEditText(etNote));
        order.put("paymentMethod",   paymentMethod);
        order.put("status",          "pending");
        order.put("subtotal",        subtotal);
        order.put("shippingFee",     shipping);
        order.put("discountAmount",  discountAmount);
        order.put("voucherCode",     appliedVoucherCode); 
        order.put("totalPrice",      total);
        order.put("items",           orderItems);
        order.put("bookIds",         bookIds);
        order.put("createdAt",       nowStr);
        order.put("updatedAt",       nowStr);

        GHNOrderRequest ghnRequest = new GHNOrderRequest();
        ghnRequest.paymentTypeId = paymentMethod.equals("vnpay") ? 1 : 2; 
        ghnRequest.note = getEditText(etNote);
        ghnRequest.requiredNote = "CHOXEMHANGKHONGTHU"; 
        ghnRequest.toName = getEditText(etFullName);
        ghnRequest.toPhone = getEditText(etPhone);
        ghnRequest.toAddress = getEditText(etAddress); 
        ghnRequest.toWardCode = selectedWard.WardCode;
        ghnRequest.toDistrictId = selectedDistrict.DistrictID;
        ghnRequest.codAmount = paymentMethod.equals("vnpay") ? 0 : total; 
        ghnRequest.content = "Đơn hàng sách Bookstore";
        ghnRequest.weight = totalWeight;
        ghnRequest.length = 20; 
        ghnRequest.width = 15;
        ghnRequest.height = 10;
        ghnRequest.serviceTypeId = 2; 
        ghnRequest.items = ghnItems;

        this.pendingOrder = order;
        this.pendingGhnRequest = ghnRequest;

        if ("vnpay".equals(paymentMethod)) {
            String orderId = String.valueOf(System.currentTimeMillis());
            processVnPayPayment(orderId, total);
        } else {
            pendingOrder.put("paymentStatus", "pending");
            saveOrderToFirestoreAndShowPopup("Đặt hàng thành công! Đơn hàng của bạn đang được xử lý.");
        }
    }

    private void processVnPayPayment(String orderId, long amount) {
        btnPlaceOrder.setText("Đang kết nối VNPAY...");
        
        HashMap<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("orderDescription", "Thanh toan don hang " + orderId);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.createPaymentUrl(body).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String paymentUrl = response.body().getPaymentUrl();
                    Intent intent = new Intent(CheckoutActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra(PaymentWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                    vnpayLauncher.launch(intent);
                } else {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                    Toast.makeText(CheckoutActivity.this, "Không thể tạo link thanh toán VNPAY", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                Toast.makeText(CheckoutActivity.this, "Lỗi mạng khi kết nối VNPAY", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveOrderToFirestoreAndShowPopup(String message) {
        if (pendingOrder == null || pendingGhnRequest == null) return;
        
        int SHOP_ID = 200948;
        RetrofitClientGHN.getApiService().createOrder(GHN_TOKEN, SHOP_ID, pendingGhnRequest).enqueue(new Callback<GHNResponse<GHNOrderData>>() {
            @Override
            public void onResponse(Call<GHNResponse<GHNOrderData>> call, Response<GHNResponse<GHNOrderData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    String ghnOrderCode = response.body().data.orderCode;
                    pendingOrder.put("ghnOrderCode", ghnOrderCode);
                    pendingOrder.put("displayId", ghnOrderCode);
                    
                    db.collection("orders")
                            .document(ghnOrderCode)
                            .set(pendingOrder)
                            .addOnSuccessListener(aVoid -> {
                                removeItemsFromCart();
                                showPopupAndRedirect(message + "\nMã vận đơn GHN: " + ghnOrderCode);
                            })
                            .addOnFailureListener(e -> {
                                btnPlaceOrder.setEnabled(true);
                                btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                                Toast.makeText(CheckoutActivity.this, "Lưu đơn hàng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                    Toast.makeText(CheckoutActivity.this, "Lỗi tạo đơn GHN. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GHNResponse<GHNOrderData>> call, Throwable t) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("ĐẶT HÀNG NGAY");
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối GHN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPopupAndRedirect(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage(message)
                .setCancelable(false)
                .show();

        mainHandler.postDelayed(() -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, 3000);
    }

    @SuppressLint("SetTextI18n")
    private void removeItemsFromCart() {
        if (currentUserId == null) return;
        for (CartItem ci : selectedItems) {
            db.collection("carts")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("bookId", ci.getBookId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                            doc.getReference().delete();
                        }
                    });
        }
    }

    private void showEmptyCheckout() {
        layoutEmptyCheckout.setVisibility(View.VISIBLE);
        scrollCheckout.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
    }

    private void showCheckoutContent() {
        layoutEmptyCheckout.setVisibility(View.GONE);
        scrollCheckout.setVisibility(View.VISIBLE);
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
    }

    private String getEditText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String fmt(long price) {
        return VND.format(price) + " đ";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
