const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.resolve(__dirname, '../database.sqlite');
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Failed to connect to SQLite server database:', err.message);
  } else {
    console.log('Successfully connected to SQLite backend database.');
  }
});

db.serialize(() => {
  // Create Users table
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT UNIQUE NOT NULL,
      passwordHash TEXT NOT NULL,
      createdAt INTEGER NOT NULL
    )
  `);

  // Create Transactions/Ledger table
  db.run(`
    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      local_id INTEGER,
      amount REAL NOT NULL,
      category TEXT NOT NULL,
      date INTEGER NOT NULL,
      paymentMethod TEXT NOT NULL,
      description TEXT,
      type TEXT CHECK(type IN ('INCOME', 'EXPENSE')) NOT NULL,
      userId INTEGER NOT NULL,
      FOREIGN KEY (userId) REFERENCES users(id)
    )
  `);
});

module.exports = db;
