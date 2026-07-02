const express = require('express');
const cors = require('cors');
require('dotenv').config();
const multer = require('multer');
const cloudinary = require('cloudinary').v2;
const { CloudinaryStorage } = require('multer-storage-cloudinary');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const { OAuth2Client } = require('google-auth-library');
const { initializeApp } = require('firebase/app');
const { getFirestore, collection, query, where, getDocs, addDoc, updateDoc, doc, getDoc, setDoc } = require('firebase/firestore');

const firebaseConfig = {
    apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
    projectId: "bookstore-500314",
    storageBucket: "bookstore-500314.firebasestorage.app"
};

// Khởi tạo Firebase
const firebaseApp = initializeApp(firebaseConfig);
const db = getFirestore(firebaseApp);

// Google OAuth2 Client để verify idToken
const GOOGLE_CLIENT_ID = "156167272606-ahuk0t1gr5biq7b69a24kh0i9so84vp4.apps.googleusercontent.com";
const GOOGLE_ANDROID_CLIENT_ID = "156167272606-ftq1ike17ekmorja3trn0bbot04btoh6.apps.googleusercontent.com";
const ACCEPTED_CLIENT_IDS = [GOOGLE_CLIENT_ID, GOOGLE_ANDROID_CLIENT_ID];
const googleClient = new OAuth2Client(GOOGLE_CLIENT_ID);

const app = express();
app.use(cors());

// =============================================
// OTP & RESET TOKEN IN-MEMORY STORE
// =============================================
const otpStore = new Map();      // email -> { otp, expiresAt }
const resetTokenStore = new Map(); // email -> { token, expiresAt }

// Cấu hình Nodemailer - Gmail SMTP
const emailTransporter = nodemailer.createTransport({
    host: 'smtp.gmail.com',
    port: 587,
    secure: false, // Dùng STARTTLS thay vì SSL trực tiếp
    auth: {
        user: 'aurasound.contact@gmail.com',
        pass: 'vssg ofwa bqcw yjll'
    },
    tls: {
        rejectUnauthorized: false
    },
    connectionTimeout: 5000, // Timeout 5s để không làm treo Android app
    greetingTimeout: 5000,
    socketTimeout: 5000
});
app.use(express.json());

// =============================================
// CẤU HÌNH CLOUDINARY & MULTER
// =============================================
cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});

// 1. Storage cho Avatar User
const storage = new CloudinaryStorage({
    cloudinary: cloudinary,
    params: {
        folder: 'bookstore_avatars',
        allowedFormats: ['jpg', 'png', 'jpeg']
    }
});
const upload = multer({ storage: storage });

// 2. Storage riêng cho Ảnh Bìa Sách (Bổ sung mới)
const bookStorage = new CloudinaryStorage({
    cloudinary: cloudinary,
    params: {
        folder: 'bookstore_covers',
        allowedFormats: ['jpg', 'png', 'jpeg']
    }
});
const uploadBookCover = multer({ storage: bookStorage });

// =============================================
// HELPER: Hash mật khẩu bằng MD5
// =============================================
function hashMD5(password) {
    return crypto.createHash('md5').update(password).digest('hex');
}

// =============================================
// AUTH APIs
// =============================================

/**
 * @api {post} /api/auth/register Đăng ký tài khoản thường
 * @description Xử lý luồng tạo tài khoản mới bằng Email/Password.
 * 1. Nhận Name, Email, Password từ body. Validate đầu vào (rỗng, độ dài pass >= 6).
 * 2. Query collection 'users' xem email đã tồn tại chưa. Nếu có -> trả về lỗi 400.
 * 3. Băm mật khẩu (Hash) bằng thuật toán MD5 để bảo mật trước khi lưu.
 * 4. Tạo document mới trong collection 'users', mặc định role = 'customer'.
 * 5. Update document để lưu 'uid' bằng chính ID của document vừa tạo.
 */
