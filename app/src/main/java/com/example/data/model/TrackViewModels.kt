package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", exception.message ?: exception.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId @get:DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val avatarUrl: String = "",
  val statusMessage: String = "FamLink Active • Family Protected",
  val ghostModeEnabled: Boolean = false,
  val shareBatteryStatus: Boolean = true,
  val darkModeMapStyling: Boolean = false,
  val speedUnit: String = "MPH",
  val planTier: String = "FREE",
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "displayName" to displayName,
      "email" to email,
      "avatarUrl" to avatarUrl,
      "statusMessage" to statusMessage,
      "ghostModeEnabled" to ghostModeEnabled,
      "shareBatteryStatus" to shareBatteryStatus,
      "darkModeMapStyling" to darkModeMapStyling,
      "speedUnit" to speedUnit,
      "planTier" to planTier,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class FamilyMember(
  @DocumentId @get:DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val role: String = "CHILD", // SPOUSE, CHILD, SIBLING, GROUP, BOT
  val avatarUrl: String = "",
  val online: Boolean = false,
  val liveSharing: Boolean = false,
  val pinned: Boolean = false,
  val favorite: Boolean = false,
  val batteryLevel: Int = 0,
  val speedMph: Int = 0,
  val locationLabel: String = "No location report yet",
  val lastMessageSnippet: String = "Device paired",
  val lastActivityTimeLabel: String = "Pending sync",
  val unreadCount: Int = 0,
  val screenTimeMinutesToday: Int = 0,
  val screenTimeLimitMinutes: Int = 180,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val accuracyMeters: Int = 10,
  val notifyOnSchoolDeparture: Boolean = true,
  val ghostMode: Boolean = false,
  val appRestrictionsEnabled: Boolean = true,
  val websiteSafetyEnabled: Boolean = true,
  val bedtimeSchedule: String = "9:30 PM - 6:30 AM",
  val pairingCode: String = "",
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "name" to name,
      "role" to role,
      "avatarUrl" to avatarUrl,
      "online" to online,
      "liveSharing" to liveSharing,
      "pinned" to pinned,
      "favorite" to favorite,
      "batteryLevel" to batteryLevel,
      "speedMph" to speedMph,
      "locationLabel" to locationLabel,
      "lastMessageSnippet" to lastMessageSnippet,
      "lastActivityTimeLabel" to lastActivityTimeLabel,
      "unreadCount" to unreadCount,
      "screenTimeMinutesToday" to screenTimeMinutesToday,
      "screenTimeLimitMinutes" to screenTimeLimitMinutes,
      "latitude" to latitude,
      "longitude" to longitude,
      "accuracyMeters" to accuracyMeters,
      "notifyOnSchoolDeparture" to notifyOnSchoolDeparture,
      "ghostMode" to ghostMode,
      "appRestrictionsEnabled" to appRestrictionsEnabled,
      "websiteSafetyEnabled" to websiteSafetyEnabled,
      "bedtimeSchedule" to bedtimeSchedule,
      "pairingCode" to pairingCode,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId @get:DocumentId val id: String = "",
  val userId: String = "",
  val memberId: String = "",
  val senderName: String = "",
  val outgoing: Boolean = false,
  val text: String = "",
  val messageType: String = "TEXT", // TEXT, LIVE_LOCATION, STATIC_PIN, VOICE_NOTE, GEOFENCE_EVENT
  val timeLabel: String = "10:30 AM",
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "memberId" to memberId,
      "senderName" to senderName,
      "outgoing" to outgoing,
      "text" to text,
      "messageType" to messageType,
      "timeLabel" to timeLabel,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class SafetyAlert(
  @DocumentId @get:DocumentId val id: String = "",
  val userId: String = "",
  val memberName: String = "",
  val alertType: String = "ARRIVAL", // ARRIVAL, DEPARTURE, OFFLINE, LOW_BATTERY, SCREEN_TIME, BEDTIME, EMERGENCY
  val title: String = "",
  val description: String = "",
  val timeLabel: String = "Just now",
  val read: Boolean = false,
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "memberName" to memberName,
      "alertType" to alertType,
      "title" to title,
      "description" to description,
      "timeLabel" to timeLabel,
      "read" to read,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class SafeZone(
  @DocumentId @get:DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val address: String = "",
  val radiusMeters: Int = 250,
  val latitude: Double = 41.9214,
  val longitude: Double = -87.6513,
  val notifyOnArrival: Boolean = true,
  val notifyOnDeparture: Boolean = true,
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "name" to name,
      "address" to address,
      "radiusMeters" to radiusMeters,
      "latitude" to latitude,
      "longitude" to longitude,
      "notifyOnArrival" to notifyOnArrival,
      "notifyOnDeparture" to notifyOnDeparture,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class UserDevice(
  @DocumentId @get:DocumentId val id: String = "",
  val deviceId: String = "",
  val userId: String = "",
  val name: String = "",
  val model: String = "",
  val online: Boolean = false,
  val batteryLevel: Int = 0,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val accuracyMeters: Int = 10,
  val locationSharingApproved: Boolean = false,
  val cameraSessionApproved: Boolean = false,
  val microphoneApproved: Boolean = false,
  val pairingCode: String = "",
  @ServerTimestamp @get:ServerTimestamp val lastSeenAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(
    uid: String,
    existingCreatedAt: Any = FieldValue.serverTimestamp(),
    existingLastSeenAt: Any = FieldValue.serverTimestamp(),
  ): Map<String, Any> =
    mapOf(
      "deviceId" to deviceId.ifBlank { id },
      "userId" to uid,
      "name" to name,
      "model" to model,
      "online" to online,
      "batteryLevel" to batteryLevel.coerceIn(0, 100),
      "latitude" to latitude,
      "longitude" to longitude,
      "accuracyMeters" to accuracyMeters.coerceIn(1, 5000),
      "locationSharingApproved" to locationSharingApproved,
      "cameraSessionApproved" to cameraSessionApproved,
      "microphoneApproved" to microphoneApproved,
      "pairingCode" to pairingCode,
      "lastSeenAt" to existingLastSeenAt,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class DeviceSession(
  @DocumentId @get:DocumentId val id: String = "",
  val sessionId: String = "",
  val userId: String = "",
  val fromDeviceId: String = "",
  val fromDeviceName: String = "",
  val toDeviceId: String = "",
  val toDeviceName: String = "",
  val sessionType: String = "CAMERA", // LOCATION, CAMERA, VOICE
  val status: String = "REQUESTED", // REQUESTED, APPROVED, REJECTED, ACTIVE, ENDED
  val offerSdp: String = "",
  val answerSdp: String = "",
  val iceCandidatesJson: String = "[]",
  @ServerTimestamp @get:ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp @get:ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toWriteMap(uid: String, existingCreatedAt: Any = FieldValue.serverTimestamp()): Map<String, Any> =
    mapOf(
      "sessionId" to sessionId.ifBlank { id },
      "userId" to uid,
      "fromDeviceId" to fromDeviceId,
      "fromDeviceName" to fromDeviceName,
      "toDeviceId" to toDeviceId,
      "toDeviceName" to toDeviceName,
      "sessionType" to sessionType,
      "status" to status,
      "offerSdp" to offerSdp,
      "answerSdp" to answerSdp,
      "iceCandidatesJson" to iceCandidatesJson,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

object Avatars {
  const val LOGO = ""
  const val SARAH = ""
  const val LUCAS = ""
  const val EMMA = ""
  const val MARCUS = ""
  const val DAVID = ""
  const val MAP_BG =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAHj10GSswJRDUMIMW9caBD1MhyclegrqJEYlGfVNqvG7sEGZJsylt49dV8_sfjlKzG6yHcz_-6hovcLYfeFPIWd74jZpEkX3vOYXT9u8Q32etmPUhvnMl1E__fncPjnVGRpZq3OngsxSE2faElLzmpefT1Mwy0cue4UlCG5DfyKHkZ8zzGAza5CFU2CTPZzUDGbXiLBQtwpG7ktqPzGlVBWIOf9J59511rlj1V-mudQB8wH91cGOBm"
}
