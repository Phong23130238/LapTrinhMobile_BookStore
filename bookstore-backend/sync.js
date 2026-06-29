const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs } = require('firebase/firestore');
const fs = require('fs');
const path = require('path');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Các bảng dữ liệu trên Firebase
const collections = ["categories", "series", "books", "users", "reviews", "orders", "vouchers", "carts"];

async function syncData() {
    console.log("Đang bắt đầu tải dữ liệu từ Firebase...");
    let result = {};
    
    for (const colName of collections) {
        console.log(`Đang tải bảng: ${colName}...`);
        try {
            const querySnapshot = await getDocs(collection(db, colName));
            let colData = {};
            querySnapshot.forEach((doc) => {
                let data = doc.data();
                // Chuyển đổi Timestamp sang dạng string để ghi ra file JSON
                for (let key in data) {
                    if (data[key] && typeof data[key] === 'object' && data[key].toDate) {
                        data[key] = data[key].toDate().toISOString();
                    }
                }
                colData[doc.id] = data;
            });
            // Viết hoa tên bảng để giống format db_firebase.json cũ
            result[colName.toUpperCase()] = colData;
        } catch (error) {
            console.error(`Lỗi khi tải bảng ${colName}:`, error.message);
        }
    }
    
    // Đường dẫn trỏ tới file db_firebase.json trong Android project
    const outputPath = path.join(__dirname, '../app/src/main/assets/db_firebase.json');
    fs.writeFileSync(outputPath, JSON.stringify(result, null, 2), 'utf-8');
    console.log(`\n✅ Đã đồng bộ thành công dữ liệu từ Database Online về file:`);
    console.log(outputPath);
    process.exit(0);
}

syncData();
