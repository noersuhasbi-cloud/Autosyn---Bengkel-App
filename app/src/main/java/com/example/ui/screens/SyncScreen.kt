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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncAction
import com.example.ui.components.PrimaryNavyButton
import com.example.ui.components.SuccessBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockBorder
import com.example.ui.theme.LowStockText
import com.example.ui.theme.SuccessBadgeBg
import com.example.ui.theme.SuccessBadgeText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary

@Composable
fun SyncScreen(
    syncQueue: List<SyncAction>,
    isOnline: Boolean,
    isSyncing: Boolean,
    onToggleOnline: () -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingCount = syncQueue.count { it.status == "Pending" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Sync Status Hero Card
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isOnline) Color(0xFF15803D) else Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Cloud Database Online" else "Offline Storage Mode",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkshopBrandNavy
                            )
                        }
                        Text(
                            text = if (isOnline) "Changes sync automatically to workshop cloud" else "Subterranean garage mode: queuing locally",
                            fontSize = 11.sp,
                            color = WorkshopTextSecondary
                        )
                    }

                    // Online/Offline simulation toggle
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { onToggleOnline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WorkshopBrandNavy
                        ),
                        modifier = Modifier.testTag("network_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = WorkshopBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PENDING OUTBOX QUEUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WorkshopTextSecondary, letterSpacing = 1.sp)
                        Text(
                            text = "$pendingCount actions queued for sync",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCount > 0) LowStockText else Color(0xFF15803D)
                        )
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WorkshopBrandNavy, strokeWidth = 2.dp)
                    } else {
                        PrimaryNavyButton(
                            text = if (pendingCount > 0) "Sync Now ($pendingCount)" else "Force Sync",
                            onClick = onTriggerSync,
                            icon = Icons.Default.CloudSync,
                            enabled = isOnline && !isSyncing
                        )
                    }
                }
            }
        }

        // Subterranean Offline Info Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkshopActivePill)
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.SignalCellularAlt else Icons.Default.SignalCellularOff,
                        contentDescription = null,
                        tint = WorkshopBrandNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline-First Automotive Architecture",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkshopBrandNavy
                        )
                        Text(
                            text = "Work seamlessly in basement workshop bays without WiFi. All stock deductions, voice drafts, and core swaps are cached and automatically push when signal resumes.",
                            fontSize = 11.sp,
                            color = WorkshopTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Transaction Queue List
        item {
            Text(
                text = "SYNCHRONIZATION ACTIVITY LOG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WorkshopTextSecondary,
                letterSpacing = 1.sp
            )
        }

        items(syncQueue) { item ->
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.timestamp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WorkshopTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item.actionType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(item.summary, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = WorkshopTextPrimary)
                    }

                    if (item.status == "Synced") {
                        SuccessBadge("Synced")
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LowStockBg)
                                .border(1.dp, LowStockBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Queued", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LowStockText)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
