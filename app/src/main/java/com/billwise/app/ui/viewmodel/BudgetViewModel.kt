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

    private val _budget = MutableStateFlow<Budget?>(null)
    val budget: StateFlow<Budget?> = _budget.asStateFlow()

    init {
        viewModelScope.launch {
            budgetRepository.getBudget().collect { _budget.value = it }
        }
    }

    fun saveBudget(amountText: String) {
        val amount = amountText.toDoubleOrNull() ?: return
        val cal = Calendar.getInstance()
        viewModelScope.launch {
            budgetRepository.saveBudget(
                Budget(
                    monthlyLimit = amount,
                    month = cal.get(Calendar.MONTH) + 1, // 1-indexed
                    year = cal.get(Calendar.YEAR)
                )
            )
        }
    }
}
