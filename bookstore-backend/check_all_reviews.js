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
  let counts = {};
  s.forEach(d => {
    let bookId = d.data().bookId;
    counts[bookId] = (counts[bookId] || 0) + 1;
  });
  console.log("Review counts by bookId:", counts);
  process.exit(0);
}
run();
