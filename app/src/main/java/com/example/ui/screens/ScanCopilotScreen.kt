package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.data.model.AftermarketCrossMatch
import com.example.data.model.JobCard
import com.example.data.model.VisionInspectionResult
import com.example.data.model.WorkshopPart
import com.example.ui.components.CoreEligibleBadge
import com.example.ui.components.PrimaryNavyButton
import com.example.ui.components.SecondaryOutlinedButton
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.CoreBadgeBg
import com.example.ui.theme.CoreBadgeText
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockText
import com.example.ui.theme.OutOfStockBg
import com.example.ui.theme.OutOfStockText
import com.example.ui.theme.SuccessBadgeBg
import com.example.ui.theme.SuccessBadgeText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary

@Composable
fun ScanCopilotScreen(
    parts: List<WorkshopPart>,
    jobCards: List<JobCard>,
    onAssignToJob: (String, String, Int) -> Unit,
    onAddCoreReturn: (String, String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var scanMode by remember { mutableStateOf("AI_VISION") } // "AI_VISION" or "BARCODE"
    var flashEnabled by remember { mutableStateOf(false) }

    // Defensive lifecycle cleanup to avoid memory leaks when navigating away
    DisposableEffect(Unit) {
        onDispose {
            flashEnabled = false
        }
    }

    // Preset Inspection Results for worn/corroded parts
    val inspectionPresets = listOf(
        VisionInspectionResult(
            partIdentified = "Worn Front Brake Pad Set (2.1mm Remaining)",
            conditionScore = 14,
            conditionLabel = "Severe Wear - Metal on Metal Risk",
            oemNumber = "OE 8V0698151C",
            recommendedAction = "Immediate replacement recommended. Rotor inspection advised.",
            aftermarketMatches = listOf(
                AftermarketCrossMatch("Brembo (In Stock)", "BRK-BRM-844", true, 5, 85.00),
                AftermarketCrossMatch("Akebono Euro", "ACT1760", false, 0, 92.00),
                AftermarketCrossMatch("Textar German", "2568301", false, 0, 78.50)
            ),
            matchedPartId = "part_4"
        ),
        VisionInspectionResult(
            partIdentified = "Corroded Alternator (Diode Pack Failure)",
            conditionScore = 8,
            conditionLabel = "Internal Short - Core Deposit Eligible",
            oemNumber = "OE 06F903023F",
            recommendedAction = "Replace with Reman unit & claim $60 supplier core credit.",
            aftermarketMatches = listOf(
                AftermarketCrossMatch("Valeo Reman (In Stock)", "ALT-VAL-120", true, 2, 240.00),
                AftermarketCrossMatch("Bosch OE", "AL0844X", false, 0, 275.00),
                AftermarketCrossMatch("Denso Heavy-Duty", "DAN1014", false, 0, 260.00)
            ),
            matchedPartId = "part_5"
        ),
        VisionInspectionResult(
            partIdentified = "Carbon-Fouled Spark Plug (Worn Electrode)",
            conditionScore = 22,
            conditionLabel = "Heavy Carbon - Gap Gap Exceeded",
            oemNumber = "OE 101905626",
            recommendedAction = "Replace full cylinder set of 4.",
            aftermarketMatches = listOf(
                AftermarketCrossMatch("NGK Laser Iridium (In Stock)", "SPK-NGK-ILZ", true, 6, 12.00),
                AftermarketCrossMatch("Denso Iridium TT", "IK20TT", false, 0, 13.50),
                AftermarketCrossMatch("Bosch Double Platinum", "FR7KPP332U", false, 0, 11.00)
            ),
            matchedPartId = "part_3"
        )
    )

    var currentResult by remember { mutableStateOf(inspectionPresets[0]) }
    var assignedFeedback by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    // Laser scanning bar animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Viewfinder & Camera Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, WorkshopBorder, RoundedCornerShape(16.dp))
            ) {
                // Viewfinder Reticle Corners
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Scanning Sweep Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFF38BDF8))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (scanMode == "AI_VISION") Icons.Default.AutoAwesome else Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (scanMode == "AI_VISION") "POINT AT CORRODED OR UNMARKED PART" else "ALIGN BARCODE OR QR CODE IN FRAME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Controls inside Viewfinder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (scanMode == "AI_VISION") "AI Part Copilot 2.4" else "Laser Scanner Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (flashEnabled) Color(0xFFFACC15) else Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flashlight",
                            tint = if (flashEnabled) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mode Switcher Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scanMode == "AI_VISION") WorkshopBrandNavy else Color.Transparent)
                        .clickable { scanMode = "AI_VISION" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI Vision Part Copilot",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scanMode == "AI_VISION") Color.White else WorkshopTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scanMode == "BARCODE") WorkshopBrandNavy else Color.Transparent)
                        .clickable { scanMode = "BARCODE" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Standard Barcode/QR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scanMode == "BARCODE") Color.White else WorkshopTextSecondary
                    )
                }
            }
        }

        // Interactive Inspection Presets for Mechanics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT WORN PART SCAN SIMULATION:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkshopTextSecondary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    inspectionPresets.forEachIndexed { index, preset ->
                        val isSelected = currentResult == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) WorkshopActivePill else Color.White)
                                .border(1.dp, if (isSelected) WorkshopBrandNavy else WorkshopBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    currentResult = preset
                                    assignedFeedback = null
                                    isFeedbackError = false
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "Worn Brake Pad"
                                    1 -> "Corroded Alternator"
                                    else -> "Fouled Spark Plug"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) WorkshopBrandNavy else WorkshopTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // AI Vision Analysis Card
        item {
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = WorkshopBrandNavy, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI IDENTIFIED PART", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WorkshopTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentResult.partIdentified, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WorkshopTextPrimary)
                        Text(currentResult.oemNumber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WorkshopBrandNavy)
                    }

                    // Wear Health Gauge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentResult.conditionScore < 20) OutOfStockBg else LowStockBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${currentResult.conditionScore}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (currentResult.conditionScore < 20) OutOfStockText else LowStockText)
                            Text("Life Left", fontSize = 9.sp, color = if (currentResult.conditionScore < 20) OutOfStockText else LowStockText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentResult.conditionLabel} • ${currentResult.recommendedAction}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF991B1B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = WorkshopBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // OEM CROSS REFERENCE LOOKUP TABLE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OEM CROSS-REFERENCE & IN-STOCK MATCHES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Icon(Icons.Default.CompareArrows, null, tint = WorkshopTextSecondary, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentResult.aftermarketMatches.forEach { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (match.inStock) SuccessBadgeBg.copy(alpha = 0.5f) else WorkshopCanvasBg)
                                .border(1.dp, if (match.inStock) Color(0xFF86EFAC) else WorkshopBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(match.brand, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WorkshopTextPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(match.partNumber, fontSize = 11.sp, color = WorkshopTextSecondary)
                                }
                                Text(
                                    text = if (match.inStock) "✓ Ready in warehouse (${match.stockQty} in stock)" else "✗ Out of stock at distributor",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (match.inStock) Color(0xFF15803D) else Color(0xFFDC2626)
                                )
                            }

                            Text(
                                text = "$${"%.2f".format(match.price)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkshopBrandNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (assignedFeedback != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFeedbackError) OutOfStockBg else SuccessBadgeBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = assignedFeedback!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFeedbackError) OutOfStockText else SuccessBadgeText
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 1-Tap Action: Deduct & Assign to Active Job Card with defensive stock check
                val activeJob = jobCards.firstOrNull { it.status == "Active" }
                val matchedWarehousePart = parts.find { it.id == currentResult.matchedPartId }

                if (activeJob != null && matchedWarehousePart != null) {
                    val canAssign = matchedWarehousePart.qty > 0
                    PrimaryNavyButton(
                        text = if (canAssign) "Deduct & Assign to ${activeJob.jobCardId} (${activeJob.vehiclePlate})" else "Out of Stock in Warehouse",
                        onClick = {
                            if (canAssign) {
                                onAssignToJob(activeJob.jobCardId, matchedWarehousePart.id, 1)
                                isFeedbackError = false
                                assignedFeedback = "✓ Assigned 1 unit to ${activeJob.jobCardId} (${activeJob.vehiclePlate}) & deducted from warehouse stock."
                            } else {
                                isFeedbackError = true
                                assignedFeedback = "⚠ Insufficient stock: ${matchedWarehousePart.name} is currently out of stock."
                            }
                        },
                        icon = Icons.Default.CheckCircle,
                        enabled = canAssign,
                        modifier = Modifier.fillMaxWidth().testTag("scan_assign_btn")
                    )
                }

                // Core Return Action if Core Eligible
                if (currentResult.partIdentified.contains("Alternator")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryOutlinedButton(
                        text = "Queue Worn Core for Return ($60 Credit)",
                        onClick = {
                            onAddCoreReturn("Valeo 120A Reman Alternator", "B 1234 XYZ", "EuroParts Dist. Warehouse", 60.00)
                            isFeedbackError = false
                            assignedFeedback = "✓ Logged core return in warehouse storage ($60.00 credit deposit)."
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
