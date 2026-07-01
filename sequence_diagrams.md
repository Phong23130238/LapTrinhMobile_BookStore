# Sequence Diagrams: Các Chức Năng Của Ứng Dụng Bookstore

Dưới đây là mã Mermaid để vẽ Sequence Diagram (Biểu đồ tuần tự) cho 6 chức năng tương ứng trong `tai_lieu_ky_thuat_chuc_nang.md`. Các mã số chú thích (ví dụ: `1.1.1`, `1.2.1`) đã được gắn trực tiếp vào các thông điệp để bạn dễ dàng tìm kiếm và đối chiếu với mã nguồn.

## 1. Chi Tiết Đơn Hàng (Order Detail)

```mermaid
sequenceDiagram
    actor User
    participant App as OrderDetailActivity
    participant Server as Node.js Server
    participant DB as Firestore

    note over App: 1.1 Khởi tạo và thiết lập
    App->>App: 1.1.1 initViews()
    App->>App: 1.1.2 onCreate() lấy ORDER_ID từ Intent

    note over App,Server: 1.2 Tải dữ liệu từ backend
    App->>Server: 1.2.1 getOrderDetails(orderId)
    Server-->>App: 1.2.2 onResponse(isSuccess, data)

    note over App: 1.3 Hiển thị giao diện
    App->>App: 1.3.1 getStatus() & chuyển text tiếng Việt
    App->>App: 1.3.2 Xử lý ẩn/hiện btnCancelOrder
    App->>App: 1.3.3 Format ngày, địa chỉ, phí
    App->>App: 1.3.4 notifyDataSetChanged()

    note over User,App: 1.4 Xử lý hủy đơn
    User->>App: Nhấn btnCancelOrder
    App->>User: 1.4.1 showCancelDialog() nhập lý do
    User->>App: Xác nhận hủy
    App->>App: 1.4.2 Kiểm tra rỗng, gọi cancelOrder(reason)

    note over App,DB: 1.5 Cập nhật Firestore
    App->>DB: 1.5.1 update() status="cancelled", cancelReason
    DB-->>App: 1.5.2 addOnSuccessListener
    App->>Server: 1.5.2 loadOrderDetails(orderId) làm mới UI
```

---

## 2. Đơn Hàng Của Tôi (My Orders)

```mermaid
sequenceDiagram
    actor User
    participant App as OrdersActivity
    participant DB as Firestore

    note over App: 2.1 Khởi tạo (onCreate)
    App->>App: 2.1.1 Kiểm tra SessionManager (currentUser)
    App->>App: 2.1.2 initViews() & thiết lập sự kiện ChipGroup/Sort
    App->>App: 2.1.3 Khởi tạo OrderAdapter với lambda ORDER_ID

    note over App,DB: 2.2 Truy xuất dữ liệu từ Firebase
    App->>DB: 2.2.1 get() danh sách theo "userId"
    DB-->>App: QueryDocumentSnapshot
    App->>App: 2.2.2 Ép kiểu Document ID thành orderId, lưu vào originalOrderList

    note over User,App: 2.3 Lọc và sắp xếp (applyFilterAndSort)
    User->>App: Chọn filter/sort
    App->>App: 2.3.1 Lọc originalOrderList theo currentStatusFilter
    App->>App: 2.3.2 Sắp xếp CreatedAt kết hợp isSortDesc
    App->>App: 2.3.3 Kiểm tra isEmpty, hiển thị layoutEmpty hoặc notifyDataSetChanged()
```

---

## 3. Chi Tiết Sản Phẩm (Product Detail)

```mermaid
sequenceDiagram
    actor User
    participant App as BookDetailActivity
    participant DB as Firestore

    note over App: 3.1 Nhận ID sách (onCreate)
    App->>App: 3.1.1 getIntent().getStringExtra("BOOK_ID")
    App->>App: 3.1.2 Dự phòng lấy currentBookIdInt

    note over App,DB: 3.2 Truy vấn sách từ Firestore (loadBookData)
    App->>DB: 3.2.1 get() theo Document ID (bookIdStr)
    DB-->>App: DocumentSnapshot (exists=false)
    App->>DB: 3.2.2 Fallback: whereEqualTo("bookId", currentBookIdInt/bookIdStr)
    DB-->>App: QuerySnapshot

    note over App: 3.3 Hiển thị thông tin sách (bindBookData)
    App->>App: 3.3.1 Trích xuất title, price, rating...
    App->>App: 3.3.2 Đẩy dữ liệu vào UI (gạch ngang tvOriginalPrice)
    App->>App: 3.3.3 Tải ảnh bìa qua Glide bất đồng bộ
```

