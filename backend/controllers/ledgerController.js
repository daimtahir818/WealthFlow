const db = require('../config/database');

exports.syncLedger = (req, res) => {
  const { userId, transactions } = req.body;

  if (!userId || !Array.isArray(transactions)) {
    return res.status(400).json({ success: false, message: 'Sync payload structure invalid.' });
  }

  if (transactions.length === 0) {
    return res.status(200).json({ success: true, syncedIds: [], message: 'No records to synchronize.' });
  }

  const syncedIds = [];
  let processedCount = 0;
  let hasError = false;

  transactions.forEach((tx) => {
    // Check if transaction was already synced previously to avoid double records
    db.get(
      'SELECT id FROM transactions WHERE userId = ? AND local_id = ? AND amount = ? AND date = ?',
      [userId, tx.id, tx.amount, tx.date],
      (err, row) => {
        if (err) {
          hasError = true;
          processedCount++;
          checkFinish();
          return;
        }

        if (row) {
          // Already synced in the past
          syncedIds.push(tx.id);
          processedCount++;
          checkFinish();
        } else {
          // Insert new record
          const stmt = db.prepare(
            'INSERT INTO transactions (local_id, amount, category, date, paymentMethod, description, type, userId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
          );
          stmt.run(
            tx.id,
            tx.amount,
            tx.category,
            tx.date,
            tx.paymentMethod,
            tx.description,
            tx.type,
            userId,
            function (err2) {
              if (err2) {
                console.error('Error inserting transaction element in sync:', err2.message);
                hasError = true;
              } else {
                syncedIds.push(tx.id);
              }
              processedCount++;
              checkFinish();
            }
          );
          stmt.finalize();
        }
      }
    );
  });

  function checkFinish() {
    if (processedCount === transactions.length) {
      if (hasError && syncedIds.length === 0) {
        return res.status(500).json({
          success: false,
          syncedIds: [],
          message: 'Failed to sync ledger items due to internal error.'
        });
      }
      return res.status(200).json({
        success: true,
        syncedIds: syncedIds,
        message: `Successfully synchronized ${syncedIds.length} elements to primary ledger.`
      });
    }
  }
};