app.post('/api/auth/register', async (req, res) => {
    try {
        const { name, email, password } = req.body;

        // Validate dữ liệu đầu vào
        if (!name || !email || !password) {
            return res.status(400).json({ success: false, message: "Vui lòng điền đầy đủ thông tin" });
        }

        if (password.length < 6) {
            return res.status(400).json({ success: false, message: "Mật khẩu phải có ít nhất 6 ký tự" });
        }

        // Kiểm tra email đã tồn tại chưa
        const usersRef = collection(db, "users");
        const q = query(usersRef, where("email", "==", email));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            return res.status(400).json({ success: false, message: "Email đã được sử dụng" });
        }

        // Hash mật khẩu bằng MD5
        const hashedPassword = hashMD5(password);

        // Tạo user mới
        const newUser = {
            name,
            email,
            password: hashedPassword,
            phone: "",
            address: "",
            avatarUrl: "",
            role: "customer",
            createdAt: new Date().toISOString()
        };

        const docRef = await addDoc(usersRef, newUser);

        // Cập nhật uid = document ID
        await updateDoc(docRef, { uid: docRef.id });

        // Trả về user data (không trả password)
        res.json({
            success: true,
            message: "Đăng ký thành công",
            data: {
                uid: docRef.id,
                name: newUser.name,
                email: newUser.email,
                phone: newUser.phone,
                address: newUser.address,
                avatarUrl: newUser.avatarUrl,
                role: newUser.role,
                createdAt: newUser.createdAt
            }
        });

    } catch (error) {
        console.error("Lỗi đăng ký:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

/**
 * @api {post} /api/auth/login Đăng nhập thường
 * @description Xử lý luồng đăng nhập bằng Email/Password.
 * 1. Nhận Email, Password từ body.
 * 2. Query collection 'users' để tìm user theo Email.
 * 3. Kiểm tra trường 'password': nếu rỗng (null), tức là tài khoản này được tạo bởi Google -> Bắt buộc dùng Google Login.
 * 4. Băm mật khẩu (MD5) nhập vào và so sánh với mật khẩu trong DB.
 * 5. Trả về thông tin User (loại bỏ trường password) để Client lưu phiên đăng nhập.
 */
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ success: false, message: "Vui lòng nhập email và mật khẩu" });
        }

        // Tìm user theo email
        const usersRef = collection(db, "users");
        const q = query(usersRef, where("email", "==", email));
        const querySnapshot = await getDocs(q);

        if (querySnapshot.empty) {
            return res.status(401).json({ success: false, message: "Sai email hoặc mật khẩu" });
        }

        const userDoc = querySnapshot.docs[0];
        const userData = userDoc.data();

        // Kiểm tra tài khoản đăng ký bằng Google (password == null hoặc rỗng)
        if (!userData.password) {
            return res.status(401).json({
                success: false,
                message: "Tài khoản này được đăng ký bằng Google, vui lòng sử dụng nút Đăng nhập Google."
            });
        }

        // So sánh password đã hash
        const hashedPassword = hashMD5(password);
        if (userData.password !== hashedPassword) {
            return res.status(401).json({ success: false, message: "Sai email hoặc mật khẩu" });
        }

        // Đăng nhập thành công - trả về user data (không trả password)
        res.json({
            success: true,
            message: "Đăng nhập thành công",
            data: {
                uid: userData.uid || userDoc.id,
                name: userData.name,
                email: userData.email,
                phone: userData.phone || "",
                address: userData.address || "",
                avatarUrl: userData.avatarUrl || "",
                role: userData.role,
                createdAt: userData.createdAt
            }
        });

    } catch (error) {
        console.error("Lỗi đăng nhập:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

/**
 * @api {post} /api/auth/google Đăng nhập bằng Google
 * @description Xử lý luồng OAuth2 Google Sign-In.
 * 1. Client truyền lên 'idToken' do Google cấp.
 * 2. Dùng thư viện google-auth-library (googleClient.verifyIdToken) để verify token với ACCEPTED_CLIENT_IDS (Web & Android).
 * 3. Giải mã lấy Payload (Email, Name, Picture).
 * 4. Query Firestore theo Email. 
 *    - Nếu có: Cho đăng nhập thẳng. Cập nhật avatar nếu trước đó chưa có.
 *    - Nếu chưa có: Tạo mới User trong DB với trường 'password' là null.
 */
app.post('/api/auth/google', async (req, res) => {
    try {
        const { idToken } = req.body;

        if (!idToken) {
            return res.status(400).json({ success: false, message: "Thiếu Google ID Token" });
        }

        // Verify Google ID Token
        let payload;
        try {
            const ticket = await googleClient.verifyIdToken({
                idToken: idToken,
                audience: ACCEPTED_CLIENT_IDS,
            });
            payload = ticket.getPayload();
        } catch (verifyError) {
            console.error("Lỗi verify Google token:", verifyError);
            return res.status(401).json({ success: false, message: "Xác thực Google thất bại" });
        }

        const googleEmail = payload.email;
        const googleName = payload.name || "Người dùng Google";
        const googleAvatar = payload.picture || "";

        // Tìm user theo email trong Firestore
        const usersRef = collection(db, "users");
        const q = query(usersRef, where("email", "==", googleEmail));
        const querySnapshot = await getDocs(q);

        let userToReturn;

        if (!querySnapshot.empty) {
            // Email đã tồn tại (dù đăng ký thường hay Google) → đăng nhập thẳng
            const existingDoc = querySnapshot.docs[0];
            const existingData = existingDoc.data();

            userToReturn = {
                uid: existingData.uid || existingDoc.id,
                name: existingData.name,
                email: existingData.email,
                phone: existingData.phone || "",
                address: existingData.address || "",
                avatarUrl: existingData.avatarUrl || googleAvatar,
                role: existingData.role,
                createdAt: existingData.createdAt
            };

            // Nếu user chưa có avatar, cập nhật avatar từ Google
            if (!existingData.avatarUrl && googleAvatar) {
                await updateDoc(existingDoc.ref, { avatarUrl: googleAvatar });
            }
        } else {
            // Email chưa tồn tại → tạo user mới với password = null
            const newUser = {
                name: googleName,
                email: googleEmail,
                password: null,
                phone: "",
                address: "",
                avatarUrl: googleAvatar,
                role: "customer",
                createdAt: new Date().toISOString()
            };

            const docRef = await addDoc(usersRef, newUser);
            await updateDoc(docRef, { uid: docRef.id });

            userToReturn = {
                uid: docRef.id,
                name: newUser.name,
                email: newUser.email,
                phone: newUser.phone,
                address: newUser.address,
                avatarUrl: newUser.avatarUrl,
                role: newUser.role,
                createdAt: newUser.createdAt
            };
        }

        res.json({
            success: true,
            message: "Đăng nhập Google thành công",
            data: userToReturn
        });

    } catch (error) {
        console.error("Lỗi đăng nhập Google:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// =============================================
// USER PROFILE APIs
// =============================================

// Cập nhật Profile (có hỗ trợ upload ảnh)
app.put('/api/users/profile', upload.single('avatar'), async (req, res) => {
    try {
        const { uid, name, phone, address } = req.body;

        if (!uid) {
            return res.status(400).json({ success: false, message: "Thiếu UID người dùng" });
        }

        const updateData = {};
        if (name) updateData.name = name;
        if (phone) updateData.phone = phone;
        if (address !== undefined) updateData.address = address; // allow empty address

        // Nếu có file ảnh được upload thành công lên Cloudinary
        if (req.file) {
            updateData.avatarUrl = req.file.path; // req.file.path chứa URL ảnh trên Cloudinary
        }

        const userRef = doc(db, "users", uid);
        const userSnap = await getDoc(userRef);

        if (!userSnap.exists()) {
            return res.status(404).json({ success: false, message: "Không tìm thấy người dùng" });
        }

        await updateDoc(userRef, updateData);

        // Lấy dữ liệu mới nhất trả về
        const updatedSnap = await getDoc(userRef);

        res.json({
            success: true,
            message: "Cập nhật hồ sơ thành công",
            data: updatedSnap.data()
        });

    } catch (error) {
        console.error("Lỗi cập nhật profile:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Cập nhật Mật khẩu
app.put('/api/users/password', async (req, res) => {
    try {
        const { uid, oldPassword, newPassword } = req.body;

        if (!uid || !oldPassword || !newPassword) {
            return res.status(400).json({ success: false, message: "Vui lòng nhập đủ thông tin" });
        }

        if (newPassword.length < 6) {
            return res.status(400).json({ success: false, message: "Mật khẩu mới phải có ít nhất 6 ký tự" });
        }

        const userRef = doc(db, "users", uid);
        const userSnap = await getDoc(userRef);

        if (!userSnap.exists()) {
            return res.status(404).json({ success: false, message: "Không tìm thấy người dùng" });
        }

        const userData = userSnap.data();

        // Tài khoản đăng nhập Google không có password
        if (!userData.password) {
            return res.status(400).json({ success: false, message: "Tài khoản Google không có mật khẩu để đổi" });
        }

        const hashedOld = hashMD5(oldPassword);
        if (userData.password !== hashedOld) {
            return res.status(400).json({ success: false, message: "Mật khẩu cũ không chính xác" });
        }

        const hashedNew = hashMD5(newPassword);
        await updateDoc(userRef, { password: hashedNew });

        res.json({ success: true, message: "Đổi mật khẩu thành công" });

    } catch (error) {
        console.error("Lỗi đổi mật khẩu:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// =============================================
// BOOK APIs (BỔ SUNG MỚI)
// =============================================

// Cập nhật hoặc Thêm mới Sách (có hỗ trợ upload ảnh bìa)
app.post('/api/books/upload-cover', uploadBookCover.single('bookCover'), async (req, res) => {
    try {
        const { bookId } = req.body;

        if (!bookId) {
            return res.status(400).json({ success: false, message: "Thiếu ID của sách" });
        }

        if (!req.file) {
            return res.status(400).json({ success: false, message: "Không tìm thấy file ảnh" });
        }

        // Lấy đường dẫn ảnh từ Cloudinary
        const imageUrl = req.file.path;

        // Cập nhật url ảnh vào Firestore
        const bookRef = doc(db, "books", bookId);
        const bookSnap = await getDoc(bookRef);

        if (!bookSnap.exists()) {
            // Nếu chưa tồn tại document (đang tạo mới sách chưa lưu), ta sẽ trả về URL để Client tự gắn vào object Book và lưu sau.
            return res.json({
                success: true,
                message: "Upload ảnh tạm thời thành công",
                imageUrl: imageUrl
            });
        }

        // Nếu sách đã tồn tại, cập nhật trực tiếp vào Firestore
        await updateDoc(bookRef, { imageUrl: imageUrl });

        res.json({
            success: true,
            message: "Upload và cập nhật ảnh sách thành công",
            imageUrl: imageUrl
        });

    } catch (error) {
        console.error("Lỗi upload ảnh sách:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});


// =============================================
// REVIEW APIs (giữ nguyên)
// =============================================

// 1. API: Lấy danh sách đánh giá của 1 sách
app.get('/api/reviews/:bookId', async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const reviewsRef = collection(db, "reviews");

        // Không dùng orderBy để tránh lỗi thiếu Index trên Firestore, ta sẽ sort ở backend
        const q = query(reviewsRef, where("bookId", "==", bookId));
        const querySnapshot = await getDocs(q);

        let reviews = [];
        querySnapshot.forEach((docSnap) => {
            let data = docSnap.data();
            // Xử lý timestamp để trả về dạng string/number
            if (data.createdAt && data.createdAt.toDate) {
                data.createdAt = data.createdAt.toDate().toISOString();
            }
            reviews.push(data);
        });

        // Sort mới nhất lên đầu
        reviews.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        res.json({ success: true, data: reviews });
    } catch (error) {
        console.error("Lỗi lấy reviews:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// 2. API: Gửi đánh giá mới
app.post('/api/reviews', async (req, res) => {
    try {
        const { bookId, userId, orderId, userName, rating, comment } = req.body;

        if (!bookId || !userId || !orderId || !rating || !comment) {
            return res.status(400).json({ success: false, message: "Thiếu dữ liệu bắt buộc" });
        }

        const reviewsRef = collection(db, "reviews");

        // Tạo object đánh giá mới
        const newReview = {
            reviewId: "", // sẽ cập nhật sau
            bookId,
            userId,
            orderId,
            userName: userName || "Khách hàng",
            rating: parseFloat(rating),
            comment,
            createdAt: new Date()
        };

        const docRef = await addDoc(reviewsRef, newReview);
        // Cập nhật lại reviewId
        await updateDoc(docRef, { reviewId: docRef.id });

        // Cập nhật rating trung bình của sách
        const bookRef = doc(db, "books", bookId);
        const bookSnap = await getDoc(bookRef);

        if (bookSnap.exists()) {
            const bookData = bookSnap.data();
            let currentAvgRating = bookData.rating || 0;
            let reviewCount = bookData.reviewCount || 0;

            let newAvg = ((currentAvgRating * reviewCount) + parseFloat(rating)) / (reviewCount + 1);
            newAvg = Math.round(newAvg * 10) / 10;

            await updateDoc(bookRef, {
                rating: newAvg,
                reviewCount: reviewCount + 1
            });
        }

        res.json({ success: true, message: "Đánh giá thành công", reviewId: docRef.id });

    } catch (error) {
        console.error("Lỗi gửi review:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// 3. API: Kiểm tra quyền đánh giá (đã mua hàng chưa)
app.post('/api/reviews/check-purchase', async (req, res) => {
    try {
        const { userId, bookId } = req.body;

        const ordersRef = collection(db, "orders");
        // Lấy tất cả đơn hàng của user, sau đó lọc trạng thái bằng code để tránh lỗi thiếu Index trên Firestore
        const q = query(ordersRef, where("userId", "==", userId));

        const querySnapshot = await getDocs(q);

        let hasPurchased = false;
        let orderId = null;

        // Quét mảng bookIds thủ công bằng node.js để tránh lỗi ArrayContains Index và lọc thêm trạng thái delivered
        querySnapshot.forEach((docSnap) => {
            const data = docSnap.data();
            if (data.status === "delivered" && data.bookIds && data.bookIds.includes(bookId)) {
                hasPurchased = true;
                orderId = docSnap.id;
            }
        });

        if (hasPurchased) {
            res.json({ success: true, canReview: true, orderId: orderId });
        } else {
            res.json({ success: true, canReview: false });
        }

    } catch (error) {
        console.error("Lỗi kiểm tra mua hàng:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// =============================================
// ORDER APIs
// =============================================

// Lấy chi tiết đơn hàng
app.get('/api/orders/:orderId', async (req, res) => {
    try {
        const orderId = req.params.orderId;
        const orderRef = doc(db, "orders", orderId);
        const orderSnap = await getDoc(orderRef);

        if (!orderSnap.exists()) {
            return res.status(404).json({ success: false, message: "Không tìm thấy đơn hàng" });
        }

        res.json({ success: true, data: orderSnap.data() });
    } catch (error) {
        console.error("Lỗi lấy chi tiết đơn hàng:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// =============================================
// FORGOT PASSWORD APIs
// =============================================

// Gửi OTP qua email
app.post('/api/auth/forgot-password', async (req, res) => {
    try {
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ success: false, message: "Vui lòng nhập email" });
        }

        // Tìm user theo email
        const usersRef = collection(db, "users");
        const q = query(usersRef, where("email", "==", email));
        const querySnapshot = await getDocs(q);

        if (querySnapshot.empty) {
            return res.status(404).json({ success: false, message: "Email không tồn tại trong hệ thống" });
        }

        const userData = querySnapshot.docs[0].data();

        // Kiểm tra tài khoản Google
        if (!userData.password) {
            return res.status(400).json({
                success: false,
                message: "Tài khoản này được đăng ký bằng Google, không có mật khẩu để khôi phục."
            });
        }

        // Sinh OTP 6 chữ số
        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = Date.now() + 5 * 60 * 1000; // 5 phút

        // Lưu OTP vào bộ nhớ
        otpStore.set(email, { otp, expiresAt });

        // Gửi email
        const mailOptions = {
            from: '"BookStore App" <aurasound.contact@gmail.com>',
            to: email,
            subject: 'Mã xác thực đặt lại mật khẩu - BookStore',
            html: `
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 30px; background: #f8fafc; border-radius: 12px;">
                    <h2 style="color: #0F172A; text-align: center;">📚 BookStore</h2>
                    <p style="color: #334155;">Xin chào,</p>
                    <p style="color: #334155;">Bạn đã yêu cầu đặt lại mật khẩu. Dưới đây là mã xác thực của bạn:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0F172A; background: #E2E8F0; padding: 16px 32px; border-radius: 8px;">${otp}</span>
                    </div>
                    <p style="color: #64748B; font-size: 14px; text-align: center;">Mã có hiệu lực trong <strong>5 phút</strong>.</p>
                    <p style="color: #64748B; font-size: 13px; text-align: center;">Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
                </div>
            `
        };

        await emailTransporter.sendMail(mailOptions);

        console.log(`OTP ${otp} đã gửi đến ${email}`);
        res.json({ success: true, message: "Mã xác thực đã được gửi đến email của bạn" });

    } catch (error) {
        console.error("Lỗi gửi OTP:", error);
        res.status(500).json({ success: false, message: "Không thể gửi email. Vui lòng thử lại sau." });
    }
});

/**
 * @api {post} /api/auth/verify-otp Xác thực mã OTP (Bước 2 - Quên MK)
 * @description 
 * 1. Lấy thông tin (OTP, thời gian hết hạn) từ bộ nhớ 'otpStore' dựa vào email.
 * 2. So khớp chuỗi OTP và kiểm tra thời gian (TTL 5 phút).
 * 3. Nếu hợp lệ: Xóa OTP cũ, sinh 'resetToken' ngẫu nhiên 32 byte.
 * 4. Lưu resetToken vào 'resetTokenStore' (TTL 10 phút) và trả về cho Client.
 */
app.post('/api/auth/verify-otp', async (req, res) => {
    try {
        const { email, otp } = req.body;

        if (!email || !otp) {
            return res.status(400).json({ success: false, message: "Vui lòng nhập đầy đủ thông tin" });
        }

        const storedData = otpStore.get(email);

        if (!storedData) {
            return res.status(400).json({ success: false, message: "Mã xác thực không tồn tại. Vui lòng gửi lại." });
        }

        // Kiểm tra hết hạn
        if (Date.now() > storedData.expiresAt) {
            otpStore.delete(email);
            return res.status(400).json({ success: false, message: "Mã xác thực đã hết hạn. Vui lòng gửi lại." });
        }

        // So sánh OTP
        if (storedData.otp !== otp) {
            return res.status(400).json({ success: false, message: "Mã xác thực không đúng" });
        }

        // OTP đúng → tạo resetToken
        otpStore.delete(email);
        const resetToken = crypto.randomBytes(32).toString('hex');
        const tokenExpiresAt = Date.now() + 10 * 60 * 1000; // 10 phút
        resetTokenStore.set(email, { token: resetToken, expiresAt: tokenExpiresAt });

        res.json({ success: true, message: "Xác thực thành công", resetToken });

    } catch (error) {
        console.error("Lỗi verify OTP:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

/**
 * @api {post} /api/auth/reset-password Đặt lại mật khẩu (Bước 3 - Quên MK)
 * @description 
 * 1. Nhận email, resetToken, newPassword. Validate pass >= 6 ký tự.
 * 2. Kiểm tra tính hợp lệ và thời hạn (TTL 10 phút) của 'resetToken' trong 'resetTokenStore'.
 * 3. Nếu token đúng, băm MD5 mật khẩu mới và update vào document user trong Firestore.
 * 4. Xóa resetToken khỏi bộ nhớ để không bị dùng lại.
 */
app.post('/api/auth/reset-password', async (req, res) => {
    try {
        const { email, resetToken, newPassword } = req.body;

        if (!email || !resetToken || !newPassword) {
            return res.status(400).json({ success: false, message: "Thiếu thông tin bắt buộc" });
        }

        if (newPassword.length < 6) {
            return res.status(400).json({ success: false, message: "Mật khẩu mới phải có ít nhất 6 ký tự" });
        }

        // Verify resetToken
        const storedData = resetTokenStore.get(email);

        if (!storedData) {
            return res.status(400).json({ success: false, message: "Phiên đặt lại mật khẩu không hợp lệ" });
        }

        if (Date.now() > storedData.expiresAt) {
            resetTokenStore.delete(email);
            return res.status(400).json({ success: false, message: "Phiên đặt lại mật khẩu đã hết hạn" });
        }

        if (storedData.token !== resetToken) {
            return res.status(400).json({ success: false, message: "Token không hợp lệ" });
        }

        // Tìm user và cập nhật password
        const usersRef = collection(db, "users");
        const q = query(usersRef, where("email", "==", email));
        const querySnapshot = await getDocs(q);

        if (querySnapshot.empty) {
            return res.status(404).json({ success: false, message: "Không tìm thấy tài khoản" });
        }

        const userDoc = querySnapshot.docs[0];
        const hashedNew = hashMD5(newPassword);
        await updateDoc(userDoc.ref, { password: hashedNew });

        // Xóa resetToken
        resetTokenStore.delete(email);

        res.json({ success: true, message: "Đổi mật khẩu thành công! Vui lòng đăng nhập lại." });

    } catch (error) {
        console.error("Lỗi đặt lại mật khẩu:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(3000, '0.0.0.0', () => {
    console.log('Server is running on port 3000');
});