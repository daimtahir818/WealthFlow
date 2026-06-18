package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    object Login : Screen()
    object Signup : Screen()
    object Dashboard : Screen()
    object History : Screen()
    object BudgetSettings : Screen()
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Synced(val count: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = WealthFlowRepository(context, database)

    // Current screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Logged in User Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Sync state
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Simulated JWT Auth Token
    private var token: String = "simulated_jwt_token"

    // Observed items for logged in user
    val transactions: StateFlow<List<Transaction>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getTransactionsFlow(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getBudgetsFlow(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active filters
    val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow("All")
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        searchQuery,
        filterCategory,
        filterStartDate,
        filterEndDate
    ) { txList, query, cat, start, end ->
        txList.filter { tx ->
            val matchesQuery = tx.description.contains(query, ignoreCase = true) || 
                               tx.category.contains(query, ignoreCase = true)
            val matchesCat = (cat == "All") || tx.category.equals(cat, ignoreCase = true)
            val matchesStart = start == null || tx.date >= start
            val matchesEnd = end == null || tx.date <= end
            matchesQuery && matchesCat && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Startup Session Restoration - check if there's an account saved locally
        viewModelScope.launch {
            // Restore last user from local DB if any
            withContext(Dispatchers.IO) {
                // For demonstration/session resilience: find first user or use secure store setup
                val sharedPref = context.getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE)
                val lastEmail = sharedPref.getString("logged_in_user_email", null)
                if (lastEmail != null) {
                    val user = database.userDao().getUserByEmail(lastEmail)
                    if (user != null) {
                        _currentUser.value = user
                        _currentScreen.value = Screen.Dashboard
                    }
                }
            }
        }

        // Start Background Sync Loop
        viewModelScope.launch {
            while (true) {
                delay(12000) // Trigger sync attempt every 12 seconds
                val user = _currentUser.value
                if (user != null) {
                    performSync(user.id)
                }
            }
        }
    }

    // Navigation helper
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Authentication Actions
    fun login(email: String, passwordRaw: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            val result = withContext(Dispatchers.IO) {
                repository.login(email, passwordRaw)
            }
            _isAuthenticating.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                
                // Persist session locally (securely)
                val sharedPref = context.getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("logged_in_user_email", email).apply()
                
                _currentScreen.value = Screen.Dashboard
                performSync(user!!.id)
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Authentication failed"
            }
        }
    }

    fun signup(email: String, passwordRaw: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            val result = withContext(Dispatchers.IO) {
                repository.signup(email, passwordRaw)
            }
            _isAuthenticating.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                
                // Persist session
                val sharedPref = context.getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("logged_in_user_email", email).apply()
                
                // Create initial standard budgets for this user so they don't start blank
                withContext(Dispatchers.IO) {
                    database.budgetDao().insertBudget(Budget("Global", 2500.0, user!!.id))
                    database.budgetDao().insertBudget(Budget("Food", 600.0, user.id))
                    database.budgetDao().insertBudget(Budget("Entertainment", 300.0, user.id))
                    database.budgetDao().insertBudget(Budget("Shopping", 400.0, user.id))
                }
                
                _currentScreen.value = Screen.Dashboard
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Signup failed"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        val sharedPref = context.getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("logged_in_user_email").apply()
        _currentScreen.value = Screen.Login
    }

    // Core transaction actions
    fun addTransaction(amount: Double, category: String, date: Long, paymentMethod: String, description: String, type: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                description = description,
                type = type,
                userId = user.id
            )
            withContext(Dispatchers.IO) {
                repository.insertTransaction(tx)
                // Evaluate budgets and potentially trigger notification alert!
                checkBudgetLevels(user.id, category, amount, type)
            }
        }
    }

    fun editTransaction(id: Int, amount: Double, category: String, date: Long, paymentMethod: String, description: String, type: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val tx = Transaction(
                id = id,
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                description = description,
                type = type,
                userId = user.id
            )
            withContext(Dispatchers.IO) {
                repository.updateTransaction(tx)
                checkBudgetLevels(user.id, category, 0.0, type) // Re-evaluate budgets
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTransaction(tx)
            }
        }
    }

    // Budget Configuration
    fun setBudgetLimit(category: String, limit: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val budget = Budget(category = category, limitAmount = limit, userId = user.id)
            withContext(Dispatchers.IO) {
                repository.saveBudget(budget)
            }
        }
    }

    // Budget checks and Alerts
    private suspend fun checkBudgetLevels(userId: Int, category: String, addedAmount: Double, type: String) {
        if (type != "EXPENSE") return

        // 1. Check Category Specific Budget
        val catBudget = database.budgetDao().getBudgetByCategory(category, userId)
        val allTx = database.transactionDao().getAllTransactions(userId)
        
        // Sum expenses in this current month for this category
        val currentMonthStart = getStartOfCurrentMonth()
        val categoryExpenses = allTx.filter { 
            it.type == "EXPENSE" && 
            it.category.equals(category, ignoreCase = true) && 
            it.date >= currentMonthStart 
        }.sumOf { it.amount }

        if (catBudget != null && categoryExpenses > catBudget.limitAmount) {
            triggerLocalAlert(
                title = "⚠️ Category Budget Exceeded",
                message = "Your $category spending of $${String.format("%.2f", categoryExpenses)} exceeded your limit of $${catBudget.limitAmount}!"
            )
        }

        // 2. Check Global Budget
        val globalBudget = database.budgetDao().getBudgetByCategory("Global", userId)
        val totalExpenses = allTx.filter { 
            it.type == "EXPENSE" && 
            it.date >= currentMonthStart 
        }.sumOf { it.amount }

        if (globalBudget != null && totalExpenses > globalBudget.limitAmount) {
            triggerLocalAlert(
                title = "🚨 Global Budget Danger!",
                message = "Total monthly expenses of $${String.format("%.2f", totalExpenses)} have exceeded your global budget limit of $${globalBudget.limitAmount}!"
            )
        }
    }

    // Utility for start of month
    private fun getStartOfCurrentMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Manual or scheduled synchronization task
    fun runManualSync() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            performSync(user.id)
        }
    }

    private suspend fun performSync(userId: Int) {
        if (!repository.isNetworkAvailable()) {
            _syncStatus.value = SyncStatus.Error("No connection available")
            return
        }

        _syncStatus.value = SyncStatus.Syncing
        val result = repository.syncTransactions(userId, token)
        if (result.isSuccess) {
            val count = result.getOrDefault(0)
            _syncStatus.value = SyncStatus.Synced(count)
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown sync error"
            _syncStatus.value = SyncStatus.Error(errorMsg)
        }
    }

    // Local notification system
    private fun triggerLocalAlert(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "budget_alerts_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies the user when budget consumption exceeds targets"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e("AppViewModel", "Missing permission for notifications: ${e.message}")
        }
    }
}

class AppViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
