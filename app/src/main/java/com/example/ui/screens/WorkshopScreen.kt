package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.JobCard
import com.example.data.model.VoiceDraft
import com.example.data.model.WorkshopPart
import com.example.ui.components.GreasyTouchQuantityAdjuster
import com.example.ui.components.PrimaryNavyButton
import com.example.ui.components.SecondaryOutlinedButton
import com.example.ui.components.SuccessBadge
import com.example.ui.components.WorkshopCard
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
fun WorkshopScreen(
    jobCards: List<JobCard>,
    parts: List<WorkshopPart>,
    voiceDrafts: List<VoiceDraft>,
    coreReturns: List<CoreReturn>,
    onAssignPartToJob: (String, String, Int) -> Unit,
    onCompleteJob: (String) -> Unit,
    onCreateJob: (String, String, String, String) -> Unit,
    onAddVoiceTranscript: (String) -> Unit,
    onConfirmVoiceDraft: (String) -> Unit,
    onRejectVoiceDraft: (String) -> Unit,
    onAdvanceCoreStatus: (String) -> Unit,
    onAddCoreReturn: (String, String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("JOB_CARDS") } // "JOB_CARDS", "VOICE_STATION", "CORE_RETURNS"
    var showNewJobDialog by remember { mutableStateOf(false) }
    var selectedJobForPartDeduction by remember { mutableStateOf<JobCard?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Section Tabs: Modern Pill Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                Triple("JOB_CARDS", "Job Cards (${jobCards.count { it.status == "Active" }})", Icons.Default.DirectionsCar),
                Triple("VOICE_STATION", "Voice Drafts", Icons.Default.RecordVoiceOver),
                Triple("CORE_RETURNS", "Core Returns (${coreReturns.count { it.status != "Returned to Supplier" }})", Icons.Default.SwapHoriz)
            )

            tabs.forEach { (key, label, icon) ->
                val isSelected = selectedSection == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) WorkshopBrandNavy else Color.Transparent)
                        .clickable { selectedSection = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else WorkshopTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else WorkshopTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSection) {
            "JOB_CARDS" -> {
                JobCardsListSection(
                    jobCards = jobCards,
                    onOpenNewJob = { showNewJobDialog = true },
                    onDeductPart = { job -> selectedJobForPartDeduction = job },
                    onCompleteJob = onCompleteJob
                )
            }
            "VOICE_STATION" -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        VoiceDraftStation(
                            voiceDrafts = voiceDrafts,
                            onAddTranscript = onAddVoiceTranscript,
                            onConfirmDraft = onConfirmVoiceDraft,
                            onRejectDraft = onRejectVoiceDraft
                        )
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            "CORE_RETURNS" -> {
                CoreReturnsScreen(
                    coreReturns = coreReturns,
                    onAdvanceStatus = onAdvanceCoreStatus,
                    onAddCore = onAddCoreReturn
                )
            }
        }
    }

    // Modal: Deduct & Assign Part to Job Card
    if (selectedJobForPartDeduction != null) {
        DeductPartToJobDialog(
            jobCard = selectedJobForPartDeduction!!,
            parts = parts,
            onDismiss = { selectedJobForPartDeduction = null },
            onConfirm = { partId, qty ->
                onAssignPartToJob(selectedJobForPartDeduction!!.jobCardId, partId, qty)
                selectedJobForPartDeduction = null
            }
        )
    }

    // Modal: Create New Work Order
    if (showNewJobDialog) {
        CreateJobCardDialog(
            onDismiss = { showNewJobDialog = false },
            onCreate = { plate, customer, vehicle, mechanic ->
                onCreateJob(plate, customer, vehicle, mechanic)
                showNewJobDialog = false
            }
        )
    }
}

