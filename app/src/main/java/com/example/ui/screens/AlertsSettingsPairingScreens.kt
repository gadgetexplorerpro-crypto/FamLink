package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import com.example.ui.auth.LegalAndPrivacyDialog
import com.example.ui.auth.LegalTab
import com.example.ui.auth.TRACKVIEW_DELETE_ACCOUNT_URL
import com.example.ui.auth.TRACKVIEW_PRIVACY_URL
import com.example.ui.auth.TRACKVIEW_TERMS_URL
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Avatars
import com.example.data.model.SafeZone
import com.example.data.model.SafetyAlert
import com.example.data.model.UserProfile
import com.example.ui.theme.TrackViewDarkGreen
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewTeal
import com.example.ui.viewmodel.UiState

@Composable
fun AlertsScreen(
  alertsState: UiState<List<SafetyAlert>>,
  onMarkAlertRead: (SafetyAlert) -> Unit,
  onMarkAllRead: () -> Unit,
  onTriggerCheckIn: () -> Unit,
  onTriggerEmergencySos: () -> Unit,
) {
  var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
  val alerts = (alertsState as? UiState.Success)?.data.orEmpty()

  val filteredAlerts =
    remember(alerts, selectedCategory) {
      alerts.filter { alert ->
        when (selectedCategory) {
          "UNREAD" -> !alert.read
          "SAFEZONE" -> alert.alertType in listOf("ARRIVAL", "DEPARTURE")
          "DEVICE" -> alert.alertType in listOf("LOW_BATTERY", "OFFLINE", "SCREEN_TIME", "BEDTIME")
          "EMERGENCY" -> alert.alertType == "EMERGENCY"
          else -> true
        }
      }
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Header
    Surface(
      color =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else TrackViewDarkGreen,
      contentColor = Color.White,
      shadowElevation = 2.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Safety Alerts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Text(
            text = "${alerts.count { !it.read }} unread • Real-time SafeZone & device logs",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }

        TextButton(
          onClick = onMarkAllRead,
          modifier = Modifier.testTag("alerts_mark_all_read_button"),
        ) {
          Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = null,
            tint = TrackViewGreen,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Mark All Read",
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("alerts_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Quick Emergency & Check-In Action Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onTriggerCheckIn,
            modifier = Modifier.weight(1f).testTag("alerts_send_checkin_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SafeZone Check-In", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onTriggerEmergencySos,
            modifier = Modifier.weight(1f).testTag("alerts_emergency_sos_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
              ),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Emergency,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SOS Broadcast", fontWeight = FontWeight.Bold)
          }
        }
      }

      // Filter Chips
      item {
        val categories =
          listOf(
            "ALL" to "All Alerts",
            "UNREAD" to "Unread",
            "SAFEZONE" to "Arrivals & Departures",
            "DEVICE" to "Battery & Screen Time",
            "EMERGENCY" to "Emergency",
          )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(categories, key = { it.first }) { (key, label) ->
            val selected = selectedCategory == key
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = key },
              label = { Text(label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TrackViewGreen.copy(alpha = 0.2f),
                  selectedLabelColor = TrackViewTeal,
                ),
              modifier = Modifier.testTag("alert_filter_${key.lowercase()}"),
            )
          }
        }
      }

      // Notification Cards
      items(filteredAlerts, key = { it.id.ifBlank { it.title + it.timeLabel } }) { alert ->
        SafetyAlertCard(
          alert = alert,
          onClick = { onMarkAlertRead(alert) },
        )
      }
    }
  }
}

