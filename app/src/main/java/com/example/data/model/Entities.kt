package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "user_default",
    val phone: String = "+234 803 123 4567",
    val name: String = "Tunde Adeleke",
    val isHelper: Boolean = false,
    val shareLocationOptIn: Boolean = true,
    val panicPin: String = "9999", // Fake PIN for duress unlock
    val realPin: String = "1234",  // Standard unlock PIN
    val activeCrisisMode: String = "KIDNAP_SILENT", // KIDNAP_SILENT, ACCIDENT_LOUD, DISASTER_CHECKIN
    val hasCompletedConsent: Boolean = false
)

@Entity(
    tableName = "trusted_contacts",
    indices = [Index(value = ["phone"])]
)
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "user_default",
    val name: String,
    val phone: String,
    val relationship: String = "Family",
    val isPrimary: Boolean = true,
    val lastNotifiedTimestamp: Long? = null
)

@Entity(
    tableName = "sos_events",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class SosEventEntity(
    @PrimaryKey val id: String, // e.g. "SOS-847291"
    val userId: String = "user_default",
    val lat: Double,
    val lng: Double,
    val address: String = "Locating address...",
    val audioUrl: String? = null, // local path or storage link
    val battery: Int = 85,
    val status: String = "active", // "active", "resolved"
    val mode: String = "KIDNAP_SILENT", // "KIDNAP_SILENT", "ACCIDENT_LOUD", "DISASTER_CHECKIN", "RISKY_TRIP"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(
    tableName = "location_pings",
    indices = [Index(value = ["sosId"]), Index(value = ["timestamp"])]
)
data class LocationPingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sosId: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "helper_alerts")
data class HelperAlertEntity(
    @PrimaryKey val id: String,
    val anonymizedLabel: String, // "Distress signal near Lekki Phase 1"
    val distanceMeters: Int, // e.g. 500
    val crisisType: String, // "Silent SOS", "Accident alert", "Disaster check"
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val timestamp: Long = System.currentTimeMillis(),
    val lat: Double,
    val lng: Double
)
