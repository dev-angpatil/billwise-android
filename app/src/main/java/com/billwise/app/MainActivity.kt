package com.billwise.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.room.Room
import com.billwise.app.data.local.AppDatabase
import com.billwise.app.data.local.SmsReader
import com.billwise.app.data.repository.BillRepositoryImpl
import com.billwise.app.data.repository.BudgetRepositoryImpl
import com.billwise.app.data.repository.TransactionRepositoryImpl
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            syncSms()
        } else {
            Toast.makeText(this, "SMS Permission is required for auto-tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual DI
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "billwise-db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        val transactionRepo = TransactionRepositoryImpl(db.transactionDao())
        val billRepo        = BillRepositoryImpl(db.billDao())
        val budgetRepo      = BudgetRepositoryImpl(db.budgetDao())

        val categorizeUseCase    = CategorizeTransactionUseCase()
        val deduplicateUseCase   = DeduplicateTransactionUseCase()
        val analyzeUseCase       = AnalyzeSpendingUseCase()
        val generateInsightsUseCase = GenerateInsightsUseCase()

        transactionViewModel = TransactionViewModel(transactionRepo, categorizeUseCase, deduplicateUseCase)
        insightViewModel     = InsightViewModel(transactionRepo, budgetRepo, analyzeUseCase, generateInsightsUseCase)
        budgetViewModel      = BudgetViewModel(budgetRepo)
        billViewModel        = BillViewModel(billRepo) { transaction ->
            transactionViewModel.addTransaction(transaction)
        }

        smsReader = SmsReader(this)

        // Check Permissions
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED -> {
                syncSms()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }

        setContent {
            BillWiseTheme {
                MainScreen(transactionViewModel, insightViewModel, billViewModel, budgetViewModel)
            }
        }
    }

    private fun syncSms() {
        lifecycleScope.launch {
            val transactions = smsReader.readSmsMessages()
            transactions.forEach {
                transactionViewModel.addTransaction(it)
            }
        }
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

