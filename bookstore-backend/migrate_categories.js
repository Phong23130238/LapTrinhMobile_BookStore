const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs, doc, updateDoc, deleteField } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyAxkEKnqnAIX4FMWsb-jxmSkPLZLwf0wO4",
  projectId: "bookstore-500314",
  storageBucket: "bookstore-500314.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function migrateCategories() {
  try {
    console.log("Fetching categories...");
    const categoriesSnap = await getDocs(collection(db, 'categories'));
    const categoryMap = {}; // Maps lower case category name to categoryId
    
    categoriesSnap.forEach(doc => {
      const data = doc.data();
      if (data.name) {
        categoryMap[data.name.trim().toLowerCase()] = doc.id;
      }
    });

    console.log("Category map built:", categoryMap);

    console.log("Fetching books...");
    const booksSnap = await getDocs(collection(db, 'books'));
    let updatedCount = 0;

    for (const bookDoc of booksSnap.docs) {
      const data = bookDoc.data();
      
      // If book has 'category' field, replace it with 'categoryId'
      if (data.category && typeof data.category === 'string') {
        const catName = data.category.trim().toLowerCase();
        const catId = categoryMap[catName];
        
        if (catId) {
          const updateData = {
            categoryId: String(catId),
            category: deleteField()
          };
          
          await updateDoc(doc(db, 'books', bookDoc.id), updateData);
          console.log(`Updated book ${bookDoc.id}: Changed category '${data.category}' to categoryId '${catId}'`);
          updatedCount++;
        } else {
          console.log(`WARNING: Book ${bookDoc.id} has unknown category '${data.category}'. Skipping.`);
        }
      } else if (data.categoryId) {
        // Ensure categoryId is a string if it's currently a number
        if (typeof data.categoryId !== 'string') {
          await updateDoc(doc(db, 'books', bookDoc.id), { categoryId: String(data.categoryId) });
          console.log(`Updated book ${bookDoc.id}: Casted categoryId to string`);
          updatedCount++;
        }
      }
    }

    console.log(`Migration completed. Updated ${updatedCount} books.`);
    process.exit(0);
  } catch (err) {
    console.error("Migration failed:", err);
    process.exit(1);
  }
}

migrateCategories();
