package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FamLinkLocationService : Service() {

  companion object {
    private const val TAG = "FamLinkLocationService"
    const val CHANNEL_ID = "famlink_location_channel"
    const val NOTIFICATION_ID = 9011
    const val ACTION_START = "com.example.service.ACTION_START"
    const val ACTION_STOP = "com.example.service.ACTION_STOP"
    const val PREFS_NAME = "famlink_service_prefs"
    const val KEY_LOCATION_SHARING_ENABLED = "is_location_sharing_enabled"

    fun start(context: Context) {
      val intent = Intent(context, FamLinkLocationService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putBoolean(KEY_LOCATION_SHARING_ENABLED, true)
          .apply()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start location service", e)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, FamLinkLocationService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.startService(intent)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putBoolean(KEY_LOCATION_SHARING_ENABLED, false)
          .apply()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop location service", e)
      }
    }

    fun isEnabled(context: Context): Boolean {
      return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_LOCATION_SHARING_ENABLED, false)
    }
  }

  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private var locationCallback: LocationCallback? = null

  override fun onCreate() {
    super.onCreate()
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopLocationUpdates()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildForegroundNotification("Locating device for Family Circle...")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    startLocationUpdates()
    return START_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "FamLink Location Sharing",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Maintains persistent, visible location sharing with authorized family members."
          setShowBadge(false)
        }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(contentText: String): Notification {
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val stopIntent =
      Intent(this, FamLinkLocationService::class.java).apply {
        action = ACTION_STOP
      }
    val stopPendingIntent =
      PendingIntent.getService(
        this,
        1,
        stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("FamLink Active Location Sharing")
      .setContentText(contentText)
      .setSmallIcon(android.R.drawable.ic_menu_mylocation)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .addAction(android.R.drawable.ic_delete, "Stop Sharing", stopPendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  private fun startLocationUpdates() {
    if (
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      Log.w(TAG, "Location permissions missing for foreground service")
      return
    }

    val locationRequest =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
        .setMinUpdateDistanceMeters(5f)
        .setMinUpdateIntervalMillis(10000L)
        .build()

    locationCallback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          val lastLocation = result.lastLocation ?: return
          onNewLocation(lastLocation)
        }
      }

    try {
      fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback!!,
        Looper.getMainLooper(),
      )
    } catch (e: SecurityException) {
      Log.e(TAG, "SecurityException requesting location updates", e)
    }
  }

  private fun stopLocationUpdates() {
    locationCallback?.let {
      try {
        fusedLocationClient.removeLocationUpdates(it)
      } catch (e: Exception) {
        Log.e(TAG, "Error removing location updates", e)
      }
      locationCallback = null
    }
  }

  private fun onNewLocation(location: Location) {
    // 1. Update notification text
    val notification =
      buildForegroundNotification(
        "Live GPS Active • Accuracy: ±${location.accuracy.toInt()}m",
      )
    val manager = getSystemService(NotificationManager::class.java)
    manager?.notify(NOTIFICATION_ID, notification)

    // 2. Read real battery percentage
    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val batteryLevel =
      batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85

    // 3. Save locally in SharedPreferences
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val deviceId = prefs.getString("current_device_id", null) ?: "device_primary"

    prefs.edit()
      .putFloat("last_lat", location.latitude.toFloat())
      .putFloat("last_lng", location.longitude.toFloat())
      .putFloat("last_acc", location.accuracy)
      .putLong("last_time", System.currentTimeMillis())
      .apply()

    // 4. Update Firestore for this authenticated user and device
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    try {
      val databaseId = getString(R.string.firestore_database_id)
      val db = FirebaseFirestore.getInstance(databaseId)
      val deviceRef =
        db.collection("users")
          .document(currentUser.uid)
          .collection("devices")
          .document(deviceId)

      deviceRef.update(
        mapOf(
          "latitude" to location.latitude,
          "longitude" to location.longitude,
          "accuracyMeters" to location.accuracy.toInt().coerceIn(1, 5000),
          "batteryLevel" to batteryLevel.coerceIn(0, 100),
          "online" to true,
          "lastSeenAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).addOnFailureListener { e ->
        Log.w(TAG, "Failed to sync device location to Firestore: ${e.message}")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error syncing location", e)
    }
  }

  override fun onDestroy() {
    stopLocationUpdates()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
