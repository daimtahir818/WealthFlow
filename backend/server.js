require('dotenv').config();
const express = require('express');
const cors = require('cors');
const db = require('./config/database');
const verifyToken = require('./middleware/verifyToken');
const userController = require('./controllers/userController');
const ledgerController = require('./controllers/ledgerController');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS and Express body parsing
app.use(cors());
app.use(express.json());

// Auth Routes
app.post('/api/auth/signup', userController.signup);
app.post('/api/auth/login', userController.login);

// Protected Ledger Sync Route
app.post('/api/ledger/sync', verifyToken, ledgerController.syncLedger);

// Start Server listener
app.listen(PORT, () => {
  console.log(`===============================================`);
  console.log(`WealthFlow Enterprise Backend running on port ${PORT}`);
  console.log(`API Base: http://localhost:${PORT}/api`);
  console.log(`SQLite database file: database.sqlite`);
  console.log(`===============================================`);
});

module.exports = app;
