package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.repository.TransactionRepository
import com.billwise.app.domain.usecase.CategorizeTransactionUseCase
import com.billwise.app.domain.usecase.DeduplicateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase,
    private val deduplicateTransactionUseCase: DeduplicateTransactionUseCase
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { list ->
                _transactions.value = list
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val categorized = categorizeTransactionUseCase(transaction)
            val isDuplicate = deduplicateTransactionUseCase(categorized, _transactions.value)
            
            if (!isDuplicate) {
                transactionRepository.addTransaction(categorized)
            }
        }
    }
}
