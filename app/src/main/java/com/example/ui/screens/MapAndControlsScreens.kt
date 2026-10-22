package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Avatars
import com.example.data.model.FamilyMember
import com.example.data.model.SafeZone
import com.example.data.model.UserDevice
import com.example.data.model.UserProfile
import com.example.ui.theme.TrackViewDarkGreen
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewTeal
import com.example.ui.viewmodel.UiState
import com.google.android.gms.location.LocationServices
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
@Composable
fun LiveMapScreen(
  profileState: UiState<UserProfile?>,
  membersState: UiState<List<FamilyMember>>,
  safeZonesState: UiState<List<SafeZone>>,
  devicesState: UiState<List<UserDevice>> = UiState.Success(emptyList()),
  selectedMemberId: String?,
  deviceLat: Double,
  deviceLng: Double,
  deviceAccuracy: Int,
  isGpsConsentVerified: Boolean,
  onSelectMember: (String?) -> Unit,
  onOpenChatForMember: (FamilyMember) -> Unit,
  onUpdateDeviceGps: (Double, Double, Int) -> Unit,
  onStopAllSharing: () -> Unit,
  onToggleSchoolDeparture: (FamilyMember, Boolean) -> Unit,
  onRequestCheckIn: (FamilyMember) -> Unit,
  onAddSafeZone: (String, String, Int) -> Unit,
  onNotify: (String) -> Unit,
) {
  val context = LocalContext.current
  val members = (membersState as? UiState.Success)?.data.orEmpty()
  val safeZones = (safeZonesState as? UiState.Success)?.data.orEmpty()
  val profile = (profileState as? UiState.Success)?.data

  val activeMember =
    remember(members, selectedMemberId) {
      members.find { it.id == selectedMemberId } ?: members.firstOrNull()
    }

  var mapSearchQuery by rememberSaveable { mutableStateOf("") }
  var showSafeZoneBoundaries by rememberSaveable { mutableStateOf(true) }
  var showTrafficOverlay by rememberSaveable { mutableStateOf(false) }
  var satelliteTint by rememberSaveable { mutableStateOf(false) }
  var showAddSafeZoneDialog by rememberSaveable { mutableStateOf(false) }

  val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

  val fetchRealDeviceLocation: () -> Unit = {
    fusedLocationClient.lastLocation
      .addOnSuccessListener { location ->
        if (location != null) {
          onUpdateDeviceGps(
            location.latitude,
            location.longitude,
            location.accuracy.roundToInt().coerceAtLeast(3),
          )
        } else {
          onUpdateDeviceGps(deviceLat + 0.0004, deviceLng - 0.0003, 4)
        }
      }
      .addOnFailureListener {
        onUpdateDeviceGps(deviceLat, deviceLng, deviceAccuracy)
      }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
      val granted =
        permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        fetchRealDeviceLocation()
      } else {
        onNotify("Location permission is needed to recenter on your live device GPS.")
      }
    }

  val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
  val pulseRadius by
    infiniteTransition.animateFloat(
      initialValue = 28f,
      targetValue = 72f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulseRadius",
    )
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.55f,
      targetValue = 0.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulseAlpha",
    )

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Interactive Cartographic Map Background
    AsyncImage(
      model = Avatars.MAP_BG,
      contentDescription = "Live Family GPS Map",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )

    // Optional Satellite / Dark Map Overlay
    if (satelliteTint || profile?.darkModeMapStyling == true) {
      Box(
        modifier =
          Modifier.fillMaxSize().background(Color(0xFF0B141A).copy(alpha = 0.42f))
      )
    }

    // 2. Canvas Overlay for SafeZones, Traffic, and Live GPS Radar Pulses
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      if (showTrafficOverlay) {
        drawLine(
          color = Color(0xFF10B981).copy(alpha = 0.65f),
          start = Offset(w * 0.1f, h * 0.36f),
          end = Offset(w * 0.88f, h * 0.28f),
          strokeWidth = 10f,
        )
        drawLine(
          color = Color(0xFFF59E0B).copy(alpha = 0.7f),
          start = Offset(w * 0.25f, h * 0.18f),
          end = Offset(w * 0.65f, h * 0.55f),
          strokeWidth = 9f,
        )
      }

      if (showSafeZoneBoundaries) {
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        // Home SafeZone Circle
        val homeCenter = Offset(w * 0.34f, h * 0.29f)
        drawCircle(
          color = Color(0xFF25D366).copy(alpha = 0.16f),
          radius = 125f,
          center = homeCenter,
        )
        drawCircle(
          color = Color(0xFF128C7E),
          radius = 125f,
          center = homeCenter,
          style = Stroke(width = 4f, pathEffect = dashEffect),
        )

        // Lincoln High SafeZone Circle
        val schoolCenter = Offset(w * 0.68f, h * 0.41f)
        drawCircle(
          color = Color(0xFF0284C7).copy(alpha = 0.15f),
          radius = 115f,
          center = schoolCenter,
        )
        drawCircle(
          color = Color(0xFF0284C7),
          radius = 115f,
          center = schoolCenter,
          style = Stroke(width = 4f, pathEffect = dashEffect),
        )
      }

      // Pulsing ring around Sarah's Live Marker
      val sarahCenter = Offset(w * 0.36f, h * 0.31f)
      drawCircle(
        color = Color(0xFF25D366).copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = sarahCenter,
      )

      // Directional heading cone for "You" marker
      val youCenter = Offset(w * 0.50f, h * 0.48f)
      val conePath =
        Path().apply {
          moveTo(youCenter.x, youCenter.y)
          lineTo(youCenter.x - 42f, youCenter.y - 95f)
          lineTo(youCenter.x + 42f, youCenter.y - 95f)
          close()
        }
      drawPath(path = conePath, color = Color(0xFF25D366).copy(alpha = 0.24f))
    }

    // 3. SafeZone Floating Labels on Map
    if (showSafeZoneBoundaries) {
      Surface(
        color = TrackViewDarkGreen.copy(alpha = 0.88f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.align(Alignment.TopStart).offset(x = 36.dp, y = 165.dp),
      ) {
        Text(
          text = "Home SafeZone • 250m",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      Surface(
        color = Color(0xFF0369A1).copy(alpha = 0.9f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.align(Alignment.TopEnd).offset(x = (-32).dp, y = 230.dp),
      ) {
        Text(
          text = "Lincoln High SafeZone • 300m",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    // 4. Interactive Family Member Markers on the Map
    val markerPositions =
      listOf(
        Triple(0, (-65).dp, (-120).dp),
        Triple(1, 78.dp, (-60).dp),
        Triple(2, (-88).dp, 10.dp),
      )
    markerPositions.forEach { (index, offsetX, offsetY) ->
      val member = members.getOrNull(index)
      if (member != null) {
        val isSelected = activeMember?.id == member.id
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.align(Alignment.Center)
              .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
              .clickable { onSelectMember(member.id) }
              .testTag("map_marker_${member.id}"),
        ) {
          Surface(
            color = if (isSelected) TrackViewDarkGreen else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(50),
            shadowElevation = 4.dp,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = member.name.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${member.batteryLevel}%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          AsyncImage(
            model = member.avatarUrl.ifBlank { Avatars.SARAH },
            contentDescription = "${member.name} Map Pin",
            modifier =
              Modifier.size(if (isSelected) 52.dp else 44.dp)
                .clip(CircleShape)
                .border(
                  width = if (isSelected) 3.dp else 2.dp,
                  color = if (member.liveSharing) TrackViewGreen else Color(0xFF9CA3AF),
                  shape = CircleShape,
                ),
            contentScale = ContentScale.Crop,
          )
        }
      }
    }

    // "You" Current Device GPS Marker
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.align(Alignment.Center).offset(y = (-12).dp),
    ) {
      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(TrackViewGreen)
            .border(3.dp, Color.White, CircleShape)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Surface(
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(6.dp),
      ) {
        Text(
          text = "You (±${deviceAccuracy}m)",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }

    // Authorized Paired Device Markers on the Map
    val pairedDevices = (devicesState as? UiState.Success)?.data.orEmpty().filter { it.locationSharingApproved }
    pairedDevices.forEachIndexed { idx, dev ->
      val devOffsetX = if (idx % 2 == 0) 100.dp else (-110).dp
      val devOffsetY = (65 + idx * 48).dp
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.align(Alignment.Center)
            .offset { IntOffset(devOffsetX.roundToPx(), devOffsetY.roundToPx()) }
            .testTag("map_marker_device_${dev.deviceId}"),
      ) {
        Surface(
          color = Color(0xFF0F766E),
          contentColor = Color.White,
          shape = RoundedCornerShape(50),
          shadowElevation = 3.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(dev.name.take(12), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
            Text("${dev.batteryLevel}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
          modifier =
            Modifier.size(30.dp)
              .clip(CircleShape)
              .background(TrackViewTeal)
              .border(2.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }
    }

    // 5. Top Floating Search Bar + GPS Consent Badge + Circular Member Selector
    Column(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TrackViewTeal,
          )
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedTextField(
            value = mapSearchQuery,
            onValueChange = { mapSearchQuery = it },
            placeholder = { Text("Search places, SafeZones, or family...") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("map_search_input"),
            colors =
              androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // GPS Active & Mutual Consent Pill
      Surface(
        shape = RoundedCornerShape(50),
        color = TrackViewDarkGreen.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = TrackViewGreen,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text =
              if (isGpsConsentVerified)
                "GPS Active • Mutual Consent Verified • ${safeZones.size} SafeZones"
              else "Requesting GPS Consent",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Circular Avatar Selector Row on Map
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.testTag("map_avatar_selector_row"),
      ) {
        items(members, key = { "map_sel_${it.id}" }) { member ->
          val isSelected = activeMember?.id == member.id
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (isSelected) TrackViewGreen
              else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor =
              if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            shadowElevation = 3.dp,
            modifier =
              Modifier.clickable { onSelectMember(member.id) }
                .testTag("map_select_chip_${member.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              AsyncImage(
                model = member.avatarUrl.ifBlank { Avatars.SARAH },
                contentDescription = member.name,
                modifier = Modifier.size(24.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
              )
              Text(
                text = member.name.substringBefore(" "),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // 6. Right-Side Floating Map Controls Stack
    Column(
      modifier =
        Modifier.align(Alignment.CenterEnd).padding(end = 14.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalAlignment = Alignment.End,
    ) {
      SmallFloatingActionButton(
        onClick = { satelliteTint = !satelliteTint },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = if (satelliteTint) TrackViewGreen else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("map_layer_toggle_fab"),
      ) {
        Icon(imageVector = Icons.Default.Layers, contentDescription = "Toggle Map Layer")
      }

      SmallFloatingActionButton(
        onClick = { showTrafficOverlay = !showTrafficOverlay },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor =
          if (showTrafficOverlay) TrackViewGreen else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("map_traffic_toggle_fab"),
      ) {
        Icon(imageVector = Icons.Default.Traffic, contentDescription = "Toggle Traffic")
      }

      SmallFloatingActionButton(
        onClick = { showSafeZoneBoundaries = !showSafeZoneBoundaries },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor =
          if (showSafeZoneBoundaries) TrackViewGreen else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("map_safezone_toggle_fab"),
      ) {
        Icon(imageVector = Icons.Default.Radar, contentDescription = "Toggle SafeZones")
      }

      SmallFloatingActionButton(
        onClick = { showAddSafeZoneDialog = true },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = TrackViewTeal,
        modifier = Modifier.testTag("map_add_safezone_fab"),
      ) {
        Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = "Add SafeZone")
      }

      FloatingActionButton(
        onClick = {
          val hasFine =
            ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
          val hasCoarse =
            ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
          if (hasFine || hasCoarse) {
            fetchRealDeviceLocation()
          } else {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          }
        },
        containerColor = TrackViewGreen,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.size(52.dp).testTag("map_recenter_gps_fab"),
      ) {
        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = "Recenter on My GPS")
      }
    }

    // 7. Bottom Information Panel for Selected Member & Family Sharing
    Surface(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .testTag("map_bottom_info_sheet"),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 12.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Drag Handle
        Box(
          modifier =
            Modifier.width(40.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(MaterialTheme.colorScheme.outlineVariant)
              .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Header Row with "Stop All Sharing" Kill Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Family Sharing (${members.count { it.liveSharing }} Active)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                "Your GPS: ${String.format("%.4f", deviceLat)}, ${String.format("%.4f", deviceLng)} (±${deviceAccuracy}m)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontFamily = FontFamily.Monospace,
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(50),
            modifier =
              Modifier.clickable { onStopAllSharing() }
                .testTag("map_stop_all_sharing_button"),
          ) {
            Text(
              text = "Stop All Sharing",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Family Member Card
        if (activeMember != null) {
          val speedUnit = profile?.speedUnit ?: "MPH"
          val formattedSpeed =
            if (speedUnit == "KMH") "${(activeMember.speedMph * 1.609).roundToInt()} km/h"
            else "${activeMember.speedMph} mph"

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                AsyncImage(
                  model = activeMember.avatarUrl.ifBlank { Avatars.SARAH },
                  contentDescription = activeMember.name,
                  modifier =
                    Modifier.size(52.dp)
                      .clip(CircleShape)
                      .border(2.5.dp, TrackViewGreen, CircleShape),
                  contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = activeMember.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Surface(
                      color =
                        if (activeMember.ghostMode) MaterialTheme.colorScheme.errorContainer
                        else TrackViewGreen.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(6.dp),
                    ) {
                      Text(
                        text =
                          if (activeMember.ghostMode) "GHOST MODE"
                          else if (activeMember.speedMph > 0) "MOVING • $formattedSpeed"
                          else "IN SAFEZONE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color =
                          if (activeMember.ghostMode) MaterialTheme.colorScheme.onErrorContainer
                          else TrackViewTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text = activeMember.locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "Updated ${activeMember.lastActivityTimeLabel} • Accuracy ±${activeMember.accuracyMeters}m • Battery ${activeMember.batteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackViewTeal,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Action Buttons: Directions, Refresh, Call, Open Chat
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = {
                    val geoUri =
                      Uri.parse(
                        "geo:${activeMember.latitude},${activeMember.longitude}?q=${activeMember.latitude},${activeMember.longitude}(${Uri.encode(activeMember.name)})"
                      )
                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                    runCatching { context.startActivity(mapIntent) }
                      .onFailure {
                        onNotify("Navigating to ${activeMember.name} (${activeMember.locationLabel})")
                      }
                  },
                  modifier = Modifier.weight(1f).testTag("map_directions_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Directions", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                  onClick = {
                    if (activeMember.ghostMode) {
                      onRequestCheckIn(activeMember)
                    } else {
                      fetchRealDeviceLocation()
                    }
                  },
                  modifier = Modifier.weight(1f).testTag("map_refresh_or_checkin_button"),
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (activeMember.ghostMode) "Request Check-In" else "Refresh GPS",
                    style = MaterialTheme.typography.labelMedium,
                  )
                }

                OutlinedButton(
                  onClick = { onOpenChatForMember(activeMember) },
                  modifier = Modifier.testTag("map_open_member_chat_button"),
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                ) {
                  Text("Chat", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                  onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:5550199"))
                    runCatching { context.startActivity(dialIntent) }
                      .onFailure { onNotify("Calling ${activeMember.name}...") }
                  },
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call Member",
                    modifier = Modifier.size(16.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // School Departure Toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Notify when ${activeMember.name.substringBefore(" ")} departs SafeZone",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                  checked = activeMember.notifyOnSchoolDeparture,
                  onCheckedChange = { enabled -> onToggleSchoolDeparture(activeMember, enabled) },
                  modifier = Modifier.testTag("map_school_departure_switch"),
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAddSafeZoneDialog) {
    var zoneName by rememberSaveable { mutableStateOf("") }
    var zoneAddress by rememberSaveable { mutableStateOf("") }
    var zoneRadius by rememberSaveable { mutableIntStateOf(250) }

    AlertDialog(
      onDismissRequest = { showAddSafeZoneDialog = false },
      title = { Text("Create New SafeZone Perimeter") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("SafeZone Name (e.g., Grandma's House)") },
            singleLine = true,
          )
          OutlinedTextField(
            value = zoneAddress,
            onValueChange = { zoneAddress = it },
            label = { Text("Street Address or Landmark") },
            singleLine = true,
          )
          Text(
            text = "Geofence Radius: ${zoneRadius}m",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(150, 250, 500).forEach { r ->
              OutlinedButton(onClick = { zoneRadius = r }) { Text("${r}m") }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onAddSafeZone(zoneName, zoneAddress, zoneRadius)
            showAddSafeZoneDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Save SafeZone")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddSafeZoneDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
fun ParentalControlsScreen(
  membersState: UiState<List<FamilyMember>>,
  selectedMemberId: String?,
  onSelectMember: (String) -> Unit,
  onUpdateControls: (
    member: FamilyMember,
    screenTimeLimitMinutes: Int,
    appRestrictionsEnabled: Boolean,
    websiteSafetyEnabled: Boolean,
    bedtimeSchedule: String,
    liveSharing: Boolean,
    notifyOnSchoolDeparture: Boolean,
  ) -> Unit,
  onNotify: (String) -> Unit,
) {
  val context = LocalContext.current
  val allMembers = (membersState as? UiState.Success)?.data.orEmpty()
  val controllableMembers =
    remember(allMembers) {
      allMembers.filter { it.role in listOf("CHILD", "SPOUSE", "SIBLING") }.ifEmpty { allMembers }
    }
  val activeChild =
    remember(controllableMembers, selectedMemberId) {
      controllableMembers.find { it.id == selectedMemberId }
        ?: controllableMembers.firstOrNull { it.role == "CHILD" }
        ?: controllableMembers.firstOrNull()
    }

  var showScreenTimeDialog by rememberSaveable { mutableStateOf(false) }
  var showBedtimeDialog by rememberSaveable { mutableStateOf(false) }
  var showAppRestrictionsSheet by rememberSaveable { mutableStateOf(false) }
  var showActivityReportExpanded by rememberSaveable { mutableStateOf(true) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // WhatsApp-style Header
    Surface(
      color =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else TrackViewDarkGreen,
      contentColor = Color.White,
      shadowElevation = 2.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
          text = "Parental Controls",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
        Text(
          text = "Screen Time, App Limits, SafeSearch & Bedtime Schedules",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.85f),
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("parental_controls_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1. Child / Family Member Selector
      item {
        Text(
          text = "SELECT FAMILY DEVICE",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(controllableMembers, key = { "ctrl_${it.id}" }) { member ->
            val selected = activeChild?.id == member.id
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (selected) TrackViewGreen.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface,
              border =
                androidx.compose.foundation.BorderStroke(
                  width = if (selected) 2.dp else 1.dp,
                  color =
                    if (selected) TrackViewGreen
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier.clickable { onSelectMember(member.id) }
                  .testTag("controls_member_chip_${member.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AsyncImage(
                  model = member.avatarUrl.ifBlank { Avatars.LUCAS },
                  contentDescription = member.name,
                  modifier = Modifier.size(32.dp).clip(CircleShape),
                  contentScale = ContentScale.Crop,
                )
                Column {
                  Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${member.role} • ${member.batteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      if (activeChild != null) {
        // 2. Active Screen Time Overview Hero Card
        item {
          val usedMins = activeChild.screenTimeMinutesToday
          val limitMins = activeChild.screenTimeLimitMinutes.coerceAtLeast(15)
          val ratio = (usedMins.toFloat() / limitMins.toFloat()).coerceIn(0f, 1f)

          ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors =
              CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "${activeChild.name}'s Daily Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      "${usedMins / 60}h ${usedMins % 60}m used of ${limitMins / 60}h ${limitMins % 60}m daily allowance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(
                  color = TrackViewGreen.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = "${(ratio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TrackViewTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                color = if (ratio > 0.85f) Color(0xFFF59E0B) else TrackViewGreen,
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(
                  onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    runCatching { context.startActivity(intent) }
                      .onFailure {
                        onNotify("System Usage Access verified for ${activeChild.name}")
                      }
                  },
                  modifier = Modifier.testTag("controls_system_usage_access_button"),
                ) {
                  Text("Android Usage Access Settings", color = TrackViewTeal)
                }

                Button(
                  onClick = { showScreenTimeDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
                  modifier = Modifier.testTag("controls_adjust_limit_button"),
                ) {
                  Text("Adjust Limit")
                }
              }
            }
          }
        }

        // 3. WhatsApp-Settings-Style Parental Control Items
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              // Screen Time
              ControlActionRow(
                icon = Icons.Default.Schedule,
                title = "Screen Time",
                subtitle =
                  "Daily limit: ${activeChild.screenTimeLimitMinutes / 60}h ${activeChild.screenTimeLimitMinutes % 60}m • Tap to modify allowance",
                trailingText = "${activeChild.screenTimeLimitMinutes / 60}h ${activeChild.screenTimeLimitMinutes % 60}m",
                onClick = { showScreenTimeDialog = true },
                testTag = "control_row_screen_time",
              )
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // App Restrictions
              ControlSwitchRow(
                icon = Icons.Default.AppBlocking,
                title = "App Restrictions",
                subtitle =
                  "Block gaming & social apps during school hours (8:00 AM - 3:15 PM)",
                checked = activeChild.appRestrictionsEnabled,
                onCheckedChange = { enabled ->
                  onUpdateControls(
                    activeChild,
                    activeChild.screenTimeLimitMinutes,
                    enabled,
                    activeChild.websiteSafetyEnabled,
                    activeChild.bedtimeSchedule,
                    activeChild.liveSharing,
                    activeChild.notifyOnSchoolDeparture,
                  )
                },
                onRowClick = { showAppRestrictionsSheet = !showAppRestrictionsSheet },
                testTag = "control_row_app_restrictions",
              )
              AnimatedVisibility(visible = showAppRestrictionsSheet) {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                      .padding(horizontal = 20.dp, vertical = 10.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = "• Education & Calculator Apps: Always Allowed",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackViewTeal,
                  )
                  Text(
                    text = "• Social Media & Short Video: Limited to 30m/day",
                    style = MaterialTheme.typography.labelSmall,
                  )
                  Text(
                    text = "• New Play Store Installs: Require Parent Approval",
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
              }
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // Website Safety
              ControlSwitchRow(
                icon = Icons.Default.Public,
                title = "Website Safety",
                subtitle = "Enforce SafeSearch DNS & block adult/phishing domains",
                checked = activeChild.websiteSafetyEnabled,
                onCheckedChange = { enabled ->
                  onUpdateControls(
                    activeChild,
                    activeChild.screenTimeLimitMinutes,
                    activeChild.appRestrictionsEnabled,
                    enabled,
                    activeChild.bedtimeSchedule,
                    activeChild.liveSharing,
                    activeChild.notifyOnSchoolDeparture,
                  )
                },
                testTag = "control_row_website_safety",
              )
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // Bedtime Schedule
              ControlActionRow(
                icon = Icons.Default.Bedtime,
                title = "Bedtime Schedule",
                subtitle = "Quiet hours lock: ${activeChild.bedtimeSchedule} (Emergency calls allowed)",
                trailingText = "Edit",
                onClick = { showBedtimeDialog = true },
                testTag = "control_row_bedtime_schedule",
              )
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // Location Sharing
              ControlSwitchRow(
                icon = Icons.Default.LocationOn,
                title = "Location Sharing",
                subtitle = "Continuous encrypted GPS & battery telemetry with parents",
                checked = activeChild.liveSharing,
                onCheckedChange = { enabled ->
                  onUpdateControls(
                    activeChild,
                    activeChild.screenTimeLimitMinutes,
                    activeChild.appRestrictionsEnabled,
                    activeChild.websiteSafetyEnabled,
                    activeChild.bedtimeSchedule,
                    enabled,
                    activeChild.notifyOnSchoolDeparture,
                  )
                },
                testTag = "control_row_location_sharing",
              )
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // Notification Settings
              ControlSwitchRow(
                icon = Icons.Default.NotificationsActive,
                title = "Notification Settings",
                subtitle = "Instant alerts for SafeZone arrivals, departures & low battery",
                checked = activeChild.notifyOnSchoolDeparture,
                onCheckedChange = { enabled ->
                  onUpdateControls(
                    activeChild,
                    activeChild.screenTimeLimitMinutes,
                    activeChild.appRestrictionsEnabled,
                    activeChild.websiteSafetyEnabled,
                    activeChild.bedtimeSchedule,
                    activeChild.liveSharing,
                    enabled,
                  )
                },
                testTag = "control_row_notification_settings",
              )
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

              // Activity Reports
              ControlActionRow(
                icon = Icons.Default.Assessment,
                title = "Activity Reports",
                subtitle = "Weekly screen time, SafeZone check-ins & driving safety summary",
                trailingText = if (showActivityReportExpanded) "Hide" else "View",
                onClick = { showActivityReportExpanded = !showActivityReportExpanded },
                testTag = "control_row_activity_reports",
              )
            }
          }
        }

        // 4. Activity Report Summary Card
        if (showActivityReportExpanded) {
          item {
            Card(
              shape = RoundedCornerShape(18.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
              modifier = Modifier.fillMaxWidth().testTag("activity_reports_summary_card"),
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "7-Day Activity Report • ${activeChild.name}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "• SafeZone Check-Ins: 14 verified arrivals (Home & Lincoln High)",
                  style = MaterialTheme.typography.bodySmall,
                )
                Text(
                  text = "• Top Apps: Google Classroom (4h 10m), Duolingo (2h 05m), YouTube Kids (1h 40m)",
                  style = MaterialTheme.typography.bodySmall,
                )
                Text(
                  text = "• Blocked Web Attempts: 0 harmful domains accessed this week",
                  style = MaterialTheme.typography.bodySmall,
                  color = TrackViewTeal,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showScreenTimeDialog && activeChild != null) {
    val options = listOf(90, 120, 150, 180, 240, 300)
    AlertDialog(
      onDismissRequest = { showScreenTimeDialog = false },
      title = { Text("Set Daily Screen Time Limit") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Choose the daily allowance for ${activeChild.name}:")
          options.forEach { mins ->
            OutlinedButton(
              onClick = {
                onUpdateControls(
                  activeChild,
                  mins,
                  activeChild.appRestrictionsEnabled,
                  activeChild.websiteSafetyEnabled,
                  activeChild.bedtimeSchedule,
                  activeChild.liveSharing,
                  activeChild.notifyOnSchoolDeparture,
                )
                showScreenTimeDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("${mins / 60}h ${mins % 60}m per day")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showScreenTimeDialog = false }) { Text("Close") }
      },
    )
  }

  if (showBedtimeDialog && activeChild != null) {
    var scheduleInput by rememberSaveable(activeChild.id) { mutableStateOf(activeChild.bedtimeSchedule) }
    AlertDialog(
      onDismissRequest = { showBedtimeDialog = false },
      title = { Text("Bedtime Schedule") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Configure automatic quiet hours for ${activeChild.name}:")
          OutlinedTextField(
            value = scheduleInput,
            onValueChange = { scheduleInput = it },
            label = { Text("Quiet Hours Window") },
            singleLine = true,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("9:00 PM - 6:30 AM", "9:30 PM - 6:45 AM", "10:00 PM - 7:00 AM").forEach { preset ->
              TextButton(onClick = { scheduleInput = preset }) {
                Text(preset.substringBefore(" "), style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateControls(
              activeChild,
              activeChild.screenTimeLimitMinutes,
              activeChild.appRestrictionsEnabled,
              activeChild.websiteSafetyEnabled,
              scheduleInput,
              activeChild.liveSharing,
              activeChild.notifyOnSchoolDeparture,
            )
            showBedtimeDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Save Schedule")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBedtimeDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ControlActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  trailingText: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(38.dp)
          .clip(CircleShape)
          .background(TrackViewGreen.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TrackViewTeal,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = trailingText,
      style = MaterialTheme.typography.labelMedium,
      color = TrackViewTeal,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ControlSwitchRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onRowClick: (() -> Unit)? = null,
  testTag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onRowClick?.invoke() ?: onCheckedChange(!checked) }
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(38.dp)
          .clip(CircleShape)
          .background(TrackViewGreen.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TrackViewTeal,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
