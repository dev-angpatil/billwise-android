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
                    val matchesQuery = query.isBlank() || 
                                       tx.merchant.contains(query, ignoreCase = true) ||
                                       (tx.merchantAlias?.contains(query, ignoreCase = true) == true)
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
            val categorized = categorizeTransactionUseCase(transaction, _allTransactions.value)
            val isDuplicate = deduplicateTransactionUseCase(categorized, _allTransactions.value)
            if (!isDuplicate) {
                transactionRepository.addTransaction(categorized)
            }
        }
    }

    fun updateMerchantAlias(transactionId: String, newAlias: String) {
        viewModelScope.launch {
            val tx = _allTransactions.value.find { it.id == transactionId }
            if (tx != null) {
                val finalAlias = newAlias.takeIf { it.isNotBlank() }
                val updatedTx = tx.copy(merchantAlias = finalAlias)
                transactionRepository.updateTransaction(updatedTx)
                
                // Auto-apply to other transactions with the exact same raw merchant name
                _allTransactions.value.filter { it.merchant == tx.merchant && it.id != tx.id }.forEach { other ->
                    transactionRepository.updateTransaction(other.copy(merchantAlias = finalAlias))
                }
            }
        }
    }

    fun updateTransactionCategory(transactionId: String, newCategory: String) {
        viewModelScope.launch {
            val tx = _allTransactions.value.find { it.id == transactionId }
            if (tx != null) {
                val updatedTx = tx.copy(category = newCategory)
                transactionRepository.updateTransaction(updatedTx)
                
                // Auto-apply this new category to other transactions with the exact same raw merchant name
                _allTransactions.value.filter { it.merchant == tx.merchant && it.id != tx.id }.forEach { other ->
                    transactionRepository.updateTransaction(other.copy(category = newCategory))
                }
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

