# Tài liệu Phân Tích Logic Tính Năng Xác Thực (Auth)

Tài liệu này mô tả chi tiết luồng xử lý từ lúc người dùng thao tác trên giao diện Android (Client) cho đến khi Backend (Node.js) xử lý dữ liệu và phản hồi.

---

## 1. Đăng ký tài khoản (Register)

**Mục đích:** Cho phép người dùng tạo tài khoản mới bằng Email và Mật khẩu.

### Luồng xử lý (Flow)
1. **Client (`RegisterActivity.java`)**:
   - Người dùng nhập Họ tên, Email, Mật khẩu, Xác nhận mật khẩu.
   - **Validate Input**: Kiểm tra rỗng, định dạng email, độ dài mật khẩu (>= 6), mật khẩu và xác nhận phải khớp nhau.
   - **Call API**: Đóng gói dữ liệu vào `HashMap` và gọi hàm `apiService.register()`. Hiển thị ProgressBar (Loading).
   - Nếu thành công: Hiển thị Toast và gọi `finish()` để quay lại màn hình Login.

2. **Server (`server.js` - `POST /api/auth/register`)**:
   - Nhận `name, email, password` từ body.
   - **Kiểm tra trùng lặp**: Query Collection `users` trong Firestore với `where("email", "==", email)`. Nếu đã tồn tại, trả về lỗi 400.
   - **Mã hóa (Hashing)**: Dùng `crypto` để băm mật khẩu bằng thuật toán **MD5**.
   - **Lưu Database**: Gọi `addDoc()` tạo document mới. Thuộc tính `password` lưu chuỗi đã băm, `role` mặc định là `"customer"`.
   - **Cập nhật UID**: Lấy `docRef.id` và dùng `updateDoc()` để gán vào trường `uid`.

---

## 2. Đăng nhập thường (Login)

**Mục đích:** Đăng nhập bằng tài khoản (Email/Password) đã đăng ký.

### Luồng xử lý (Flow)
1. **Client (`LoginActivity.java`)**:
   - Nhập Email, Mật khẩu -> Validate rỗng, định dạng.
   - **Call API**: Gọi `apiService.login()` truyền email, password.
   - **Lưu Session**: Nhận response chứa object `User`, lưu vào SharedPreferences thông qua lớp `SessionManager`.
   - **Điều hướng (Routing)**: Kiểm tra trường `role` của User. Nếu `"admin"` -> `AdminDashboardActivity`, ngược lại -> `HomeActivity`. Xóa cờ activity stack để không cho bấm Back về Login.

2. **Server (`server.js` - `POST /api/auth/login`)**:
   - Query Firestore tìm user theo `email`. Nếu không thấy -> 401.
   - **Chặn tài khoản Google**: Kiểm tra `userData.password`. Nếu rỗng (`null`), nghĩa là tài khoản này được tạo bởi Google Sign-In -> Bắt buộc người dùng dùng nút Google để đăng nhập.
   - **Xác thực**: Hash MD5 password người dùng vừa nhập và so sánh với chuỗi trong database. Nếu khớp -> trả về Data (không kèm password).

---

## 3. Đăng nhập bằng Google (Google Sign-In)

**Mục đích:** Đăng nhập nhanh bằng tài khoản Google (OAuth 2.0).

### Luồng xử lý (Flow)
1. **Client (`LoginActivity.java`)**:
   - Khởi tạo `GoogleSignInClient` với `GoogleSignInOptions`, truyền vào `WEB_CLIENT_ID` (156167272606-...).
   - Nhấn nút Google -> Mở Popup chọn tài khoản Google thông qua `ActivityResultLauncher`.
   - Lấy thành công `GoogleSignInAccount` -> Lấy chuỗi **`idToken`** (Google cấp).
   - **Call API**: Gọi `apiService.googleLogin()` truyền `idToken` lên server.
   - Lưu `SessionManager` và điều hướng như đăng nhập thường.

