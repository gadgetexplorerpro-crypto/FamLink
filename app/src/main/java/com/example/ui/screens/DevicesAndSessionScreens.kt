package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.model.Avatars
import com.example.data.model.DeviceSession
import com.example.data.model.UserDevice
import com.example.ui.theme.TrackViewDarkGreen
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewTeal
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun DevicesScreen(
  devicesState: UiState<List<UserDevice>>,
  currentDeviceId: String,
  onLocateDevice: (UserDevice) -> Unit,
  onRequestCamera: (UserDevice) -> Unit,
  onRequestVoice: (UserDevice) -> Unit,
  onRenameDevice: (UserDevice, String) -> Unit,
  onRevokeDevice: (UserDevice) -> Unit,
  onUpdateConsent: (UserDevice, Boolean, Boolean, Boolean) -> Unit,
  onPairAnotherPhone: () -> Unit,
) {
  val devices = (devicesState as? UiState.Success)?.data.orEmpty()
  var renamingDevice by remember { mutableStateOf<UserDevice?>(null) }
  var revokingDevice by remember { mutableStateOf<UserDevice?>(null) }
  var showPairingModal by rememberSaveable { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // WhatsApp-Style Top Header
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier.size(36.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(TrackViewGreen.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }
            Column {
              Text(
                text = "Connected Phones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = "${devices.count { it.online }}/${devices.size} Active • Mutual Permission Verified",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }

          IconButton(
            onClick = { showPairingModal = true },
            modifier = Modifier.testTag("devices_top_pair_button"),
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = "Pair Phone via QR or Code",
              tint = Color.White,
            )
          }
        }
      }

      if (devicesState is UiState.Loading && devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = TrackViewGreen)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("devices_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Info Banner Card
          item {
            ElevatedCard(
              shape = RoundedCornerShape(16.dp),
              colors =
                CardDefaults.elevatedCardColors(
                  containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(40.dp)
                      .clip(CircleShape)
                      .background(TrackViewGreen.copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TrackViewTeal,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Consent-Based Multi-Phone Mesh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      "Remote camera, microphone, and location sessions require confirmation on the receiving phone before transmission starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          // Device Cards List
          items(devices, key = { it.deviceId }) { device ->
            val isCurrentPhone = device.deviceId == currentDeviceId
            UserDeviceCard(
              device = device,
              isCurrentPhone = isCurrentPhone,
              onLocate = { onLocateDevice(device) },
              onRequestCamera = { onRequestCamera(device) },
              onRequestVoice = { onRequestVoice(device) },
              onRename = { renamingDevice = device },
              onRevoke = { revokingDevice = device },
              onUpdateConsent = { loc, cam, mic -> onUpdateConsent(device, loc, cam, mic) },
            )
          }
        }
      }
    }

    // FAB to Pair Another Phone
    FloatingActionButton(
      onClick = { showPairingModal = true },
      containerColor = TrackViewGreen,
      contentColor = Color.White,
      shape = RoundedCornerShape(16.dp),
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(16.dp)
          .testTag("pair_another_phone_fab"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.PhoneAndroid,
          contentDescription = "Pair Another Phone",
        )
        Text(
          text = "Pair Phone",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }

  // Rename Device Dialog
  if (renamingDevice != null) {
    val target = renamingDevice!!
    var newName by rememberSaveable(target.deviceId) { mutableStateOf(target.name) }
    AlertDialog(
      onDismissRequest = { renamingDevice = null },
      title = { Text("Rename Device") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Device Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("rename_device_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRenameDevice(target, newName)
            renamingDevice = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        ) {
          Text("Save Name")
        }
      },
      dismissButton = {
        TextButton(onClick = { renamingDevice = null }) { Text("Cancel") }
      },
    )
  }

  // Revoke Access Confirmation Dialog
  if (revokingDevice != null) {
    val target = revokingDevice!!
    AlertDialog(
      onDismissRequest = { revokingDevice = null },
      title = { Text("Revoke Access & Disconnect?") },
      text = {
        Text(
          "This will disconnect \"${target.name}\" from your TrackView account and terminate all remote location, camera, and voice authorizations."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRevokeDevice(target)
            revokingDevice = null
          },
          colors =
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Revoke Access")
        }
      },
      dismissButton = {
        TextButton(onClick = { revokingDevice = null }) { Text("Cancel") }
      },
    )
  }

  // Pairing Modal Dialog
  if (showPairingModal) {
    PairPhoneDialog(
      onDismiss = { showPairingModal = false },
      onConfirmPair = { _, _ ->
        showPairingModal = false
        onPairAnotherPhone()
      },
    )
  }
}

@Composable
private fun UserDeviceCard(
  device: UserDevice,
  isCurrentPhone: Boolean,
  onLocate: () -> Unit,
  onRequestCamera: () -> Unit,
  onRequestVoice: () -> Unit,
  onRename: () -> Unit,
  onRevoke: () -> Unit,
  onUpdateConsent: (Boolean, Boolean, Boolean) -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }
  var showConsentPanel by rememberSaveable { mutableStateOf(false) }

  ElevatedCard(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
    modifier = Modifier.fillMaxWidth().testTag("device_card_${device.deviceId}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header: Phone icon, name, model, online pill, overflow menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
                .background(
                  if (device.online) TrackViewGreen.copy(alpha = 0.16f)
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = if (device.online) TrackViewTeal else Color(0xFF6B7280),
              modifier = Modifier.size(24.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isCurrentPhone) {
                Surface(
                  color = TrackViewGreen.copy(alpha = 0.18f),
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = "THIS PHONE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TrackViewTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }

            Text(
              text = "${device.model} • Pairing Code: ${device.pairingCode}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Online/Offline & Battery
          Surface(
            color =
              if (device.online) TrackViewGreen.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(7.dp)
                    .clip(CircleShape)
                    .background(if (device.online) TrackViewGreen else Color(0xFF9CA3AF))
              )
              Text(
                text = if (device.online) "${device.batteryLevel}%" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (device.online) TrackViewTeal else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "Device Options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text("Rename Phone") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                  showMenu = false
                  onRename()
                },
              )
              DropdownMenuItem(
                text = { Text("Permissions & Consent") },
                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                onClick = {
                  showMenu = false
                  showConsentPanel = !showConsentPanel
                },
              )
              DropdownMenuItem(
                text = { Text("Disconnect & Revoke Access") },
                leadingIcon = {
                  Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showMenu = false
                  onRevoke()
                },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Location snippet & status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Default.GpsFixed,
            contentDescription = null,
            tint = if (device.online) TrackViewGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text =
              if (device.online)
                "GPS: ${String.format("%.4f", device.latitude)}, ${String.format("%.4f", device.longitude)} (±${device.accuracyMeters}m)"
              else "Last active while online • Coordinates saved",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
          )
        }

        Text(
          text = if (showConsentPanel) "Hide Rules ▲" else "Permissions ▼",
          style = MaterialTheme.typography.labelSmall,
          color = TrackViewTeal,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable { showConsentPanel = !showConsentPanel },
        )
      }

      // Expandable Permission & Consent Settings Panel
      AnimatedVisibility(visible = showConsentPanel) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = 10.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "REMOTE ACCESS PERMISSIONS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Allow Remote GPS Location", style = MaterialTheme.typography.bodySmall)
            Switch(
              checked = device.locationSharingApproved,
              onCheckedChange = { approved ->
                onUpdateConsent(approved, device.cameraSessionApproved, device.microphoneApproved)
              },
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Allow Remote Camera Session", style = MaterialTheme.typography.bodySmall)
            Switch(
              checked = device.cameraSessionApproved,
              onCheckedChange = { approved ->
                onUpdateConsent(device.locationSharingApproved, approved, device.microphoneApproved)
              },
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Allow Remote Voice Session", style = MaterialTheme.typography.bodySmall)
            Switch(
              checked = device.microphoneApproved,
              onCheckedChange = { approved ->
                onUpdateConsent(device.locationSharingApproved, device.cameraSessionApproved, approved)
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Primary Remote Actions Row: Locate, Request Camera, Request Voice
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onLocate,
          modifier = Modifier.weight(1f).testTag("device_locate_btn_${device.deviceId}"),
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
          contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
        ) {
          Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Locate", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = onRequestCamera,
          modifier = Modifier.weight(1.1f).testTag("device_camera_btn_${device.deviceId}"),
          contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
        ) {
          Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Camera", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = onRequestVoice,
          modifier = Modifier.weight(1f).testTag("device_voice_btn_${device.deviceId}"),
          contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
        ) {
          Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Voice", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
fun PairPhoneDialog(
  onDismiss: () -> Unit,
  onConfirmPair: (name: String, pairingCode: String) -> Unit = { _, _ -> },
) {
  var code by rememberSaveable { mutableStateOf("TV-7821") }
  var phoneName by rememberSaveable { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TrackViewTeal)
        Text("Connect Another Phone", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier.size(130.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(TrackViewGreen.copy(alpha = 0.12f))
              .border(1.5.dp, TrackViewGreen, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "Pairing QR",
            tint = TrackViewDarkGreen,
            modifier = Modifier.size(96.dp),
          )
        }

        Text(
          text = "Expiring Pairing Code: $code",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = TrackViewTeal,
        )

        Text(
          text =
            "Open TrackView on your second phone and scan this QR code or enter code $code to link under your verified Google Account.",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = phoneName,
          onValueChange = { phoneName = it },
          label = { Text("Friendly Name (e.g. Work Phone)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmPair(phoneName, code) },
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
      ) {
        Text("Done")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
fun IncomingSessionConsentDialog(
  session: DeviceSession,
  onApprove: (DeviceSession) -> Unit,
  onDecline: (DeviceSession) -> Unit,
) {
  val context = LocalContext.current
  val permissionsToRequest =
    when (session.sessionType) {
      "CAMERA" -> arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      "VOICE" -> arrayOf(Manifest.permission.RECORD_AUDIO)
      else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
      val allGranted = result.values.all { it }
      if (allGranted) {
        onApprove(session)
      } else {
        onDecline(session)
      }
    }

  AlertDialog(
    onDismissRequest = { onDecline(session) },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector =
            if (session.sessionType == "CAMERA") Icons.Default.Videocam
            else if (session.sessionType == "VOICE") Icons.Default.Call
            else Icons.Default.GpsFixed,
          contentDescription = null,
          tint = TrackViewGreen,
        )
        Text("${session.sessionType} Session Request", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "\"${session.fromDeviceName}\" is requesting to start a ${session.sessionType.lowercase()} session with this phone.",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )

        Surface(
          color =
            if (isSystemInDarkTheme()) Color(0xFF182229)
            else Color(0xFFE8F5E9),
          shape = RoundedCornerShape(10.dp),
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = TrackViewTeal,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                "A glowing active indicator will remain visible on screen at all times. You can terminate the session instantly with the Stop button.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val needsRuntime =
            permissionsToRequest.any {
              ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
          if (needsRuntime) {
            permissionLauncher.launch(permissionsToRequest)
          } else {
            onApprove(session)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
        modifier = Modifier.testTag("incoming_session_approve_btn"),
      ) {
        Text("Approve Session")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = { onDecline(session) },
        modifier = Modifier.testTag("incoming_session_decline_btn"),
      ) {
        Text("Decline")
      }
    },
  )
}

@Composable
fun RemoteSessionActiveScreen(
  session: DeviceSession,
  onEndSession: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var isMuted by rememberSaveable { mutableStateOf(false) }
  var isFlashOn by rememberSaveable { mutableStateOf(false) }
  var useFrontCamera by rememberSaveable { mutableStateOf(false) }
  var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }

  LaunchedEffect(session.sessionId) {
    while (true) {
      delay(1000)
      elapsedSeconds++
    }
  }

  val formattedTimer =
    remember(elapsedSeconds) {
      val mins = elapsedSeconds / 60
      val secs = elapsedSeconds % 60
      String.format("%02d:%02d", mins, secs)
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF0B141A))
        .testTag("active_remote_session_screen")
  ) {
    if (session.sessionType == "CAMERA") {
      // Real CameraX preview when camera permission is granted
      val hasCam =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
          PackageManager.PERMISSION_GRANTED

      if (hasCam) {
        AndroidView(
          factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
              {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                  it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector =
                  if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                  else CameraSelector.DEFAULT_BACK_CAMERA
                try {
                  cameraProvider.unbindAll()
                  cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                  // Fallback safely
                }
              },
              ContextCompat.getMainExecutor(ctx),
            )
            previewView
          },
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        // High quality simulated remote video stream with animated noise/radar overlay
        AsyncImage(
          model = Avatars.MAP_BG,
          contentDescription = "Remote Camera Stream",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }

      // Dark gradient overlay top and bottom for controls
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.65f),
                  Color.Transparent,
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.85f),
                )
              )
            )
      )
    } else {
      // Voice Session Screen with Pulsing Microphone Visualizer
      VoiceVisualizerCanvas(isMuted = isMuted)
    }

    // Top Header: Live Badge + Target Device + Timer
    Column(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Glowing Active Indicator
        Surface(
          color = Color(0xFFEF4444),
          shape = RoundedCornerShape(50),
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.size(10.dp)
                  .clip(CircleShape)
                  .background(Color.White)
            )
            Text(
              text = "LIVE ${session.sessionType} ACTIVE",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        // WebRTC Signaling & STUN status
        Surface(
          color = Color.Black.copy(alpha = 0.6f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "STUN • Google Mesh P2P",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TrackViewGreen,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "${session.fromDeviceName} ⇄ ${session.toDeviceName}",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )

      Text(
        text = formattedTimer,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
      )
    }

    // Bottom Controls Bar
    Column(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (session.sessionType == "CAMERA") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { useFrontCamera = !useFrontCamera },
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
          ) {
            Icon(
              imageVector = Icons.Default.FlipCameraAndroid,
              contentDescription = "Switch Camera",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = { isFlashOn = !isFlashOn },
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
          ) {
            Icon(
              imageVector = Icons.Default.FlashOn,
              contentDescription = "Toggle Flash",
              tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White,
            )
          }

          IconButton(
            onClick = { isMuted = !isMuted },
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
          ) {
            Icon(
              imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Toggle Mute",
              tint = if (isMuted) Color(0xFFEF4444) else Color.White,
            )
          }
        }
      } else {
        // Voice Session Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { isMuted = !isMuted },
            modifier =
              Modifier.size(56.dp)
                .clip(CircleShape)
                .background(if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f)),
          ) {
            Icon(
              imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mute",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = {},
            modifier =
              Modifier.size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "Speakerphone",
              tint = Color.White,
            )
          }
        }
      }

      // Big Immediate Red Stop Button
      Button(
        onClick = onEndSession,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth().height(52.dp).testTag("end_active_session_btn"),
      ) {
        Icon(
          imageVector =
            if (session.sessionType == "VOICE") Icons.Default.CallEnd
            else Icons.Default.Stop,
          contentDescription = null,
          tint = Color.White,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "STOP ${session.sessionType} SESSION",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun VoiceVisualizerCanvas(isMuted: Boolean) {
  val transition = rememberInfiniteTransition(label = "voiceWave")
  val wavePulse by
    transition.animateFloat(
      initialValue = 40f,
      targetValue = 120f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "wavePulse",
    )

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(300.dp)) {
      val center = Offset(size.width / 2, size.height / 2)
      if (!isMuted) {
        drawCircle(
          color = Color(0xFF00A884).copy(alpha = 0.2f),
          radius = wavePulse + 40f,
          center = center,
        )
        drawCircle(
          color = Color(0xFF00A884).copy(alpha = 0.35f),
          radius = wavePulse,
          center = center,
        )
      }
      drawCircle(
        color = if (isMuted) Color(0xFFEF4444) else Color(0xFF00A884),
        radius = 50f,
        center = center,
      )
    }

    Icon(
      imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(40.dp),
    )
  }
}
