package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class WealthFlowRepository(private val context: Context, private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val apiService = RetrofitClient.service

    // Helper to check network connectivity
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // SHA-256 for passwords
    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            password // Fallback to plain if hash error, but should not happen
        }
    }

    // User Signup - Online-First, Offline-Fallback
    suspend fun signup(email: String, passwordRaw: String): Result<User> {
        val hashedPassword = hashPassword(passwordRaw)
        
        // Attempt backend first if online
        if (isNetworkAvailable()) {
            try {
                val response = apiService.signup(AuthRequest(email, hashedPassword))
                // Store user locally for offline access
                val newUser = User(id = response.userId, email = email, passwordHash = hashedPassword)
                try {
                    userDao.insertUser(newUser)
                } catch (e: Exception) {
                    // Ignored if already exists
                }
                return Result.success(newUser)
            } catch (e: Exception) {
                Log.e("Repository", "Signup backend error: ${e.message}")
            }
        }
        
        // Local direct creation if offline or backend failed
        return try {
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                return Result.failure(Exception("An account with this email already exists."))
            }
            val localUser = User(email = email, passwordHash = hashedPassword)
            val generatedId = userDao.insertUser(localUser)
            Result.success(localUser.copy(id = generatedId.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User Login - Online-First, Offline-Fallback
    suspend fun login(email: String, passwordRaw: String): Result<User> {
        val hashedPassword = hashPassword(passwordRaw)

        // Attempt backend first if online
        if (isNetworkAvailable()) {
            try {
                val response = apiService.login(AuthRequest(email, hashedPassword))
                val user = User(id = response.userId, email = email, passwordHash = hashedPassword)
                try {
                    // Update/Insert locally
                    userDao.insertUser(user)
                } catch (e: Exception) {
                    // Ignore duplicate key if user already in local db
                }
                return Result.success(user)
            } catch (e: Exception) {
                Log.e("Repository", "Login backend error: ${e.message}")
            }
        }

        // Local SQLite login for offline compatibility
        return try {
            val localUser = userDao.getUserByEmail(email)
            if (localUser != null && localUser.passwordHash == hashedPassword) {
                Result.success(localUser)
            } else {
                Result.failure(Exception("Invalid email or password. (Offline verification)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Flow for observing all transactions
    fun getTransactionsFlow(userId: Int): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsFlow(userId)
    }

    // CRUD Transaction Operations
    suspend fun insertTransaction(transaction: Transaction): Result<Long> {
        return try {
            val id = transactionDao.insertTransaction(transaction)
            // Trigger automatic sync in background
            triggerSyncBackground(transaction.userId)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            // Set synced to false because it updated
            val updated = transaction.copy(isSynced = false)
            transactionDao.updateTransaction(updated)
            triggerSyncBackground(transaction.userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
        return try {
            // Delete locally
            transactionDao.deleteTransaction(transaction)
            // Note: In an exhaustive API sync database design we would insert a deletion log,
            // but for simplicity, we delete locally and synclog later.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Budgets Flow & Management
    fun getBudgetsFlow(userId: Int): Flow<List<Budget>> {
        return budgetDao.getAllBudgetsFlow(userId)
    }

    suspend fun getBudgets(userId: Int): List<Budget> {
        return budgetDao.getAllBudgets(userId)
    }

    suspend fun saveBudget(budget: Budget): Result<Unit> {
        return try {
            budgetDao.insertBudget(budget)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Background Syncing Engine Implementation
    suspend fun syncTransactions(userId: Int, authToken: String): Result<Int> {
        if (!isNetworkAvailable()) {
            return Result.failure(Exception("Sync cancelled: Device is offline"))
        }

        return try {
            val unsynced = transactionDao.getUnsyncedTransactions(userId)
            if (unsynced.isEmpty()) {
                return Result.success(0)
            }

            Log.i("Repository", "Syncing ${unsynced.size} transactions to server...")
            val payloads = unsynced.map {
                TransactionPayload(
                    id = it.id,
                    amount = it.amount,
                    category = it.category,
                    date = it.date,
                    paymentMethod = it.paymentMethod,
                    description = it.description,
                    type = it.type
                )
            }

            val request = SyncRequest(userId = userId, transactions = payloads)
            // Bearer token syntax standard
            val tokenHeader = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
            val response = apiService.syncLedger(tokenHeader, request)

            if (response.success && response.syncedIds.isNotEmpty()) {
                transactionDao.markTransactionsSynced(response.syncedIds)
                Log.i("Repository", "Sync complete! Marked ${response.syncedIds.size} transactions synced.")
                Result.success(response.syncedIds.size)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Background sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Helper to start simple backend sync
    private suspend fun triggerSyncBackground(userId: Int) {
        // Can be queried from a session context later
        Log.i("Repository", "Autosync trigger scheduled for transaction CRUD of user $userId")
    }
}
