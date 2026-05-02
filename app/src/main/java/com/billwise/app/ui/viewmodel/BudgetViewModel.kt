package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.model.Budget
import com.billwise.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    init {
        val cal = Calendar.getInstance()
        viewModelScope.launch {
            budgetRepository.getBudgetsForMonth(
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            ).collect { _budgets.value = it }
        }
    }

    fun saveBudget(category: String, amountText: String) {
        val amount = amountText.toDoubleOrNull() ?: return
        val cal = Calendar.getInstance()
        viewModelScope.launch {
            val existing = budgetRepository.getBudgetByCategory(
                category,
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
            
            budgetRepository.saveBudget(
                Budget(
                    id = existing?.id ?: 0,
                    category = category,
                    monthlyLimit = amount,
                    month = cal.get(Calendar.MONTH) + 1,
                    year = cal.get(Calendar.YEAR),
                    hasNotified75 = existing?.hasNotified75 ?: false,
                    hasNotified100 = existing?.hasNotified100 ?: false
                )
            )
        }
    }
}
