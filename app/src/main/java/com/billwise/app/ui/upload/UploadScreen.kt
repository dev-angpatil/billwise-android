package com.billwise.app.ui.upload

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billwise.app.ui.viewmodel.BillViewModel

@Composable
fun UploadScreen(viewModel: BillViewModel) {
    var billText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Upload Bill", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Paste bill text here (Mock OCR):")
        OutlinedTextField(
            value = billText,
            onValueChange = { billText = it },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("Example:\nStarbucks\nCoffee - 250\nTotal - 250") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (billText.isNotBlank()) {
                    viewModel.uploadBill(billText)
                    billText = ""
                    statusMessage = "Bill parsed and added successfully!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Process Bill")
        }
        
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}