---

## 4. Đánh Giá Sản Phẩm (Product Review)

```mermaid
sequenceDiagram
    actor User
    participant App as BookDetailActivity
    participant Server as Node.js Server
    participant DB as Firestore

    note over App,Server: 4.1 Tải danh sách đánh giá (loadReviews)
    App->>Server: 4.1.1 getReviews(bookId)
    Server-->>App: JSON Array
    App->>App: 4.1.2 notifyDataSetChanged() hoặc hiện tvNoReview

    note over App,Server: 4.2 Kiểm tra điều kiện mua hàng
    User->>App: Bấm nút Đánh giá
    App->>Server: 4.2.1 checkPurchase(userId, bookId)
    Server-->>App: 4.2.2 isCanReview == true/false
    App->>User: Gọi showWriteReviewDialog() hoặc Toast từ chối

    note over User,App: 4.3 Nhập thông tin đánh giá
    User->>App: 4.3.1 Chọn RatingBar & nhập EditText trong Dialog
    User->>App: Bấm Xác nhận
    App->>DB: 4.3.2 Truy vấn lấy tên (name) từ User collection

    note over App,Server: 4.4 Gửi đánh giá (submitReview)
    App->>Server: 4.4.1 submitReview(body: userId, bookId, rating...)
    Server-->>App: Thành công
    App->>App: 4.4.2 Đóng dialog, gọi loadReviews() & loadBookData()
```

---

## 5. Thông Tin Profile (Xem, đổi thông tin)

```mermaid
sequenceDiagram
    actor User
    participant App as ProfileActivity
    participant Server as Node.js Server

    note over App: 5.1 Khởi tạo giao diện
    App->>App: 5.1.1 initViews() & loadUserData()
    App->>App: 5.1.2 parseAndAutoFillAddress(address)

    note over User,App: 5.2 Xử lý ảnh đại diện
    User->>App: Chọn ảnh từ thư viện
    App->>App: 5.2.1 ActivityResultLauncher trả về Uri
    App->>App: 5.2.2 getFileFromUri() tạo File tạm (khi nhấn Lưu)

    note over App,Server: 5.3 Lưu thông tin hồ sơ
    User->>App: Nhấn nút Lưu Profile
    App->>App: 5.3.1 Bọc Text thành RequestBody
    App->>Server: 5.3.2 updateProfile(MultipartBody chứa ảnh)
    Server-->>App: Kết quả trả về (Object User mới)
    App->>App: 5.3.3 SessionManager.saveUser()

    note over User,Server: 5.4 Đổi mật khẩu
    User->>App: Nhấn Đổi mật khẩu
    App->>App: 5.4.1 showChangePasswordDialog() & validate
    App->>Server: 5.4.2 updatePassword(body)
    Server-->>App: Xử lý response/errorBody()
```

---

## 6. Địa Chỉ API GHN (Giao Hàng Nhanh)

```mermaid
sequenceDiagram
    actor User
    participant App as ProfileActivity
    participant GHN as GHN API

    note over App,GHN: 6.1 Tải danh sách Tỉnh/Thành phố
    App->>GHN: 6.1.1 getProvinces()
    GHN-->>App: JSON Province List
    App->>App: 6.1.2 Đổ vào provinceList & spinProvince
    App->>App: 6.1.3 Auto-fill: So khớp targetProvinceName -> gọi loadDistricts()

    note over User,GHN: 6.2 Tải danh sách Quận/Huyện
    User->>App: 6.2.1 Chọn spinProvince -> Xóa Quận/Phường cũ
    App->>GHN: 6.2.2 getDistricts(provinceId)
    GHN-->>App: JSON District List
    App->>App: 6.2.2 Auto-fill: So khớp targetDistrictName -> gọi loadWards()

    note over User,GHN: 6.3 Tải danh sách Phường/Xã
    User->>App: 6.3.1 Chọn thủ công spinDistrict
    App->>GHN: 6.3.1 getWards(districtId)
    GHN-->>App: JSON Ward List
    App->>App: 6.3.2 Auto-fill: So khớp targetWardName -> lưu selectedWard
```
