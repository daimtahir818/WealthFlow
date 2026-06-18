package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightBackground
                ) {
                    AppContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppContent() {
    val context = LocalContext.current
    val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppViewModelFactory(context.applicationContext as android.app.Application)
    )

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Login -> LoginScreen(viewModel)
                is Screen.Signup -> SignupScreen(viewModel)
                else -> {
                    // Logged in scaffold
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MidnightBackground,
                        bottomBar = {
                            AppBottomBar(
                                currentScreen = screen,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (screen) {
                                is Screen.Dashboard -> DashboardScreen(viewModel)
                                is Screen.History -> HistoryScreen(viewModel)
                                is Screen.BudgetSettings -> BudgetSettingsScreen(viewModel)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

// Bottom Bar with Neon Coral Active indicator
@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DeepMidnightCard,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        val items = listOf(
            Triple(Screen.Dashboard, Icons.Default.Home, "Overview"),
            Triple(Screen.History, Icons.Default.List, "Ledger"),
            Triple(Screen.BudgetSettings, Icons.Default.Settings, "Budgets")
        )

        items.forEach { (screen, icon, label) ->
            val isSelected = currentScreen::class == screen::class
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NeonCoral else TextMuted
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCoral else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = HighIndigo
                ),
                modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
            )
        }
    }
}

// ---------------- LOGIN SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo representation
        LogoWidget()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to WealthFlow",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Your offline-first, premium wealth ledger",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Secure Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authError != null) {
            Text(
                text = authError ?: "",
                color = ExpenseRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.trim().isNotEmpty() && password.isNotEmpty()) {
                    viewModel.login(email.trim(), password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isAuthenticating
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Access Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = { viewModel.navigateTo(Screen.Signup) },
            modifier = Modifier.testTag("to_signup_button")
        ) {
            Text("Create a new encrypted local account", color = AccentBlue, fontSize = 14.sp)
        }
    }
}

