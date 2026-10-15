package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoiceDraft
import com.example.ui.components.SuccessBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopCardBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary
import kotlinx.coroutines.delay

@Composable
fun VoiceDraftStation(
    voiceDrafts: List<VoiceDraft>,
    onAddTranscript: (String) -> Unit,
    onConfirmDraft: (String) -> Unit,
    onRejectDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var currentTranscript by remember { mutableStateOf("Tap the microphone or a greasy-hands shortcut below to dictate parts used...") }

    // Defensive auto-completion timeout to prevent app freezing during voice recognition
    LaunchedEffect(isRecording) {
        if (isRecording) {
            try {
                delay(3000)
                if (isRecording) {
                    isRecording = false
                    val spoken = "Took 2 liters 5W-30 oil for plate B 1234 XYZ"
                    currentTranscript = "Processed: 2x 5W-30 oil parsed & added to draft"
                    onAddTranscript(spoken)
                }
            } catch (e: Exception) {
                isRecording = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Hands-Free Audio Hub Card
        WorkshopCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = WorkshopBrandNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hands-Free Voice Station",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkshopBrandNavy
                        )
                    }
                    Text(
                        text = "Optimized for greasy mechanics - no typing required",
                        fontSize = 12.sp,
                        color = WorkshopTextSecondary
                    )
                }

                // Active recording indicator
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("LISTENING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform & Transcript Box (#F8FAFC)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkshopCanvasBg)
                    .border(1.dp, WorkshopBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "LIVE TRANSCRIPTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentTranscript,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isRecording) WorkshopBrandNavy else WorkshopTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large 64dp Greasy Hand Voice Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFDC2626) else WorkshopBrandNavy)
                        .clickable {
                            if (!isRecording) {
                                isRecording = true
                                currentTranscript = "Listening... 'Took 2 liters 5W-30 oil for plate B 1234 XYZ'"
                            } else {
                                isRecording = false
                                onAddTranscript("Took 2 liters 5W-30 oil for plate B 1234 XYZ")
                                currentTranscript = "Processed: 2x 5W-30 oil assigned to draft"
                            }
                        }
                        .testTag("voice_record_mic_btn")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Voice Dictate",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isRecording) "Listening... tap mic to finish early" else "Tap Mic or select a quick voice preset below:",
                fontSize = 12.sp,
                color = WorkshopTextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Greasy Hands Voice Presets
            val quickPresets = listOf(
                "Took 2 liters 5W-30 oil for plate B 1234 XYZ",
                "Used 4 spark plugs for B 8821 KLS",
                "Installed 1 brake pad set on D 9902 TYU"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPresets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkshopActivePill)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                            .clickable {
                                isRecording = false
                                currentTranscript = preset
                                onAddTranscript(preset)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = WorkshopBrandNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\"$preset\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkshopBrandNavy
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unconfirmed Drafts List Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unconfirmed Voice Drafts (${voiceDrafts.count { it.status == "Pending" }})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WorkshopTextPrimary
            )
            Text(
                text = "End-of-Day Review",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = WorkshopTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (voiceDrafts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No voice drafts pending approval.", color = WorkshopTextSecondary)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voiceDrafts.forEach { draft ->
                    VoiceDraftItem(
                        draft = draft,
                        onConfirm = { onConfirmDraft(draft.draftId) },
                        onReject = { onRejectDraft(draft.draftId) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceDraftItem(
    draft: VoiceDraft,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    WorkshopCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = draft.draftId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkshopTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = draft.timestamp,
                        fontSize = 11.sp,
                        color = WorkshopTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    when (draft.status) {
                        "Confirmed" -> SuccessBadge("Confirmed")
                        "Rejected" -> Text("Rejected", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        else -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LowStockBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Pending Approval", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LowStockText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "\"${draft.rawTranscript}\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = WorkshopTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NLP Smart Extraction Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Part
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WorkshopActivePill)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, null, tint = WorkshopBrandNavy, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(draft.parsedPart, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WorkshopBrandNavy)
                    }

                    // Qty
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Numbers, null, tint = WorkshopTextPrimary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Qty: ${draft.parsedQty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WorkshopTextPrimary)
                    }

                    // Target Plate
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(draft.targetPlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }
            }

            if (draft.status == "Pending") {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onReject,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7))
                            .testTag("confirm_draft_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm & Deduct", tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
