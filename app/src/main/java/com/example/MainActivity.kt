package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FamilyMember
import com.example.data.repository.TrackViewRepository
import com.example.ui.auth.AuthAndOnboardingScreen
import com.example.ui.auth.signOutTrackView
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DevicePairingDialog
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingSessionConsentDialog
import com.example.ui.screens.LiveMapScreen
import com.example.ui.screens.MemberDetailAndChatScreen
import com.example.ui.screens.PairPhoneDialog
import com.example.ui.screens.ParentalControlsScreen
import com.example.ui.screens.PremiumSubscriptionDialog
import com.example.ui.screens.RemoteSessionActiveScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewTeal
import com.example.ui.viewmodel.TrackViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TrackViewRootApp()
      }
    }
  }
}

private fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
  val auth = Firebase.auth
  val listener =
    FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
  auth.addAuthStateListener(listener)
  awaitClose { auth.removeAuthStateListener(listener) }
}

private enum class TrackViewTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
  MAP("Map", Icons.Filled.Map, Icons.Outlined.Map, "nav_tab_map"),
  DEVICES("Devices", Icons.Filled.Devices, Icons.Outlined.Devices, "nav_tab_devices"),
  CONTROLS("Controls", Icons.Filled.Security, Icons.Outlined.Security, "nav_tab_controls"),
  ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_tab_alerts"),
  SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings"),
}

@Composable
fun TrackViewRootApp() {
  val currentUser by observeAuthState().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  if (currentUser == null) {
    AuthAndOnboardingScreen(onAuthSuccess = {})
  } else {
    val user = currentUser!!
    TrackViewAuthenticatedApp(user = user)
  }
}

