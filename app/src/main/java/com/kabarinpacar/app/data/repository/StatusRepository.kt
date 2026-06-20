package com.kabarinpacar.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.kabarinpacar.app.data.local.StatusDao
import com.kabarinpacar.app.data.model.PartnerStatus
import com.kabarinpacar.app.data.model.StatusUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val statusDao: StatusDao,
    private val firestore: FirebaseFirestore,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PREF_USER_ID = "user_id"
        const val PREF_PAIR_ID = "pair_id"
        const val PREF_PARTNER_ID = "partner_id"
        const val PREF_LOCATION_OPT_IN = "location_opt_in"
        const val PREF_REMINDER_INTERVAL = "reminder_interval_hours"
        const val PREF_IS_PAIRED = "is_paired"
        const val PREF_NICKNAME = "nickname"
        const val PREF_PARTNER_LAST_SEEN = "partner_last_seen_ts"
        const val DEFAULT_REMINDER_INTERVAL = 3
    }

    val userId: String
        get() = sharedPreferences.getString(PREF_USER_ID, "") ?: ""

    val pairId: String
        get() = sharedPreferences.getString(PREF_PAIR_ID, "") ?: ""

    val partnerId: String
        get() = sharedPreferences.getString(PREF_PARTNER_ID, "") ?: ""

    val isPaired: Boolean
        get() = sharedPreferences.getBoolean(PREF_IS_PAIRED, false)

    val locationOptIn: Boolean
        get() = sharedPreferences.getBoolean(PREF_LOCATION_OPT_IN, false)

    val reminderIntervalHours: Int
        get() = sharedPreferences.getInt(PREF_REMINDER_INTERVAL, DEFAULT_REMINDER_INTERVAL)

    val nickname: String
        get() = sharedPreferences.getString(PREF_NICKNAME, "") ?: ""

    val partnerLastSeenTimestamp: Long
        get() = sharedPreferences.getLong(PREF_PARTNER_LAST_SEEN, 0L)

    fun setNickname(name: String) = sharedPreferences.edit().putString(PREF_NICKNAME, name).apply()
    fun setPartnerLastSeenTimestamp(ts: Long) = sharedPreferences.edit().putLong(PREF_PARTNER_LAST_SEEN, ts).apply()

    fun setUserId(id: String) = sharedPreferences.edit().putString(PREF_USER_ID, id).apply()
    fun setPairId(id: String) = sharedPreferences.edit().putString(PREF_PAIR_ID, id).apply()
    fun setPartnerId(id: String) = sharedPreferences.edit().putString(PREF_PARTNER_ID, id).apply()
    fun setIsPaired(paired: Boolean) = sharedPreferences.edit().putBoolean(PREF_IS_PAIRED, paired).apply()
    fun setLocationOptIn(optIn: Boolean) = sharedPreferences.edit().putBoolean(PREF_LOCATION_OPT_IN, optIn).apply()
    fun setReminderInterval(hours: Int) = sharedPreferences.edit().putInt(PREF_REMINDER_INTERVAL, hours).apply()

    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun cleanOldRecords() {
        statusDao.deleteOldStatuses(getStartOfDay())
    }

    suspend fun insertStatus(status: StatusUpdate): Long {
        return statusDao.insertStatus(status)
    }

    fun getTodayStatuses(): Flow<List<StatusUpdate>> {
        return statusDao.getTodayStatuses(userId, getStartOfDay())
    }

    fun getLatestStatusFlow(): Flow<StatusUpdate?> {
        return statusDao.getLatestStatusFlow(userId)
    }

    suspend fun getLatestStatus(): StatusUpdate? {
        return statusDao.getLatestStatus(userId)
    }

    suspend fun pushStatusToFirestore(status: StatusUpdate) {
        if (pairId.isEmpty() || userId.isEmpty()) return
        val data = hashMapOf(
            "activity" to status.activity,
            "note" to status.note,
            "locationName" to status.locationName,
            "timestamp" to status.timestamp,
            "userId" to status.userId,
            "nickname" to nickname
        )
        firestore.collection("pairs")
            .document(pairId)
            .collection("status")
            .document(userId)
            .set(data)
            .await()
    }

    fun listenToPartnerStatus(): Flow<PartnerStatus?> = callbackFlow {
        if (pairId.isEmpty() || partnerId.isEmpty()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("pairs")
            .document(pairId)
            .collection("status")
            .document(partnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val partnerStatus = PartnerStatus(
                    activity = snapshot.getString("activity") ?: "",
                    note = snapshot.getString("note") ?: "",
                    locationName = snapshot.getString("locationName") ?: "",
                    timestamp = snapshot.getLong("timestamp") ?: 0L,
                    nickname = snapshot.getString("nickname") ?: ""
                )
                trySend(partnerStatus)
            }
        awaitClose { listener.remove() }
    }

    suspend fun fetchPartnerStatusOnce(): PartnerStatus? {
        if (pairId.isEmpty() || partnerId.isEmpty()) return null
        return try {
            val snapshot = firestore.collection("pairs")
                .document(pairId)
                .collection("status")
                .document(partnerId)
                .get()
                .await()
            if (!snapshot.exists()) return null
            PartnerStatus(
                activity = snapshot.getString("activity") ?: "",
                note = snapshot.getString("note") ?: "",
                locationName = snapshot.getString("locationName") ?: "",
                timestamp = snapshot.getLong("timestamp") ?: 0L,
                nickname = snapshot.getString("nickname") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generatePairingCode(): String {
        val code = (100000..999999).random().toString()
        val uid = userId.ifEmpty {
            val newId = java.util.UUID.randomUUID().toString()
            setUserId(newId)
            newId
        }
        firestore.collection("pairing_codes")
            .document(code)
            .set(
                hashMapOf(
                    "creatorId" to uid,
                    "createdAt" to System.currentTimeMillis(),
                    "used" to false
                )
            )
            .await()
        return code
    }

    suspend fun joinWithCode(code: String): Result<String> {
        return try {
            val doc = firestore.collection("pairing_codes")
                .document(code)
                .get()
                .await()
            if (!doc.exists()) return Result.failure(Exception("Kode tidak ditemukan"))
            if (doc.getBoolean("used") == true) return Result.failure(Exception("Kode sudah digunakan"))

            val creatorId = doc.getString("creatorId") ?: return Result.failure(Exception("Data rusak"))
            val joiningId = userId.ifEmpty {
                val newId = java.util.UUID.randomUUID().toString()
                setUserId(newId)
                newId
            }

            if (creatorId == joiningId) return Result.failure(Exception("Tidak bisa pairing dengan diri sendiri"))

            val pairDocId = if (creatorId < joiningId) "${creatorId}_${joiningId}" else "${joiningId}_${creatorId}"

            firestore.collection("pairs")
                .document(pairDocId)
                .set(
                    hashMapOf(
                        "user1" to creatorId,
                        "user2" to joiningId,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                .await()

            firestore.collection("pairing_codes")
                .document(code)
                .update("used", true)
                .await()

            setPairId(pairDocId)
            setPartnerId(creatorId)
            setIsPaired(true)

            firestore.collection("pairs")
                .document(pairDocId)
                .collection("members")
                .document(joiningId)
                .set(hashMapOf("joinedAt" to System.currentTimeMillis()))
                .await()

            Result.success(pairDocId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPairing(code: String): Result<String> {
        return try {
            val doc = firestore.collection("pairing_codes")
                .document(code)
                .get()
                .await()
            if (!doc.exists()) return Result.failure(Exception("Kode tidak valid"))
            val creatorId = doc.getString("creatorId") ?: return Result.failure(Exception("Data tidak ditemukan"))
            if (creatorId != userId) return Result.failure(Exception("Kode bukan milikmu"))

            val pairQuery = firestore.collection("pairs")
                .whereIn("user1", listOf(creatorId))
                .get()
                .await()

            Result.success("")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenForPairingConfirmation(code: String, onPaired: (String, String) -> Unit) {
        firestore.collection("pairs")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                for (doc in snapshots.documents) {
                    val user1 = doc.getString("user1") ?: continue
                    val user2 = doc.getString("user2") ?: continue
                    if (user1 == userId || user2 == userId) {
                        val partnerId = if (user1 == userId) user2 else user1
                        setPairId(doc.id)
                        setPartnerId(partnerId)
                        setIsPaired(true)
                        onPaired(doc.id, partnerId)
                        break
                    }
                }
            }
    }
}
