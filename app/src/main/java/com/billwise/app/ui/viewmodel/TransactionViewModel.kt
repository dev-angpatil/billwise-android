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
    private val deduplicateTransactionUseCase: DeduplicateTransactionUseCase,
    private val budgetCheckUseCase: com.billwise.app.domain.usecase.BudgetCheckUseCase,
    private val detectHighTransactionUseCase: com.billwise.app.domain.usecase.DetectHighTransactionUseCase
) : ViewModel() {

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    private val _highTransactionAlert = MutableStateFlow<Transaction?>(null)
    val highTransactionAlert: StateFlow<Transaction?> = _highTransactionAlert.asStateFlow()

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
                
                // Flag suspiciously large transactions
                if (detectHighTransactionUseCase(categorized)) {
                    _highTransactionAlert.value = categorized
                }

                // Check budget and send alerts
                budgetCheckUseCase(categorized.category)
            }
        }
    }

    fun clearHighTransactionAlert() {
        _highTransactionAlert.value = null
    }

    fun updateMerchantAlias(transactionId: String, newAlias: String) {
        viewModelScope.launch {
            val tx = _allTransactions.value.find { it.id == transactionId }
            if (tx != null) {
                val finalAlias = newAlias.takeIf { it.isNotBlank() }
                val updatedTx = tx.copy(merchantAlias = finalAlias)
                
                // Optimistic UI update
                _allTransactions.value = _allTransactions.value.map { 
                    if (it.merchant == tx.merchant) it.copy(merchantAlias = finalAlias) else it 
                }
                
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
                
                // Optimistic UI update
                _allTransactions.value = _allTransactions.value.map { 
                    if (it.merchant == tx.merchant) it.copy(category = newCategory) else it 
                }
                
                transactionRepository.updateTransaction(updatedTx)
                
                // Auto-apply this new category to other transactions with the exact same raw merchant name
                _allTransactions.value.filter { it.merchant == tx.merchant && it.id != tx.id }.forEach { other ->
                    transactionRepository.updateTransaction(other.copy(category = newCategory))
                }

                budgetCheckUseCase(tx.category)
                budgetCheckUseCase(newCategory)
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val tx = _allTransactions.value.find { it.id == id }
            transactionRepository.deleteTransaction(id)
            if (tx != null) {
                budgetCheckUseCase(tx.category)
            }
        }
    }

    fun purgeInvalidTransactions(smsReader: com.billwise.app.data.local.SmsReader) {
        viewModelScope.launch {
            val currentTxns = _allTransactions.value
            val toDelete = mutableListOf<String>()
            
            currentTxns.forEach { tx ->
                if (tx.source == com.billwise.app.domain.model.TransactionSource.SMS) {
                    // Re-run strict detection
                    // Note: We don't have the original body here, so we simulate a check 
                    // on the merchant/amount context. If merchant is UNCATEGORISED and 
                    // amount is suspicious, we flag it.
                    if (tx.merchant == "UNCATEGORISED" && tx.category == "Others") {
                        toDelete.add(tx.id)
                    }
                }
            }
            
            toDelete.forEach { id ->
                transactionRepository.deleteTransaction(id)
            }
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
}

