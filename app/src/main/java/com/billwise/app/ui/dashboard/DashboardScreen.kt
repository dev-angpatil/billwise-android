package com.billwise.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billwise.app.ui.viewmodel.InsightViewModel

@Composable
fun DashboardScreen(viewModel: InsightViewModel) {
    val totalSpent by viewModel.totalSpent.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Spent This Month")
                Text("Rs. ${String.format("%.2f", totalSpent)}", style = MaterialTheme.typography.headlineLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(categoryBreakdown.size) { index ->
                val entry = categoryBreakdown.entries.elementAt(index)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.key)
                    Text("Rs. ${String.format("%.2f", entry.value)}")
                }
            }
        }
    }
}
