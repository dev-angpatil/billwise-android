package com.billwise.app.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billwise.app.ui.viewmodel.InsightViewModel

@Composable
fun InsightsScreen(viewModel: InsightViewModel) {
    val insights by viewModel.insights.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Smart Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (insights.isEmpty()) {
            Text("No insights generated yet.")
        } else {
            LazyColumn {
                items(insights) { insight ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Insight", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(insight)
                        }
                    }
                }
            }
        }
    }
}