2. **Server (`server.js` - `POST /api/auth/google`)**:
   - Nhận `idToken`. Khởi tạo `OAuth2Client` của thư viện `google-auth-library`.
   - **Verify Token**: Gọi `googleClient.verifyIdToken()` với `audience` chứa cả Web và Android Client IDs. Điều này giúp xác minh token này thực sự do Google cấp cho app của mình. Giải mã token lấy Payload (email, name, picture).
   - **Đồng bộ Database**:
     - Query Firestore theo `email` của Google.
     - **Trường hợp 1 (Email đã có)**: Cho phép đăng nhập thẳng vào tài khoản cũ (merge account). Nếu tk cũ chưa có ảnh đại diện, lấy `picture` của Google cập nhật vào Firestore.
     - **Trường hợp 2 (Email chưa có)**: Gọi `addDoc` tạo user mới với `password = null` (để phân biệt với tài khoản thường) và `avatarUrl = picture`.

---

## 4. Quên mật khẩu (Forgot Password - OTP qua Email)

**Mục đích:** Cho phép khôi phục mật khẩu khi người dùng quên, thông qua xác thực email.

### Luồng xử lý (Flow)
1. **Gửi OTP (Bước 1)**:
   - **Client (`ForgotPasswordActivity.java`)**: Nhập email, validate định dạng. Gọi API `forgotPassword()`. Nếu thành công, mở `VerifyOtpActivity`.
   - **Server (`POST /api/auth/forgot-password`)**: Kiểm tra email có trong Firestore. Chặn nếu là tài khoản Google (`!password`). Sinh mã ngẫu nhiên 6 chữ số, lưu vào bộ nhớ tạm `otpStore` (Map) kèm thời gian hết hạn (TTL 5 phút). Gọi cấu hình `nodemailer` gửi email HTML đẹp mắt đến người dùng bằng Gmail SMTP (port 587, STARTTLS).

2. **Xác thực OTP (Bước 2)**:
   - **Client (`VerifyOtpActivity.java`)**: 6 ô EditText tự động focus (khi gõ xong 1 ký tự sẽ nhảy sang ô sau, bấm xóa nhảy về ô trước). Giao diện có đồng hồ đếm ngược 60s để nút "Gửi lại mã". Gắn chuỗi 6 số gọi API `verifyOtp()`. Chuyển sang màn hình đổi MK.
   - **Server (`POST /api/auth/verify-otp`)**: Tìm email trong `otpStore`. Kiểm tra thời gian `expiresAt`. So sánh OTP gửi lên. Nếu hợp lệ -> **Sinh `resetToken`** (chuỗi random hex bảo mật), lưu vào `resetTokenStore` (TTL 10 phút), xóa OTP cũ khỏi bộ nhớ. Trả về `resetToken`.

3. **Đặt lại mật khẩu (Bước 3)**:
   - **Client (`ResetPasswordActivity.java`)**: Nhập mật khẩu mới, validate >= 6 ký tự. Gọi API `resetPassword()` truyền theo `email`, `resetToken`, `newPassword`. Thành công -> Xóa cờ trở về trang Login.
   - **Server (`POST /api/auth/reset-password`)**: Lấy thông tin từ `resetTokenStore`. Verify chuỗi token để đảm bảo request hợp lệ và chưa hết hạn. Hash MD5 mật khẩu mới. Tìm user doc, gọi `updateDoc` ghi đè password. Xóa token.

---

## 5. Đổi mật khẩu trong cài đặt (Change Password)

**Mục đích:** Cho phép thay đổi mật khẩu khi người dùng ĐÃ đăng nhập.

### Luồng xử lý (Flow)
1. **Client (`ProfileActivity.java`)**:
   - Nhấn nút Đổi mật khẩu -> Bật Custom Dialog (`dialog_change_password.xml`).
   - Nhập Mật khẩu cũ, Mới, Xác nhận. Validate tương tự như trên.
   - Gọi API `updatePassword()` truyền `uid, oldPassword, newPassword`.

2. **Server (`server.js` - `PUT /api/users/password`)**:
   - Dùng `uid` get document từ Firestore.
   - **Kiểm tra loại tài khoản**: Nếu không có `password` -> Báo lỗi "Tài khoản Google không có mật khẩu để đổi".
   - **Verify Mật khẩu cũ**: Hash MD5 `oldPassword` và so sánh với DB. Nếu không khớp -> Báo lỗi.
   - **Cập nhật Mật khẩu mới**: Hash MD5 `newPassword` và `updateDoc` ghi đè vào Firestore. Trả về thành công.
