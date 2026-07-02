const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function run() {
  const s = await getDocs(collection(db, 'reviews'));
  s.forEach(d => console.log(d.id, 'bookId:', d.data().bookId, 'type:', typeof d.data().bookId));
  process.exit(0);
}
run();
