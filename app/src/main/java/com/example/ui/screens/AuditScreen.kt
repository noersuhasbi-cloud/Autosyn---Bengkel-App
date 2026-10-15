package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntry
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.OutOfStockBg
import com.example.ui.theme.OutOfStockText
import com.example.ui.theme.SuccessBadgeBg
import com.example.ui.theme.SuccessBadgeText
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary

@Composable
fun AuditScreen(
    auditLogs: List<AuditLogEntry>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Job Deductions", "Stock Adjustments", "Pack Receipts", "Voice Logs")

    val filteredLogs = auditLogs.filter { log ->
        when (selectedFilter) {
            "Job Deductions" -> log.actionType == "JOB_ASSIGNED"
            "Stock Adjustments" -> log.actionType.startsWith("STOCK_")
            "Pack Receipts" -> log.actionType == "PACK_CONVERTED"
            "Voice Logs" -> log.actionType == "VOICE_CONFIRMED"
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Audit Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = WorkshopBrandNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stock Movement Audit Trail",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkshopBrandNavy
                        )
                    }
                    Text(
                        text = "Immutable ledger of all warehouse adjustments & mechanic allocations",
                        fontSize = 11.sp,
                        color = WorkshopTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WorkshopBrandNavy else Color.White)
                            .border(1.dp, if (isSelected) WorkshopBrandNavy else WorkshopBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else WorkshopTextPrimary
                        )
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No log entries match the selected filter.", color = WorkshopTextSecondary)
                }
            }
        } else {
            items(filteredLogs) { log ->
                AuditLogItem(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLogEntry) {
    WorkshopCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.timestamp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.partSku,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopBrandNavy
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.partName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.reason,
                    fontSize = 12.sp,
                    color = WorkshopTextSecondary
                )
            }

            // Delta Badge (+ or -)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (log.quantityDelta >= 0) SuccessBadgeBg else OutOfStockBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (log.quantityDelta >= 0) "+${log.quantityDelta}" else "${log.quantityDelta}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (log.quantityDelta >= 0) SuccessBadgeText else OutOfStockText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = WorkshopBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = WorkshopTextSecondary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(log.mechanicName, fontSize = 11.sp, color = WorkshopTextSecondary)
            }

            Text(
                text = "Warehouse Balance After: ${log.balanceAfter} units",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkshopBrandNavy
            )
        }
    }
}
