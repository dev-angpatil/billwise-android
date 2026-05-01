package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.model.Budget
import com.billwise.app.domain.repository.BudgetRepository
import com.billwise.app.domain.repository.TransactionRepository
import com.billwise.app.domain.usecase.AnalyzeSpendingUseCase
import com.billwise.app.domain.usecase.GenerateInsightsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class InsightViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val analyzeSpendingUseCase: AnalyzeSpendingUseCase,
    private val generateInsightsUseCase: GenerateInsightsUseCase
) : ViewModel() {

    private val cal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(cal.get(Calendar.MONTH) + 1) // 1-12
    private val _selectedYear  = MutableStateFlow(cal.get(Calendar.YEAR))
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

    private val _monthlyTrend      = MutableStateFlow<Map<String, Double>>(emptyMap())
    val monthlyTrend: StateFlow<Map<String, Double>> = _monthlyTrend.asStateFlow()

    private val _budget            = MutableStateFlow<Budget?>(null)
    val budget: StateFlow<Budget?> = _budget.asStateFlow()

    /** 0.0 – 1.0 fraction of budget consumed this month */
    private val _budgetProgress    = MutableStateFlow(0f)
    val budgetProgress: StateFlow<Float> = _budgetProgress.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                budgetRepository.getBudget(),
                _selectedMonth,
                _selectedYear
            ) { transactions, budget, month, year ->
                val spent  = analyzeSpendingUseCase.getTotalSpentInMonth(transactions, month, year)
                val income = analyzeSpendingUseCase.getTotalIncomeInMonth(transactions, month, year)

                _totalSpent.value        = spent
                _totalIncome.value       = income
                _categoryBreakdown.value = analyzeSpendingUseCase.getCategoryBreakdownForMonth(transactions, month, year)
                _dailySpend.value        = analyzeSpendingUseCase.getDailySpendForMonth(transactions, month, year)
                _monthlyTrend.value      = analyzeSpendingUseCase.getMonthlySpendTrend(transactions)
                _insights.value          = generateInsightsUseCase.getInsights(transactions, budget?.monthlyLimit)
                _budget.value            = budget

                _budgetProgress.value = if (budget != null && budget.monthlyLimit > 0) {
                    (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f)
                } else 0f
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
        if (isCurrentMonth) return // don't go into the future
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }
}

