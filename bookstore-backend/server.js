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
const { getFirestore, collection, query, where, getDocs, addDoc, updateDoc, doc, getDoc, setDoc, deleteDoc } = require('firebase/firestore');
const { GoogleGenerativeAI } = require('@google/generative-ai');

const firebaseConfig = {
    apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
    projectId: "bookstore-500314",
    storageBucket: "bookstore-500314.firebasestorage.app"
};

// Khởi tạo Firebase
const firebaseApp = initializeApp(firebaseConfig);
const db = getFirestore(firebaseApp);

// Google OAuth2 Client để verify idToken
const GOOGLE_CLIENT_ID = "608811292447-d9cncbpmdbuf07npas15ack1o3cmdtsm.apps.googleusercontent.com"; // Web Client ID
const GOOGLE_ANDROID_CLIENT_ID = "608811292447-d9cncbpmdbuf07npas15ack1o3cmdtsm.apps.googleusercontent.com"; // Thường trùng với Web Client ID khi verify
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

function hashMD5(password) {
    return crypto.createHash('md5').update(password).digest('hex');
}

// Cấu hình Nodemailer để gửi mã OTP
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: '23130327@st.hcmuaf.edu.vn',
        pass: 'wtovolspzipsbvae'
    }
});

// API: Gửi mã OTP xác thực Email
app.post('/api/auth/send-otp', async (req, res) => {
    try {
        const { email } = req.body;
        if (!email) {
            return res.status(400).json({ success: false, message: "Vui lòng cung cấp email" });
        }

        // Tạo mã OTP 6 số
        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        // Thời gian hết hạn (5 phút)
        const expiresAt = Date.now() + 5 * 60 * 1000;

        // Lưu vào Firestore collection 'otps' (ghi đè nếu đã tồn tại)
        await setDoc(doc(db, "otps", email), {
            otp: otp,
            expiresAt: expiresAt
        });

        // Gửi email
        const mailOptions = {
            from: '"Bookstore App" <23130327@st.hcmuaf.edu.vn>',
            to: email,
            subject: 'Mã xác nhận đăng ký tài khoản Bookstore',
            text: `Chào bạn,\n\nMã xác nhận (OTP) của bạn là: ${otp}\n\nMã này sẽ hết hạn trong vòng 5 phút.\n\nTrân trọng,\nĐội ngũ Bookstore`
        };

        await transporter.sendMail(mailOptions);

        res.json({ success: true, message: "Mã OTP đã được gửi đến email của bạn." });
    } catch (error) {
        console.error("Lỗi gửi OTP:", error);
        res.status(500).json({ success: false, message: "Không thể gửi OTP. " + error.message });
    }
});

