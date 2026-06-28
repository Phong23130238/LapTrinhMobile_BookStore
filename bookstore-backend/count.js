const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs, query, where } = require('firebase/firestore');
const firebaseConfig = { apiKey: 'AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4', projectId: 'bookstore-500314', storageBucket: 'bookstore-500314.firebasestorage.app' };
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
async function count() {
  const q = query(collection(db, 'users'), where('email', '==', 'hoang.tran9x@gmail.com'));
  const snap = await getDocs(q);
  console.log('COUNT:', snap.size);
  snap.forEach(d => console.log(d.id, d.data().password));
  process.exit();
}
count();
