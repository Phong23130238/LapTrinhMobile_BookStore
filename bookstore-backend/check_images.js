const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function checkEmptyImages() {
  try {
    const booksRef = collection(db, 'books');
    const snap = await getDocs(booksRef);
    let count = 0;
    snap.forEach(doc => {
      const data = doc.data();
      if (!data.imageUrl || data.imageUrl.trim() === '') {
        console.log(`Book ID ${doc.id} (${data.title}) has empty imageUrl`);
        count++;
      }
    });
    console.log(`Found ${count} books with empty imageUrl.`);
    process.exit(0);
  } catch (err) {
    console.error("Error:", err);
    process.exit(1);
  }
}

checkEmptyImages();