// ---------------- SIGNUP SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    var localValidationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoWidget()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Data is encrypted and stored 100% locally.",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Secure Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_pw_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Secure Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_confirm_pw_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val displayedError = localValidationError ?: authError
        if (displayedError != null) {
            Text(
                text = displayedError,
                color = ExpenseRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                localValidationError = null
                if (email.trim().isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    localValidationError = "All entry fields must be completed."
                } else if (password != confirmPassword) {
                    localValidationError = "Passwords match failed."
                } else if (password.length < 6) {
                    localValidationError = "Security standards require 6+ characters."
                } else {
                    viewModel.signup(email.trim(), password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("signup_submit_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isAuthenticating
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Initialize Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = { viewModel.navigateTo(Screen.Login) },
            modifier = Modifier.testTag("to_login_button")
        ) {
            Text("Already registered? Open vault", color = AccentBlue, fontSize = 14.sp)
        }
    }
}

// Premium Pulse Wave Logo Drawing
@Composable
fun LogoWidget() {
    Canvas(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(DeepMidnightCard)
            .border(1.5.dp, NeonCoralMuted, CircleShape)
    ) {
        // Draw geometric elements representing WealthFlow
        val width = size.width
        val height = size.height

        // Background concentric circle
        drawCircle(
            color = HighIndigo,
            radius = width * 0.35f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw neon coral pulse wave line
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.15f, height * 0.5f)
            lineTo(width * 0.35f, height * 0.5f)
            lineTo(width * 0.42f, height * 0.25f)
            lineTo(width * 0.52f, height * 0.72f)
            lineTo(width * 0.59f, height * 0.45f)
            lineTo(width * 0.65f, height * 0.53f)
            lineTo(width * 0.85f, height * 0.5f)
        }

        drawPath(
            path = path,
            color = NeonCoral,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}


// ---------------- DASHBOARD / HOME SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }

    // Summary math
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val savings = totalIncome - totalExpense

    // Budget tracking math
    val globalBudgetObj = budgets.firstOrNull { it.category == "Global" }
    val globalLimit = globalBudgetObj?.limitAmount ?: 2500.0

    // Sum expenses in this current month helper
    val currentMonthStart = getStartOfCurrentMonth()
    val thisMonthExpenses = transactions.filter { 
        it.type == "EXPENSE" && it.date >= currentMonthStart 
    }.sumOf { it.amount }

    val budgetUsageFraction = if (globalLimit > 0) (thisMonthExpenses / globalLimit).toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Safe padding spacer for edgeToEdge
        Spacer(modifier = Modifier.height(12.dp))

        // Synced badge & Manual sync button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Sync pill
            SyncIndicatorPill(syncStatus = syncStatus) {
                viewModel.runManualSync()
                Toast.makeText(context, "Initiating Server Sync Ledger...", Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Premium Account balance card
        WalletCardWidget(savings = savings, income = totalIncome, expense = totalExpense, onLogout = { viewModel.logout() })

        Spacer(modifier = Modifier.height(24.dp))

        // Progress components and charts grid
        Text(
            text = "Budget Overview",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Budget circle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepMidnightCard)
                    .padding(16.dp)
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Spent of Budget",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        BudgetProgressBarCircular(progressFraction = budgetUsageFraction)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(budgetUsageFraction * 100).toInt()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetUsageFraction >= 1.0f) ExpenseRed else NeonCoral
                            )
                        }
                    }

                    Text(
                        text = "$${String.format("%.0f", thisMonthExpenses)} / $${String.format("%.0f", globalLimit)}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Category targets tracking card
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepMidnightCard)
                    .padding(16.dp)
                    .height(180.dp)
            ) {
                Column {
                    Text(
                        text = "Category Limits",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val nonGlobalBudgets = budgets.filter { it.category != "Global" }.take(3)
                    if (nonGlobalBudgets.isEmpty()) {
                        Text(
                            text = "No category limits configured.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            nonGlobalBudgets.forEach { b ->
                                val catExpenses = transactions.filter { 
                                    it.type == "EXPENSE" && it.category.equals(b.category, ignoreCase = true) && it.date >= currentMonthStart 
                                }.sumOf { it.amount }
                                val bFraction = if (b.limitAmount > 0) (catExpenses / b.limitAmount).toFloat() else 0f

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(b.category, fontSize = 11.sp, color = TextPrimary)
                                        Text(
                                            "$${catExpenses.toInt()}/$${b.limitAmount.toInt()}",
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = bFraction.coerceIn(0f, 1f),
                                        color = if (bFraction >= 1f) ExpenseRed else AccentBlue,
                                        trackColor = HighIndigo,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spending trend custom column bar chart
        Text(
            text = "Monthly Spending Trend",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DeepMidnightCard)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Weekly Breakdown",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Render custom Canvas drawn bar charts
                CustomBarChartWidget(transactions = transactions)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent transaction list preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = { viewModel.navigateTo(Screen.History) }) {
                Text("See All", color = NeonCoral, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded transactions. Tap + below to add first income/expense.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                transactions.take(5).forEach { tx ->
                    TransactionItemRow(tx = tx, onDelete = { viewModel.deleteTransaction(tx) })
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Extra margin scroll buffer
    }

    // Floating action button inside the box corner
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = NeonCoral,
            contentColor = TextPrimary,
            modifier = Modifier
                .size(56.dp)
                .testTag("add_transaction_fab")
        ) {
            Icon(Icons.Default.Add, "Add Transaction")
        }
    }

    // Add Transaction Dialog Modal
    if (showAddDialog) {
        AddEditTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amt, cat, dt, pMethod, desc, t ->
                viewModel.addTransaction(amt, cat, dt, pMethod, desc, t)
                showAddDialog = false
            }
        )
    }
}

// Circular progress ring drawing
@Composable
fun BudgetProgressBarCircular(progressFraction: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthPx = 10.dp.toPx()
        // Draw Track
        drawArc(
            color = HighIndigo,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Draw Active Progress Line (Neon Coral or Expense Red Warning glow)
        drawArc(
            color = if (progressFraction >= 1.0f) ExpenseRed else NeonCoral,
            startAngle = 140f,
            sweepAngle = 260f * animatedProgress,
            useCenter = false,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

// Premium account card layout containing glowing colors
@Composable
fun WalletCardWidget(savings: Double, income: Double, expense: Double, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        HighIndigo,
                        DeepMidnightCard,
                    )
                )
            )
            .border(1.dp, SurfaceLine, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL SAVINGS BALANCE",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (savings >= 0) "$${String.format("%,.2f", savings)}" else "-$${String.format("%,.2f", -savings)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (savings >= 0) TextPrimary else ExpenseRed
            )

            Spacer(modifier = Modifier.height(24.dp))

            Divider(color = SurfaceLine.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Income detail block
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Income",
                            tint = IncomeGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Income", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$${String.format("%,.0f", income)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Expense detail block
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expenses",
                            tint = ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Expenses", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$${String.format("%,.0f", expense)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// Synchronizations status indicator
@Composable
fun SyncIndicatorPill(syncStatus: SyncStatus, onSyncTrigger: () -> Unit) {
    val text = when (syncStatus) {
        is SyncStatus.Idle -> "Online State"
        is SyncStatus.Syncing -> "Synching ledger..."
        is SyncStatus.Synced -> "Synced updates (${syncStatus.count})"
        is SyncStatus.Error -> "Offline: local mode"
    }

    val color = when (syncStatus) {
        is SyncStatus.Idle -> AccentBlue
        is SyncStatus.Syncing -> NeonCoral
        is SyncStatus.Synced -> IncomeGreen
        is SyncStatus.Error -> TextMuted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .clickable { onSyncTrigger() }
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (syncStatus is SyncStatus.Syncing) alpha else 1.0f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// Sleek single line items representation
@Composable
fun TransactionItemRow(
    tx: Transaction,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (tx.id % 2 == 0) DeepMidnightCard else DeepMidnightCard.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (tx.isSynced) "• Synced" else "• Pending Sync",
                        fontSize = 10.sp,
                        color = if (tx.isSynced) IncomeGreen.copy(alpha = 0.7f) else NeonCoral.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tx.description,
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.date)),
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (tx.type == "INCOME") "+$${String.format("%.2f", tx.amount)}" else "-$${String.format("%.2f", tx.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.type == "INCOME") IncomeGreen else TextPrimary
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ExpenseRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Custom column spending bar charts using Canvas coordinates
@Composable
fun CustomBarChartWidget(transactions: List<Transaction>) {
    // Break down expenses in past 4 weeks
    val currentMillis = System.currentTimeMillis()
    val weekMillis = 7 * 24 * 60 * 60 * 1000L

    val weekSums = DoubleArray(4)
    for (i in 0..3) {
        val start = currentMillis - (i + 1) * weekMillis
        val end = currentMillis - i * weekMillis
        weekSums[3 - i] = transactions.filter { 
            it.type == "EXPENSE" && it.date in start..end 
        }.sumOf { it.amount }
    }

    val maxVal = weekSums.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0..3) {
            val amount = weekSums[i]
            val barFraction = (amount / maxVal).toFloat().coerceIn(0.01f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = "$${amount.toInt()}",
                    fontSize = 10.sp,
                    color = if (barFraction > 0.5f) NeonCoral else TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Render Canvas Column rounded bar
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(0.75f * barFraction)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NeonCoral,
                                    AccentBlue
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "W${i + 1}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// ---------------- HISTORY / TRANSACTIONS LIST SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterCat by viewModel.filterCategory.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf("All", "Salary", "Food", "Entertainment", "Shopping", "Utilities", "Investment", "Others")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Transaction Ledger",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text Search bar parsing descriptions
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
            placeholder = { Text("Search description...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCoral,
                unfocusedBorderColor = SurfaceLine,
                focusedContainerColor = DeepMidnightCard,
                unfocusedContainerColor = DeepMidnightCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row of filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category filter filter drop button
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showCategoryDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMidnightCard),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SurfaceLine)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterCat == "All") "Select Category" else filterCat,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Icon(Icons.Default.ArrowDropDown, "Select", tint = TextMuted)
                    }
                }

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false },
                    modifier = Modifier.background(DeepMidnightCard)
                ) {
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c, color = TextPrimary) },
                            onClick = {
                                viewModel.filterCategory.value = c
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // Quick reset filter buttons
            Button(
                onClick = {
                    viewModel.searchQuery.value = ""
                    viewModel.filterCategory.value = "All"
                    viewModel.filterStartDate.value = null
                    viewModel.filterEndDate.value = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Reset", color = TextPrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching records found in ledger.",
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions) { tx ->
                    TransactionItemRow(tx = tx, onDelete = { viewModel.deleteTransaction(tx) })
                }
            }
        }
    }

    // Add dialog trigger
    if (showAddDialog) {
        AddEditTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amt, cat, dt, pMethod, desc, t ->
                viewModel.addTransaction(amt, cat, dt, pMethod, desc, t)
                showAddDialog = false
            }
        )
    }
}


