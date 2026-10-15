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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkshopPart
import com.example.ui.components.BinLocationBadge
import com.example.ui.components.CoreEligibleBadge
import com.example.ui.components.GreasyTouchQuantityAdjuster
import com.example.ui.components.LowStockBadge
import com.example.ui.components.OutOfStockBadge
import com.example.ui.components.SecondaryOutlinedButton
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockBorder
import com.example.ui.theme.LowStockText
import com.example.ui.theme.OutOfStockBg
import com.example.ui.theme.OutOfStockText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary

@Composable
fun StockScreen(
    parts: List<WorkshopPart>,
    onAdjustQuantity: (String, Int) -> Unit,
    onConvertPack: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var packConversionPart by remember { mutableStateOf<WorkshopPart?>(null) }

    val categories = listOf("All", "Fluids", "Filters", "Ignition", "Brakes", "Electrical", "Cooling / HVAC", "Engine Timing")

    val filteredParts = parts.filter { part ->
        val matchesCategory = selectedCategory == "All" || part.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                part.name.contains(searchQuery, ignoreCase = true) ||
                part.sku.contains(searchQuery, ignoreCase = true) ||
                part.oemCrossReference.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val lowStockCount = parts.count { it.isLowStock }
    val outOfStockCount = parts.count { it.isOutOfStock }
    val totalStockUnits = parts.sumOf { it.qty }
    val totalInventoryValue = parts.sumOf { it.qty * it.costPrice }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Inventory Summary Metrics Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, WorkshopBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Stock", fontSize = 11.sp, color = WorkshopTextSecondary)
                    Text("$totalStockUnits units", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(WorkshopBorder))
                Column {
                    Text("Inventory Value", fontSize = 11.sp, color = WorkshopTextSecondary)
                    Text("$${"%.0f".format(totalInventoryValue)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(WorkshopBorder))
                Column {
                    Text("Stock Alerts", fontSize = 11.sp, color = WorkshopTextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lowStockCount > 0) {
                            Text("$lowStockCount Low", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LowStockText)
                        }
                        if (outOfStockCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("• $outOfStockCount Out", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OutOfStockText)
                        }
                    }
                }
            }
        }

        // PREDICTIVE REORDER ENGINE (USP Requirement)
        item {
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Predictive Reorder Forecast",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkshopBrandNavy
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("AI Velocity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "3 active Job Cards require 5W-30 Oil & Spark Plugs. At current velocity, Castrol 5W-30 will reach 0 in ~18 hours. Recommended: Reorder 24L bulk pack today.",
                            fontSize = 12.sp,
                            color = WorkshopTextPrimary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, WorkshopBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    // 1-tap reorder simulation
                                    val oilPart = parts.find { it.sku == "OIL-5W30-SYN" }
                                    if (oilPart != null) onConvertPack(oilPart.id, 2)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Quick Reorder: Auto-receive 2x Box (24 Units) to Stock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkshopBrandNavy
                            )
                        }
                    }
                }
            }
        }

        // Search Bar & Category Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by SKU, Part Name, or OEM...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = WorkshopTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) WorkshopBrandNavy else Color.White)
                                .border(1.dp, if (isSelected) WorkshopBrandNavy else WorkshopBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else WorkshopTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Parts Inventory List
        if (filteredParts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No parts match \"$searchQuery\"." else "No parts found in this category.",
                        color = WorkshopTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredParts, key = { it.id }) { part ->
                PartInventoryItem(
                    part = part,
                    onDecrease = {
                        if (part.qty > 0) {
                            onAdjustQuantity(part.id, -1)
                        }
                    },
                    onIncrease = { onAdjustQuantity(part.id, +1) },
                    onUnpack = { packConversionPart = part }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Modal: Multi-Unit & Pack Conversion
    if (packConversionPart != null) {
        PackConversionDialog(
            part = packConversionPart!!,
            onDismiss = { packConversionPart = null },
            onConfirm = { packs ->
                onConvertPack(packConversionPart!!.id, packs)
                packConversionPart = null
            }
        )
    }
}

@Composable
fun PartInventoryItem(
    part: WorkshopPart,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onUnpack: () -> Unit
) {
    WorkshopCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = part.sku,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${part.category}",
                        fontSize = 11.sp,
                        color = WorkshopTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = part.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // OEM Cross Reference
                Text(
                    text = "OEM Ref: ${part.oemCrossReference}",
                    fontSize = 11.sp,
                    color = WorkshopTextSecondary,
                    lineHeight = 15.sp
                )
            }

            // Status badges
            Column(horizontalAlignment = Alignment.End) {
                when {
                    part.isOutOfStock -> OutOfStockBadge()
                    part.isLowStock -> LowStockBadge(label = "Low: ${part.qty} left")
                }
                if (part.coreEligible) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CoreEligibleBadge(deposit = part.coreDeposit)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bin & Aisle Warehouse Location Chip
        BinLocationBadge(location = part.binLocation)

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = WorkshopBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Controls: Pricing + Greasy Hands +/- adjuster + Pack Conversion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$${"%.2f".format(part.unitPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopBrandNavy
                )
                Text(
                    text = "Cost: $${"%.2f".format(part.costPrice)} (${"%.0f".format(part.marginPercent)}% margin)",
                    fontSize = 11.sp,
                    color = WorkshopTextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bulk Pack Conversion Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkshopActivePill)
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                        .clickable { onUnpack() }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inbox, null, tint = WorkshopBrandNavy, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unpack", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                    }
                }

                // Greasy Touch +/- buttons
                GreasyTouchQuantityAdjuster(
                    currentQty = part.qty,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease
                )
            }
        }
    }
}

@Composable
fun PackConversionDialog(
    part: WorkshopPart,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var packsReceived by remember { mutableStateOf(1) }
    val resultingUnits = packsReceived * part.packRatio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Bulk Pack Conversion", fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                Text("Unpack & Convert to Individual Units", fontSize = 12.sp, color = WorkshopTextSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Receive master packs of ${part.name} and automatically expand into single inventory units.",
                    fontSize = 13.sp,
                    color = WorkshopTextPrimary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkshopCanvasBg)
                        .border(1.dp, WorkshopBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Pack Specification: 1 ${part.packUnitName} = ${part.packRatio} Individual Units", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WorkshopBrandNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Current balance in warehouse: ${part.qty} units", fontSize = 11.sp, color = WorkshopTextSecondary)
                    }
                }

                Text("How many ${part.packUnitName}s received?", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GreasyTouchQuantityAdjuster(
                        currentQty = packsReceived,
                        onDecrease = { if (packsReceived > 1) packsReceived-- },
                        onIncrease = { packsReceived++ }
                    )
                }

                Text(
                    text = "= Adding +$resultingUnits units to stock (New total: ${part.qty + resultingUnits})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(packsReceived) },
                colors = ButtonDefaults.buttonColors(containerColor = WorkshopBrandNavy)
            ) {
                Text("Confirm & Unpack")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorkshopTextSecondary)
            }
        }
    )
}
