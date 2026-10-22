package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
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
) {
  val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
  val safeZones = (safeZonesState as? UiState.Success)?.data.orEmpty()

  var showQrDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
  var tripRemainingMinutes by rememberSaveable { mutableIntStateOf(42) }
  var tripActive by rememberSaveable { mutableStateOf(true) }

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
      // 1. WhatsApp-style Profile Card
      item {
        ElevatedCard(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            AsyncImage(
              model = profile.avatarUrl.ifBlank { Avatars.DAVID },
              contentDescription = "Profile Avatar",
              modifier =
                Modifier.size(64.dp)
                  .clip(CircleShape)
                  .border(2.5.dp, TrackViewGreen, CircleShape),
              contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = profile.displayName,
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

      // 3. Active Live Trip Session Card
      if (tripActive && !profile.ghostModeEnabled) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = TrackViewGreen.copy(alpha = 0.14f),
            border =
              androidx.compose.foundation.BorderStroke(
                1.dp,
                TrackViewGreen.copy(alpha = 0.4f),
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "Active Live Trip Session",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Sharing with Miller Family Hub • ${tripRemainingMinutes}m remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = {
                      tripRemainingMinutes += 30
                      onNotify("Extended live trip session by 30 minutes")
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Text("+30m", style = MaterialTheme.typography.labelMedium)
                  }
                  Button(
                    onClick = {
                      tripActive = false
                      onNotify("Live trip session ended")
                    },
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                      ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Text("End Trip", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
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

      // 6. Privacy, Data Export & Account Management
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
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
              title = "Delete Account & Wipe History",
              subtitle = "Permanently erase all cloud locations, messages & SafeZones",
              trailing = "Wipe",
              onClick = { showDeleteConfirmDialog = true },
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

      // Footer Compliance Metadata
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "TrackView v4.2.0 • End-to-End Encrypted Family Safety Mesh",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Zero background tracking without explicit mutual family consent.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
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
            text = "Invite Code: TV-MILLER-8841",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              "Scan this code from your child or spouse's TrackView app to establish mutual encrypted location sharing.",
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

  // Delete Account Confirmation Dialog
  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Delete Account & Wipe All History?") },
      text = {
        Text(
          "This will permanently delete your profile, paired family devices, SafeZones, and encrypted messages from Firestore."
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
        ) {
          Text("Delete Permanently")
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
) {
  var selectedRole by rememberSaveable { mutableStateOf("CHILD") }
  var memberName by rememberSaveable { mutableStateOf("") }
  var pairingCode by rememberSaveable { mutableStateOf("TV-9042") }
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
          label = { Text("Family Member Name (e.g., Noah Miller)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("pair_member_name_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "2. Pairing Method",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TrackViewTeal,
          )
          TextButton(onClick = { qrMode = !qrMode }) {
            Icon(
              imageVector = if (qrMode) Icons.Default.Lock else Icons.Default.QrCodeScanner,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (qrMode) "Use Pairing Code" else "Scan QR Code")
          }
        }

        if (qrMode) {
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
            label = { Text("6-Character Pairing Code") },
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
              "Both devices have accepted the visible TrackView location & parental control consent notice.",
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmPair(
            memberName.ifBlank { "Noah Miller" },
            selectedRole,
            pairingCode,
          )
        },
        enabled = mutualConsentChecked,
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        modifier = Modifier.testTag("pair_confirm_button"),
      ) {
        Text("Complete Pairing")
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
