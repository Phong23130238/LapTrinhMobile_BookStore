const express = require('express');
const cors = require('cors');
const { initializeApp } = require('firebase/app');
const { getFirestore, collection, query, where, getDocs, addDoc, updateDoc, doc, getDoc } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

// Khởi tạo Firebase
const firebaseApp = initializeApp(firebaseConfig);
const db = getFirestore(firebaseApp);

const app = express();
app.use(cors());
app.use(express.json());

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

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Node.js Backend đang chạy tại http://localhost:${PORT}`);
});
