package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.data.model.CoreReturn
import com.example.ui.components.PrimaryNavyButton
import com.example.ui.components.SuccessBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.CoreBadgeBg
import com.example.ui.theme.CoreBadgeBorder
import com.example.ui.theme.CoreBadgeText
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockBorder
import com.example.ui.theme.LowStockText
import com.example.ui.theme.SuccessBadgeBg
import com.example.ui.theme.SuccessBadgeBorder
import com.example.ui.theme.SuccessBadgeText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary

@Composable
fun CoreReturnsScreen(
    coreReturns: List<CoreReturn>,
    onAdvanceStatus: (String) -> Unit,
    onAddCore: (String, String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalPendingDeposit = coreReturns
        .filter { it.status != "Returned to Supplier" }
        .sumOf { it.depositValue }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header & Total Deposit Banner
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unclaimed Core Deposits",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkshopBrandNavy
                            )
                        }
                        Text(
                            text = "Deposit money owed back to shop from parts suppliers",
                            fontSize = 11.sp,
                            color = WorkshopTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCFCE7))
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$${"%.2f".format(totalPendingDeposit)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Core Lifecycle Stage Tracker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkshopCanvasBg)
                        .border(1.dp, WorkshopBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StageIndicator("1. Car Removal", Icons.Default.DirectionsCar)
                    Icon(Icons.Default.ArrowForward, null, tint = WorkshopTextSecondary, modifier = Modifier.size(14.dp))
                    StageIndicator("2. Warehouse Storage", Icons.Default.Inventory)
                    Icon(Icons.Default.ArrowForward, null, tint = WorkshopTextSecondary, modifier = Modifier.size(14.dp))
                    StageIndicator("3. Supplier Credit", Icons.Default.LocalShipping)
                }

                Spacer(modifier = Modifier.height(12.dp))

                PrimaryNavyButton(
                    text = "+ Log Swapped Core Deposit",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(coreReturns) { core ->
            CoreReturnItem(
                core = core,
                onAdvance = { onAdvanceStatus(core.coreId) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        AddCoreReturnDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { part, plate, supplier, deposit ->
                onAddCore(part, plate, supplier, deposit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StageIndicator(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WorkshopBrandNavy, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = WorkshopTextPrimary)
    }
}

@Composable
fun CoreReturnItem(
    core: CoreReturn,
    onAdvance: () -> Unit
) {
    WorkshopCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(core.coreId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WorkshopTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(core.customerPlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(core.originalPart, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WorkshopTextPrimary)
                Text("Supplier: ${core.supplierName}", fontSize = 11.sp, color = WorkshopTextSecondary)
            }

            // Deposit Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoreBadgeBg)
                    .border(1.dp, CoreBadgeBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$${"%.2f".format(core.depositValue)} Credit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoreBadgeText
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = WorkshopBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Tag
            when (core.status) {
                "Removed from Car" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LowStockBg)
                            .border(1.dp, LowStockBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Stage 1: Removed from Car", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LowStockText)
                    }
                }
                "Warehouse Storage" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WorkshopActivePill)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Stage 2: Warehouse Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                    }
                }
                else -> {
                    SuccessBadge("Stage 3: Returned & Claimed")
                }
            }

            if (core.status != "Returned to Supplier") {
                Button(
                    onClick = onAdvance,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopBrandNavy),
                    modifier = Modifier.testTag("advance_core_btn")
                ) {
                    Text(
                        text = if (core.status == "Removed from Car") "Move to Warehouse" else "Claim Credit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Credit Applied", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
            }
        }
    }
}

@Composable
fun AddCoreReturnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double) -> Unit
) {
    var part by remember { mutableStateOf("Denso AC Compressor (Sanden Pattern)") }
    var plate by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("LKQ Auto Components") }
    var depositStr by remember { mutableStateOf("80.00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Core Deposit Return", fontWeight = FontWeight.Bold, color = WorkshopBrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = part,
                    onValueChange = { part = it },
                    label = { Text("Core Part Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Vehicle License Plate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = depositStr,
                    onValueChange = { depositStr = it },
                    label = { Text("Deposit Value ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deposit = depositStr.toDoubleOrNull() ?: 50.0
                    if (plate.isNotBlank()) {
                        onConfirm(part, plate, supplier, deposit)
                    }
                },
                enabled = plate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WorkshopBrandNavy)
            ) {
                Text("Log Core")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorkshopTextSecondary)
            }
        }
    )
}
