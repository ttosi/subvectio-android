package com.tdc.subvectio.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "delivery")
data class Delivery(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,

    @ColumnInfo(name = "store_name") var storeName: String,
    @ColumnInfo(name = "store_address") var storeAddress: String,
    @ColumnInfo(name = "customer_address") var customerAddress: String,

    @ColumnInfo(name = "distance") var distance: Double = 0.0,
    @ColumnInfo(name = "to_store") var toStoreMins: Double = 0.0,
    @ColumnInfo(name = "at_store") var atStoreMins: Double = 0.0,
    @ColumnInfo(name = "to_deliver") var toDeliverMins: Double = 0.0,

    @ColumnInfo(name = "est_duration") var estDuration: Double = 0.0,
    @ColumnInfo(name = "actual_duration") var actualDuration: Double = 0.0,

    @ColumnInfo(name = "offer") var offer: Double = 0.0,
    @ColumnInfo(name = "tip") var tip: Double = 0.0,
    @ColumnInfo(name = "base") var basePay: Double = 0.0,
    @ColumnInfo(name = "peak") var peakPay: Double = 0.0,
    @ColumnInfo(name = "actual") var actualPay: Double = 0.0,

    @ColumnInfo(name = "est_delivery_index") var estDeliveryIndex: Double = 0.0,
    @ColumnInfo(name = "actual_delivery_index") var actualDeliveryIndex: Double = 0.0,

    @ColumnInfo(name = "created_at") var createdAt: LocalDateTime,
    @ColumnInfo(name = "accepted_at") var acceptedAt: LocalDateTime? = null,
    @ColumnInfo(name = "arrived_store_at") var arrivedStoreAt: LocalDateTime? = null,
    @ColumnInfo(name = "departed_store_at") var departedStoreAt: LocalDateTime? = null,
    @ColumnInfo(name = "completed_at") var completedAt: LocalDateTime? = null,

    @ColumnInfo(name = "is_active") var isActive: Boolean = false,
    @ColumnInfo(name = "is_declined") var isDeclined: Boolean = false,
    @ColumnInfo(name = "is_unassigned") var isUnassigned: Boolean = false,
    @ColumnInfo(name = "is_synced") var isSynced: Boolean = false,
    @ColumnInfo(name = "is_debug") var isDebug: Boolean = false
)
