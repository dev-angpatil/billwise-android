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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase,
    private val deduplicateTransactionUseCase: DeduplicateTransactionUseCase
) : ViewModel() {

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    init {
        loadTransactions()
        viewModelScope.launch {
            combine(_allTransactions, _searchQuery, _selectedCategory) { txns, query, cat ->
                txns.filter { tx ->
                    val matchesQuery = query.isBlank() || tx.merchant.contains(query, ignoreCase = true)
                    val matchesCategory = cat == "All" || tx.category == cat
                    matchesQuery && matchesCategory
                }
            }.collect { _filteredTransactions.value = it }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { list ->
                _allTransactions.value = list
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val categorized = categorizeTransactionUseCase(transaction)
            val isDuplicate = deduplicateTransactionUseCase(categorized, _allTransactions.value)
            if (!isDuplicate) {
                transactionRepository.addTransaction(categorized)
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
}

