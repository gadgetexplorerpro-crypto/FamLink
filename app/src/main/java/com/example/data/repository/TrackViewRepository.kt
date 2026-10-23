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

  private val legacyDemoMemberIds =
    setOf(
      "member_sarah",
      "member_lucas",
      "member_emma",
      "member_marcus",
      "member_family_hub",
      "member_guardian_bot",
    )
  private val legacyDemoDeviceIds =
    setOf(
      "pixel_8_pro",
      "galaxy_s24_ultra",
      "ipad_mini_car",
    )
  private val legacyDemoZoneIds =
    setOf(
      "zone_home",
      "zone_school",
      "zone_soccer",
    )
  private val legacyDemoAlertIds =
    setOf(
      "alert_1",
      "alert_2",
      "alert_3",
      "alert_4",
      "alert_5",
    )
  private val legacyDemoMsgIds =
    setOf(
      "msg_sarah_1",
      "msg_sarah_2",
      "msg_sarah_3",
      "msg_sarah_4",
      "msg_lucas_1",
      "msg_lucas_2",
      "msg_lucas_3",
      "msg_emma_1",
      "msg_emma_2",
      "msg_hub_1",
      "msg_hub_2",
    )

  private fun isLegacyDemoMember(m: FamilyMember): Boolean {
    return m.id in legacyDemoMemberIds ||
      m.name.contains("Miller", ignoreCase = true) ||
      m.name.contains("Family Shield Bot", ignoreCase = true)
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return userDocRef(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          val raw = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (raw != null && raw.displayName.equals("David Miller", ignoreCase = true)) {
            raw.copy(
              displayName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "FamLink User",
              avatarUrl = auth.currentUser?.photoUrl?.toString().orEmpty(),
            )
          } else {
            raw
          }
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
          .filterNot { isLegacyDemoMember(it) }
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
          .filterNot { it.id in legacyDemoMsgIds || it.memberId in legacyDemoMemberIds }
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
          .filterNot {
            it.id in legacyDemoAlertIds ||
              it.memberName.contains("Miller", ignoreCase = true) ||
              it.memberName in setOf("Lucas", "Emma", "Sarah", "Marcus", "Noah")
          }
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
          .filterNot {
            it.id in legacyDemoZoneIds ||
              it.address.contains("1428 Elm Street", ignoreCase = true) ||
              it.address.contains("800 Academic Way", ignoreCase = true) ||
              it.address.contains("350 Parkside Blvd", ignoreCase = true)
          }
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
          .filterNot {
            it.deviceId in legacyDemoDeviceIds ||
              it.id in legacyDemoDeviceIds ||
              it.name.contains("Miller", ignoreCase = true)
          }
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

  suspend fun ensureRealUserInitialized(
    displayName: String?,
    email: String?,
    photoUrl: String? = null,
    currentDeviceId: String = "device_primary",
    currentDeviceName: String = "This Phone",
    currentDeviceModel: String = "Android Device",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val profileRef = userDocRef(uid)

    // 1. Purge any legacy demo documents that were seeded by earlier versions of the app
    runCatching {
      for (demoId in legacyDemoMemberIds) {
        profileRef.collection("members").document(demoId).delete()
      }
      for (demoDevId in legacyDemoDeviceIds) {
        profileRef.collection("devices").document(demoDevId).delete()
      }
      for (demoZoneId in legacyDemoZoneIds) {
        profileRef.collection("safezones").document(demoZoneId).delete()
      }
      for (demoAlertId in legacyDemoAlertIds) {
        profileRef.collection("alerts").document(demoAlertId).delete()
      }
      for (demoMsgId in legacyDemoMsgIds) {
        profileRef.collection("messages").document(demoMsgId).delete()
      }
      // Also delete any member document whose name contains "Miller" or "Family Shield Bot"
      val allMembers = profileRef.collection("members").get().await()
      for (doc in allMembers.documents) {
        val name = doc.getString("name").orEmpty()
        if (name.contains("Miller", ignoreCase = true) || name.contains("Family Shield Bot", ignoreCase = true)) {
          doc.reference.delete()
        }
      }
    }

    val existingProfile = profileRef.get().await()
    val existingName = existingProfile.getString("displayName").orEmpty()
    if (!existingProfile.exists() || existingName.equals("David Miller", ignoreCase = true)) {
      val initialProfile =
        UserProfile(
          userId = uid,
          displayName = (displayName?.takeIf { it.isNotBlank() } ?: "FamLink User").take(80),
          email = (email?.takeIf { it.isNotBlank() } ?: "user@famlink.app").take(150),
          avatarUrl = (photoUrl?.takeIf { it.isNotBlank() } ?: "").take(600),
          statusMessage = "FamLink Active • Family Protected",
          ghostModeEnabled = false,
          shareBatteryStatus = true,
          darkModeMapStyling = false,
          speedUnit = "MPH",
          planTier = "FREE",
        )
      createOrUpdateUserProfile(initialProfile).getOrThrow()
    }

    // Register ONLY the real current phone as the primary active device if not present
    val cleanDevId = currentDeviceId.ifBlank { "device_primary" }
    val deviceRef = profileRef.collection("devices").document(cleanDevId)
    val existingDev = deviceRef.get().await()
    if (!existingDev.exists()) {
      val realDevice =
        UserDevice(
          deviceId = cleanDevId,
          userId = uid,
          name = currentDeviceName.take(80).ifBlank { "This Phone" },
          model = currentDeviceModel.take(80).ifBlank { "Android Device" },
          online = true,
          batteryLevel = 100,
          latitude = 0.0,
          longitude = 0.0,
          accuracyMeters = 10,
          locationSharingApproved = true,
          cameraSessionApproved = true,
          microphoneApproved = true,
          pairingCode = "FL-" + (1000..9999).random(),
        )
      registerOrUpdateDevice(realDevice).getOrThrow()
    } else {
      deviceRef.update(
        mapOf(
          "online" to true,
          "lastSeenAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    }
  }

  suspend fun pairChildDevice(
    childName: String,
    role: String,
    pairingCode: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val cleanName = childName.trim().take(80).ifBlank { "Child Device" }
    val memberId = "member_" + System.currentTimeMillis()
    val memberRef = userDocRef(uid).collection("members").document(memberId)

    val newMember =
      FamilyMember(
        id = memberId,
        userId = uid,
        name = cleanName,
        role = role,
        avatarUrl = "",
        online = false,
        liveSharing = false,
        batteryLevel = 0,
        speedMph = 0,
        locationLabel = "Awaiting child phone link",
        lastMessageSnippet = "Pairing code: $pairingCode",
        lastActivityTimeLabel = "Not connected yet",
        unreadCount = 0,
        latitude = 0.0,
        longitude = 0.0,
        accuracyMeters = 10,
        pairingCode = pairingCode,
      )

    memberRef.set(newMember.toWriteMap(uid)).await()

    // Also create matching device entry for this child phone
    val deviceId = "dev_" + memberId
    val childDevice =
      UserDevice(
        id = deviceId,
        deviceId = deviceId,
        userId = uid,
        name = "$cleanName's Phone",
        model = "Child Phone",
        online = false,
        batteryLevel = 0,
        latitude = 0.0,
        longitude = 0.0,
        accuracyMeters = 10,
        locationSharingApproved = true,
        cameraSessionApproved = true,
        microphoneApproved = true,
        pairingCode = pairingCode,
      )
    registerOrUpdateDevice(childDevice).getOrThrow()

    memberId
  }

  suspend fun completeChildPairing(
    pairingCode: String,
    childDeviceId: String,
    childDeviceName: String,
    childDeviceModel: String,
  ): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val cleanCode = pairingCode.trim().uppercase()
    val membersCol = userDocRef(uid).collection("members")
    val querySnapshot = membersCol.whereEqualTo("pairingCode", cleanCode).get().await()

    if (querySnapshot.isEmpty) {
      return@runCatching false
    }

    val memberDoc = querySnapshot.documents.first()
    memberDoc.reference.update(
      mapOf(
        "online" to true,
        "locationLabel" to "Device Paired & Connected",
        "lastMessageSnippet" to "Connected via pairing code $cleanCode",
        "lastActivityTimeLabel" to "Just now",
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    ).await()

    // Update or register child device
    val devRef = userDocRef(uid).collection("devices").document(childDeviceId)
    devRef.set(
      UserDevice(
        id = childDeviceId,
        deviceId = childDeviceId,
        userId = uid,
        name = childDeviceName.take(80),
        model = childDeviceModel.take(80),
        online = true,
        batteryLevel = 100,
        latitude = 0.0,
        longitude = 0.0,
        accuracyMeters = 10,
        locationSharingApproved = true,
        cameraSessionApproved = true,
        microphoneApproved = true,
        pairingCode = cleanCode,
      ).toWriteMap(uid),
    ).await()

    true
  }

  suspend fun saveProfileImageToLocalStorage(
    context: Context,
    sourceUri: android.net.Uri,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val avatarsDir = java.io.File(context.filesDir, "avatars").apply { mkdirs() }
    val destFile = java.io.File(avatarsDir, "user_${uid.take(12)}_${System.currentTimeMillis()}.jpg")

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
      val originalBitmap = android.graphics.BitmapFactory.decodeStream(input)
      if (originalBitmap != null) {
        val maxDim = 512
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale =
          if (width > maxDim || height > maxDim) {
            maxDim.toFloat() / maxOf(width, height).toFloat()
          } else {
            1f
          }
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        val scaled =
          if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
          } else {
            originalBitmap
          }
        destFile.outputStream().use { out ->
          scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
        }
      } else {
        context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
          destFile.outputStream().use { out -> rawIn.copyTo(out) }
        }
      }
    } ?: throw IllegalArgumentException("Unable to read selected image")

    val localUriString = android.net.Uri.fromFile(destFile).toString().take(600)

    // Also update FirebaseAuth currentUser profile photoUri
    runCatching {
      val profileUpdates =
        com.google.firebase.auth.UserProfileChangeRequest.Builder()
          .setPhotoUri(android.net.Uri.parse(localUriString))
          .build()
      auth.currentUser?.updateProfile(profileUpdates)?.await()
    }

    localUriString
  }
}
