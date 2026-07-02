const { initializeApp } = require('firebase/app');
const { getFirestore, doc, updateDoc } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const updates = {
  "11": "https://upload.wikimedia.org/wikipedia/vi/a/a2/Detective_Conan_Vol_1.jpg",
  "12": "https://upload.wikimedia.org/wikipedia/vi/9/94/Detective_Conan_Vol_2.jpg",
  "13": "https://upload.wikimedia.org/wikipedia/vi/9/90/Detective_Conan_Vol_3.jpg",
  "14": "https://upload.wikimedia.org/wikipedia/vi/0/05/Detective_Conan_Vol_4.jpg",
  "15": "https://upload.wikimedia.org/wikipedia/vi/f/f6/Detective_Conan_Vol_5.jpg",
  "16": "https://upload.wikimedia.org/wikipedia/vi/6/69/Detective_Conan_Vol_6.jpg",
  "17": "https://upload.wikimedia.org/wikipedia/vi/8/8c/Doraemon_volume_1_cover.jpg",
  "18": "https://upload.wikimedia.org/wikipedia/vi/1/13/Doraemon_volume_2_cover.jpg",
  "19": "https://upload.wikimedia.org/wikipedia/vi/3/30/Doraemon_volume_3_cover.jpg",
  "20": "https://upload.wikimedia.org/wikipedia/vi/b/bc/Doraemon_volume_4_cover.jpg",
  "21": "https://upload.wikimedia.org/wikipedia/vi/0/03/Doraemon_volume_5_cover.jpg"
};

async function fixImages() {
  try {
    for (const [bookId, imageUrl] of Object.entries(updates)) {
      const bookRef = doc(db, 'books', bookId);
      await updateDoc(bookRef, { imageUrl });
      console.log(`Updated book ${bookId}`);
    }
    console.log("All done!");
    process.exit(0);
  } catch (err) {
    console.error("Error updating books:", err);
    process.exit(1);
  }
}

fixImages();
