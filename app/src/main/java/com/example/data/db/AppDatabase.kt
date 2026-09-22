package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.HelperAlertEntity
import com.example.data.model.LocationPingEntity
import com.example.data.model.SosEventEntity
import com.example.data.model.TrustedContactEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUser(userId: String = "user_default"): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserSync(userId: String = "user_default"): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET isHelper = :isHelper WHERE id = :userId")
    suspend fun updateHelperStatus(isHelper: Boolean, userId: String = "user_default")

    @Query("UPDATE users SET shareLocationOptIn = :optIn WHERE id = :userId")
    suspend fun updateLocationOptIn(optIn: Boolean, userId: String = "user_default")

    @Query("UPDATE users SET activeCrisisMode = :mode WHERE id = :userId")
    suspend fun updateCrisisMode(mode: String, userId: String = "user_default")

    @Query("UPDATE users SET hasCompletedConsent = 1 WHERE id = :userId")
    suspend fun markConsentCompleted(userId: String = "user_default")
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, id ASC")
    fun getAllContacts(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, id ASC")
    suspend fun getAllContactsSync(): List<TrustedContactEntity>

    @Query("SELECT COUNT(*) FROM trusted_contacts")
    fun getContactCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContactEntity)

    @Delete
    suspend fun deleteContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun updateContact(contact: TrustedContactEntity)

    @Query("UPDATE trusted_contacts SET lastNotifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastNotified(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface SosEventDao {
    @Query("SELECT * FROM sos_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<SosEventEntity>>

    @Query("SELECT * FROM sos_events WHERE status = 'active' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveSos(): Flow<SosEventEntity?>

    @Query("SELECT * FROM sos_events WHERE status = 'active' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSosSync(): SosEventEntity?

    @Query("SELECT * FROM sos_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SosEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SosEventEntity)

    @Update
    suspend fun updateEvent(event: SosEventEntity)

    @Query("UPDATE sos_events SET lat = :lat, lng = :lng, address = :address WHERE id = :id")
    suspend fun updateLocation(id: String, lat: Double, lng: Double, address: String)

    @Query("UPDATE sos_events SET audioUrl = :audioUrl WHERE id = :id")
    suspend fun updateAudioUrl(id: String, audioUrl: String)

    @Query("UPDATE sos_events SET status = 'resolved', resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun resolveEvent(id: String, resolvedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sos_events WHERE createdAt < :cutoffTime AND status = 'resolved'")
    suspend fun deleteResolvedOlderThan(cutoffTime: Long)

    @Query("DELETE FROM sos_events")
    suspend fun clearAll()
}

@Dao
interface LocationPingDao {
    @Query("SELECT * FROM location_pings WHERE sosId = :sosId ORDER BY timestamp ASC")
    fun getPingsForSos(sosId: String): Flow<List<LocationPingEntity>>

    @Query("SELECT * FROM location_pings WHERE sosId = :sosId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPing(sosId: String): LocationPingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPing(ping: LocationPingEntity)

    @Query("DELETE FROM location_pings WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("DELETE FROM location_pings WHERE sosId = :sosId")
    suspend fun deleteBySosId(sosId: String)
}

@Dao
interface HelperAlertDao {
    @Query("SELECT * FROM helper_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<HelperAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: HelperAlertEntity)

    @Query("UPDATE helper_alerts SET status = :newStatus WHERE id = :alertId")
    suspend fun updateAlertStatus(alertId: String, newStatus: String)

    @Query("DELETE FROM helper_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)
}

@Database(
    entities = [
        UserEntity::class,
        TrustedContactEntity::class,
        SosEventEntity::class,
        LocationPingEntity::class,
        HelperAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun sosEventDao(): SosEventDao
    abstract fun locationPingDao(): LocationPingDao
    abstract fun helperAlertDao(): HelperAlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ajiya_safety_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
