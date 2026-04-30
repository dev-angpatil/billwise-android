package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.repository.TransactionRepository
import com.billwise.app.domain.usecase.AnalyzeSpendingUseCase
import com.billwise.app.domain.usecase.GenerateInsightsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightViewModel(
    private val transactionRepository: TransactionRepository,
    private val analyzeSpendingUseCase: AnalyzeSpendingUseCase,
    private val generateInsightsUseCase: GenerateInsightsUseCase
) : ViewModel() {

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBreakdown: StateFlow<Map<String, Double>> = _categoryBreakdown.asStateFlow()

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { list ->
                _totalSpent.value = analyzeSpendingUseCase.getTotalSpent(list)
                _categoryBreakdown.value = analyzeSpendingUseCase.getCategoryBreakdown(list)
                _insights.value = generateInsightsUseCase.getInsights(list)
            }
        }
    }
}
