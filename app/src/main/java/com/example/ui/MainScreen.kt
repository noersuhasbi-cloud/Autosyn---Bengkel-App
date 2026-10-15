package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.WorkshopRepository
import com.example.ui.screens.AuditScreen
import com.example.ui.screens.ScanCopilotScreen
import com.example.ui.screens.StockScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.WorkshopScreen
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockText
import com.example.ui.theme.OutOfStockBg
import com.example.ui.theme.OutOfStockText
import com.example.ui.theme.WorkshopActivePill
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopBrandNavy
import com.example.ui.theme.WorkshopCanvasBg
import com.example.ui.theme.WorkshopTextPrimary
import com.example.ui.theme.WorkshopTextSecondary
import kotlinx.coroutines.launch

enum class WorkshopTab(val title: String, val icon: ImageVector) {
    WORKSHOP("Workshop", Icons.Default.Build),
    STOCK("Stock", Icons.Default.Inventory2),
    SCAN("Scan", Icons.Default.QrCodeScanner),
    AUDIT("Audit", Icons.Default.History),
    SYNC("Sync", Icons.Default.CloudSync)
}

@Composable
fun MainScreen(
    repository: WorkshopRepository? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeRepository = remember(context) { repository ?: WorkshopRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(WorkshopTab.WORKSHOP) }

    val parts by activeRepository.parts.collectAsState()
    val jobCards by activeRepository.jobCards.collectAsState()
    val voiceDrafts by activeRepository.voiceDrafts.collectAsState()
    val coreReturns by activeRepository.coreReturns.collectAsState()
    val auditLogs by activeRepository.auditLogs.collectAsState()
    val syncQueue by activeRepository.syncQueue.collectAsState()
    val isOnline by activeRepository.isOnline.collectAsState()
    val isSyncing by activeRepository.isSyncing.collectAsState()

    val lowStockCount = parts.count { it.isLowStock || it.isOutOfStock }
    val pendingSyncCount = syncQueue.count { it.status == "Pending" }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkshopCanvasBg),
        topBar = {
            WorkshopTopBar(
                isOnline = isOnline,
                pendingSyncCount = pendingSyncCount,
                onQuickVoiceClick = { currentTab = WorkshopTab.WORKSHOP }
            )
        },
        bottomBar = {
            WorkshopBottomDockNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                stockAlertCount = lowStockCount,
                pendingSyncCount = pendingSyncCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WorkshopCanvasBg)
        ) {
            when (currentTab) {
                WorkshopTab.WORKSHOP -> {
                    WorkshopScreen(
                        jobCards = jobCards,
                        parts = parts,
                        voiceDrafts = voiceDrafts,
                        coreReturns = coreReturns,
                        onAssignPartToJob = { job, part, qty ->
                            activeRepository.assignPartToJobCard(job, part, qty)
                        },
                        onCompleteJob = { jobId ->
                            activeRepository.completeJobCard(jobId)
                        },
                        onCreateJob = { plate, customer, vehicle, mechanic ->
                            activeRepository.addJobCard(plate, customer, vehicle, mechanic)
                        },
                        onAddVoiceTranscript = { transcript ->
                            activeRepository.addVoiceDraftFromTranscript(transcript)
                        },
                        onConfirmVoiceDraft = { draftId ->
                            activeRepository.confirmVoiceDraft(draftId)
                        },
                        onRejectVoiceDraft = { draftId ->
                            activeRepository.rejectVoiceDraft(draftId)
                        },
                        onAdvanceCoreStatus = { coreId ->
                            activeRepository.advanceCoreStatus(coreId)
                        },
                        onAddCoreReturn = { part, plate, supplier, deposit ->
                            activeRepository.addCoreReturn(part, plate, supplier, deposit)
                        }
                    )
                }
                WorkshopTab.STOCK -> {
                    StockScreen(
                        parts = parts,
                        onAdjustQuantity = { partId, delta ->
                            activeRepository.adjustPartQuantity(partId, delta)
                        },
                        onConvertPack = { partId, packs ->
                            activeRepository.convertPackToUnits(partId, packs)
                        }
                    )
                }
                WorkshopTab.SCAN -> {
                    ScanCopilotScreen(
                        parts = parts,
                        jobCards = jobCards,
                        onAssignToJob = { job, part, qty ->
                            activeRepository.assignPartToJobCard(job, part, qty)
                        },
                        onAddCoreReturn = { part, plate, supplier, deposit ->
                            activeRepository.addCoreReturn(part, plate, supplier, deposit)
                        }
                    )
                }
                WorkshopTab.AUDIT -> {
                    AuditScreen(auditLogs = auditLogs)
                }
                WorkshopTab.SYNC -> {
                    SyncScreen(
                        syncQueue = syncQueue,
                        isOnline = isOnline,
                        isSyncing = isSyncing,
                        onToggleOnline = { activeRepository.toggleNetworkMode() },
                        onTriggerSync = {
                            coroutineScope.launch { activeRepository.syncPendingQueue() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkshopTopBar(
    isOnline: Boolean,
    pendingSyncCount: Int,
    onQuickVoiceClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .shadow(2.dp, spotColor = Color(0x100F172A)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AutoStock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = WorkshopBrandNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(WorkshopActivePill)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WorkshopBrandNavy)
                    }
                }
                Text(
                    text = "Bay 3 • Dave Miller",
                    fontSize = 11.sp,
                    color = WorkshopTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online/Offline Status Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                        .border(1.dp, if (isOnline) Color(0xFF86EFAC) else Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF15803D) else Color(0xFFB45309))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "Cloud Sync" else "Offline ($pendingSyncCount)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Greasy Hands Voice Station Shortcut
                IconButton(
                    onClick = onQuickVoiceClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(WorkshopBrandNavy)
                        .testTag("top_voice_shortcut_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Station",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkshopBottomDockNavigation(
    currentTab: WorkshopTab,
    onTabSelected: (WorkshopTab) -> Unit,
    stockAlertCount: Int,
    pendingSyncCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(elevation = 12.dp, spotColor = Color(0x200F172A))
            .border(width = 1.dp, color = WorkshopBorder),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkshopTab.values().forEach { tab ->
                val isSelected = currentTab == tab

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) WorkshopBrandNavy else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(
                            horizontal = if (isSelected) 14.dp else 10.dp,
                            vertical = 8.dp
                        )
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Badge wrapper for stock and sync
                        when {
                            tab == WorkshopTab.STOCK && stockAlertCount > 0 -> {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = OutOfStockText,
                                            contentColor = Color.White
                                        ) {
                                            Text("$stockAlertCount", fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color.White else WorkshopTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            tab == WorkshopTab.SYNC && pendingSyncCount > 0 -> {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = LowStockText,
                                            contentColor = Color.White
                                        ) {
                                            Text("$pendingSyncCount", fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color.White else WorkshopTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color.White else WorkshopTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
