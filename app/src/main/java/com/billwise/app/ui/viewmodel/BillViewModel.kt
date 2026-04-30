package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.data.parser.BillParser
import com.billwise.app.domain.model.Bill
import com.billwise.app.domain.repository.BillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillViewModel(
    private val billRepository: BillRepository,
    private val onBillParsedToTransaction: (com.billwise.app.domain.model.Transaction) -> Unit
) : ViewModel() {

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills.asStateFlow()

    init {
        loadBills()
    }

    private fun loadBills() {
        viewModelScope.launch {
            billRepository.getAllBills().collect { list ->
                _bills.value = list
            }
        }
    }

    fun uploadBill(rawData: String) {
        viewModelScope.launch {
            val bill = BillParser.parse(rawData)
            billRepository.addBill(bill)
            val transaction = BillParser.toTransaction(bill)
            onBillParsedToTransaction(transaction)
        }
    }

    fun addManualTransaction(
        amount: Double,
        merchant: String,
        category: String,
        type: com.billwise.app.domain.model.TransactionType
    ) {
        val transaction = com.billwise.app.domain.model.Transaction(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount,
            merchant = merchant,
            datetime = System.currentTimeMillis(),
            type = type,
            category = category,
            source = com.billwise.app.domain.model.TransactionSource.MANUAL
        )
        onBillParsedToTransaction(transaction)
    }
}