@Composable
private fun TrackViewAuthenticatedApp(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val repository = remember(context) { TrackViewRepository(context.applicationContext) }

  val viewModel: TrackViewModel =
    viewModel(
      key = "trackview_vm_${user.uid}",
      factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrackViewModel(
              repository = repository,
              currentUserId = user.uid,
              userDisplayName = user.displayName,
              userEmail = user.email,
            ) as T
          }
        },
    )

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val membersState by viewModel.membersState.collectAsStateWithLifecycle()
  val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
  val safeZonesState by viewModel.safeZonesState.collectAsStateWithLifecycle()
  val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
  val selectedMemberId by viewModel.selectedMemberId.collectAsStateWithLifecycle()
  val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val liveBannerDismissed by viewModel.liveBannerDismissed.collectAsStateWithLifecycle()
  val liveBannerPaused by viewModel.liveBannerPaused.collectAsStateWithLifecycle()
  val deviceLat by viewModel.deviceGpsLatitude.collectAsStateWithLifecycle()
  val deviceLng by viewModel.deviceGpsLongitude.collectAsStateWithLifecycle()
  val deviceAccuracy by viewModel.deviceGpsAccuracy.collectAsStateWithLifecycle()
  val isGpsConsentVerified by viewModel.isGpsConsentVerified.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
  val exportedJsonData by viewModel.exportedJsonData.collectAsStateWithLifecycle()
  val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
  val activeSessionState by viewModel.activeSessionState.collectAsStateWithLifecycle()
  val incomingConsentSession by viewModel.incomingConsentSession.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableStateOf(TrackViewTab.HOME) }
  var detailMemberId by rememberSaveable { mutableStateOf<String?>(null) }
  var showPairingModal by rememberSaveable { mutableStateOf(false) }
  var showPairPhoneModal by rememberSaveable { mutableStateOf(false) }
  var showPremiumModal by rememberSaveable { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(snackbarMessage) {
    val msg = snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val members = (membersState as? UiState.Success)?.data.orEmpty()
  val alerts = (alertsState as? UiState.Success)?.data.orEmpty()
  val unreadAlertsCount = alerts.count { !it.read }
  val activeDetailMember: FamilyMember? =
    remember(members, detailMemberId) {
      members.find { it.id == detailMemberId }
    }

  if (activeSessionState != null) {
    RemoteSessionActiveScreen(
      session = activeSessionState!!,
      onEndSession = viewModel::endActiveSession,
    )
  } else {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        // Show Navigation Bar when not inside full-screen member detail/chat
        if (activeDetailMember == null) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
          ) {
            TrackViewTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                modifier = Modifier.testTag(tab.testTag),
                icon = {
                  if (tab == TrackViewTab.ALERTS && unreadAlertsCount > 0) {
                    BadgedBox(
                      badge = {
                        Badge(
                          containerColor = TrackViewGreen,
                          contentColor = Color.White,
                        ) {
                          Text(unreadAlertsCount.toString())
                        }
                      }
                    ) {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  }
                },
                label = {
                  Text(
                    text = tab.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = TrackViewTeal,
                    selectedTextColor = TrackViewTeal,
                    indicatorColor = TrackViewGreen.copy(alpha = 0.22f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (activeDetailMember != null) {
          MemberDetailAndChatScreen(
            member = activeDetailMember,
            messagesState = messagesState,
            alertsState = alertsState,
            onBack = { detailMemberId = null },
            onOpenMapForMember = { member ->
              viewModel.selectMember(member.id)
              detailMemberId = null
              currentTab = TrackViewTab.MAP
            },
            onOpenControlsForMember = { member ->
              viewModel.selectMember(member.id)
              detailMemberId = null
              currentTab = TrackViewTab.CONTROLS
            },
            onSendMessage = { text, type ->
              viewModel.sendChatMessage(activeDetailMember, text, type)
            },
            onShareLiveLocation = {
              viewModel.shareLiveLocationWithMember(activeDetailMember)
            },
            onSendEta = {
              viewModel.sendEtaToMember(activeDetailMember)
            },
            onRequestCheckIn = {
              viewModel.requestCheckInFromMember(activeDetailMember)
            },
            onToggleLiveSharing = { enabled ->
              viewModel.updateMemberParentalControls(
                member = activeDetailMember,
                liveSharing = enabled,
              )
            },
            onToggleSchoolNotification = { enabled ->
              viewModel.toggleNotifyOnSchoolDeparture(activeDetailMember, enabled)
            },
            onNotifyAction = { msg -> viewModel.postMessage(msg) },
          )
        } else {
          when (currentTab) {
            TrackViewTab.HOME ->
              HomeScreen(
                profileState = profileState,
                membersState = membersState,
                alertsState = alertsState,
                safeZonesState = safeZonesState,
                selectedFilter = selectedFilter,
                searchQuery = searchQuery,
                liveBannerDismissed = liveBannerDismissed,
                liveBannerPaused = liveBannerPaused,
                onSelectFilter = viewModel::setFilter,
                onSearchQueryChange = viewModel::setSearchQuery,
                onToggleLiveBannerPaused = viewModel::toggleLiveBannerPaused,
                onDismissLiveBanner = viewModel::dismissLiveBanner,
                onMemberClick = { member ->
                  viewModel.selectMember(member.id)
                  detailMemberId = member.id
                },
                onOpenMapTab = { memberId ->
                  if (memberId != null) viewModel.selectMember(memberId)
                  currentTab = TrackViewTab.MAP
                },
                onOpenControlsTab = { currentTab = TrackViewTab.CONTROLS },
                onOpenAlertsTab = { currentTab = TrackViewTab.ALERTS },
                onOpenSettingsTab = { currentTab = TrackViewTab.SETTINGS },
                onOpenPairingModal = { showPairingModal = true },
                onOpenPremiumModal = { showPremiumModal = true },
                onTriggerCheckIn = { viewModel.triggerFamilyCheckInOrEmergency(false) },
                onOpenDevicesTab = { currentTab = TrackViewTab.DEVICES },
              )

            TrackViewTab.MAP ->
              LiveMapScreen(
                profileState = profileState,
                membersState = membersState,
                safeZonesState = safeZonesState,
                devicesState = devicesState,
                selectedMemberId = selectedMemberId,
                deviceLat = deviceLat,
                deviceLng = deviceLng,
                deviceAccuracy = deviceAccuracy,
                isGpsConsentVerified = isGpsConsentVerified,
                onSelectMember = viewModel::selectMember,
                onOpenChatForMember = { member ->
                  viewModel.selectMember(member.id)
                  detailMemberId = member.id
                },
                onUpdateDeviceGps = viewModel::updateDeviceGpsLocation,
                onStopAllSharing = viewModel::stopAllSharing,
                onToggleSchoolDeparture = viewModel::toggleNotifyOnSchoolDeparture,
                onRequestCheckIn = viewModel::requestCheckInFromMember,
                onAddSafeZone = viewModel::addNewSafeZone,
                onNotify = viewModel::postMessage,
              )

            TrackViewTab.DEVICES ->
              DevicesScreen(
                devicesState = devicesState,
                currentDeviceId = viewModel.currentDeviceId,
                onLocateDevice = viewModel::locateRemoteDevice,
                onRequestCamera = viewModel::requestCameraSession,
                onRequestVoice = viewModel::requestVoiceSession,
                onRenameDevice = viewModel::renameDevice,
                onRevokeDevice = viewModel::revokeDeviceAccess,
                onUpdateConsent = viewModel::updateDeviceConsent,
                onPairAnotherPhone = { showPairPhoneModal = true },
              )

            TrackViewTab.CONTROLS ->
              ParentalControlsScreen(
                membersState = membersState,
                selectedMemberId = selectedMemberId,
                onSelectMember = viewModel::selectMember,
                onUpdateControls = viewModel::updateMemberParentalControls,
                onNotify = viewModel::postMessage,
              )

            TrackViewTab.ALERTS ->
              AlertsScreen(
                alertsState = alertsState,
                onMarkAlertRead = viewModel::markAlertRead,
                onMarkAllRead = viewModel::markAllAlertsRead,
                onTriggerCheckIn = { viewModel.triggerFamilyCheckInOrEmergency(false) },
                onTriggerEmergencySos = { viewModel.triggerFamilyCheckInOrEmergency(true) },
              )

            TrackViewTab.SETTINGS ->
              SettingsScreen(
                profileState = profileState,
                safeZonesState = safeZonesState,
                exportedJsonData = exportedJsonData,
                onToggleEmergencyGhostMode = viewModel::toggleEmergencyGhostMode,
                onToggleShareBattery = viewModel::toggleShareBatteryStatus,
                onToggleDarkModeMap = viewModel::toggleDarkModeMapStyling,
                onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                onOpenPairingModal = { showPairingModal = true },
                onOpenPremiumModal = { showPremiumModal = true },
                onExportData = viewModel::exportLocationData,
                onClearExportedJson = viewModel::clearExportedJson,
                onDeleteAccount = {
                  viewModel.deleteAccountAndWipeData {
                    signOutTrackView(context, onSignOutComplete = {}, scope = scope)
                  }
                },
                onSignOut = {
                  signOutTrackView(context, onSignOutComplete = {}, scope = scope)
                },
                onNotify = viewModel::postMessage,
                onOpenDevicesTab = { currentTab = TrackViewTab.DEVICES },
              )
          }
        }
      }
    }
  }

  // Incoming Consent Modal identifying requesting phone and prompting for runtime permissions
  incomingConsentSession?.let { incoming ->
    IncomingSessionConsentDialog(
      session = incoming,
      onApprove = { session -> viewModel.approveIncomingSession(session) },
      onDecline = { session -> viewModel.rejectIncomingSession(session) },
    )
  }

  // Connect another phone via QR code / pairing code
  if (showPairPhoneModal) {
    PairPhoneDialog(
      onDismiss = { showPairPhoneModal = false },
      onConfirmPair = { name, code ->
        viewModel.pairNewPhoneDevice(name, "Android Smartphone", code) {
          showPairPhoneModal = false
        }
      },
    )
  }

  if (showPairingModal) {
    DevicePairingDialog(
      onDismiss = { showPairingModal = false },
      onConfirmPair = { name, role, code ->
        viewModel.pairNewFamilyDevice(name, role, code) { newDocId ->
          showPairingModal = false
          viewModel.selectMember(newDocId)
        }
      },
    )
  }

  if (showPremiumModal) {
    val currentTier = (profileState as? UiState.Success)?.data?.planTier ?: "FREE"
    PremiumSubscriptionDialog(
      currentTier = currentTier,
      onDismiss = { showPremiumModal = false },
      onSelectPlan = viewModel::selectSubscriptionPlan,
      onRestorePurchases = {
        showPremiumModal = false
        viewModel.postMessage("Verified Family Shield subscription status with Google Play")
      },
    )
  }
}