// Đky tài khoản thường
app.post('/api/auth/register', async (req, res) => {
    try {
        const { name, email, password, otp } = req.body;

        // Validate dữ liệu đầu vào
        if (!name || !email || !password || !otp) {
            return res.status(400).json({ success: false, message: "Vui lòng điền đầy đủ thông tin và mã OTP" });
        }

        if (password.length < 6) {
            return res.status(400).json({ success: false, message: "Mật khẩu phải có ít nhất 6 ký tự" });
        }

        // Kiểm tra mã OTP
        const otpDocRef = doc(db, "otps", email);
        const otpDocSnap = await getDoc(otpDocRef);

        if (!otpDocSnap.exists()) {
            return res.status(400).json({ success: false, message: "Chưa gửi mã OTP cho email này" });
        }

        const otpData = otpDocSnap.data();

        if (otpData.otp !== otp) {
            return res.status(400).json({ success: false, message: "Mã OTP không chính xác" });
        }

        if (Date.now() > otpData.expiresAt) {
            return res.status(400).json({ success: false, message: "Mã OTP đã hết hạn" });
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

        const newUser = {
            name,
            email,
            password: hashedPassword, // (hoặc null đối với Google)
            phone: "",
            address: "",
            avatarUrl: "", // (hoặc googleAvatar)
            role: "customer",
            isLocked: false, // THÊM DÒNG NÀY
            createdAt: new Date().toISOString()
        };

        const docRef = await addDoc(usersRef, newUser);

        // Cập nhật uid = document ID
        await updateDoc(docRef, { uid: docRef.id });

        // Xóa mã OTP sau khi đăng ký thành công
        await deleteDoc(otpDocRef);

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

// API: Đăng nhập thường (email + password)
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

        // ==========================================
        // THÊM ĐOẠN NÀY: Kiểm tra tài khoản có bị khóa không
        // Bắt chặt cả trường hợp lưu là boolean (true) hoặc chuỗi text ("true")
        if (userData.isLocked === true || String(userData.isLocked).toLowerCase() === "true") {
            return res.status(403).json({
                success: false,
                message: "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên."
            });
        }
        // ==========================================


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

// API: Đăng nhập bằng Google
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

            // ==========================================
            // THÊM ĐOẠN NÀY: Kiểm tra tài khoản có bị khóa không
            // Bắt chặt cả trường hợp lưu là boolean (true) hoặc chuỗi text ("true")
            if (existingData.isLocked === true || String(existingData.isLocked).toLowerCase() === "true") {
                return res.status(403).json({
                    success: false,
                    message: "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên."
                });
            }

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
            const newUser = {
                name: googleName,
                email: googleEmail,
                password: null,
                phone: "",
                address: "",
                avatarUrl: googleAvatar,
                role: "customer",
                isLocked: false,
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
        let updatedData = updatedSnap.data();
        if (updatedData && !updatedData.uid) {
            updatedData.uid = uid;
        }

        res.json({
            success: true,
            message: "Cập nhật hồ sơ thành công",
            data: updatedData
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
        // 1. Kiểm tra file ảnh trước tiên
        if (!req.file) {
            return res.status(400).json({ success: false, message: "Không tìm thấy file ảnh" });
        }

        // Lấy đường dẫn ảnh từ Cloudinary
        const imageUrl = req.file.path;
        const { bookId } = req.body;

        // 2. TRƯỜNG HỢP: THÊM SÁCH MỚI
        // Nếu không truyền bookId, hoặc bookId rỗng/null, chỉ trả về Link ảnh
        if (!bookId || bookId === "null" || bookId === "") {
            return res.json({
                success: true,
                message: "Upload ảnh thành công (Chế độ tạo mới)",
                imageUrl: imageUrl
            });
        }

        // 3. TRƯỜNG HỢP: CẬP NHẬT SÁCH
        // Chỉ chạy đoạn này khi client gửi kèm bookId hợp lệ
        const bookRef = doc(db, "books", bookId);
        const bookSnap = await getDoc(bookRef);

        if (!bookSnap.exists()) {
            // Sách không tồn tại nhưng vẫn trả về link ảnh để Client tự xử lý nếu cần
            return res.json({
                success: true,
                message: "Upload ảnh thành công (Không tìm thấy ID sách trên database)",
                imageUrl: imageUrl
            });
        }

        // Nếu sách đã tồn tại, cập nhật trực tiếp vào Firestore
        await updateDoc(bookRef, { imageUrl: imageUrl });

        return res.json({
            success: true,
            message: "Upload và cập nhật ảnh sách thành công",
            imageUrl: imageUrl
        });

    } catch (error) {
        console.error("Lỗi upload ảnh sách:", error);
        return res.status(500).json({ success: false, message: error.message });
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

        // Hỗ trợ query cả string và number để tránh lỗi không đồng nhất kiểu dữ liệu trên Firestore
        const qStr = query(reviewsRef, where("bookId", "==", bookId));
        const querySnapshotStr = await getDocs(qStr);

        let reviews = [];
        querySnapshotStr.forEach((docSnap) => {
            let data = docSnap.data();
            if (data.createdAt && data.createdAt.toDate) {
                data.createdAt = data.createdAt.toDate().toISOString();
            }
            reviews.push(data);
        });

        // Query thêm theo số nguyên
        const bookIdNum = Number(bookId);
        if (!isNaN(bookIdNum)) {
            const qNum = query(reviewsRef, where("bookId", "==", bookIdNum));
            const querySnapshotNum = await getDocs(qNum);
            querySnapshotNum.forEach((docSnap) => {
                let data = docSnap.data();
                if (data.createdAt && data.createdAt.toDate) {
                    data.createdAt = data.createdAt.toDate().toISOString();
                }
                // Tránh trùng lặp
                if (!reviews.find(r => r.reviewId === data.reviewId)) {
                    reviews.push(data);
                }
            });
        }

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
// VNPAY APIs
// =============================================
const querystring = require('querystring');

// Cấu hình VNPAY Sandbox (Test)
const vnp_TmnCode = "5BNONW5M";
const vnp_HashSecret = "C777AQAKMIXKG56BWNBE50H6CALW8IUR";
const vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
const vnp_ReturnUrl = "http://10.0.2.2:3000/api/vnpay_return"; 

function sortObject(obj) {
    let sorted = {};
    let str = [];
    let key;
    for (key in obj){
        if (obj.hasOwnProperty(key)) {
            str.push(encodeURIComponent(key));
        }
    }
    str.sort();
    for (key = 0; key < str.length; key++) {
        sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, "+");
    }
    return sorted;
}

app.post('/api/create_payment_url', (req, res) => {
    let ipAddr = req.headers['x-forwarded-for'] ||
        req.connection.remoteAddress ||
        req.socket.remoteAddress ||
        req.connection.socket.remoteAddress;

    let date = new Date();
    // format yyyyMMddHHmmss
    const pad = (n) => (n < 10 ? '0' + n : n);
    let createDate = date.getFullYear().toString() + pad(date.getMonth() + 1) + pad(date.getDate()) + pad(date.getHours()) + pad(date.getMinutes()) + pad(date.getSeconds());
    
    // expire in 15 minutes
    let expireDateObj = new Date(date.getTime() + 15 * 60000);
    let expireDate = expireDateObj.getFullYear().toString() + pad(expireDateObj.getMonth() + 1) + pad(expireDateObj.getDate()) + pad(expireDateObj.getHours()) + pad(expireDateObj.getMinutes()) + pad(expireDateObj.getSeconds());

    let orderId = req.body.orderId || date.getTime().toString();
    let amount = req.body.amount;
    let bankCode = req.body.bankCode || ''; 
    
    let orderInfo = req.body.orderDescription || 'Thanh toan don hang Bookstore';
    let orderType = req.body.orderType || 'billpayment';
    let locale = req.body.language || 'vn';
    
    let currCode = 'VND';
    let vnp_Params = {};
    vnp_Params['vnp_Version'] = '2.1.0';
    vnp_Params['vnp_Command'] = 'pay';
    vnp_Params['vnp_TmnCode'] = vnp_TmnCode;
    vnp_Params['vnp_Amount'] = amount * 100;
    if(bankCode !== null && bankCode !== ''){
        vnp_Params['vnp_BankCode'] = bankCode;
    }
    vnp_Params['vnp_CreateDate'] = createDate;
    vnp_Params['vnp_CurrCode'] = currCode;
    vnp_Params['vnp_IpAddr'] = ipAddr;
    vnp_Params['vnp_Locale'] = locale;
    vnp_Params['vnp_OrderInfo'] = orderInfo;
    vnp_Params['vnp_OrderType'] = orderType;
    vnp_Params['vnp_ReturnUrl'] = vnp_ReturnUrl;
    vnp_Params['vnp_TxnRef'] = orderId;
    vnp_Params['vnp_ExpireDate'] = expireDate;

    vnp_Params = sortObject(vnp_Params);

    let signData = Object.keys(vnp_Params)
                         .map(key => key + '=' + vnp_Params[key])
                         .join('&');
    let hmac = crypto.createHmac("sha512", vnp_HashSecret);
    let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex"); 
    vnp_Params['vnp_SecureHash'] = signed;
    
    let paymentUrl = vnp_Url + '?' + Object.keys(vnp_Params)
                                           .map(key => key + '=' + vnp_Params[key])
                                           .join('&');

    res.json({ success: true, paymentUrl: paymentUrl });
});
// FORGOT PASSWORD APIs
// =============================================

// API 1: Gửi OTP qua email
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

        // Kiểm tra tài khoản Google-only (không có password)
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

// API 2: Xác thực OTP
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

// API 3: Đặt lại mật khẩu mới
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

// =============================================
// ADMIN: QUẢN LÝ NGƯỜI DÙNG
// =============================================

// 1. Lấy danh sách toàn bộ người dùng
app.get('/api/users', async (req, res) => {
    try {
        const usersRef = collection(db, "users");
        const querySnapshot = await getDocs(usersRef);

        let users = [];
        querySnapshot.forEach((docSnap) => {
            let data = docSnap.data();
            // Đảm bảo luôn có uid trả về
            data.uid = docSnap.id;
            users.push(data);
        });

        res.json({ success: true, data: users });
    } catch (error) {
        console.error("Lỗi lấy danh sách user:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// 2. Cập nhật trạng thái Khóa/Mở khóa tài khoản
app.put('/api/users/:uid/lock', async (req, res) => {
    try {
        const uid = req.params.uid;
        const { isLocked } = req.body;

        if (uid === undefined || isLocked === undefined) {
            return res.status(400).json({ success: false, message: "Thiếu thông tin cập nhật" });
        }

        const userRef = doc(db, "users", uid);
        await updateDoc(userRef, { isLocked: isLocked });

        res.json({
            success: true,
            message: isLocked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản"
        });
    } catch (error) {
        console.error("Lỗi cập nhật trạng thái khóa:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// 3. Lấy thống kê mua hàng của 1 user
app.get('/api/users/:uid/stats', async (req, res) => {
    try {
        const uid = req.params.uid;

        // Tìm các đơn hàng của user này
        const ordersRef = collection(db, "orders");
        const q = query(ordersRef, where("userId", "==", uid));
        const querySnapshot = await getDocs(q);

        let totalOrders = 0;
        let totalSpent = 0;

        querySnapshot.forEach((docSnap) => {
            const orderData = docSnap.data();
            // Không tính các đơn hàng đã bị hủy (nếu có trường status)
            if (orderData.status !== "cancelled") {
                totalOrders++;
                // Giả sử tổng tiền đơn hàng được lưu trong biến totalPrice
                totalSpent += (orderData.totalPrice || 0);
            }
        });

        res.json({
            success: true,
            data: {
                totalOrders: totalOrders,
                totalSpent: totalSpent
            }
        });
    } catch (error) {
        console.error("Lỗi lấy thống kê user:", error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// ==========================================
// TÍCH HỢP GEMINI AI - TÓM TẮT SÁCH
// ==========================================
app.post('/api/ai/summarize', async (req, res) => {
    try {
        const { title, description } = req.body;

        if (!title || !description) {
            return res.status(400).json({ success: false, message: "Vui lòng cung cấp tên sách và mô tả!" });
        }

        // Lấy API Key từ biến môi trường
        const apiKey = process.env.GEMINI_API_KEY;
        if (!apiKey) {
            return res.status(500).json({ success: false, message: "Lỗi cấu hình: Thiếu GEMINI_API_KEY trong file .env" });
        }

        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        const prompt = `Bạn là một trợ lý AI đọc sách chuyên nghiệp. Dưới đây là thông tin về một cuốn sách:
Tên sách: ${title}
Mô tả nội dung: ${description}

Yêu cầu: Hãy tham khảo thông tin từ các nguồn chính thống khác. Phân tích cuốn sách này theo đúng 3 ý sau đây (sử dụng gạch đầu dòng):
1. Đối tượng độc giả: (Sách này dành cho ai?)
2. Tóm tắt nội dung: (Nội dung chính là gì, cực kỳ ngắn gọn)
3. Bài học rút ra: (Giá trị cốt lõi mang lại)
Chỉ trả về 3 gạch đầu dòng này, tuyệt đối không dài dòng giải thích.`;

        const result = await model.generateContent(prompt);
        const responseText = result.response.text();

        res.json({
            success: true,
            data: responseText
        });
    } catch (error) {
        console.error("Lỗi AI Gemini:", error);
        res.status(500).json({ success: false, message: "Lỗi khi gọi AI: " + error.message });
    }
});