package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Avatars
import com.example.data.model.ChatMessage
import com.example.data.model.FamilyMember
import com.example.data.model.SafeZone
import com.example.data.model.SafetyAlert
import com.example.data.model.UserDevice
import com.example.data.model.UserProfile
import com.example.data.model.DeviceSession
import com.example.data.repository.TrackViewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModel(
  private val repository: TrackViewRepository,
  private val currentUserId: String,
  private val userDisplayName: String? = null,
  private val userEmail: String? = null,
) : ViewModel() {

  private val _selectedMemberId = MutableStateFlow<String?>(null)
  val selectedMemberId: StateFlow<String?> = _selectedMemberId.asStateFlow()

  private val _selectedFilter = MutableStateFlow("ALL")
  val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _liveBannerDismissed = MutableStateFlow(false)
  val liveBannerDismissed: StateFlow<Boolean> = _liveBannerDismissed.asStateFlow()

  private val _liveBannerPaused = MutableStateFlow(false)
  val liveBannerPaused: StateFlow<Boolean> = _liveBannerPaused.asStateFlow()

  private val _deviceGpsLatitude = MutableStateFlow(0.0)
  val deviceGpsLatitude: StateFlow<Double> = _deviceGpsLatitude.asStateFlow()

  private val _deviceGpsLongitude = MutableStateFlow(0.0)
  val deviceGpsLongitude: StateFlow<Double> = _deviceGpsLongitude.asStateFlow()

  private val _deviceGpsAccuracy = MutableStateFlow(0)
  val deviceGpsAccuracy: StateFlow<Int> = _deviceGpsAccuracy.asStateFlow()

  private val _isGpsConsentVerified = MutableStateFlow(false)
  val isGpsConsentVerified: StateFlow<Boolean> = _isGpsConsentVerified.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _exportedJsonData = MutableStateFlow<String?>(null)
  val exportedJsonData: StateFlow<String?> = _exportedJsonData.asStateFlow()

  val currentDeviceId: String =
    "dev_" +
      android.os.Build.MODEL.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(16) +
      "_" +
      currentUserId.take(6).lowercase()
  val currentDeviceName: String =
    "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL} (This Phone)"
  val currentDeviceModel: String =
    "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"

  private val _activeSessionId = MutableStateFlow<String?>(null)
  val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

  private val _incomingConsentSession = MutableStateFlow<DeviceSession?>(null)
  val incomingConsentSession: StateFlow<DeviceSession?> = _incomingConsentSession.asStateFlow()

  init {
    viewModelScope.launch {
      repository
        .ensureRealUserInitialized(
          displayName = userDisplayName,
          email = userEmail,
          currentDeviceId = currentDeviceId,
          currentDeviceName = currentDeviceName,
          currentDeviceModel = currentDeviceModel,
        )
        .onFailure { e ->
          Log.w(TAG, "Initial user initialization warning: ${e.message}", e)
        }
      repository.observeIncomingSessions(currentUserId, currentDeviceId).collect { sessions ->
        _incomingConsentSession.value = sessions.firstOrNull()
      }
    }
  }

  val profileState: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing profile", e)
        emit(UiState.Error(e.message ?: "Failed to load user profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val membersState: StateFlow<UiState<List<FamilyMember>>> =
    repository
      .observeMembers(currentUserId)
      .map<List<FamilyMember>, UiState<List<FamilyMember>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing family members", e)
        emit(UiState.Error(e.message ?: "Failed to load family members"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val alertsState: StateFlow<UiState<List<SafetyAlert>>> =
    repository
      .observeAlerts(currentUserId)
      .map<List<SafetyAlert>, UiState<List<SafetyAlert>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing safety alerts", e)
        emit(UiState.Error(e.message ?: "Failed to load safety alerts"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val safeZonesState: StateFlow<UiState<List<SafeZone>>> =
    repository
      .observeSafeZones(currentUserId)
      .map<List<SafeZone>, UiState<List<SafeZone>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing safe zones", e)
        emit(UiState.Error(e.message ?: "Failed to load safe zones"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val messagesState: StateFlow<UiState<List<ChatMessage>>> =
    _selectedMemberId
      .flatMapLatest { memberId ->
        if (memberId.isNullOrBlank()) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeMessages(currentUserId, memberId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { e ->
              Log.w(TAG, "Error observing messages for $memberId", e)
              emit(UiState.Error(e.message ?: "Failed to load messages"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val devicesState: StateFlow<UiState<List<UserDevice>>> =
    repository
      .observeDevices(currentUserId)
      .map<List<UserDevice>, UiState<List<UserDevice>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing devices", e)
        emit(UiState.Error(e.message ?: "Failed to load registered devices"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val activeSessionState: StateFlow<DeviceSession?> =
    _activeSessionId
      .flatMapLatest { sId ->
        if (sId.isNullOrBlank()) flowOf(null)
        else repository.observeActiveSession(currentUserId, sId)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun postMessage(msg: String) {
    _snackbarMessage.value = msg
  }

  fun clearExportedJson() {
    _exportedJsonData.value = null
  }

  fun selectMember(memberId: String?) {
    _selectedMemberId.value = memberId
    if (!memberId.isNullOrBlank()) {
      val currentList = (membersState.value as? UiState.Success)?.data.orEmpty()
      val target = currentList.find { it.id == memberId }
      if (target != null && target.unreadCount > 0) {
        viewModelScope.launch {
          repository.updateFamilyMember(target.copy(unreadCount = 0))
        }
      }
    }
  }

  fun setFilter(filter: String) {
    _selectedFilter.value = filter
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleLiveBannerPaused() {
    _liveBannerPaused.value = !_liveBannerPaused.value
    _snackbarMessage.value =
      if (_liveBannerPaused.value) "Live sharing stream paused" else "Live sharing stream resumed"
  }

  fun dismissLiveBanner() {
    _liveBannerDismissed.value = true
  }

  fun updateDeviceGpsLocation(lat: Double, lng: Double, accuracyMeters: Int) {
    _deviceGpsLatitude.value = lat
    _deviceGpsLongitude.value = lng
    _deviceGpsAccuracy.value = accuracyMeters.coerceIn(1, 5000)
    _isGpsConsentVerified.value = true
    viewModelScope.launch {
      repository.updateDeviceLocation(currentDeviceId, lat, lng, accuracyMeters.coerceIn(1, 5000))
    }
    _snackbarMessage.value =
      "GPS updated (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)} • ±${accuracyMeters}m)"
  }

  fun sendChatMessage(member: FamilyMember, text: String, messageType: String = "TEXT") {
    if (text.isBlank()) return
    viewModelScope.launch {
      repository
        .sendMessage(
          memberId = member.id,
          senderName = "You",
          text = text.trim(),
          messageType = messageType,
          outgoing = true,
        )
        .onSuccess {
          repository.updateFamilyMember(
            member.copy(
              lastMessageSnippet = text.trim().take(240),
              lastActivityTimeLabel = "Just now",
              unreadCount = 0,
            )
          )
        }
        .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to send message" }
    }
  }

  fun shareLiveLocationWithMember(member: FamilyMember) {
    viewModelScope.launch {
      val text =
        "Live Location Active (60m) • ${String.format("%.4f", _deviceGpsLatitude.value)}, ${String.format("%.4f", _deviceGpsLongitude.value)} • ±${_deviceGpsAccuracy.value}m acc"
      repository
        .sendMessage(
          memberId = member.id,
          senderName = "You",
          text = text,
          messageType = "LIVE_LOCATION",
          outgoing = true,
        )
        .onSuccess {
          repository.updateFamilyMember(
            member.copy(
              liveSharing = true,
              ghostMode = false,
              lastMessageSnippet = "Live: Sharing active for 60m",
              lastActivityTimeLabel = "Just now",
            )
          )
          _snackbarMessage.value = "Sharing live location with ${member.name} for 60 minutes"
        }
    }
  }

  fun sendEtaToMember(member: FamilyMember) {
    viewModelScope.launch {
      val text = "ETA Update: Arriving at ${member.locationLabel.take(40)} in ~12 mins (Traffic clear)"
      sendChatMessage(member, text, "STATIC_PIN")
      _snackbarMessage.value = "Sent ETA update to ${member.name}"
    }
  }

  fun togglePinMember(member: FamilyMember) {
    viewModelScope.launch {
      repository.updateFamilyMember(member.copy(pinned = !member.pinned))
    }
  }

  fun toggleFavoriteMember(member: FamilyMember) {
    viewModelScope.launch {
      repository.updateFamilyMember(member.copy(favorite = !member.favorite))
    }
  }

  fun toggleNotifyOnSchoolDeparture(member: FamilyMember, enabled: Boolean) {
    viewModelScope.launch {
      repository.updateFamilyMember(member.copy(notifyOnSchoolDeparture = enabled)).onSuccess {
        _snackbarMessage.value =
          if (enabled) "School departure alert enabled for ${member.name}"
          else "School departure alert muted for ${member.name}"
      }
    }
  }

  fun requestCheckInFromMember(member: FamilyMember) {
    viewModelScope.launch {
      repository.updateFamilyMember(
        member.copy(
          ghostMode = false,
          online = true,
          liveSharing = true,
          lastActivityTimeLabel = "Just now",
          lastMessageSnippet = "Check-in verified at ${member.locationLabel}",
        )
      )
      repository.addSafetyAlert(
        SafetyAlert(
          userId = currentUserId,
          memberName = member.name,
          alertType = "ARRIVAL",
          title = "SafeCheck Verified: ${member.name}",
          description = "${member.name} confirmed check-in and resumed location sharing (${member.batteryLevel}% battery).",
          timeLabel = "Just now",
          read = false,
        )
      )
      _snackbarMessage.value = "Check-in verified with ${member.name}"
    }
  }

  fun stopAllSharing() {
    val members = (membersState.value as? UiState.Success)?.data.orEmpty()
    viewModelScope.launch {
      for (m in members.filter { it.liveSharing }) {
        repository.updateFamilyMember(m.copy(liveSharing = false))
      }
      val currentProfile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
      repository.createOrUpdateUserProfile(
        currentProfile.copy(
          ghostModeEnabled = true,
          statusMessage = "Ghost Mode Active • Broadcasts Paused",
        )
      )
      _liveBannerPaused.value = true
      _snackbarMessage.value = "All outbound live location broadcasts paused"
    }
  }

  fun toggleEmergencyGhostMode() {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    val nextState = !currentProfile.ghostModeEnabled
    viewModelScope.launch {
      repository
        .createOrUpdateUserProfile(
          currentProfile.copy(
            ghostModeEnabled = nextState,
            statusMessage =
              if (nextState) "Emergency Ghost Mode • All Outbound Telemetry Severed"
              else "Available • Location shared with Family",
          )
        )
        .onSuccess {
          _snackbarMessage.value =
            if (nextState) "Emergency Ghost Mode Activated: All location broadcasts frozen"
            else "Emergency Ghost Mode Deactivated: Family Shield restored"
        }
    }
  }

  fun toggleShareBatteryStatus() {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    val next = !currentProfile.shareBatteryStatus
    viewModelScope.launch {
      repository.createOrUpdateUserProfile(currentProfile.copy(shareBatteryStatus = next))
    }
  }

  fun toggleDarkModeMapStyling() {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    val next = !currentProfile.darkModeMapStyling
    viewModelScope.launch {
      repository.createOrUpdateUserProfile(currentProfile.copy(darkModeMapStyling = next))
    }
  }

  fun toggleSpeedUnit() {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    val nextUnit = if (currentProfile.speedUnit == "MPH") "KMH" else "MPH"
    viewModelScope.launch {
      repository.createOrUpdateUserProfile(currentProfile.copy(speedUnit = nextUnit))
      _snackbarMessage.value = "Speedometer units set to $nextUnit"
    }
  }

  fun selectSubscriptionPlan(planTier: String) {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    viewModelScope.launch {
      repository.createOrUpdateUserProfile(currentProfile.copy(planTier = planTier)).onSuccess {
        _snackbarMessage.value = "Subscription plan updated to $planTier"
      }
    }
  }

  fun uploadUserProfilePhoto(context: android.content.Context, sourceUri: android.net.Uri) {
    val currentProfile =
      (profileState.value as? UiState.Success)?.data
        ?: UserProfile(
          userId = currentUserId,
          displayName = userDisplayName?.takeIf { it.isNotBlank() } ?: "FamLink User",
          email = userEmail?.takeIf { it.isNotBlank() } ?: "user@famlink.app",
        )
    viewModelScope.launch {
      repository
        .saveProfileImageToLocalStorage(context, sourceUri)
        .onSuccess { savedUriString ->
          repository
            .createOrUpdateUserProfile(
              currentProfile.copy(
                avatarUrl = savedUriString,
                displayName = currentProfile.displayName.ifBlank { "FamLink User" },
                email = currentProfile.email.ifBlank { "user@famlink.app" },
              )
            )
            .onSuccess {
              _snackbarMessage.value = "Profile photo uploaded and saved!"
            }
            .onFailure { e ->
              _snackbarMessage.value = e.localizedMessage ?: "Failed to update profile photo"
            }
        }
        .onFailure { e ->
          _snackbarMessage.value = e.localizedMessage ?: "Could not load selected image"
        }
    }
  }

  fun updateUserDisplayName(newName: String) {
    val cleanName = newName.trim().take(80)
    if (cleanName.isBlank()) {
      _snackbarMessage.value = "Display name cannot be empty"
      return
    }
    val currentProfile =
      (profileState.value as? UiState.Success)?.data
        ?: UserProfile(
          userId = currentUserId,
          email = userEmail?.takeIf { it.isNotBlank() } ?: "user@famlink.app",
        )
    viewModelScope.launch {
      repository
        .createOrUpdateUserProfile(
          currentProfile.copy(
            displayName = cleanName,
            email = currentProfile.email.ifBlank { "user@famlink.app" },
          )
        )
        .onSuccess {
          _snackbarMessage.value = "Profile name updated to $cleanName"
        }
    }
  }

  fun updateMemberParentalControls(
    member: FamilyMember,
    screenTimeLimitMinutes: Int = member.screenTimeLimitMinutes,
    appRestrictionsEnabled: Boolean = member.appRestrictionsEnabled,
    websiteSafetyEnabled: Boolean = member.websiteSafetyEnabled,
    bedtimeSchedule: String = member.bedtimeSchedule,
    liveSharing: Boolean = member.liveSharing,
    notifyOnSchoolDeparture: Boolean = member.notifyOnSchoolDeparture,
  ) {
    viewModelScope.launch {
      repository
        .updateFamilyMember(
          member.copy(
            screenTimeLimitMinutes = screenTimeLimitMinutes.coerceIn(15, 1440),
            appRestrictionsEnabled = appRestrictionsEnabled,
            websiteSafetyEnabled = websiteSafetyEnabled,
            bedtimeSchedule = bedtimeSchedule.take(60).ifBlank { "9:30 PM - 6:30 AM" },
            liveSharing = liveSharing,
            notifyOnSchoolDeparture = notifyOnSchoolDeparture,
          )
        )
        .onSuccess {
          _snackbarMessage.value = "Updated parental controls for ${member.name}"
        }
    }
  }

  fun pairNewFamilyDevice(
    name: String,
    role: String,
    pairingCode: String,
    onPaired: (String) -> Unit = {},
  ) {
    val cleanName = name.trim().take(80)
    if (cleanName.isBlank()) {
      _snackbarMessage.value = "Please enter the family member's name"
      return
    }
    val validRole =
      if (role in listOf("SPOUSE", "CHILD", "SIBLING", "GROUP", "BOT")) role else "CHILD"
    val cleanCode =
      pairingCode.trim().uppercase().take(20).ifBlank { "FL-" + (1000..9999).random() }

    viewModelScope.launch {
      repository
        .pairChildDevice(cleanName, validRole, cleanCode)
        .onSuccess { docId ->
          repository.addSafetyAlert(
            SafetyAlert(
              userId = currentUserId,
              memberName = cleanName,
              alertType = "ARRIVAL",
              title = "Pairing Invited ($cleanCode)",
              description = "Created pairing invitation for $cleanName ($validRole). Enter code $cleanCode on their phone to complete setup.",
              timeLabel = "Just now",
              read = false,
            )
          )
          _snackbarMessage.value = "Pairing code $cleanCode ready for $cleanName!"
          onPaired(docId)
        }
        .onFailure { e ->
          _snackbarMessage.value = e.localizedMessage ?: "Failed to create pairing invitation"
        }
    }
  }

  fun completeChildDevicePairing(pairingCode: String, onSuccess: () -> Unit = {}) {
    val cleanCode = pairingCode.trim().uppercase()
    if (cleanCode.isBlank()) {
      _snackbarMessage.value = "Please enter a valid pairing code"
      return
    }
    viewModelScope.launch {
      repository
        .completeChildPairing(
          pairingCode = cleanCode,
          childDeviceId = currentDeviceId,
          childDeviceName = currentDeviceName,
          childDeviceModel = currentDeviceModel,
        )
        .onSuccess { matched ->
          if (matched) {
            _snackbarMessage.value = "This phone is now paired and linked with code $cleanCode!"
            onSuccess()
          } else {
            _snackbarMessage.value = "No pending invitation found for code $cleanCode"
          }
        }
        .onFailure { e ->
          _snackbarMessage.value = e.localizedMessage ?: "Failed to complete pairing"
        }
    }
  }

  fun markAlertRead(alert: SafetyAlert) {
    viewModelScope.launch { repository.markAlertRead(alert) }
  }

  fun markAllAlertsRead() {
    val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()
    viewModelScope.launch {
      repository.markAllAlertsRead(alerts).onSuccess {
        _snackbarMessage.value = "All alerts marked as read"
      }
    }
  }

  fun triggerFamilyCheckInOrEmergency(isEmergency: Boolean) {
    viewModelScope.launch {
      val alert =
        if (isEmergency) {
          SafetyAlert(
            userId = currentUserId,
            memberName = "Family Shield SOS",
            alertType = "EMERGENCY",
            title = "Emergency SOS Check-In Broadcast",
            description =
              "High-priority panic beacon sent to all family devices with GPS coordinates (${String.format("%.4f", _deviceGpsLatitude.value)}, ${String.format("%.4f", _deviceGpsLongitude.value)}).",
            timeLabel = "Just now",
            read = false,
          )
        } else {
          SafetyAlert(
            userId = currentUserId,
            memberName = "You",
            alertType = "ARRIVAL",
            title = "Manual SafeZone Check-In",
            description = "Shared verified GPS check-in with family members.",
            timeLabel = "Just now",
            read = false,
          )
        }
      repository.addSafetyAlert(alert).onSuccess {
        _snackbarMessage.value =
          if (isEmergency) "Emergency SOS broadcast sent to all family devices"
          else "SafeZone check-in shared with family"
      }
    }
  }

  fun addNewSafeZone(name: String, address: String, radiusMeters: Int) {
    val cleanName = name.trim().take(80)
    val cleanAddress = address.trim().take(160)
    if (cleanName.isBlank() || cleanAddress.isBlank()) {
      _snackbarMessage.value = "Please enter both SafeZone name and address"
      return
    }
    viewModelScope.launch {
      val zone =
        SafeZone(
          userId = currentUserId,
          name = cleanName,
          address = cleanAddress,
          radiusMeters = radiusMeters.coerceIn(50, 5000),
          latitude = _deviceGpsLatitude.value,
          longitude = _deviceGpsLongitude.value,
          notifyOnArrival = true,
          notifyOnDeparture = true,
        )
      repository.addSafeZone(zone).onSuccess {
        _snackbarMessage.value = "Added SafeZone \"$cleanName\""
      }
    }
  }

  fun toggleSafeZoneAlerts(zone: SafeZone) {
    viewModelScope.launch {
      val next = !(zone.notifyOnArrival && zone.notifyOnDeparture)
      repository.updateSafeZone(zone.copy(notifyOnArrival = next, notifyOnDeparture = next))
    }
  }

  fun deleteSafeZone(zone: SafeZone) {
    viewModelScope.launch {
      repository.deleteSafeZone(zone.id).onSuccess {
        _snackbarMessage.value = "Removed SafeZone \"${zone.name}\""
      }
    }
  }

  fun exportLocationData() {
    viewModelScope.launch {
      repository
        .exportLocationDataJson()
        .onSuccess { json -> _exportedJsonData.value = json }
        .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Export failed" }
    }
  }

  fun locateRemoteDevice(targetDevice: UserDevice) {
    viewModelScope.launch {
      if (!targetDevice.locationSharingApproved) {
        val session =
          DeviceSession(
            userId = currentUserId,
            fromDeviceId = currentDeviceId,
            fromDeviceName = currentDeviceName,
            toDeviceId = targetDevice.deviceId,
            toDeviceName = targetDevice.name,
            sessionType = "LOCATION",
            status = "REQUESTED",
            offerSdp = "v=0\r\no=LocationRequest",
          )
        repository.createDeviceSession(session).onSuccess {
          _snackbarMessage.value = "Sent Location Sharing request to ${targetDevice.name}. Waiting for authorization..."
        }
      } else {
        _deviceGpsLatitude.value = targetDevice.latitude
        _deviceGpsLongitude.value = targetDevice.longitude
        _deviceGpsAccuracy.value = targetDevice.accuracyMeters
        _snackbarMessage.value = "Located ${targetDevice.name}: ${String.format("%.4f", targetDevice.latitude)}, ${String.format("%.4f", targetDevice.longitude)} (±${targetDevice.accuracyMeters}m)"
      }
    }
  }

  fun requestCameraSession(targetDevice: UserDevice) {
    if (!targetDevice.online) {
      _snackbarMessage.value = "${targetDevice.name} is currently offline. Turn on the device and connect to internet."
      return
    }
    viewModelScope.launch {
      val session =
        DeviceSession(
          userId = currentUserId,
          fromDeviceId = currentDeviceId,
          fromDeviceName = currentDeviceName,
          toDeviceId = targetDevice.deviceId,
          toDeviceName = targetDevice.name,
          sessionType = "CAMERA",
          status = "REQUESTED",
          offerSdp = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=TrackView Video Session\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\nc=IN IP4 0.0.0.0\r\na=rtcp-mux\r\na=sendrecv\r\n",
        )
      repository.createDeviceSession(session).onSuccess { sId ->
        _activeSessionId.value = sId
        _snackbarMessage.value = "Camera session requested. Waiting for approval on ${targetDevice.name}..."
      }.onFailure { e ->
        _snackbarMessage.value = e.localizedMessage ?: "Failed to initiate camera request"
      }
    }
  }

  fun requestVoiceSession(targetDevice: UserDevice) {
    if (!targetDevice.online) {
      _snackbarMessage.value = "${targetDevice.name} is offline. Voice call unavailable."
      return
    }
    viewModelScope.launch {
      val session =
        DeviceSession(
          userId = currentUserId,
          fromDeviceId = currentDeviceId,
          fromDeviceName = currentDeviceName,
          toDeviceId = targetDevice.deviceId,
          toDeviceName = targetDevice.name,
          sessionType = "VOICE",
          status = "REQUESTED",
          offerSdp = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=TrackView Voice Session\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nc=IN IP4 0.0.0.0\r\na=rtcp-mux\r\na=sendrecv\r\n",
        )
      repository.createDeviceSession(session).onSuccess { sId ->
        _activeSessionId.value = sId
        _snackbarMessage.value = "Voice call requested. Ringing ${targetDevice.name}..."
      }.onFailure { e ->
        _snackbarMessage.value = e.localizedMessage ?: "Failed to initiate voice call"
      }
    }
  }

  fun approveIncomingSession(session: DeviceSession) {
    viewModelScope.launch {
      val answerSdp =
        if (session.sessionType == "CAMERA") "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=TrackView Video Answer\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\nc=IN IP4 0.0.0.0\r\na=rtcp-mux\r\na=sendrecv\r\n"
        else "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=TrackView Voice Answer\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nc=IN IP4 0.0.0.0\r\na=rtcp-mux\r\na=sendrecv\r\n"
      repository.respondToDeviceSession(session.sessionId, approve = true, answerSdp = answerSdp).onSuccess {
        _activeSessionId.value = session.sessionId
        _incomingConsentSession.value = null
        repository.addSafetyAlert(
          SafetyAlert(
            userId = currentUserId,
            memberName = session.fromDeviceName,
            alertType = "ARRIVAL",
            title = "${session.sessionType} Session Connected",
            description = "Authorized remote ${session.sessionType.lowercase()} session with ${session.fromDeviceName}.",
            timeLabel = "Just now",
            read = false,
          )
        )
        _snackbarMessage.value = "${session.sessionType} session active with ${session.fromDeviceName}"
      }
    }
  }

  fun rejectIncomingSession(session: DeviceSession) {
    viewModelScope.launch {
      repository.respondToDeviceSession(session.sessionId, approve = false).onSuccess {
        _incomingConsentSession.value = null
        _snackbarMessage.value = "Declined session request from ${session.fromDeviceName}"
      }
    }
  }

  fun dismissIncomingSession() {
    _incomingConsentSession.value = null
  }

  fun endActiveSession() {
    val sId = _activeSessionId.value
    if (!sId.isNullOrBlank()) {
      viewModelScope.launch {
        repository.endDeviceSession(sId)
        _activeSessionId.value = null
        _snackbarMessage.value = "Remote session ended"
      }
    }
  }

  fun renameDevice(targetDevice: UserDevice, newName: String) {
    viewModelScope.launch {
      repository.renameDevice(targetDevice.deviceId, newName).onSuccess {
        _snackbarMessage.value = "Device renamed to \"$newName\""
      }
    }
  }

  fun revokeDeviceAccess(targetDevice: UserDevice) {
    viewModelScope.launch {
      repository.revokeDeviceAccess(targetDevice.deviceId).onSuccess {
        _snackbarMessage.value = "Revoked access and disconnected ${targetDevice.name}"
      }
    }
  }

  fun updateDeviceConsent(
    targetDevice: UserDevice,
    locationApproved: Boolean,
    cameraApproved: Boolean,
    microphoneApproved: Boolean,
  ) {
    viewModelScope.launch {
      repository
        .updateDeviceConsent(targetDevice.deviceId, locationApproved, cameraApproved, microphoneApproved)
        .onSuccess {
          _snackbarMessage.value = "Permissions updated for ${targetDevice.name}"
        }
    }
  }

  fun pairNewPhoneDevice(name: String, model: String, pairingCode: String, onComplete: () -> Unit = {}) {
    val cleanName = name.trim().take(80).ifBlank { "Child / Secondary Phone" }
    val cleanCode = pairingCode.trim().uppercase().take(20).ifBlank { "FL-" + (1000..9999).random() }
    val newId = "phone_" + cleanCode.lowercase().replace("[^a-z0-9]".toRegex(), "_")

    val newDevice =
      UserDevice(
        deviceId = newId,
        userId = currentUserId,
        name = cleanName,
        model = model.ifBlank { "Android Smartphone" },
        online = false,
        batteryLevel = 0,
        latitude = 0.0,
        longitude = 0.0,
        accuracyMeters = 10,
        locationSharingApproved = true,
        cameraSessionApproved = true,
        microphoneApproved = true,
        pairingCode = cleanCode,
      )

    viewModelScope.launch {
      repository.registerOrUpdateDevice(newDevice).onSuccess {
        repository.addSafetyAlert(
          SafetyAlert(
            userId = currentUserId,
            memberName = cleanName,
            alertType = "ARRIVAL",
            title = "New Phone Registered ($cleanCode)",
            description = "$cleanName registered under your family group. Complete pairing on the device using code $cleanCode.",
            timeLabel = "Just now",
            read = false,
          )
        )
        _snackbarMessage.value = "$cleanName registered with code $cleanCode!"
        onComplete()
      }.onFailure { e ->
        _snackbarMessage.value = e.localizedMessage ?: "Failed to register device"
      }
    }
  }

  fun deleteAccountAndWipeData(onComplete: () -> Unit) {
    viewModelScope.launch {
      repository.deleteAccountData().onSuccess {
        onComplete()
      }
    }
  }

  private companion object {
    const val TAG = "TrackViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