// ---------------- BUDGET SETTINGS SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(viewModel: AppViewModel) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val categories = listOf("Global", "Food", "Entertainment", "Shopping", "Utilities", "Investment", "Others")

    // Input fields state
    var selectedCategory by remember { mutableStateOf("Global") }
    var limitInput by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Budget Configurations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Adjust targets to automatically trigger OS push alerts if consumption caps exceed limits",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepMidnightCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Set Cap Limit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select category dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showCategoryMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HighIndigo),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category: $selectedCategory", color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, "Select", tint = TextMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.background(DeepMidnightCard)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Limit number input field
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Maximum Budget Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCoral,
                        unfocusedBorderColor = SurfaceLine,
                        focusedContainerColor = MidnightBackground,
                        unfocusedContainerColor = MidnightBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val parsedLimit = limitInput.toDoubleOrNull()
                        if (parsedLimit != null && parsedLimit >= 0) {
                            viewModel.setBudgetLimit(selectedCategory, parsedLimit)
                            limitInput = ""
                            Toast.makeText(context, "$selectedCategory budget cap saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Completed amount must be a positive value.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_budget_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Configuration Target", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display current active budgets in clean card list
        Text(
            text = "Current Monitoring Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (budgets.isEmpty()) {
            Text("No active budget targets configured.", color = TextMuted, fontSize = 13.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                budgets.forEach { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepMidnightCard.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (b.category == "Global") NeonCoralMuted else HighIndigo),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (b.category == "Global") Icons.Default.Star else Icons.Default.Info,
                                        contentDescription = b.category,
                                        tint = if (b.category == "Global") NeonCoral else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(b.category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text(
                                "$${String.format("%.2f", b.limitAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCoral
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}


// ---------------- ADD TRANSACTION MODAL DIALOG ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, date: Long, paymentMethod: String, description: String, type: String) -> Unit
) {
    val context = LocalContext.current

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    
    var dateSelected by remember { mutableStateOf(System.currentTimeMillis()) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Salary", "Food", "Entertainment", "Shopping", "Utilities", "Investment", "Others")
    val paymentMethods = listOf("Cash", "Card", "Bank Transfer")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepMidnightCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // INCOME / EXPENSE switch row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidnightBackground)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { selectedType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") ExpenseRed else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Expense", color = TextPrimary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { selectedType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") IncomeGreen else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Income", color = TextPrimary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCoral,
                        unfocusedBorderColor = SurfaceLine,
                        focusedContainerColor = MidnightBackground,
                        unfocusedContainerColor = MidnightBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCoral,
                        unfocusedBorderColor = SurfaceLine,
                        focusedContainerColor = MidnightBackground,
                        unfocusedContainerColor = MidnightBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { categoryExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBackground),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SurfaceLine)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category: $category", color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, "Select Category", tint = TextMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(DeepMidnightCard)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TextPrimary) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method dropdown selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { paymentExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBackground),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SurfaceLine)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment: $paymentMethod", color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, "Select Payment", tint = TextMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false },
                        modifier = Modifier.background(DeepMidnightCard)
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method, color = TextPrimary) },
                                onClick = {
                                    paymentMethod = method
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker row
                Button(
                    onClick = {
                        val c = Calendar.getInstance()
                        c.timeInMillis = dateSelected
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val cal = Calendar.getInstance()
                                cal.set(y, m, d)
                                dateSelected = cal.timeInMillis
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBackground),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SurfaceLine)
                ) {
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateSelected))
                    Text("Date Selected: $formattedDate", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0) {
                                Toast.makeText(context, "Completed amount must be a positive value.", Toast.LENGTH_SHORT).show()
                            } else if (description.trim().isEmpty()) {
                                Toast.makeText(context, "Completed description is required.", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(parsedAmount, category, dateSelected, paymentMethod, description, selectedType)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save record", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

// Helper start of current month
fun getStartOfCurrentMonth(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
