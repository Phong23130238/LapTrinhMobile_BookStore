const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs, updateDoc, doc } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

const firebaseApp = initializeApp(firebaseConfig);
const db = getFirestore(firebaseApp);

async function fixPasswords() {
    try {
        const usersRef = collection(db, "users");
        const querySnapshot = await getDocs(usersRef);
        
        let count = 0;
        for (const docSnap of querySnapshot.docs) {
            const userData = docSnap.data();
            // md5 of "123" is 202cb962ac59075b964b07152d234b70
            await updateDoc(docSnap.ref, {
                password: "202cb962ac59075b964b07152d234b70"
            });
            count++;
            console.log(`Đã thêm mật khẩu cho user: ${userData.email || docSnap.id}`);
        }
        console.log(`Đã cập nhật thành công ${count} tài khoản.`);
        process.exit(0);
    } catch (error) {
        console.error("Lỗi cập nhật mật khẩu:", error);
        process.exit(1);
    }
}

fixPasswords();
