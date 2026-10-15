package com.example.data.model

data class WorkshopPart(
    val id: String,
    val name: String,
    val sku: String,
    val partNumber: String,
    val oemCrossReference: String,
    val category: String,
    val qty: Int,
    val minQty: Int,
    val binLocation: String, // e.g., "Aisle 1 - Shelf A - Bin 04"
    val unitPrice: Double,
    val costPrice: Double,
    val coreEligible: Boolean = false,
    val coreDeposit: Double = 0.0,
    val packUnitName: String = "Box",
    val packRatio: Int = 12
) {
    val isOutOfStock: Boolean get() = qty <= 0
    val isLowStock: Boolean get() = qty in 1..minQty
    val marginPercent: Double get() = if (unitPrice > 0) ((unitPrice - costPrice) / unitPrice) * 100 else 0.0
}

data class JobPartUsage(
    val partId: String,
    val partName: String,
    val sku: String,
    val quantity: Int,
    val unitPrice: Double,
    val costPrice: Double
) {
    val totalRevenue: Double get() = unitPrice * quantity
    val totalCost: Double get() = costPrice * quantity
    val totalProfit: Double get() = totalRevenue - totalCost
}

data class JobCard(
    val jobCardId: String,
    val vehiclePlate: String,
    val customerName: String,
    val vehicleModel: String,
    val assignedMechanic: String,
    val status: String, // "Active", "Completed"
    val partsUsed: List<JobPartUsage> = emptyList(),
    val laborHours: Double = 1.5,
    val laborRate: Double = 85.0,
    val notes: String = "",
    val createdAt: String = "Today, 08:30 AM"
) {
    val partsTotalRevenue: Double get() = partsUsed.sumOf { it.totalRevenue }
    val partsTotalCost: Double get() = partsUsed.sumOf { it.totalCost }
    val laborTotal: Double get() = laborHours * laborRate
    val subtotal: Double get() = partsTotalRevenue + laborTotal
    val totalCost: Double get() = partsTotalCost + (laborHours * 35.0) // estimated labor cost
    val grossProfit: Double get() = subtotal - totalCost
    val profitMarginPercent: Double get() = if (subtotal > 0) (grossProfit / subtotal) * 100 else 0.0
}

data class VoiceDraft(
    val draftId: String,
    val rawTranscript: String,
    val parsedPart: String,
    val parsedQty: Int,
    val targetPlate: String,
    val timestamp: String,
    val status: String = "Pending" // "Pending", "Confirmed", "Rejected"
)

data class CoreReturn(
    val coreId: String,
    val originalPart: String,
    val customerPlate: String,
    val supplierName: String,
    val depositValue: Double,
    val status: String, // "Removed from Car", "Warehouse Storage", "Returned to Supplier"
    val dateLogged: String = "Today"
)

data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val partSku: String,
    val partName: String,
    val actionType: String, // "STOCK_DEDUCTED", "STOCK_ADDED", "JOB_ASSIGNED", "PACK_CONVERTED", "CORE_RETURNED", "VOICE_CONFIRMED"
    val quantityDelta: Int,
    val reason: String,
    val mechanicName: String,
    val balanceAfter: Int
)

data class SyncAction(
    val id: String,
    val actionType: String,
    val summary: String,
    val timestamp: String,
    val status: String = "Pending" // "Pending", "Synced"
)

data class AftermarketCrossMatch(
    val brand: String,
    val partNumber: String,
    val inStock: Boolean,
    val stockQty: Int,
    val price: Double
)

data class VisionInspectionResult(
    val partIdentified: String,
    val conditionScore: Int, // 0 - 100 (percentage remaining life)
    val conditionLabel: String,
    val oemNumber: String,
    val recommendedAction: String,
    val aftermarketMatches: List<AftermarketCrossMatch>,
    val matchedPartId: String? = null
)