@Composable
private fun SafetyAlertCard(
  alert: SafetyAlert,
  onClick: () -> Unit,
) {
  val (icon, tint) =
    when (alert.alertType) {
      "ARRIVAL" -> Icons.Default.CheckCircle to TrackViewGreen
      "DEPARTURE" -> Icons.Default.LocationOn to TrackViewTeal
      "LOW_BATTERY" -> Icons.Default.BatteryAlert to Color(0xFFF59E0B)
      "OFFLINE" -> Icons.Default.LocationOff to Color(0xFF6B7280)
      "SCREEN_TIME" -> Icons.Default.Schedule to Color(0xFF0284C7)
      "BEDTIME" -> Icons.Default.Bedtime to Color(0xFF7C3AED)
      "EMERGENCY" -> Icons.Default.Warning to Color(0xFFEF4444)
      else -> Icons.Default.NotificationsActive to TrackViewTeal
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag("alert_card_${alert.id}"),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (!alert.read) MaterialTheme.colorScheme.surface
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (!alert.read) 2.dp else 0.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(42.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = alert.alertType, tint = tint)
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = alert.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (!alert.read) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = alert.timeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!alert.read) {
              Box(
                modifier = Modifier.size(9.dp).clip(CircleShape).background(TrackViewGreen)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "${alert.memberName} • ${alert.alertType}",
          style = MaterialTheme.typography.labelSmall,
          color = tint,
          fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = alert.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun SettingsScreen(
  profileState: UiState<UserProfile?>,
  safeZonesState: UiState<List<SafeZone>>,
  exportedJsonData: String?,
  onToggleEmergencyGhostMode: () -> Unit,
  onToggleShareBattery: () -> Unit,
  onToggleDarkModeMap: () -> Unit,
  onToggleSpeedUnit: () -> Unit,
  onOpenPairingModal: () -> Unit,
  onOpenPremiumModal: () -> Unit,
  onExportData: () -> Unit,
  onClearExportedJson: () -> Unit,
  onDeleteAccount: () -> Unit,
  onSignOut: () -> Unit,
  onNotify: (String) -> Unit,
  onOpenDevicesTab: (() -> Unit)? = null,
  onUploadProfilePhoto: ((Uri) -> Unit)? = null,
  onUpdateDisplayName: ((String) -> Unit)? = null,
) {
  val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
  val safeZones = (safeZonesState as? UiState.Success)?.data.orEmpty()
  val context = LocalContext.current

  var activeLegalDialogTab by remember { mutableStateOf<LegalTab?>(null) }
  var showQrDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
  var showEditNameDialog by rememberSaveable { mutableStateOf(false) }
  var showBgLocationRationale by rememberSaveable { mutableStateOf(false) }
  var bgServiceEnabled by rememberSaveable {
    mutableStateOf(com.example.service.FamLinkLocationService.isEnabled(context))
  }
  val familyInviteCode = remember(profile.userId) {
    "FL-" + (profile.userId.takeLast(4).uppercase().ifBlank { "1000" })
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
      if (uri != null) {
        onUploadProfilePhoto?.invoke(uri)
      }
    }

  val backgroundLocationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
      com.example.service.FamLinkLocationService.start(context)
      bgServiceEnabled = true
      onNotify("Persistent background location sharing enabled")
    }

  val foregroundLocationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
      val granted =
        perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
          com.example.service.FamLinkLocationService.start(context)
          bgServiceEnabled = true
          onNotify("Persistent background location sharing enabled")
        }
      } else {
        onNotify("Location permission is required for background location sharing")
      }
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      onNotify(
        if (granted) "Push notifications enabled for SafeZone & SOS alerts"
        else "Notification permission declined"
      )
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Header
    Surface(
      color =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else TrackViewDarkGreen,
      contentColor = Color.White,
      shadowElevation = 2.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Settings & Privacy",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Text(
            text = "Account, Location Controls, Encryption & Subscription",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
        IconButton(
          onClick = { showQrDialog = true },
          modifier = Modifier.testTag("settings_qr_code_button"),
        ) {
          Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "Show Family Pairing QR Code",
            tint = Color.White,
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("settings_scroll_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. WhatsApp-style Profile Card with Quick Account & Privacy Actions
      item {
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
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Box(
                contentAlignment = Alignment.BottomEnd,
                modifier =
                  Modifier.size(68.dp)
                    .clickable {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    }
                    .testTag("settings_profile_avatar_upload"),
              ) {
                if (profile.avatarUrl.isNotBlank()) {
                  AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Profile Avatar",
                    modifier =
                      Modifier.size(64.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, TrackViewGreen, CircleShape),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Box(
                    modifier =
                      Modifier.size(64.dp)
                        .clip(CircleShape)
                        .background(TrackViewGreen.copy(alpha = 0.16f))
                        .border(2.5.dp, TrackViewGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = "Upload Profile Avatar",
                      tint = TrackViewTeal,
                      modifier = Modifier.size(34.dp),
                    )
                  }
                }
                Box(
                  modifier =
                    Modifier.size(24.dp)
                      .clip(CircleShape)
                      .background(TrackViewGreen)
                      .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Upload Photo",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = profile.displayName.ifBlank { "FamLink User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Surface(
                    color = TrackViewGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = profile.planTier,
                      style = MaterialTheme.typography.labelSmall,
                      color = TrackViewTeal,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
                Text(
                  text = profile.email,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = profile.statusMessage,
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (profile.ghostModeEnabled) MaterialTheme.colorScheme.error
                    else TrackViewTeal,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              IconButton(onClick = { showQrDialog = true }) {
                Icon(
                  imageVector = Icons.Default.QrCode2,
                  contentDescription = "Family QR",
                  tint = TrackViewTeal,
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upload Photo & Edit Name Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                modifier = Modifier.weight(1f).testTag("profile_upload_photo_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.PhotoCamera,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Upload Photo",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }

              OutlinedButton(
                onClick = { showEditNameDialog = true },
                modifier = Modifier.weight(1f).testTag("profile_edit_name_button"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = TrackViewTeal,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Edit Name",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = TrackViewTeal,
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Profile Quick Action Buttons: Delete Account & Privacy Policy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { activeLegalDialogTab = LegalTab.DELETE_ACCOUNT },
                modifier = Modifier.weight(1f).testTag("profile_delete_account_button"),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                  ),
                border =
                  androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                  ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteForever,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Delete Account",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }

              OutlinedButton(
                onClick = { activeLegalDialogTab = LegalTab.PRIVACY },
                modifier = Modifier.weight(1f).testTag("profile_privacy_policy_button"),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    contentColor = TrackViewTeal,
                  ),
                border =
                  androidx.compose.foundation.BorderStroke(
                    1.dp,
                    TrackViewGreen.copy(alpha = 0.5f),
                  ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = TrackViewTeal,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Privacy Policy",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      // 2. Emergency Ghost Mode Kill-Switch Card
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.fillMaxWidth().testTag("emergency_ghost_mode_card"),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Column {
                  Text(
                    text = "Emergency Ghost Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (profile.ghostModeEnabled)
                        "ACTIVE: All outbound location & battery telemetry frozen"
                      else "Instantly freeze all outbound GPS broadcasts across all groups",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
              Switch(
                checked = profile.ghostModeEnabled,
                onCheckedChange = { onToggleEmergencyGhostMode() },
                modifier = Modifier.testTag("emergency_ghost_mode_switch"),
              )
            }
          }
        }
      }

      // 3. Persistent Background Location Sharing Card (Foreground Service)
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = TrackViewGreen.copy(alpha = 0.14f),
          border =
            androidx.compose.foundation.BorderStroke(
              1.dp,
              TrackViewGreen.copy(alpha = 0.4f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("background_location_service_card"),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Persistent Background Location Sharing",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    if (bgServiceEnabled && !profile.ghostModeEnabled)
                      "Active • Foreground notification running for continuous family GPS updates"
                    else
                      "Keep sharing real-time GPS with paired family members when FamLink is closed",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = bgServiceEnabled && !profile.ghostModeEnabled,
                onCheckedChange = { enable ->
                  if (enable) {
                    showBgLocationRationale = true
                  } else {
                    com.example.service.FamLinkLocationService.stop(context)
                    bgServiceEnabled = false
                    onNotify("Background location sharing stopped")
                  }
                },
                modifier = Modifier.testTag("background_location_service_switch"),
              )
            }
          }
        }
      }

      // 4. Family Shield Premium Banner (Section 9)
      item {
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onOpenPremiumModal() }
              .testTag("settings_open_premium_card"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = TrackViewDarkGreen),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(TrackViewGreen.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = Color(0xFFFBBF24),
                )
              }
              Column {
                Text(
                  text = "Family Shield Premium",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "Unlimited SafeZones, 30-day route history, SOS dispatch & advanced parental insights",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = TrackViewGreen,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = if (profile.planTier == "FREE") "Upgrade" else "Manage",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      // 5. Location, Device Pairing & Display Settings
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            SettingsRowItem(
              icon = Icons.Default.PersonAdd,
              title = "Pair New Family Device",
              subtitle = "Link a Parent or Child phone via Pairing Code or QR",
              trailing = "Pair",
              onClick = onOpenPairingModal,
              testTag = "settings_pair_device_row",
            )
            if (onOpenDevicesTab != null) {
              HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
              SettingsRowItem(
                icon = Icons.Default.Devices,
                title = "Connected Phones (Multi-Device)",
                subtitle = "Manage registered devices, camera/voice consent, and remote sessions",
                trailing = "Manage",
                onClick = onOpenDevicesTab,
                testTag = "settings_connected_phones_row",
              )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsSwitchItem(
              icon = Icons.Default.BatteryAlert,
              title = "Share Battery Telemetry",
              subtitle = "Include battery percentage & low-power alerts in family updates",
              checked = profile.shareBatteryStatus,
              onCheckedChange = { onToggleShareBattery() },
              testTag = "settings_share_battery_switch",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsSwitchItem(
              icon = Icons.Default.DarkMode,
              title = "Dark Mode Map Styling",
              subtitle = "Use high-contrast night cartography on the Live Map",
              checked = profile.darkModeMapStyling,
              onCheckedChange = { onToggleDarkModeMap() },
              testTag = "settings_dark_map_switch",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsRowItem(
              icon = Icons.Default.Speed,
              title = "Speedometer Units",
              subtitle = "Telemetry display unit for driving & transit updates",
              trailing = profile.speedUnit,
              onClick = onToggleSpeedUnit,
              testTag = "settings_speed_unit_row",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsRowItem(
              icon = Icons.Default.NotificationsActive,
              title = "System Push Notification Access",
              subtitle = "${safeZones.size} SafeZones armed for arrival/departure alerts",
              trailing = "Verify",
              onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  onNotify("Push notifications are active for all SafeZones")
                }
              },
              testTag = "settings_notification_permission_row",
            )
          }
        }
      }

      // 6. Privacy, Legal & Account Management
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            SettingsRowItem(
              icon = Icons.Default.Shield,
              title = "Privacy Policy & Data Safety",
              subtitle = "Zero covert surveillance guarantee, encryption & policies",
              trailing = "View",
              onClick = { activeLegalDialogTab = LegalTab.PRIVACY },
              testTag = "settings_privacy_policy_row",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsRowItem(
              icon = Icons.Default.CloudDownload,
              title = "Export Location & SafeZone Archive",
              subtitle = "Download portable JSON report of your family telemetry",
              trailing = "Export",
              onClick = onExportData,
              testTag = "settings_export_data_row",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsRowItem(
              icon = Icons.Default.DeleteForever,
              title = "Delete Account & Cloud Wipe",
              subtitle = "Permanently purge account, paired devices, GPS logs & SafeZones",
              trailing = "Delete",
              onClick = { activeLegalDialogTab = LegalTab.DELETE_ACCOUNT },
              testTag = "settings_delete_account_row",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            SettingsRowItem(
              icon = Icons.AutoMirrored.Filled.Logout,
              title = "Sign Out",
              subtitle = "Disconnect this device from current Google session",
              trailing = "Exit",
              onClick = onSignOut,
              testTag = "settings_sign_out_row",
            )
          }
        }
      }

      // Footer Compliance Metadata & Direct Legal Links
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Privacy Policy",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
              modifier =
                Modifier.clickable { activeLegalDialogTab = LegalTab.PRIVACY }
                  .testTag("footer_privacy_policy_link"),
            )
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
              text = "Terms of Service",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
              modifier =
                Modifier.clickable { activeLegalDialogTab = LegalTab.TERMS }
                  .testTag("footer_terms_link"),
            )
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
              text = "Delete Account",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold,
              modifier =
                Modifier.clickable { activeLegalDialogTab = LegalTab.DELETE_ACCOUNT }
                  .testTag("footer_delete_account_link"),
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "FamLink v3.0 • End-to-End Encrypted Family Safety Mesh",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Account Deletion Portal: $TRACKVIEW_DELETE_ACCOUNT_URL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier =
              Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TRACKVIEW_DELETE_ACCOUNT_URL))
                context.startActivity(intent)
              }.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }

  // Edit Display Name Dialog
  if (showEditNameDialog) {
    var nameInput by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text("Update Display Name") },
      text = {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Your Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateDisplayName?.invoke(nameInput)
            showEditNameDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
      },
    )
  }

  // Background Location Access Explanation Dialog (Required before requesting ACCESS_BACKGROUND_LOCATION)
  if (showBgLocationRationale) {
    AlertDialog(
      onDismissRequest = { showBgLocationRationale = false },
      title = { Text("Background Location Access") },
      text = {
        Text(
          "FamLink collects location data in the background using a visible Android Foreground Service notification so your authorized family group can see your live location and SafeZone arrivals even when the app is closed or not in use.\n\nYou can pause or stop background sharing at any time from the persistent notification or Settings."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showBgLocationRationale = false
            foregroundLocationLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Approve & Enable")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBgLocationRationale = false }) { Text("Not Now") }
      },
    )
  }

  // QR Code Dialog
  if (showQrDialog) {
    AlertDialog(
      onDismissRequest = { showQrDialog = false },
      title = { Text("Family Circle QR & Invite Key") },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier.size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TrackViewGreen.copy(alpha = 0.12f))
                .border(2.dp, TrackViewGreen, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = "Family Pairing QR Code",
              tint = TrackViewDarkGreen,
              modifier = Modifier.size(120.dp),
            )
          }
          Text(
            text = "Invite Code: $familyInviteCode",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              "Scan this code or enter the invite key from your child or spouse's FamLink app to establish mutual encrypted location sharing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showQrDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Done")
        }
      },
    )
  }

  // Exported JSON Preview Dialog
  if (exportedJsonData != null) {
    AlertDialog(
      onDismissRequest = onClearExportedJson,
      title = { Text("Exported Telemetry Archive (JSON)") },
      text = {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = exportedJsonData.take(900),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(12.dp),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = onClearExportedJson,
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Close")
        }
      },
    )
  }

  // Legal, Privacy Policy, and Account Deletion Dialog
  activeLegalDialogTab?.let { tab ->
    LegalAndPrivacyDialog(
      initialTab = tab,
      onDismiss = { activeLegalDialogTab = null },
      actionButtonLabel = "Delete Account & Wipe Cloud Data Now",
      onInitiateAccountDeletion = {
        activeLegalDialogTab = null
        showDeleteConfirmDialog = true
      },
    )
  }

  // Delete Account Confirmation Dialog
  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Permanently Delete Account?") },
      text = {
        Text(
          "Are you sure you want to permanently delete your TrackView account?\n\nAll your family devices, pairing keys, live GPS records, SafeZones, and encrypted messages will be immediately purged from Firebase Firestore.\n\nThis action is permanent and cannot be undone."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
            onDeleteAccount()
          },
          colors =
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_account_final_button"),
        ) {
          Text("Delete Account Now")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SettingsRowItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  trailing: String,
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
      text = trailing,
      style = MaterialTheme.typography.labelMedium,
      color = TrackViewTeal,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun SettingsSwitchItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
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

@Composable
fun DevicePairingDialog(
  onDismiss: () -> Unit,
  onConfirmPair: (name: String, role: String, pairingCode: String) -> Unit,
  onCompleteChildPairing: ((pairingCode: String) -> Unit)? = null,
) {
  var isChildJoiningParent by rememberSaveable { mutableStateOf(false) }
  var selectedRole by rememberSaveable { mutableStateOf("CHILD") }
  var memberName by rememberSaveable { mutableStateOf("") }
  val generatedCode = remember { "FL-" + (1000..9999).random() }
  var pairingCode by rememberSaveable { mutableStateOf(generatedCode) }
  var mutualConsentChecked by rememberSaveable { mutableStateOf(true) }
  var qrMode by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = null,
          tint = TrackViewTeal,
        )
        Text("Pair Family Device", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mode selector: Parent generating code vs Child entering code
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              isChildJoiningParent = false
              pairingCode = generatedCode
            },
            modifier = Modifier.weight(1f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (!isChildJoiningParent) TrackViewGreen.copy(alpha = 0.16f)
                  else Color.Transparent
              ),
          ) {
            Text("Parent (Create Code)", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              isChildJoiningParent = true
              pairingCode = ""
            },
            modifier = Modifier.weight(1f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (isChildJoiningParent) TrackViewGreen.copy(alpha = 0.16f)
                  else Color.Transparent
              ),
          ) {
            Text("Child (Enter Code)", style = MaterialTheme.typography.labelSmall)
          }
        }

        if (!isChildJoiningParent) {
          Text(
            text = "1. Select Device Role",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TrackViewTeal,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf("CHILD" to "Child Device", "SPOUSE" to "Parent / Spouse").forEach { (role, label) ->
              val selected = selectedRole == role
              Surface(
                shape = RoundedCornerShape(12.dp),
                color =
                  if (selected) TrackViewGreen.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surfaceVariant,
                border =
                  androidx.compose.foundation.BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) TrackViewGreen else Color.Transparent,
                  ),
                modifier =
                  Modifier.weight(1f)
                    .clickable { selectedRole = role }
                    .testTag("pair_role_${role.lowercase()}"),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                )
              }
            }
          }

          OutlinedTextField(
            value = memberName,
            onValueChange = { memberName = it },
            label = { Text("Family Member Name") },
            placeholder = { Text("Enter child or family member name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("pair_member_name_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isChildJoiningParent) "Enter Parent Pairing Code" else "2. Expiring Pairing Code (15m)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TrackViewTeal,
          )
          if (!isChildJoiningParent) {
            TextButton(onClick = { qrMode = !qrMode }) {
              Icon(
                imageVector = if (qrMode) Icons.Default.Lock else Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (qrMode) "Use Pairing Code" else "Show QR Code")
            }
          }
        }

        if (qrMode && !isChildJoiningParent) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TrackViewGreen.copy(alpha = 0.12f))
                .border(1.5.dp, TrackViewGreen, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Code",
                tint = TrackViewDarkGreen,
                modifier = Modifier.size(64.dp),
              )
              Text(
                text = "QR Handshake Ready • Code $pairingCode",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        } else {
          OutlinedTextField(
            value = pairingCode,
            onValueChange = { pairingCode = it },
            label = { Text("Pairing Code (e.g., FL-1234)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("pair_code_input"),
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { mutualConsentChecked = !mutualConsentChecked },
        ) {
          Checkbox(
            checked = mutualConsentChecked,
            onCheckedChange = { mutualConsentChecked = it },
          )
          Text(
            text =
              "Both devices have accepted the visible FamLink location & parental control consent notice.",
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (isChildJoiningParent && onCompleteChildPairing != null) {
            onCompleteChildPairing(pairingCode.trim())
          } else {
            onConfirmPair(
              memberName.trim().ifBlank { "Family Device" },
              selectedRole,
              pairingCode.trim().ifBlank { generatedCode },
            )
          }
        },
        enabled =
          mutualConsentChecked &&
            pairingCode.isNotBlank() &&
            (isChildJoiningParent || memberName.isNotBlank()),
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        modifier = Modifier.testTag("pair_confirm_button"),
      ) {
        Text(if (isChildJoiningParent) "Approve & Link Child Phone" else "Generate & Register")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun PremiumSubscriptionDialog(
  currentTier: String,
  onDismiss: () -> Unit,
  onSelectPlan: (String) -> Unit,
  onRestorePurchases: () -> Unit,
) {
  var chosenTier by rememberSaveable {
    mutableStateOf(if (currentTier == "FREE") "ANNUAL" else currentTier)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = TrackViewGreen,
        )
        Text("Family Shield Premium", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Unlock complete peace of mind with unlimited SafeZones, 30-day route history, and real-time driving & screen time insights.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val plans =
          listOf(
            Triple("FREE", "Free Plan • Current Basic", "2 SafeZones • Live GPS • Basic Chat"),
            Triple(
              "MONTHLY",
              "Family Shield Monthly • $4.99/mo",
              "Unlimited SafeZones • Full Parental Controls • 30-Day History",
            ),
            Triple(
              "ANNUAL",
              "Family Shield Annual • $39.99/yr (Save 33%)",
              "All Premium Features + Priority SOS Dispatch & Driving Reports",
            ),
          )

        plans.forEach { (tier, title, desc) ->
          val selected = chosenTier == tier
          Surface(
            shape = RoundedCornerShape(14.dp),
            color =
              if (selected) TrackViewGreen.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border =
              androidx.compose.foundation.BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) TrackViewGreen else Color.Transparent,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .clickable { chosenTier = tier }
                .testTag("premium_plan_${tier.lowercase()}"),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        TextButton(
          onClick = onRestorePurchases,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
          Text("Restore Existing Purchases", color = TrackViewTeal)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSelectPlan(chosenTier)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        modifier = Modifier.testTag("premium_confirm_button"),
      ) {
        Text(if (chosenTier == "FREE") "Keep Free Plan" else "Activate $chosenTier")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