@Composable
fun JobCardsListSection(
    jobCards: List<JobCard>,
    onOpenNewJob: () -> Unit,
    onDeductPart: (JobCard) -> Unit,
    onCompleteJob: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Work Orders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextPrimary
                    )
                    Text(
                        text = "Real-time parts costing & margin calculation",
                        fontSize = 12.sp,
                        color = WorkshopTextSecondary
                    )
                }

                PrimaryNavyButton(
                    text = "+ New Order",
                    onClick = onOpenNewJob,
                    icon = Icons.Default.Add
                )
            }
        }

        items(jobCards) { job ->
            JobCardItem(
                job = job,
                onAddPart = { onDeductPart(job) },
                onComplete = { onCompleteJob(job.jobCardId) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun JobCardItem(
    job: JobCard,
    onAddPart: () -> Unit,
    onComplete: () -> Unit
) {
    WorkshopCard {
        // Job Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkshopBrandNavy)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = job.jobCardId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // License Plate badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = job.vehiclePlate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }

            if (job.status == "Active") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SuccessBadgeBg)
                        .border(1.dp, SuccessBadgeBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "In Workshop",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessBadgeText
                    )
                }
            } else {
                Text(
                    text = "Completed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vehicle & Customer Info
        Text(
            text = "${job.vehicleModel} • ${job.customerName}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = WorkshopTextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = WorkshopTextSecondary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.assignedMechanic, fontSize = 12.sp, color = WorkshopTextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = WorkshopTextSecondary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${job.laborHours}h labor ($${"%.0f".format(job.laborTotal)})", fontSize = 12.sp, color = WorkshopTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = WorkshopBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Linked Parts Used Section
        Text(
            text = "PARTS LINKED TO WORK ORDER (${job.partsUsed.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = WorkshopTextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (job.partsUsed.isEmpty()) {
            Text(
                text = "No parts deducted yet. Tap 'Deduct & Assign Part' below.",
                fontSize = 12.sp,
                color = WorkshopTextSecondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                job.partsUsed.forEach { part ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(WorkshopCanvasBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(part.partName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WorkshopTextPrimary)
                            Text("SKU: ${part.sku} • Qty: ${part.quantity}", fontSize = 10.sp, color = WorkshopTextSecondary)
                        }
                        Text(
                            text = "$${"%.2f".format(part.totalRevenue)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkshopBrandNavy
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Financial & Profit Margin Preview Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Quote (Parts + Labor)", fontSize = 10.sp, color = WorkshopTextSecondary)
                Text(
                    text = "$${"%.2f".format(job.subtotal)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopBrandNavy
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Gross Profit Margin", fontSize = 10.sp, color = WorkshopTextSecondary)
                Text(
                    text = "${"%.1f".format(job.profitMarginPercent)}% ($${"%.2f".format(job.grossProfit)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (job.profitMarginPercent >= 35) Color(0xFF15803D) else Color(0xFFB45309)
                )
            }
        }

        if (job.status == "Active") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryOutlinedButton(
                    text = "+ Deduct Part",
                    onClick = onAddPart,
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f)
                )

                PrimaryNavyButton(
                    text = "Complete Job",
                    onClick = onComplete,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DeductPartToJobDialog(
    jobCard: JobCard,
    parts: List<WorkshopPart>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val inStockParts = remember(parts) { parts.filter { it.qty > 0 } }
    var selectedPartId by remember { mutableStateOf(inStockParts.firstOrNull()?.id ?: "") }
    var quantityToDeduct by remember { mutableStateOf(1) }
    val selectedPart = inStockParts.find { it.id == selectedPartId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Deduct & Assign Part", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WorkshopBrandNavy)
                Text("Linking to ${jobCard.jobCardId} (${jobCard.vehiclePlate})", fontSize = 12.sp, color = WorkshopTextSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select workshop part to draw from inventory:", fontSize = 12.sp, color = WorkshopTextSecondary)

                if (inStockParts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No parts currently in stock in the warehouse.", fontSize = 12.sp, color = WorkshopTextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        inStockParts.forEach { part ->
                            val isSelected = part.id == selectedPartId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) WorkshopActivePill else WorkshopCanvasBg)
                                    .border(1.dp, if (isSelected) WorkshopBrandNavy else WorkshopBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedPartId = part.id
                                        quantityToDeduct = 1
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(part.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WorkshopTextPrimary)
                                    Text("${part.binLocation} • In Stock: ${part.qty}", fontSize = 10.sp, color = WorkshopTextSecondary)
                                }
                                Text("$${"%.2f".format(part.unitPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                            }
                        }
                    }
                }

                if (selectedPart != null) {
                    Text("Quantity to Deduct:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    GreasyTouchQuantityAdjuster(
                        currentQty = quantityToDeduct,
                        onDecrease = { if (quantityToDeduct > 1) quantityToDeduct-- },
                        onIncrease = { if (quantityToDeduct < selectedPart.qty) quantityToDeduct++ }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPartId, quantityToDeduct) },
                colors = ButtonDefaults.buttonColors(containerColor = WorkshopBrandNavy),
                enabled = selectedPart != null && selectedPart.qty >= quantityToDeduct && quantityToDeduct > 0
            ) {
                Text("Deduct & Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorkshopTextSecondary)
            }
        }
    )
}

@Composable
fun CreateJobCardDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var mechanic by remember { mutableStateOf("Dave Miller") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open New Work Order", fontWeight = FontWeight.Bold, color = WorkshopBrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Vehicle Plate (e.g. B 1234 XYZ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_job_plate_input")
                )
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle Make & Model (e.g. BMW 320i)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mechanic,
                    onValueChange = { mechanic = it },
                    label = { Text("Assigned Mechanic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plate.isNotBlank() && customer.isNotBlank()) {
                        onCreate(plate, customer, if (vehicle.isBlank()) "Sedan" else vehicle, mechanic)
                    }
                },
                enabled = plate.isNotBlank() && customer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WorkshopBrandNavy)
            ) {
                Text("Create Work Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorkshopTextSecondary)
            }
        }
    )
}
