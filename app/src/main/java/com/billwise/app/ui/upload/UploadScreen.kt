package com.billwise.app.ui.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billwise.app.data.ocr.OcrProcessor
import com.billwise.app.ui.viewmodel.BillViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(viewModel: BillViewModel) {
    val tabs = listOf("📷 Scan", "📋 Paste", "✏️ Manual")
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Add Bill / Transaction",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> OcrScanTab(viewModel)
            1 -> PasteTextTab(viewModel)
            2 -> ManualTransactionForm(viewModel, onSuccess = { selectedTab = 0 })
        }
    }
}

@Composable
private fun OcrScanTab(viewModel: BillViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var extractedText by remember { mutableStateOf("") }
    var isProcessing  by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pickedUri     by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            isProcessing = true
            scope.launch {
                try {
                    extractedText = OcrProcessor.extractText(context, uri)
                    statusMessage = if (extractedText.isBlank()) "No text found in image." else null
                } catch (e: Exception) {
                    statusMessage = "OCR failed: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pick a bill photo from your gallery to extract text automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose from Gallery")
        }

        if (isProcessing) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (extractedText.isNotBlank()) {
            OutlinedTextField(
                value = extractedText,
                onValueChange = { extractedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Extracted Text (editable)") }
            )

            Button(
                onClick = {
                    viewModel.uploadBill(extractedText)
                    statusMessage = "✅ Bill parsed and added!"
                    extractedText = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Bill", fontWeight = FontWeight.SemiBold)
            }
        }

        statusMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.startsWith("✅"))
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.startsWith("✅"))
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PasteTextTab(viewModel: BillViewModel) {
    var billText      by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Paste your bill text below. BillWise will extract the merchant and total.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        OutlinedTextField(
            value = billText,
            onValueChange = { billText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = {
                Text("Example:\nStarbucks\nCappuccino  250\nTotal  250")
            }
        )

        Button(
            onClick = {
                if (billText.isNotBlank()) {
                    viewModel.uploadBill(billText)
                    billText = ""
                    statusMessage = "✅ Bill parsed and added!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Process Bill", fontWeight = FontWeight.SemiBold)
        }

        statusMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

