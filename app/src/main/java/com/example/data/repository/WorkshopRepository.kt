package com.example.data.repository

import android.content.Context
import com.example.data.model.AftermarketCrossMatch
import com.example.data.model.AuditLogEntry
import com.example.data.model.CoreReturn
import com.example.data.model.JobCard
import com.example.data.model.JobPartUsage
import com.example.data.model.SyncAction
import com.example.data.model.VisionInspectionResult
import com.example.data.model.VoiceDraft
import com.example.data.model.WorkshopPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WorkshopRepository(private val context: Context? = null) {

    private val _parts = MutableStateFlow<List<WorkshopPart>>(seedParts())
    val parts: StateFlow<List<WorkshopPart>> = _parts.asStateFlow()

    private val _jobCards = MutableStateFlow<List<JobCard>>(seedJobCards())
    val jobCards: StateFlow<List<JobCard>> = _jobCards.asStateFlow()

    private val _voiceDrafts = MutableStateFlow<List<VoiceDraft>>(seedVoiceDrafts())
    val voiceDrafts: StateFlow<List<VoiceDraft>> = _voiceDrafts.asStateFlow()

    private val _coreReturns = MutableStateFlow<List<CoreReturn>>(seedCoreReturns())
    val coreReturns: StateFlow<List<CoreReturn>> = _coreReturns.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(seedAuditLogs())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _syncQueue = MutableStateFlow<List<SyncAction>>(seedSyncQueue())
    val syncQueue: StateFlow<List<SyncAction>> = _syncQueue.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun currentTime(): String {
        return try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "10:00"
        }
    }

    private fun currentDateFormatted(): String {
        return try {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "Today, 10:00"
        }
    }

    fun toggleNetworkMode() {
        _isOnline.update { !it }
    }

    fun adjustPartQuantity(partId: String, delta: Int, reason: String = "Manual Adjustment", mechanic: String = "Dave Miller"): Boolean {
        val part = _parts.value.find { it.id == partId } ?: return false
        val newQty = (part.qty + delta).coerceAtLeast(0)
        if (newQty == part.qty) return false // Prevent false deduction/log when already 0

        val actualDelta = newQty - part.qty
        _parts.update { list ->
            list.map { if (it.id == partId) it.copy(qty = newQty) else it }
        }

        logAudit(
            partSku = part.sku,
            partName = part.name,
            actionType = if (actualDelta < 0) "STOCK_DEDUCTED" else "STOCK_ADDED",
            delta = actualDelta,
            reason = reason,
            mechanic = mechanic,
            balanceAfter = newQty
        )

        queueSync("STOCK_ADJUSTMENT", "Adjusted ${part.sku} ($actualDelta units). New balance: $newQty")
        persistState()
        return true
    }

    fun assignPartToJobCard(jobCardId: String, partId: String, quantity: Int, mechanic: String = "Dave Miller"): Boolean {
        if (quantity <= 0) return false
        val part = _parts.value.find { it.id == partId } ?: return false
        if (part.qty < quantity) return false
        val targetJob = _jobCards.value.find { it.jobCardId == jobCardId } ?: return false
        if (targetJob.status == "Completed") return false

        // Deduct inventory
        val newQty = (part.qty - quantity).coerceAtLeast(0)
        _parts.update { list ->
            list.map { if (it.id == partId) it.copy(qty = newQty) else it }
        }

        // Add to Job Card
        val usage = JobPartUsage(
            partId = part.id,
            partName = part.name,
            sku = part.sku,
            quantity = quantity,
            unitPrice = part.unitPrice,
            costPrice = part.costPrice
        )

        _jobCards.update { list ->
            list.map { job ->
                if (job.jobCardId == jobCardId) {
                    val existing = job.partsUsed.find { it.partId == partId }
                    val updatedParts = if (existing != null) {
                        job.partsUsed.map {
                            if (it.partId == partId) it.copy(quantity = it.quantity + quantity) else it
                        }
                    } else {
                        job.partsUsed + usage
                    }
                    job.copy(partsUsed = updatedParts)
                } else job
            }
        }

        logAudit(
            partSku = part.sku,
            partName = part.name,
            actionType = "JOB_ASSIGNED",
            delta = -quantity,
            reason = "Deducted for $jobCardId",
            mechanic = mechanic,
            balanceAfter = newQty
        )

        queueSync("JOB_DEDUCTION", "Assigned $quantity x ${part.sku} to Job $jobCardId")
        persistState()
        return true
    }

    fun convertPackToUnits(partId: String, packsReceived: Int, mechanic: String = "Inventory Admin"): Boolean {
        if (packsReceived <= 0) return false
        val part = _parts.value.find { it.id == partId } ?: return false
        val unitsToAdd = packsReceived * part.packRatio
        val newQty = part.qty + unitsToAdd

        _parts.update { list ->
            list.map { if (it.id == partId) it.copy(qty = newQty) else it }
        }

        logAudit(
            partSku = part.sku,
            partName = part.name,
            actionType = "PACK_CONVERTED",
            delta = unitsToAdd,
            reason = "Received $packsReceived ${part.packUnitName}(s) @ ${part.packRatio} units/pack",
            mechanic = mechanic,
            balanceAfter = newQty
        )

        queueSync("PACK_RECEIPT", "Unpacked $packsReceived ${part.packUnitName}(s) -> +$unitsToAdd units to ${part.sku}")
        persistState()
        return true
    }

    // Voice Draft Station NLP Parser
    fun addVoiceDraftFromTranscript(rawTranscript: String) {
        if (rawTranscript.isBlank()) return
        val lower = rawTranscript.lowercase()

        // Extract quantity
        var qty = 1
        val qtyMatch = Regex("""(\d+)\s*(liters?|ltr|bottle|pack|set|units?|box|pieces?|pcs)?""").find(lower)
        if (qtyMatch != null) {
            qty = qtyMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
        }

        // Match part
        val matchedPart = _parts.value.find { part ->
            val nameLower = part.name.lowercase()
            val skuLower = part.sku.lowercase()
            when {
                lower.contains("5w-30") || (lower.contains("oil") && nameLower.contains("5w-30")) -> nameLower.contains("5w-30")
                lower.contains("filter") && nameLower.contains("filter") -> true
                lower.contains("spark") || lower.contains("plug") -> nameLower.contains("spark")
                lower.contains("brake") || lower.contains("pad") -> nameLower.contains("brake")
                lower.contains("alternator") -> nameLower.contains("alternator")
                lower.contains("compressor") -> nameLower.contains("compressor")
                lower.contains("fluid") -> nameLower.contains("fluid")
                lower.contains("starter") -> nameLower.contains("starter")
                else -> lower.contains(skuLower)
            }
        }?.name ?: "Castrol Edge 5W-30 Full Synthetic Oil (1L)"

        // Match Plate
        val plateMatch = Regex("""plate\s*([a-zA-Z0-9\s]+)""").find(lower)
            ?: Regex("""([a-zA-Z]{1,2}\s*\d{1,4}\s*[a-zA-Z]{1,3})""").find(rawTranscript)
        val plate = plateMatch?.groupValues?.get(1)?.trim()?.uppercase() ?: "B 1234 XYZ"

        val draft = VoiceDraft(
            draftId = "#VD-${(100..999).random()}",
            rawTranscript = rawTranscript,
            parsedPart = matchedPart,
            parsedQty = qty,
            targetPlate = plate,
            timestamp = currentTime(),
            status = "Pending"
        )

        _voiceDrafts.update { listOf(draft) + it }
        queueSync("VOICE_DRAFT_LOGGED", "Greasy-hands voice draft logged: ${draft.draftId}")
        persistState()
    }

    fun confirmVoiceDraft(draftId: String, mechanic: String = "Dave Miller"): Boolean {
        val draft = _voiceDrafts.value.find { it.draftId == draftId } ?: return false
        if (draft.status != "Pending") return false

        val part = _parts.value.find { it.name.equals(draft.parsedPart, ignoreCase = true) }
            ?: _parts.value.find { it.name.contains(draft.parsedPart, ignoreCase = true) }
            ?: _parts.value.firstOrNull() ?: return false

        // Check if there's a matching active job card with target plate
        val targetJob = _jobCards.value.find {
            it.status == "Active" && it.vehiclePlate.replace(" ", "").equals(draft.targetPlate.replace(" ", ""), ignoreCase = true)
        }

        val success = if (targetJob != null) {
            assignPartToJobCard(targetJob.jobCardId, part.id, draft.parsedQty, mechanic)
        } else {
            if (part.qty >= draft.parsedQty) {
                adjustPartQuantity(part.id, -draft.parsedQty, "Voice draft for plate ${draft.targetPlate}", mechanic)
                true
            } else false
        }

        if (success) {
            _voiceDrafts.update { list ->
                list.map { if (it.draftId == draftId) it.copy(status = "Confirmed") else it }
            }
            queueSync("VOICE_CONFIRMED", "Confirmed voice draft ${draft.draftId} for plate ${draft.targetPlate}")
            persistState()
            return true
        }
        return false
    }

    fun rejectVoiceDraft(draftId: String) {
        _voiceDrafts.update { list ->
            list.map { if (it.draftId == draftId) it.copy(status = "Rejected") else it }
        }
        persistState()
    }

    // Core Return Tracking
    fun advanceCoreStatus(coreId: String) {
        var updatedCore: CoreReturn? = null
        _coreReturns.update { list ->
            list.map { core ->
                if (core.coreId == coreId) {
                    val nextStatus = when (core.status) {
                        "Removed from Car" -> "Warehouse Storage"
                        "Warehouse Storage" -> "Returned to Supplier"
                        else -> core.status
                    }
                    val updated = core.copy(status = nextStatus)
                    updatedCore = updated
                    updated
                } else core
            }
        }
        if (updatedCore != null) {
            queueSync("CORE_STATUS_UPDATE", "${updatedCore!!.coreId} updated to ${updatedCore!!.status}")
            persistState()
        }
    }

    fun addCoreReturn(originalPart: String, customerPlate: String, supplierName: String, depositValue: Double) {
        val newCore = CoreReturn(
            coreId = "#CR-${(200..999).random()}",
            originalPart = originalPart.ifBlank { "Alternator / Starter Core" },
            customerPlate = customerPlate.trim().uppercase().ifBlank { "UNREGISTERED" },
            supplierName = supplierName.ifBlank { "General Distributor" },
            depositValue = depositValue.coerceAtLeast(0.0),
            status = "Removed from Car",
            dateLogged = "Today"
        )
        _coreReturns.update { listOf(newCore) + it }
        queueSync("NEW_CORE_RETURN", "Created deposit core record for ${newCore.customerPlate}")
        persistState()
    }

    fun addJobCard(plate: String, customer: String, vehicle: String, mechanic: String) {
        val newJob = JobCard(
            jobCardId = "#WO-${(1050..1999).random()}",
            vehiclePlate = plate.trim().uppercase().ifBlank { "B 0000 NEW" },
            customerName = customer.trim().ifBlank { "Walk-in Customer" },
            vehicleModel = vehicle.trim().ifBlank { "Vehicle" },
            assignedMechanic = mechanic.trim().ifBlank { "Dave Miller" },
            status = "Active",
            createdAt = "Today, ${currentTime()}"
        )
        _jobCards.update { listOf(newJob) + it }
        queueSync("JOB_CREATED", "Opened new work order ${newJob.jobCardId}")
        persistState()
    }

    fun completeJobCard(jobCardId: String) {
        val job = _jobCards.value.find { it.jobCardId == jobCardId } ?: return
        if (job.status == "Completed") return

        _jobCards.update { list ->
            list.map { if (it.jobCardId == jobCardId) it.copy(status = "Completed") else it }
        }
        queueSync("JOB_COMPLETED", "Finalized $jobCardId")
        persistState()
    }

    suspend fun syncPendingQueue() {
        if (_syncQueue.value.none { it.status == "Pending" }) return
        _isSyncing.value = true
        try {
            kotlinx.coroutines.delay(1000)
            _syncQueue.update { list ->
                list.map { it.copy(status = "Synced") }
            }
            persistState()
        } catch (e: Exception) {
            // Defensive failure recovery
        } finally {
            _isSyncing.value = false
        }
    }

    private fun logAudit(
        partSku: String,
        partName: String,
        actionType: String,
        delta: Int,
        reason: String,
        mechanic: String,
        balanceAfter: Int
    ) {
        try {
            val entry = AuditLogEntry(
                id = UUID.randomUUID().toString().take(8),
                timestamp = currentDateFormatted(),
                partSku = partSku,
                partName = partName,
                actionType = actionType,
                quantityDelta = delta,
                reason = reason,
                mechanicName = mechanic,
                balanceAfter = balanceAfter
            )
            _auditLogs.update { listOf(entry) + it }
        } catch (e: Exception) {
            // Defensive check
        }
    }

    private fun queueSync(actionType: String, summary: String) {
        try {
            val action = SyncAction(
                id = UUID.randomUUID().toString().take(6),
                actionType = actionType,
                summary = summary,
                timestamp = currentTime(),
                status = if (_isOnline.value) "Synced" else "Pending"
            )
            _syncQueue.update { listOf(action) + it }
        } catch (e: Exception) {
            // Defensive check
        }
    }

    // Defensive Local Persistence Engine (Safe Storage with try-catch)
    private fun persistState() {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences("autostock_local_store", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            // Save counts and states defensively
            editor.putInt("total_parts_count", _parts.value.size)
            editor.putInt("active_jobs_count", _jobCards.value.count { it.status == "Active" })
            editor.putInt("pending_sync_count", _syncQueue.value.count { it.status == "Pending" })
            editor.apply()
        } catch (e: Exception) {
            // Catch storage write failures silently without crashing
        }
    }

    private fun loadPersistedState() {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences("autostock_local_store", Context.MODE_PRIVATE)
            // Validates storage access without throwing
            prefs.getInt("total_parts_count", -1)
        } catch (e: Exception) {
            // Storage read safe fallback
        }
    }

    // Seed Data
    private fun seedParts(): List<WorkshopPart> = listOf(
        WorkshopPart(
            id = "part_1",
            name = "Castrol Edge 5W-30 Full Synthetic Oil (1L)",
            sku = "OIL-5W30-SYN",
            partNumber = "04E115561H",
            oemCrossReference = "VW 504 00 / MB 229.51 / BMW LL-04",
            category = "Fluids",
            qty = 4,
            minQty = 12,
            binLocation = "Aisle 1 - Shelf A - Bin 04",
            unitPrice = 18.50,
            costPrice = 11.00,
            packUnitName = "Box of 12",
            packRatio = 12
        ),
        WorkshopPart(
            id = "part_2",
            name = "Bosch Premium Oil Filter",
            sku = "FLT-BOS-940",
            partNumber = "06A115561B",
            oemCrossReference = "Mann W712/94 / Mahle OC264 / Fram PH9599",
            category = "Filters",
            qty = 18,
            minQty = 8,
            binLocation = "Aisle 1 - Shelf B - Bin 11",
            unitPrice = 14.00,
            costPrice = 6.50,
            packUnitName = "Master Pack",
            packRatio = 10
        ),
        WorkshopPart(
            id = "part_3",
            name = "NGK Iridium Spark Plug (Pack of 4)",
            sku = "SPK-NGK-ILZ",
            partNumber = "101905626",
            oemCrossReference = "Denso IK20TT / Bosch FR7KPP332U / Champion OE178",
            category = "Ignition",
            qty = 6,
            minQty = 16,
            binLocation = "Aisle 2 - Shelf A - Bin 02",
            unitPrice = 12.00,
            costPrice = 5.20,
            packUnitName = "Box of 12",
            packRatio = 12
        ),
        WorkshopPart(
            id = "part_4",
            name = "Brembo Front Brake Pad Set",
            sku = "BRK-BRM-844",
            partNumber = "8V0698151C",
            oemCrossReference = "Akebono ACT1760 / Textar 2568301 / Ferodo FDB4433",
            category = "Brakes",
            qty = 5,
            minQty = 4,
            binLocation = "Aisle 2 - Shelf C - Bin 19",
            unitPrice = 85.00,
            costPrice = 48.00,
            packUnitName = "Pair Pack",
            packRatio = 2
        ),
        WorkshopPart(
            id = "part_5",
            name = "Valeo 120A Reman Alternator",
            sku = "ALT-VAL-120",
            partNumber = "06F903023F",
            oemCrossReference = "Bosch AL0844X / Denso DAN1014 / Hella 8EL012428",
            category = "Electrical",
            qty = 2,
            minQty = 1,
            binLocation = "Aisle 4 - Shelf A - Bin 01",
            unitPrice = 240.00,
            costPrice = 160.00,
            coreEligible = true,
            coreDeposit = 60.00,
            packUnitName = "Single Crate",
            packRatio = 1
        ),
        WorkshopPart(
            id = "part_6",
            name = "Denso AC Compressor (Sanden Pattern)",
            sku = "CMP-DEN-471",
            partNumber = "1K0820808F",
            oemCrossReference = "Sanden PXE16 / Valeo 813136 / Mahle ACP38",
            category = "Cooling / HVAC",
            qty = 1,
            minQty = 2,
            binLocation = "Aisle 4 - Shelf B - Bin 05",
            unitPrice = 380.00,
            costPrice = 250.00,
            coreEligible = true,
            coreDeposit = 80.00,
            packUnitName = "Single Unit",
            packRatio = 1
        ),
        WorkshopPart(
            id = "part_7",
            name = "Ferodo Dot 4 Brake Fluid (500ml)",
            sku = "FLD-DOT4-500",
            partNumber = "B000750M3",
            oemCrossReference = "Castrol React / Motul DOT4 / Pentosin Super DOT4",
            category = "Fluids",
            qty = 22,
            minQty = 10,
            binLocation = "Aisle 1 - Shelf C - Bin 08",
            unitPrice = 9.50,
            costPrice = 4.20,
            packUnitName = "Case of 24",
            packRatio = 24
        ),
        WorkshopPart(
            id = "part_8",
            name = "Bosch Starter Motor 1.4kW",
            sku = "STR-BOS-772",
            partNumber = "02Z911023F",
            oemCrossReference = "Valeo 438183 / Remy 17542 / Lucas LRS02241",
            category = "Electrical",
            qty = 0,
            minQty = 2,
            binLocation = "Aisle 4 - Shelf A - Bin 03",
            unitPrice = 210.00,
            costPrice = 140.00,
            coreEligible = true,
            coreDeposit = 50.00,
            packUnitName = "Single Unit",
            packRatio = 1
        ),
        WorkshopPart(
            id = "part_9",
            name = "Mann Engine Air Filter",
            sku = "FLT-MAN-C35",
            partNumber = "1K0129620D",
            oemCrossReference = "Fram CA10226 / Bosch 5369WS / Wix WA9748",
            category = "Filters",
            qty = 14,
            minQty = 6,
            binLocation = "Aisle 2 - Shelf B - Bin 15",
            unitPrice = 22.00,
            costPrice = 10.50,
            packUnitName = "Box of 6",
            packRatio = 6
        ),
        WorkshopPart(
            id = "part_10",
            name = "Gates Timing Belt Kit w/ Water Pump",
            sku = "BLT-GAT-556",
            partNumber = "03L198119F",
            oemCrossReference = "Continental CT1139WP6 / Dayco KTBWP5320 / INA 530055032",
            category = "Engine Timing",
            qty = 3,
            minQty = 2,
            binLocation = "Aisle 3 - Shelf B - Bin 07",
            unitPrice = 165.00,
            costPrice = 98.00,
            packUnitName = "Kit",
            packRatio = 1
        )
    )

    private fun seedJobCards(): List<JobCard> = listOf(
        JobCard(
            jobCardId = "#WO-1042",
            vehiclePlate = "B 1234 XYZ",
            customerName = "Marcus Vance",
            vehicleModel = "Audi A4 2.0T Quattro",
            assignedMechanic = "Dave Miller",
            status = "Active",
            laborHours = 2.0,
            notes = "Annual inspection & oil service. Customer reported minor squeak on front left.",
            partsUsed = listOf(
                JobPartUsage(
                    partId = "part_1",
                    partName = "Castrol Edge 5W-30 Full Synthetic Oil (1L)",
                    sku = "OIL-5W30-SYN",
                    quantity = 5,
                    unitPrice = 18.50,
                    costPrice = 11.00
                ),
                JobPartUsage(
                    partId = "part_2",
                    partName = "Bosch Premium Oil Filter",
                    sku = "FLT-BOS-940",
                    quantity = 1,
                    unitPrice = 14.00,
                    costPrice = 6.50
                )
            ),
            createdAt = "Today, 08:30 AM"
        ),
        JobCard(
            jobCardId = "#WO-1043",
            vehiclePlate = "B 8821 KLS",
            customerName = "Sarah Jenkins",
            vehicleModel = "VW Golf GTI Mk7",
            assignedMechanic = "Alex Rivera",
            status = "Active",
            laborHours = 1.5,
            notes = "Cylinder misfire code P0301. Spark plug replacement & air filter upgrade.",
            partsUsed = listOf(
                JobPartUsage(
                    partId = "part_3",
                    partName = "NGK Iridium Spark Plug (Pack of 4)",
                    sku = "SPK-NGK-ILZ",
                    quantity = 4,
                    unitPrice = 12.00,
                    costPrice = 5.20
                ),
                JobPartUsage(
                    partId = "part_9",
                    partName = "Mann Engine Air Filter",
                    sku = "FLT-MAN-C35",
                    quantity = 1,
                    unitPrice = 22.00,
                    costPrice = 10.50
                )
            ),
            createdAt = "Today, 09:15 AM"
        ),
        JobCard(
            jobCardId = "#WO-1044",
            vehiclePlate = "D 9902 TYU",
            customerName = "Kevin Zhang",
            vehicleModel = "BMW 328i F30",
            assignedMechanic = "Dave Miller",
            status = "Active",
            laborHours = 2.5,
            notes = "Front brake pad wear sensor triggered. Rotor resurfacing & pad installation.",
            partsUsed = listOf(
                JobPartUsage(
                    partId = "part_4",
                    partName = "Brembo Front Brake Pad Set",
                    sku = "BRK-BRM-844",
                    quantity = 1,
                    unitPrice = 85.00,
                    costPrice = 48.00
                ),
                JobPartUsage(
                    partId = "part_7",
                    partName = "Ferodo Dot 4 Brake Fluid (500ml)",
                    sku = "FLD-DOT4-500",
                    quantity = 1,
                    unitPrice = 9.50,
                    costPrice = 4.20
                )
            ),
            createdAt = "Today, 10:00 AM"
        )
    )

    private fun seedVoiceDrafts(): List<VoiceDraft> = listOf(
        VoiceDraft(
            draftId = "#VD-801",
            rawTranscript = "Took 2 liters 5W-30 oil for plate B 1234 XYZ",
            parsedPart = "Castrol Edge 5W-30 Full Synthetic Oil (1L)",
            parsedQty = 2,
            targetPlate = "B 1234 XYZ",
            timestamp = "10:14 AM",
            status = "Pending"
        ),
        VoiceDraft(
            draftId = "#VD-802",
            rawTranscript = "Installed 4 spark plugs for B 8821 KLS",
            parsedPart = "NGK Iridium Spark Plug (Pack of 4)",
            parsedQty = 4,
            targetPlate = "B 8821 KLS",
            timestamp = "09:45 AM",
            status = "Confirmed"
        )
    )

    private fun seedCoreReturns(): List<CoreReturn> = listOf(
        CoreReturn(
            coreId = "#CR-201",
            originalPart = "Valeo 120A Reman Alternator",
            customerPlate = "B 1234 XYZ",
            supplierName = "EuroParts Dist. Warehouse",
            depositValue = 60.00,
            status = "Warehouse Storage",
            dateLogged = "Oct 12, 2026"
        ),
        CoreReturn(
            coreId = "#CR-202",
            originalPart = "Bosch Starter Motor 1.4kW",
            customerPlate = "D 7714 QWE",
            supplierName = "LKQ Auto Components",
            depositValue = 50.00,
            status = "Removed from Car",
            dateLogged = "Today, 08:50 AM"
        )
    )

    private fun seedAuditLogs(): List<AuditLogEntry> = listOf(
        AuditLogEntry(
            id = "aud_01",
            timestamp = "Today, 08:45 AM",
            partSku = "OIL-5W30-SYN",
            partName = "Castrol Edge 5W-30 Full Synthetic Oil (1L)",
            actionType = "JOB_ASSIGNED",
            quantityDelta = -5,
            reason = "Assigned to #WO-1042 (Audi A4)",
            mechanicName = "Dave Miller",
            balanceAfter = 4
        ),
        AuditLogEntry(
            id = "aud_02",
            timestamp = "Today, 09:20 AM",
            partSku = "SPK-NGK-ILZ",
            partName = "NGK Iridium Spark Plug",
            actionType = "JOB_ASSIGNED",
            quantityDelta = -4,
            reason = "Assigned to #WO-1043 (VW Golf)",
            mechanicName = "Alex Rivera",
            balanceAfter = 6
        ),
        AuditLogEntry(
            id = "aud_03",
            timestamp = "Yesterday, 04:30 PM",
            partSku = "FLT-BOS-940",
            partName = "Bosch Premium Oil Filter",
            actionType = "PACK_CONVERTED",
            quantityDelta = 10,
            reason = "Unpacked 1 Master Pack into individual units",
            mechanicName = "Inventory Admin",
            balanceAfter = 18
        )
    )

    private fun seedSyncQueue(): List<SyncAction> = listOf(
        SyncAction("sy_01", "STOCK_DEDUCTION", "Deducted 5x OIL-5W30-SYN for #WO-1042", "08:45 AM", "Synced"),
        SyncAction("sy_02", "STOCK_DEDUCTION", "Deducted 4x SPK-NGK-ILZ for #WO-1043", "09:20 AM", "Synced"),
        SyncAction("sy_03", "VOICE_DRAFT", "Queued hands-free draft #VD-801", "10:14 AM", "Pending")
    )
}
