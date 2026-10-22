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
  val displayName: String = "David Miller",
  val email: String = "david.miller@family.org",
  val avatarUrl: String = Avatars.DAVID,
  val statusMessage: String = "Available • Location shared with Family",
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
  val online: Boolean = true,
  val liveSharing: Boolean = false,
  val pinned: Boolean = false,
  val favorite: Boolean = false,
  val batteryLevel: Int = 85,
  val speedMph: Int = 0,
  val locationLabel: String = "Home SafeZone",
  val lastMessageSnippet: String = "Connected to Family Shield",
  val lastActivityTimeLabel: String = "Just now",
  val unreadCount: Int = 0,
  val screenTimeMinutesToday: Int = 110,
  val screenTimeLimitMinutes: Int = 180,
  val latitude: Double = 41.9214,
  val longitude: Double = -87.6513,
  val accuracyMeters: Int = 5,
  val notifyOnSchoolDeparture: Boolean = true,
  val ghostMode: Boolean = false,
  val appRestrictionsEnabled: Boolean = true,
  val websiteSafetyEnabled: Boolean = true,
  val bedtimeSchedule: String = "9:30 PM - 6:30 AM",
  val pairingCode: String = "TV-4021",
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
  val online: Boolean = true,
  val batteryLevel: Int = 100,
  val latitude: Double = 41.9214,
  val longitude: Double = -87.6513,
  val accuracyMeters: Int = 5,
  val locationSharingApproved: Boolean = true,
  val cameraSessionApproved: Boolean = true,
  val microphoneApproved: Boolean = true,
  val pairingCode: String = "TV-8841",
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
  const val LOGO =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCKK4s12E2rIvPeCpON-H1dSTZhvMVU44aqKfFe9LGAje8GCiVe6-KPWHzX162q19oZ9dBRNFTfFdUpsREZZOcfGPlA5EDFmQCEBRG8sQWXC2BwZseOiySRZhhiw6VzAP4ElrEQWtZFRvh1SSD5OGnzUFpy75YHQHZeHuVqcn52nSrUs620mNrHXZgaIxJv-9XXq0Sxx9QptSk4oZvWbtULlTac2TtXlfNCa-B8Znd8aWSjf1lcXLza"
  const val SARAH =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCbhtWSlqO5raSNRq6HMqgfhKzq6ebrqov9AR76_wrx0Ig_FUPWRk0jRdLtvZRCUMi1-9sGPZRqM6zS1wEJH0lsPejmU7VyL4pen5nc2mfAv1FGK-hskX3fteoSZY_ttsm9qIsdeDvKgjFjH-oaZ-M4fQ-sb3YOo35Op7ig1cg_aofpXu9W0l9e1C-px4ycDQPCdUtBGZu5xgtUzFWwUSOGhBNo9NWiEho0DPaUEekTKx1_ha4hJN3X"
  const val LUCAS =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuC1O3o0PrFCszg04rVs_DByshMCvcIGNm3m0NiUPOaUQoiQ7mwzK_29fzXDRNYL2DM4s1u9x0Rxp7fagLUx-vLhhbiRoxhqNxC2gxNWEKxzaKWB0Y9UU_VLnt9EgqzcjgNB1tPqcYhon71Nywrt59N-VosMj_7uSbdtF3_1HRHmMTlyxgT0wJrHokblkZAtZFXBZPUMCr4fjlb7x-fheV5Jqlejh48HcofOIzC-MY4yrRCK1vBZO1HV"
  const val EMMA =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuD9L048gkjgnhHpcYOTgvl6H64J9jmsC0hjVkfPDLYXEzKsrX102GFXGkGas0TJjIvDbdBkoWg-ctbAcSpfe0Ymdc8kg9Tn2tWlYlmxhgBLpOPQYoZ0Q88fh3_PlqWtbKlwp7QcbQC8A1Eavt62M9KsBH3mB-WRgypVPe71vtLiS_7Mf7jNCtuZ1bD1Vz6aiuuiCbQupgKWBxTilnrS4OsDrl0KbYfZrXXT7AF4QqkDqBwj-QVqZ5bp"
  const val MARCUS =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAl_TxrzpB5qijdssz8FsPdgsyI0TGKWS5xKj00kfWYBm5NiKrltUIL6Xybv9fUYs8h_sB4CggVg3_YCnJbQfMjfpzE7sSLjOoN7dhZDlSYsNoLk4qaFKNmB8pCSIH9qkEaCuJjhgahJB_U9Yww-a8v8ys-Fn5J0ROnM7bwo0GSo97-Pq_kqPjrVtsjtTYs5iA4YZ0G-YXCEt35wlYaLl-i5rupLQTXe2dX7YzekuKk-5CW9qIswBPP"
  const val DAVID =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCCNPfzLpRP8EXjG2X7KvlpySCK1s3uUhbN1U2SyeK3uZPwAokLTxqDXiwE2zyk5iL8XGG1fxmE1K-bIZxHq-aXQ6w1EsfpzFt3t1YjfH0-kLOzPQTGqcEI2CWw3vwXbgRR13QJ1ovsldb4KpXe3QS4U67Zv27T4s4YqbUK8IuA67V5mWmO9m4S6qmAgyDBWRNQXwAc78WLdFiJgPsejjHASNgtDHxSTvcKpqWpJ5gtysQXZJPcG5xY"
  const val MAP_BG =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAHj10GSswJRDUMIMW9caBD1MhyclegrqJEYlGfVNqvG7sEGZJsylt49dV8_sfjlKzG6yHcz_-6hovcLYfeFPIWd74jZpEkX3vOYXT9u8Q32etmPUhvnMl1E__fncPjnVGRpZq3OngsxSE2faElLzmpefT1Mwy0cue4UlCG5DfyKHkZ8zzGAza5CFU2CTPZzUDGbXiLBQtwpG7ktqPzGlVBWIOf9J59511rlj1V-mudQB8wH91cGOBm"
}
