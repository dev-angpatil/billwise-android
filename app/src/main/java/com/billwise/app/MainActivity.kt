package com.billwise.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.billwise.app.data.local.AppDatabase
import com.billwise.app.data.local.SmsReader
import com.billwise.app.data.repository.BudgetRepositoryImpl
import com.billwise.app.domain.usecase.AnalyzeSpendingUseCase
import com.billwise.app.domain.usecase.CategorizeTransactionUseCase
import com.billwise.app.domain.usecase.DeduplicateTransactionUseCase
import com.billwise.app.domain.usecase.GenerateInsightsUseCase
import com.billwise.app.ui.budget.BudgetSettingsScreen
import com.billwise.app.ui.dashboard.DashboardScreen
import com.billwise.app.ui.insights.InsightsScreen
import com.billwise.app.ui.theme.BillWiseTheme
import com.billwise.app.ui.transactions.TransactionsScreen
import com.billwise.app.ui.upload.UploadScreen
import com.billwise.app.ui.viewmodel.BillViewModel
import com.billwise.app.ui.viewmodel.BudgetViewModel
import com.billwise.app.ui.viewmodel.InsightViewModel
import com.billwise.app.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var insightViewModel: InsightViewModel
    private lateinit var billViewModel: BillViewModel
    private lateinit var budgetViewModel: BudgetViewModel
    private lateinit var smsReader: SmsReader

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            syncSms()
        } else {
            Toast.makeText(this, "SMS Permission is required for auto-tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as BillWiseApplication
        db = app.db
        val transactionRepo = app.transactionRepository
        val budgetRepo      = BudgetRepositoryImpl(db.budgetDao())

        val categorizeUseCase    = CategorizeTransactionUseCase()
        val deduplicateUseCase   = DeduplicateTransactionUseCase()
        val analyzeUseCase       = AnalyzeSpendingUseCase()
        val generateInsightsUseCase = GenerateInsightsUseCase()
        
        val notificationHelper = com.billwise.app.core.NotificationHelper(this)
        val budgetCheckUseCase = com.billwise.app.domain.usecase.BudgetCheckUseCase(
            db.budgetDao(),
            db.transactionDao(),
            notificationHelper
        )
        val detectHighTransactionUseCase = com.billwise.app.domain.usecase.DetectHighTransactionUseCase()

        transactionViewModel = TransactionViewModel(
            transactionRepo,
            categorizeUseCase,
            deduplicateUseCase,
            budgetCheckUseCase,
            detectHighTransactionUseCase
        )
        insightViewModel     = InsightViewModel(transactionRepo, budgetRepo, analyzeUseCase, generateInsightsUseCase)
        budgetViewModel      = BudgetViewModel(budgetRepo)
        
        // BillViewModel now takes a direct callback to TransactionViewModel
        billViewModel = BillViewModel { transaction ->
            transactionViewModel.addTransaction(transaction)
        }

        smsReader = SmsReader(this)

        // Check Permissions
        val smsReadGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val smsReceiveGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        
        if (smsReadGranted && smsReceiveGranted) {
            syncSms()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }

        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Please enable Notification Access to track PhonePe/GPay", Toast.LENGTH_LONG).show()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        setContent {
            BillWiseTheme {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    transactionViewModel.purgeInvalidTransactions(smsReader)
                    syncSms()
                }
                MainScreen(transactionViewModel, insightViewModel, billViewModel, budgetViewModel)
            }
        }
    }

    private fun syncSms() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("billwise_prefs", MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sms_sync", 0L)
            
            val transactions = smsReader.readSmsMessages(sinceTimestamp = lastSync)
            if (transactions.isNotEmpty()) {
                transactions.forEach { transactionViewModel.addTransaction(it) }
                
                // Update last sync to the newest message's date
                val newestDate = transactions.maxOf { it.datetime }
                prefs.edit().putLong("last_sms_sync", newestDate).apply()
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
}

@Composable
fun MainScreen(
    transactionViewModel: TransactionViewModel,
    insightViewModel: InsightViewModel,
    billViewModel: BillViewModel,
    budgetViewModel: BudgetViewModel
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Upload,
        Screen.Insights,
        Screen.Budget
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route)     { DashboardScreen(insightViewModel) }
            composable(Screen.Transactions.route)  { TransactionsScreen(transactionViewModel) }
            composable(Screen.Upload.route)        { UploadScreen(billViewModel) }
            composable(Screen.Insights.route)      { InsightsScreen(insightViewModel) }
            composable(Screen.Budget.route)        { BudgetSettingsScreen(budgetViewModel, insightViewModel) }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard    : Screen("dashboard",    "Dashboard",     Icons.Filled.Dashboard)
    object Transactions : Screen("transactions", "Transactions",  Icons.Filled.List)
    object Upload       : Screen("upload",       "Upload",        Icons.Filled.Add)
    object Insights     : Screen("insights",     "Insights",      Icons.Filled.Insights)
    object Budget       : Screen("budget",       "Budget",        Icons.Filled.Savings)
}
