const { initializeApp } = require('firebase/app');
const { getFirestore, doc, getDoc } = require('firebase/firestore');
const fs = require('fs');
const firebaseConfig = { apiKey: 'AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4', projectId: 'bookstore-500314', storageBucket: 'bookstore-500314.firebasestorage.app' };
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
async function test() {
  const d = await getDoc(doc(db, 'users', '2'));
  fs.writeFileSync('out.txt', JSON.stringify(d.data(), null, 2));
  process.exit();
}
test();
