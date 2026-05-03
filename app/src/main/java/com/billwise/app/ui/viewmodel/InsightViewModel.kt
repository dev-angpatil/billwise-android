package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.model.Budget
import com.billwise.app.domain.repository.BudgetRepository
import com.billwise.app.domain.repository.TransactionRepository
import com.billwise.app.domain.usecase.AnalyzeSpendingUseCase
import com.billwise.app.domain.usecase.GenerateInsightsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class InsightViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val analyzeSpendingUseCase: AnalyzeSpendingUseCase,
    private val generateInsightsUseCase: GenerateInsightsUseCase,
) : ViewModel() {

    private val cal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(cal[Calendar.MONTH] + 1)
    private val _selectedYear  = MutableStateFlow(cal[Calendar.YEAR])
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()
    val selectedYear: StateFlow<Int>  = _selectedYear.asStateFlow()

    private val _totalSpent        = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent.asStateFlow()

    private val _totalIncome       = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBreakdown: StateFlow<Map<String, Double>> = _categoryBreakdown.asStateFlow()

    private val _dailySpend        = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val dailySpend: StateFlow<Map<Int, Double>> = _dailySpend.asStateFlow()

    private val _insights          = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    // New flows for advanced analytics
    private val _topMerchants      = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val topMerchants: StateFlow<List<Pair<String, Double>>> = _topMerchants.asStateFlow()

    private val _weeklyComparison  = MutableStateFlow(0.0 to 0.0)
    val weeklyComparison: StateFlow<Pair<Double, Double>> = _weeklyComparison.asStateFlow()

    private val _incomeVsExpenseTrend = MutableStateFlow<List<Triple<String, Double, Double>>>(emptyList())
    val incomeVsExpenseTrend: StateFlow<List<Triple<String, Double, Double>>> = _incomeVsExpenseTrend.asStateFlow()

    private val _budget            = MutableStateFlow<Budget?>(null)
    val budget: StateFlow<Budget?> = _budget.asStateFlow()

    private val _budgetProgress    = MutableStateFlow(0f)
    val budgetProgress: StateFlow<Float> = _budgetProgress.asStateFlow()

    private val _averageDailySpend = MutableStateFlow(0.0)
    val averageDailySpend: StateFlow<Double> = _averageDailySpend.asStateFlow()

    private val _dailyBudget = MutableStateFlow(0.0)
    val dailyBudget: StateFlow<Double> = _dailyBudget.asStateFlow()

    private val _budgets           = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _overLimitCategories = MutableStateFlow<List<String>>(emptyList())
    val overLimitCategories: StateFlow<List<String>> = _overLimitCategories.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_selectedMonth, _selectedYear) { m, y -> m to y }
                .flatMapLatest { (m, y) ->
                    combine(
                        transactionRepository.getAllTransactions(),
                        budgetRepository.getBudgetsForMonth(m, y)
                    ) { transactions, budgetList ->
                        val spent = analyzeSpendingUseCase.getTotalSpentInMonth(transactions, m, y)
                        val breakdown = analyzeSpendingUseCase.getCategoryBreakdownForMonth(transactions, m, y)
                        
                        _totalSpent.value        = spent
                        _totalIncome.value       = analyzeSpendingUseCase.getTotalIncomeInMonth(transactions, m, y)
                        _categoryBreakdown.value = breakdown
                        _dailySpend.value        = analyzeSpendingUseCase.getDailySpendForMonth(transactions, m, y)
                        _topMerchants.value      = analyzeSpendingUseCase.getTopMerchants(transactions, m, y)
                        _weeklyComparison.value  = analyzeSpendingUseCase.getWeeklyComparison(transactions)
                        _incomeVsExpenseTrend.value = analyzeSpendingUseCase.getIncomeVsExpenseTrend(transactions)
                        _averageDailySpend.value = analyzeSpendingUseCase.getAverageDailySpend(transactions, m, y)

                        _budgets.value = budgetList
                        val totalBudget = budgetList.find { it.category == "Total" }
                        _budget.value = totalBudget
                        
                        _budgetProgress.value = if (totalBudget != null && (totalBudget.monthlyLimit > 0)) {
                            (spent / totalBudget.monthlyLimit).toFloat().coerceIn(0f, 1.2f)
                        } else 0f

                        _dailyBudget.value = if (totalBudget != null) analyzeSpendingUseCase.getDailyBudget(totalBudget.monthlyLimit, m, y) else 0.0

                        val overLimit = mutableListOf<String>()
                        breakdown.forEach { (cat, catSpent) ->
                            val b = budgetList.find { it.category.equals(cat, ignoreCase = true) }
                            if (b != null && catSpent > b.monthlyLimit) {
                                overLimit.add(cat)
                            }
                        }
                        _overLimitCategories.value = overLimit
                        _insights.value = generateInsightsUseCase.getInsights(transactions, totalBudget?.monthlyLimit, m, y)
                    }
                }.collect {}
        }
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        val now = Calendar.getInstance()
        val isCurrentMonth = _selectedMonth.value == now.get(Calendar.MONTH) + 1 &&
            _selectedYear.value == now.get(Calendar.YEAR)
        if (isCurrentMonth) return
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun generateAiAdvice() {
        val transactions = transactionRepository.getAllTransactions()
        viewModelScope.launch {
            transactions.collect { txList ->
                val context = generateInsightsUseCase.formatTransactionsForAi(txList)
                if (context.isBlank()) {
                    _aiAdvice.value = "Add more transactions for a detailed AI analysis!"
                    return@collect
                }
                
                _aiAdvice.value = "Analyzing your habits..."
                
                try {
                    val apiKey = com.billwise.app.core.Config.OPENAI_API_KEY
                    val response = com.billwise.app.data.remote.RetrofitClient.openAiApi.categorizeTransaction(
                        "Bearer $apiKey",
                        com.billwise.app.data.remote.OpenAiRequest(
                            messages = listOf(
                                com.billwise.app.data.remote.Message(
                                    role = "system",
                                    content = "You are BillWise AI, a friendly Indian financial coach. Analyze the user's spending habits (amounts in INR). Provide 3 concise, actionable tips. Be supportive but firm on overspending. Keep total response under 100 words."
                                ),
                                com.billwise.app.data.remote.Message(role = "user", content = "Here are my recent transactions:\n$context")
                            )
                        )
                    )
                    _aiAdvice.value = response.choices.firstOrNull()?.message?.content ?: "Could not generate advice right now."
                } catch (e: Exception) {
                    _aiAdvice.value = "Error connecting to AI: ${e.message}"
                }
            }
        }
    }

}
