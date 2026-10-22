package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Avatars
import com.example.data.model.ChatMessage
import com.example.data.model.FamilyMember
import com.example.data.model.OperationType
import com.example.data.model.SafeZone
import com.example.data.model.SafetyAlert
import com.example.data.model.UserDevice
import com.example.data.model.UserProfile
import com.example.data.model.DeviceSession
import com.example.data.model.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class TrackViewRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  private fun userDocRef(uid: String) = db.collection("users").document(uid)

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return userDocRef(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  fun observeMembers(userId: String): Flow<List<FamilyMember>> {
    val path = "users/$userId/members"
    return userDocRef(userId)
      .collection("members")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(FamilyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareByDescending<FamilyMember> { it.pinned }.thenBy { it.name })
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMessages(userId: String, memberId: String): Flow<List<ChatMessage>> {
    val path = "users/$userId/messages"
    return userDocRef(userId)
      .collection("messages")
      .whereEqualTo("memberId", memberId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeAlerts(userId: String): Flow<List<SafetyAlert>> {
    val path = "users/$userId/alerts"
    return userDocRef(userId)
      .collection("alerts")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(SafetyAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeSafeZones(userId: String): Flow<List<SafeZone>> {
    val path = "users/$userId/safezones"
    return userDocRef(userId)
      .collection("safezones")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(SafeZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.name }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeDevices(userId: String): Flow<List<UserDevice>> {
    val path = "users/$userId/devices"
    return userDocRef(userId)
      .collection("devices")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(UserDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.name }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeIncomingSessions(userId: String, toDeviceId: String): Flow<List<DeviceSession>> {
    val path = "users/$userId/device_sessions"
    return userDocRef(userId)
      .collection("device_sessions")
      .whereEqualTo("toDeviceId", toDeviceId)
      .whereEqualTo("status", "REQUESTED")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(DeviceSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeActiveSession(userId: String, sessionId: String): Flow<DeviceSession?> {
    val path = "users/$userId/device_sessions/$sessionId"
    return userDocRef(userId)
      .collection("device_sessions")
      .document(sessionId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(DeviceSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun registerOrUpdateDevice(device: UserDevice): Result<Unit> = runCatching {
    val uid = requireUserId()
    val targetDeviceId = device.deviceId.ifBlank { device.id }.takeIf { it.isNotBlank() } ?: "device_primary"
    val docRef = userDocRef(uid).collection("devices").document(targetDeviceId)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(device.copy(deviceId = targetDeviceId).toWriteMap(uid)).await()
      } else {
        val currentCreatedAt =
          existing.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: FieldValue.serverTimestamp()
        docRef.set(device.copy(deviceId = targetDeviceId).toWriteMap(uid, currentCreatedAt, FieldValue.serverTimestamp())).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateDeviceLocation(deviceId: String, lat: Double, lng: Double, accuracyMeters: Int): Result<Unit> =
    runCatching {
      val uid = requireUserId()
      val docRef = userDocRef(uid).collection("devices").document(deviceId)
      try {
        docRef
          .update(
            mapOf(
              "latitude" to lat,
              "longitude" to lng,
              "accuracyMeters" to accuracyMeters.coerceIn(1, 5000),
              "lastSeenAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }

  suspend fun renameDevice(deviceId: String, newName: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanName = newName.trim().take(80)
    require(cleanName.isNotBlank()) { "Device name cannot be blank" }
    val docRef = userDocRef(uid).collection("devices").document(deviceId)
    try {
      docRef
        .update(
          mapOf(
            "name" to cleanName,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateDeviceConsent(
    deviceId: String,
    locationApproved: Boolean,
    cameraApproved: Boolean,
    microphoneApproved: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDocRef(uid).collection("devices").document(deviceId)
    try {
      docRef
        .update(
          mapOf(
            "locationSharingApproved" to locationApproved,
            "cameraSessionApproved" to cameraApproved,
            "microphoneApproved" to microphoneApproved,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun revokeDeviceAccess(deviceId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDocRef(uid).collection("devices").document(deviceId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun createDeviceSession(session: DeviceSession): Result<String> = runCatching {
    val uid = requireUserId()
    val colRef = userDocRef(uid).collection("device_sessions")
    val docRef = if (session.sessionId.isNotBlank()) colRef.document(session.sessionId) else colRef.document()
    val finalSession = session.copy(sessionId = docRef.id)
    try {
      docRef.set(finalSession.toWriteMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun respondToDeviceSession(sessionId: String, approve: Boolean, answerSdp: String = ""): Result<Unit> =
    runCatching {
      val uid = requireUserId()
      val docRef = userDocRef(uid).collection("device_sessions").document(sessionId)
      try {
        val nextStatus = if (approve) "ACTIVE" else "REJECTED"
        docRef
          .update(
            mapOf(
              "status" to nextStatus,
              "answerSdp" to answerSdp.take(5000),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }

  suspend fun endDeviceSession(sessionId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDocRef(uid).collection("device_sessions").document(sessionId)
    try {
      docRef
        .update(
          mapOf(
            "status" to "ENDED",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun createOrUpdateUserProfile(profile: UserProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDocRef(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(profile.toWriteMap(uid)).await()
      } else {
        val currentCreatedAt =
          existing.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: FieldValue.serverTimestamp()
        docRef.set(profile.toWriteMap(uid, currentCreatedAt)).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun addFamilyMember(member: FamilyMember, customDocId: String? = null): Result<String> =
    runCatching {
      val uid = requireUserId()
      val membersCol = userDocRef(uid).collection("members")
      val docRef = if (customDocId != null) membersCol.document(customDocId) else membersCol.document()
      try {
        docRef.set(member.toWriteMap(uid)).await()
        docRef.id
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }

  suspend fun getFamilyMemberById(targetUserId: String, memberId: String): Result<FamilyMember?> =
    runCatching {
      val docRef = userDocRef(targetUserId).collection("members").document(memberId)
      try {
        docRef
          .get()
          .await()
          .toObject(FamilyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }
    }

  suspend fun updateFamilyMember(member: FamilyMember): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(member.id.isNotBlank()) { "Member ID must not be blank" }
    val docRef = userDocRef(uid).collection("members").document(member.id)
    try {
      val ensuredCreatedAt =
        member.createdAt
          ?: docRef
            .get()
            .await()
            .getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: FieldValue.serverTimestamp()
      docRef.set(member.toWriteMap(uid, ensuredCreatedAt)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun sendMessage(
    memberId: String,
    senderName: String,
    text: String,
    messageType: String = "TEXT",
    outgoing: Boolean = true,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val timeLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
    val colRef = userDocRef(uid).collection("messages")
    val docRef = colRef.document()
    val msg =
      ChatMessage(
        userId = uid,
        memberId = memberId,
        senderName = senderName,
        outgoing = outgoing,
        text = text.take(1000).ifBlank { "Shared location update" },
        messageType = messageType,
        timeLabel = timeLabel,
      )
    try {
      docRef.set(msg.toWriteMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun addSafetyAlert(alert: SafetyAlert, customDocId: String? = null): Result<String> =
    runCatching {
      val uid = requireUserId()
      val colRef = userDocRef(uid).collection("alerts")
      val docRef = if (customDocId != null) colRef.document(customDocId) else colRef.document()
      try {
        docRef.set(alert.toWriteMap(uid)).await()
        docRef.id
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }

  suspend fun markAlertRead(alert: SafetyAlert): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(alert.id.isNotBlank()) { "Alert ID required" }
    val docRef = userDocRef(uid).collection("alerts").document(alert.id)
    try {
      docRef
        .update(
          mapOf(
            "read" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun markAllAlertsRead(alerts: List<SafetyAlert>): Result<Unit> = runCatching {
    val uid = requireUserId()
    for (alert in alerts.filter { !it.read && it.id.isNotBlank() }) {
      val docRef = userDocRef(uid).collection("alerts").document(alert.id)
      try {
        docRef
          .update(
            mapOf(
              "read" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun addSafeZone(safeZone: SafeZone, customDocId: String? = null): Result<String> =
    runCatching {
      val uid = requireUserId()
      val colRef = userDocRef(uid).collection("safezones")
      val docRef = if (customDocId != null) colRef.document(customDocId) else colRef.document()
      try {
        docRef.set(safeZone.toWriteMap(uid)).await()
        docRef.id
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }

  suspend fun updateSafeZone(safeZone: SafeZone): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(safeZone.id.isNotBlank()) { "SafeZone ID required" }
    val docRef = userDocRef(uid).collection("safezones").document(safeZone.id)
    try {
      val ensuredCreatedAt =
        safeZone.createdAt
          ?: docRef
            .get()
            .await()
            .getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: FieldValue.serverTimestamp()
      docRef.set(safeZone.toWriteMap(uid, ensuredCreatedAt)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteSafeZone(zoneId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDocRef(uid).collection("safezones").document(zoneId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun exportLocationDataJson(): Result<String> = runCatching {
    val uid = requireUserId()
    val members =
      userDocRef(uid)
        .collection("members")
        .get()
        .await()
        .toObjects(FamilyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    val zones =
      userDocRef(uid)
        .collection("safezones")
        .get()
        .await()
        .toObjects(SafeZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    val root = JSONObject()
    root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
    root.put("userId", uid)
    root.put("protocol", "TrackView Encrypted Family Mesh v4.2.0")

    val membersArray = JSONArray()
    for (m in members) {
      membersArray.put(
        JSONObject().apply {
          put("name", m.name)
          put("role", m.role)
          put("latitude", m.latitude)
          put("longitude", m.longitude)
          put("batteryLevel", m.batteryLevel)
          put("locationLabel", m.locationLabel)
        }
      )
    }
    root.put("familyMembers", membersArray)

    val zonesArray = JSONArray()
    for (z in zones) {
      zonesArray.put(
        JSONObject().apply {
          put("name", z.name)
          put("address", z.address)
          put("radiusMeters", z.radiusMeters)
          put("latitude", z.latitude)
          put("longitude", z.longitude)
        }
      )
    }
    root.put("safeZones", zonesArray)
    root.toString(2)
  }

  suspend fun deleteAccountData(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = userDocRef(uid)
    for (sub in listOf("members", "messages", "alerts", "safezones", "devices", "device_sessions")) {
      val docs = userRef.collection(sub).get().await().documents
      for (doc in docs) {
        doc.reference.delete().await()
      }
    }
    userRef.delete().await()
  }

  suspend fun ensureInitialDataSeeded(displayName: String?, email: String?): Result<Unit> =
    runCatching {
      val uid = requireUserId()
      val profileRef = userDocRef(uid)
      val existingProfile = profileRef.get().await()
      if (!existingProfile.exists()) {
        val initialProfile =
          UserProfile(
            userId = uid,
            displayName = (displayName?.takeIf { it.isNotBlank() } ?: "David Miller").take(80),
            email = (email?.takeIf { it.isNotBlank() } ?: "david.miller@family.org").take(150),
            avatarUrl = Avatars.DAVID,
            statusMessage = "Available • Location shared with Family",
            ghostModeEnabled = false,
            shareBatteryStatus = true,
            darkModeMapStyling = false,
            speedUnit = "MPH",
            planTier = "FREE",
          )
        createOrUpdateUserProfile(initialProfile).getOrThrow()
      }

      val existingMembers = profileRef.collection("members").get().await()
      if (existingMembers.isEmpty) {
        val defaultMembers =
          listOf(
            "sarah_miller" to
              FamilyMember(
                userId = uid,
                name = "Sarah Miller",
                role = "SPOUSE",
                avatarUrl = Avatars.SARAH,
                online = true,
                liveSharing = true,
                pinned = true,
                favorite = true,
                batteryLevel = 84,
                speedMph = 24,
                locationLabel = "Moving along Grand Ave • 8 min ETA",
                lastMessageSnippet = "Live: 84% battery • 24 mph on I-80",
                lastActivityTimeLabel = "10:42 AM",
                unreadCount = 0,
                screenTimeMinutesToday = 115,
                screenTimeLimitMinutes = 300,
                latitude = 41.8917,
                longitude = -87.6243,
                accuracyMeters = 5,
                notifyOnSchoolDeparture = true,
                ghostMode = false,
                appRestrictionsEnabled = false,
                websiteSafetyEnabled = true,
                bedtimeSchedule = "11:00 PM - 6:30 AM",
                pairingCode = "TV-8841",
              ),
            "miller_family_hub" to
              FamilyMember(
                userId = uid,
                name = "Miller Family Hub",
                role = "GROUP",
                avatarUrl = Avatars.DAVID,
                online = true,
                liveSharing = true,
                pinned = false,
                favorite = true,
                batteryLevel = 92,
                speedMph = 0,
                locationLabel = "Westside High School SafeZone",
                lastMessageSnippet = "Dad: Just arrived at Westside High School",
                lastActivityTimeLabel = "9:15 AM",
                unreadCount = 2,
                screenTimeMinutesToday = 45,
                screenTimeLimitMinutes = 240,
                latitude = 41.9102,
                longitude = -87.6510,
                accuracyMeters = 6,
                notifyOnSchoolDeparture = true,
                ghostMode = false,
                appRestrictionsEnabled = false,
                websiteSafetyEnabled = true,
                bedtimeSchedule = "10:00 PM - 6:30 AM",
                pairingCode = "TV-1001",
              ),
            "lucas_miller" to
              FamilyMember(
                userId = uid,
                name = "Lucas Miller",
                role = "CHILD",
                avatarUrl = Avatars.LUCAS,
                online = true,
                liveSharing = true,
                pinned = false,
                favorite = true,
                batteryLevel = 91,
                speedMph = 0,
                locationLabel = "Lincoln Park • At School (since 8:15 AM)",
                lastMessageSnippet = "Can you pick me up from soccer practice?",
                lastActivityTimeLabel = "Yesterday",
                unreadCount = 1,
                screenTimeMinutesToday = 135,
                screenTimeLimitMinutes = 150,
                latitude = 41.9255,
                longitude = -87.6385,
                accuracyMeters = 4,
                notifyOnSchoolDeparture = true,
                ghostMode = false,
                appRestrictionsEnabled = true,
                websiteSafetyEnabled = true,
                bedtimeSchedule = "9:30 PM - 6:45 AM",
                pairingCode = "TV-3049",
              ),
            "emma_miller" to
              FamilyMember(
                userId = uid,
                name = "Emma Miller",
                role = "CHILD",
                avatarUrl = Avatars.EMMA,
                online = false,
                liveSharing = false,
                pinned = false,
                favorite = true,
                batteryLevel = 64,
                speedMph = 0,
                locationLabel = "Central City Library (Study Room 3)",
                lastMessageSnippet = "Location: Central City Library (Study Room 3)",
                lastActivityTimeLabel = "Yesterday",
                unreadCount = 0,
                screenTimeMinutesToday = 160,
                screenTimeLimitMinutes = 180,
                latitude = 41.8762,
                longitude = -87.6281,
                accuracyMeters = 8,
                notifyOnSchoolDeparture = true,
                ghostMode = true,
                appRestrictionsEnabled = true,
                websiteSafetyEnabled = true,
                bedtimeSchedule = "10:00 PM - 6:30 AM",
                pairingCode = "TV-7192",
              ),
            "marcus_miller" to
              FamilyMember(
                userId = uid,
                name = "Marcus Miller",
                role = "SIBLING",
                avatarUrl = Avatars.MARCUS,
                online = true,
                liveSharing = false,
                pinned = false,
                favorite = false,
                batteryLevel = 78,
                speedMph = 0,
                locationLabel = "Downtown River North",
                lastMessageSnippet = "Voice message (0:24)",
                lastActivityTimeLabel = "Tuesday",
                unreadCount = 0,
                screenTimeMinutesToday = 90,
                screenTimeLimitMinutes = 360,
                latitude = 41.8924,
                longitude = -87.6341,
                accuracyMeters = 7,
                notifyOnSchoolDeparture = false,
                ghostMode = false,
                appRestrictionsEnabled = false,
                websiteSafetyEnabled = false,
                bedtimeSchedule = "11:30 PM - 7:00 AM",
                pairingCode = "TV-5518",
              ),
            "safezone_bot" to
              FamilyMember(
                userId = uid,
                name = "SafeZone Alert Bot",
                role = "BOT",
                avatarUrl = Avatars.LOGO,
                online = true,
                liveSharing = true,
                pinned = false,
                favorite = false,
                batteryLevel = 100,
                speedMph = 0,
                locationLabel = "All 3 SafeZones Protected",
                lastMessageSnippet = "Lucas entered SafeZone \"Home\" safely at 3:45 PM",
                lastActivityTimeLabel = "Monday",
                unreadCount = 0,
                screenTimeMinutesToday = 0,
                screenTimeLimitMinutes = 1440,
                latitude = 41.9028,
                longitude = -87.6315,
                accuracyMeters = 1,
                notifyOnSchoolDeparture = true,
                ghostMode = false,
                appRestrictionsEnabled = false,
                websiteSafetyEnabled = true,
                bedtimeSchedule = "Always Active",
                pairingCode = "TV-SAFE",
              ),
          )
        for ((docId, member) in defaultMembers) {
          addFamilyMember(member, docId).getOrThrow()
        }

        val sarahMessages =
          listOf(
            ChatMessage(
              userId = uid,
              memberId = "sarah_miller",
              senderName = "Sarah Miller",
              outgoing = false,
              text = "Hey! Leaving the downtown office now, heading towards 5th Avenue.",
              messageType = "TEXT",
              timeLabel = "10:30 AM",
            ),
            ChatMessage(
              userId = uid,
              memberId = "sarah_miller",
              senderName = "You",
              outgoing = true,
              text =
                "Sounds good! Traffic is heavy on Main St. Can you share your live location so I know when dinner should be ready?",
              messageType = "TEXT",
              timeLabel = "10:32 AM",
            ),
            ChatMessage(
              userId = uid,
              memberId = "sarah_miller",
              senderName = "Sarah Miller",
              outgoing = false,
              text = "Downtown → 5th Ave (ETA 14 min) • 24 mph • 5m acc • 82% battery",
              messageType = "LIVE_LOCATION",
              timeLabel = "10:35 AM",
            ),
            ChatMessage(
              userId = uid,
              memberId = "sarah_miller",
              senderName = "You",
              outgoing = true,
              text = "Got it, see you moving. Drive safe!",
              messageType = "TEXT",
              timeLabel = "10:36 AM",
            ),
            ChatMessage(
              userId = uid,
              memberId = "sarah_miller",
              senderName = "SafeZone Monitor",
              outgoing = false,
              text = "Sarah crossed Broadway Hub (3.2 mi away)",
              messageType = "GEOFENCE_EVENT",
              timeLabel = "10:38 AM",
            ),
          )
        for ((idx, msg) in sarahMessages.withIndex()) {
          profileRef
            .collection("messages")
            .document("sarah_msg_${idx + 1}")
            .set(msg.toWriteMap(uid))
            .await()
        }

        val lucasMessages =
          listOf(
            ChatMessage(
              userId = uid,
              memberId = "lucas_miller",
              senderName = "Lucas Miller",
              outgoing = false,
              text = "Arrived at Lincoln High School SafeZone (91% battery)",
              messageType = "STATIC_PIN",
              timeLabel = "8:15 AM",
            ),
            ChatMessage(
              userId = uid,
              memberId = "lucas_miller",
              senderName = "Lucas Miller",
              outgoing = false,
              text = "Can you pick me up from soccer practice?",
              messageType = "TEXT",
              timeLabel = "Yesterday",
            ),
          )
        for ((idx, msg) in lucasMessages.withIndex()) {
          profileRef
            .collection("messages")
            .document("lucas_msg_${idx + 1}")
            .set(msg.toWriteMap(uid))
            .await()
        }
      }

      val existingZones = profileRef.collection("safezones").get().await()
      if (existingZones.isEmpty) {
        val defaultZones =
          listOf(
            "zone_home" to
              SafeZone(
                userId = uid,
                name = "Home SafeZone",
                address = "742 Evergreen Terrace, Lincoln Park",
                radiusMeters = 250,
                latitude = 41.9214,
                longitude = -87.6513,
                notifyOnArrival = true,
                notifyOnDeparture = true,
              ),
            "zone_school" to
              SafeZone(
                userId = uid,
                name = "Lincoln High SafeZone",
                address = "2001 N Orchard St, Chicago, IL",
                radiusMeters = 300,
                latitude = 41.9185,
                longitude = -87.6412,
                notifyOnArrival = true,
                notifyOnDeparture = true,
              ),
            "zone_soccer" to
              SafeZone(
                userId = uid,
                name = "Soccer Academy",
                address = "Montrose Harbor Athletic Fields",
                radiusMeters = 350,
                latitude = 41.9635,
                longitude = -87.6380,
                notifyOnArrival = true,
                notifyOnDeparture = true,
              ),
          )
        for ((docId, zone) in defaultZones) {
          addSafeZone(zone, docId).getOrThrow()
        }
      }

      val existingAlerts = profileRef.collection("alerts").get().await()
      if (existingAlerts.isEmpty) {
        val defaultAlerts =
          listOf(
            "alert_1" to
              SafetyAlert(
                userId = uid,
                memberName = "Lucas Miller",
                alertType = "ARRIVAL",
                title = "Arrived at Lincoln High SafeZone",
                description = "Lucas entered Lincoln High School perimeter at 8:15 AM (91% battery).",
                timeLabel = "8:15 AM",
                read = false,
              ),
            "alert_2" to
              SafetyAlert(
                userId = uid,
                memberName = "Sarah Miller",
                alertType = "DEPARTURE",
                title = "Departed Downtown Office",
                description = "Sarah started a 1-hour live route share heading toward 5th Ave.",
                timeLabel = "10:30 AM",
                read = false,
              ),
            "alert_3" to
              SafetyAlert(
                userId = uid,
                memberName = "Lucas Miller",
                alertType = "SCREEN_TIME",
                title = "Screen Time Approaching Daily Limit",
                description = "Lucas has used 2h 15m of his 2h 30m daily allowance on school apps & games.",
                timeLabel = "Yesterday",
                read = false,
              ),
            "alert_4" to
              SafetyAlert(
                userId = uid,
                memberName = "Emma Miller",
                alertType = "OFFLINE",
                title = "Ghost Mode Paused Location",
                description = "Emma paused live location sharing near Central City Library.",
                timeLabel = "2h ago",
                read = true,
              ),
            "alert_5" to
              SafetyAlert(
                userId = uid,
                memberName = "Emma Miller",
                alertType = "LOW_BATTERY",
                title = "Battery Saver Triggered",
                description = "Emma's device dropped to 18% during evening study session.",
                timeLabel = "Yesterday",
                read = true,
              ),
            "alert_6" to
              SafetyAlert(
                userId = uid,
                memberName = "Lucas Miller",
                alertType = "BEDTIME",
                title = "Bedtime Schedule Enforced",
                description = "Child device locked for scheduled rest (9:30 PM - 6:45 AM).",
                timeLabel = "Last night",
                read = true,
              ),
          )
        for ((docId, alert) in defaultAlerts) {
          addSafetyAlert(alert, docId).getOrThrow()
        }
      }

      val existingDevices = profileRef.collection("devices").get().await()
      if (existingDevices.isEmpty) {
        val defaultDevices =
          listOf(
            "pixel_8_pro" to
              UserDevice(
                deviceId = "pixel_8_pro",
                userId = uid,
                name = "Pixel 8 Pro (This Phone)",
                model = "Google Pixel 8 Pro",
                online = true,
                batteryLevel = 94,
                latitude = 41.9214,
                longitude = -87.6513,
                accuracyMeters = 4,
                locationSharingApproved = true,
                cameraSessionApproved = true,
                microphoneApproved = true,
                pairingCode = "TV-8841",
              ),
            "samsung_s24" to
              UserDevice(
                deviceId = "samsung_s24",
                userId = uid,
                name = "Galaxy S24 (Sarah's Phone)",
                model = "Samsung SM-S928B",
                online = true,
                batteryLevel = 84,
                latitude = 41.8917,
                longitude = -87.6243,
                accuracyMeters = 5,
                locationSharingApproved = true,
                cameraSessionApproved = true,
                microphoneApproved = true,
                pairingCode = "TV-1002",
              ),
            "iphone_lucas" to
              UserDevice(
                deviceId = "iphone_lucas",
                userId = uid,
                name = "iPhone 15 (Lucas's Phone)",
                model = "Apple iPhone 15",
                online = true,
                batteryLevel = 91,
                latitude = 41.9255,
                longitude = -87.6385,
                accuracyMeters = 6,
                locationSharingApproved = true,
                cameraSessionApproved = false,
                microphoneApproved = true,
                pairingCode = "TV-3049",
              ),
            "tablet_emma" to
              UserDevice(
                deviceId = "tablet_emma",
                userId = uid,
                name = "Galaxy Tab A9 (Emma's Device)",
                model = "Samsung SM-X110",
                online = false,
                batteryLevel = 45,
                latitude = 41.8762,
                longitude = -87.6281,
                accuracyMeters = 10,
                locationSharingApproved = false,
                cameraSessionApproved = false,
                microphoneApproved = false,
                pairingCode = "TV-7192",
              ),
          )
        for ((_, dev) in defaultDevices) {
          registerOrUpdateDevice(dev).getOrThrow()
        }
      }
    }
}
