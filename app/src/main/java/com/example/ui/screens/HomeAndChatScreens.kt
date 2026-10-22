package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Avatars
import com.example.data.model.ChatMessage
import com.example.data.model.FamilyMember
import com.example.data.model.SafeZone
import com.example.data.model.SafetyAlert
import com.example.data.model.UserProfile
import com.example.ui.theme.TrackViewChatWallpaperDark
import com.example.ui.theme.TrackViewChatWallpaperLight
import com.example.ui.theme.TrackViewDarkGreen
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewIncomingBubbleDark
import com.example.ui.theme.TrackViewIncomingBubbleLight
import com.example.ui.theme.TrackViewOutgoingBubbleDark
import com.example.ui.theme.TrackViewOutgoingBubbleLight
import com.example.ui.theme.TrackViewTeal
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
  profileState: UiState<UserProfile?>,
  membersState: UiState<List<FamilyMember>>,
  alertsState: UiState<List<SafetyAlert>>,
  safeZonesState: UiState<List<SafeZone>>,
  selectedFilter: String,
  searchQuery: String,
  liveBannerDismissed: Boolean,
  liveBannerPaused: Boolean,
  onSelectFilter: (String) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onToggleLiveBannerPaused: () -> Unit,
  onDismissLiveBanner: () -> Unit,
  onMemberClick: (FamilyMember) -> Unit,
  onOpenMapTab: (String?) -> Unit,
  onOpenControlsTab: () -> Unit,
  onOpenAlertsTab: () -> Unit,
  onOpenSettingsTab: () -> Unit,
  onOpenPairingModal: () -> Unit,
  onOpenPremiumModal: () -> Unit,
  onTriggerCheckIn: () -> Unit,
  onOpenDevicesTab: (() -> Unit)? = null,
) {
  var showSearchBar by rememberSaveable { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }

  val members = (membersState as? UiState.Success)?.data.orEmpty()
  val alerts = (alertsState as? UiState.Success)?.data.orEmpty()
  val safeZones = (safeZonesState as? UiState.Success)?.data.orEmpty()
  val profile = (profileState as? UiState.Success)?.data
  val unreadAlertsCount = alerts.count { !it.read }

  val filteredMembers =
    remember(members, selectedFilter, searchQuery) {
      members.filter { member ->
        val matchesQuery =
          searchQuery.isBlank() ||
            member.name.contains(searchQuery, ignoreCase = true) ||
            member.locationLabel.contains(searchQuery, ignoreCase = true) ||
            member.lastMessageSnippet.contains(searchQuery, ignoreCase = true)
        val matchesFilter =
          when (selectedFilter) {
            "UNREAD" -> member.unreadCount > 0
            "FAVORITES" -> member.favorite
            "FAMILY" -> member.role in listOf("SPOUSE", "CHILD", "SIBLING")
            "GROUPS" -> member.role == "GROUP"
            else -> true
          }
        matchesQuery && matchesFilter
      }
    }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top WhatsApp-inspired Header Bar
      Surface(
        color =
          if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface
          else TrackViewDarkGreen,
        contentColor = Color.White,
        shadowElevation = 2.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier =
              Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TrackViewGreen.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = "TrackView Shield Logo",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
              }
              Column {
                Text(
                  text = "TrackView",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text =
                    if (profile?.ghostModeEnabled == true) "Ghost Mode Active"
                    else "Family Safety & GPS",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.8f),
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onOpenPairingModal,
                modifier = Modifier.testTag("home_qr_pair_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.QrCodeScanner,
                  contentDescription = "Pair Family Device",
                  tint = Color.White,
                )
              }

              IconButton(
                onClick = { showSearchBar = !showSearchBar },
                modifier = Modifier.testTag("home_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search Family",
                  tint = Color.White,
                )
              }

              IconButton(
                onClick = onOpenAlertsTab,
                modifier = Modifier.testTag("home_notifications_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (unreadAlertsCount > 0) {
                      Badge(containerColor = TrackViewGreen, contentColor = Color.White) {
                        Text(unreadAlertsCount.toString())
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Safety Alerts",
                    tint = Color.White,
                  )
                }
              }

              // Profile Avatar
              IconButton(
                onClick = onOpenSettingsTab,
                modifier = Modifier.testTag("home_profile_avatar_button"),
              ) {
                AsyncImage(
                  model = profile?.avatarUrl ?: Avatars.DAVID,
                  contentDescription = "Profile Settings",
                  modifier =
                    Modifier.size(30.dp)
                      .clip(CircleShape)
                      .border(1.5.dp, TrackViewGreen, CircleShape),
                  contentScale = ContentScale.Crop,
                )
              }

              Box {
                IconButton(
                  onClick = { showOverflowMenu = true },
                  modifier = Modifier.testTag("home_overflow_menu_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.White,
                  )
                }
                DropdownMenu(
                  expanded = showOverflowMenu,
                  onDismissRequest = { showOverflowMenu = false },
                ) {
                  DropdownMenuItem(
                    text = { Text("Pair New Family Device") },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      onOpenPairingModal()
                    },
                  )
                  if (onOpenDevicesTab != null) {
                    DropdownMenuItem(
                      text = { Text("Connected Phones & Remote") },
                      leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                      onClick = {
                        showOverflowMenu = false
                        onOpenDevicesTab()
                      },
                    )
                  }
                  DropdownMenuItem(
                    text = { Text("Send SafeZone Check-In") },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      onTriggerCheckIn()
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Parental Controls") },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      onOpenControlsTab()
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Family Shield Premium") },
                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      onOpenPremiumModal()
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Settings & Privacy") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      onOpenSettingsTab()
                    },
                  )
                }
              }
            }
          }

          AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChange,
              placeholder = { Text("Search family members, locations, or messages...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true,
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp)
                  .testTag("home_search_input"),
              shape = RoundedCornerShape(24.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                  focusedTextColor = MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
          }
        }
      }

      if (membersState is UiState.Loading && members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = TrackViewGreen)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("home_member_list"),
          contentPadding = PaddingValues(bottom = 96.dp),
        ) {
          // 1. Horizontally Scrollable Family Circle Section
          item {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surface)
                  .padding(top = 12.dp, bottom = 10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "FAMILY CIRCLE • LIVE STATUS",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${members.count { it.online }}/${members.size} Online",
                  style = MaterialTheme.typography.labelSmall,
                  color = TrackViewTeal,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.testTag("family_circle_horizontal_row"),
              ) {
                items(members, key = { "circle_${it.id}" }) { member ->
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                      Modifier.width(64.dp)
                        .clickable { onMemberClick(member) }
                        .testTag("circle_avatar_${member.id}"),
                  ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                      AsyncImage(
                        model = member.avatarUrl.ifBlank { Avatars.SARAH },
                        contentDescription = "${member.name} avatar",
                        modifier =
                          Modifier.size(56.dp)
                            .clip(CircleShape)
                            .border(
                              width = if (member.liveSharing) 2.5.dp else 1.dp,
                              color =
                                if (member.liveSharing) TrackViewGreen
                                else MaterialTheme.colorScheme.outlineVariant,
                              shape = CircleShape,
                            ),
                        contentScale = ContentScale.Crop,
                      )
                      Box(
                        modifier =
                          Modifier.size(14.dp)
                            .clip(CircleShape)
                            .background(
                              if (member.online) TrackViewGreen else Color(0xFF9CA3AF)
                            )
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = member.name.substringBefore(" "),
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      fontWeight = FontWeight.Medium,
                    )
                  }
                }
                item {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                      Modifier.width(64.dp)
                        .clickable { onOpenPairingModal() }
                        .testTag("circle_add_device_button"),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(56.dp)
                          .clip(CircleShape)
                          .background(TrackViewGreen.copy(alpha = 0.12f))
                          .border(1.5.dp, TrackViewGreen, CircleShape),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Family Device",
                        tint = TrackViewGreen,
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Pair New",
                      style = MaterialTheme.typography.labelSmall,
                      color = TrackViewTeal,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
          }

          // 2. Family Safety Overview Card
          item {
            val latestAlert = alerts.firstOrNull()
            val children = members.filter { it.role == "CHILD" }
            val avgChildScreenTimeMins =
              if (children.isNotEmpty()) children.sumOf { it.screenTimeMinutesToday } / children.size
              else 135

            ElevatedCard(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("family_safety_overview_card"),
              shape = RoundedCornerShape(18.dp),
              colors =
                CardDefaults.elevatedCardColors(
                  containerColor = MaterialTheme.colorScheme.surface
                ),
              elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(32.dp)
                          .clip(CircleShape)
                          .background(TrackViewGreen.copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TrackViewTeal,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Column {
                      Text(
                        text = "Family Safety Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "Encrypted Mesh • ${safeZones.size} Active SafeZones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Surface(
                    color = TrackViewGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                  ) {
                    Text(
                      text = "PROTECTED",
                      style = MaterialTheme.typography.labelSmall,
                      color = TrackViewTeal,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OverviewMetricTile(
                    modifier =
                      Modifier.weight(1f).clickable {
                        if (onOpenDevicesTab != null) onOpenDevicesTab() else onOpenMapTab(null)
                      },
                    title = "Connected Devices",
                    value = "${members.size} Paired",
                    subtitle = "${members.count { it.liveSharing }} sharing live GPS",
                  )
                  OverviewMetricTile(
                    modifier = Modifier.weight(1f).clickable { onOpenControlsTab() },
                    title = "Screen Time Avg",
                    value = "${avgChildScreenTimeMins / 60}h ${avgChildScreenTimeMins % 60}m",
                    subtitle = "2 children monitored",
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onOpenAlertsTab() },
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TrackViewTeal,
                        modifier = Modifier.size(18.dp),
                      )
                      Column {
                        Text(
                          text = latestAlert?.title ?: "All family members inside SafeZones",
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text =
                            latestAlert?.let { "${it.memberName} • ${it.timeLabel}" }
                              ?: "Location sharing verified",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Text(
                      text = "Alerts →",
                      style = MaterialTheme.typography.labelMedium,
                      color = TrackViewTeal,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }

          // 3. Active Live Sharing Banner
          if (!liveBannerDismissed) {
            item {
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("active_live_sharing_banner"),
                shape = RoundedCornerShape(14.dp),
                color = TrackViewGreen.copy(alpha = 0.14f),
                border =
                  androidx.compose.foundation.BorderStroke(
                    1.dp,
                    TrackViewGreen.copy(alpha = 0.35f),
                  ),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(32.dp)
                          .clip(CircleShape)
                          .background(TrackViewGreen),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Column {
                      Text(
                        text =
                          if (liveBannerPaused) "Live Location Broadcast Paused"
                          else "Sharing Live Location with Family",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text =
                          if (liveBannerPaused) "Tap Resume to restore telemetry"
                          else "Remaining: 42m 18s • High Accuracy GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                      onClick = onToggleLiveBannerPaused,
                      modifier = Modifier.testTag("banner_pause_resume_button"),
                    ) {
                      Text(
                        text = if (liveBannerPaused) "Resume" else "Pause",
                        color = TrackViewTeal,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    IconButton(
                      onClick = onDismissLiveBanner,
                      modifier = Modifier.size(28.dp).testTag("banner_dismiss_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Live Banner",
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              }
            }
          }

          // 4. Filter Pills Row
          item {
            val filters =
              listOf(
                "ALL" to "All",
                "UNREAD" to "Unread",
                "FAVORITES" to "Favorites",
                "FAMILY" to "Family",
                "GROUPS" to "Groups",
              )
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(filters, key = { it.first }) { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                  selected = isSelected,
                  onClick = { onSelectFilter(key) },
                  label = {
                    Text(
                      text = label,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                  },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = TrackViewGreen.copy(alpha = 0.2f),
                      selectedLabelColor = TrackViewTeal,
                    ),
                  modifier = Modifier.testTag("filter_chip_${key.lowercase()}"),
                )
              }
            }
          }

          // 5. WhatsApp-style Family Conversation & Location List
          items(filteredMembers, key = { it.id }) { member ->
            FamilyMemberConversationRow(
              member = member,
              onClick = { onMemberClick(member) },
            )
            HorizontalDivider(
              modifier = Modifier.padding(start = 82.dp, end = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
          }

          // 6. Active SafeZones Summary Footer Card
          item {
            Card(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 14.dp)
                  .clickable { onOpenMapTab(null) }
                  .testTag("home_safezones_card"),
              shape = RoundedCornerShape(16.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
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
                      Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(TrackViewTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Place,
                      contentDescription = null,
                      tint = TrackViewTeal,
                    )
                  }
                  Column {
                    Text(
                      text = "Active SafeZones (${safeZones.size} Configured)",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text =
                        safeZones.joinToString(" • ") { it.name }.ifBlank {
                          "Home • Lincoln High • Soccer Academy"
                        },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Text(
                  text = "View Map",
                  style = MaterialTheme.typography.labelMedium,
                  color = TrackViewTeal,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    // Floating Action Button for Pairing a New Family Device
    FloatingActionButton(
      onClick = onOpenPairingModal,
      containerColor = TrackViewGreen,
      contentColor = Color.White,
      shape = RoundedCornerShape(16.dp),
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(16.dp)
          .testTag("pair_new_device_fab"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = "Pair New Family Device",
        )
        Text(
          text = "Pair Device",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun OverviewMetricTile(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  subtitle: String,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = TrackViewTeal,
      )
    }
  }
}

@Composable
private fun FamilyMemberConversationRow(
  member: FamilyMember,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .testTag("member_row_${member.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Avatar with Live Ring + Online Dot
    Box(contentAlignment = Alignment.BottomEnd) {
      AsyncImage(
        model = member.avatarUrl.ifBlank { Avatars.SARAH },
        contentDescription = "${member.name} avatar",
        modifier =
          Modifier.size(54.dp)
            .clip(CircleShape)
            .border(
              width = if (member.liveSharing) 2.5.dp else 1.dp,
              color =
                if (member.liveSharing) TrackViewGreen
                else MaterialTheme.colorScheme.outlineVariant,
              shape = CircleShape,
            ),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.size(14.dp)
            .clip(CircleShape)
            .background(if (member.online) TrackViewGreen else Color(0xFF9CA3AF))
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = member.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = member.role,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
        Text(
          text = member.lastActivityTimeLabel,
          style = MaterialTheme.typography.labelSmall,
          color =
            if (member.unreadCount > 0) TrackViewGreen
            else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (member.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
        )
      }

      Spacer(modifier = Modifier.height(3.dp))

      // Location label line
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = if (member.liveSharing) TrackViewGreen else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = member.locationLabel,
          style = MaterialTheme.typography.labelSmall,
          color =
            if (member.liveSharing) TrackViewTeal
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontWeight = FontWeight.Medium,
        )
      }

      Spacer(modifier = Modifier.height(3.dp))

      // Snippet & Badges row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          if (member.liveSharing) {
            Surface(
              color = TrackViewGreen,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          } else {
            Icon(
              imageVector = Icons.Default.DoneAll,
              contentDescription = null,
              tint = Color(0xFF53BDEB),
              modifier = Modifier.size(15.dp),
            )
          }
          Text(
            text = member.lastMessageSnippet,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          // Battery indicator
          Surface(
            color =
              if (member.batteryLevel <= 20) MaterialTheme.colorScheme.errorContainer
              else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint =
                  if (member.batteryLevel <= 20) MaterialTheme.colorScheme.error
                  else TrackViewTeal,
              )
              Text(
                text = "${member.batteryLevel}%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          if (member.pinned) {
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pinned",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
          }

          if (member.unreadCount > 0) {
            Box(
              modifier =
                Modifier.size(20.dp)
                  .clip(CircleShape)
                  .background(TrackViewGreen),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = member.unreadCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MemberDetailAndChatScreen(
  member: FamilyMember,
  messagesState: UiState<List<ChatMessage>>,
  alertsState: UiState<List<SafetyAlert>>,
  onBack: () -> Unit,
  onOpenMapForMember: (FamilyMember) -> Unit,
  onOpenControlsForMember: (FamilyMember) -> Unit,
  onSendMessage: (String, String) -> Unit,
  onShareLiveLocation: () -> Unit,
  onSendEta: () -> Unit,
  onRequestCheckIn: () -> Unit,
  onToggleLiveSharing: (Boolean) -> Unit,
  onToggleSchoolNotification: (Boolean) -> Unit,
  onNotifyAction: (String) -> Unit,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  var messageInput by rememberSaveable { mutableStateOf("") }
  var showDetailsCard by rememberSaveable { mutableStateOf(true) }
  var showAttachmentDrawer by rememberSaveable { mutableStateOf(false) }

  val messages = (messagesState as? UiState.Success)?.data.orEmpty()
  val memberAlerts =
    (alertsState as? UiState.Success)
      ?.data
      .orEmpty()
      .filter { it.memberName.contains(member.name.substringBefore(" "), ignoreCase = true) }
      .take(2)

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(if (isDark) TrackViewChatWallpaperDark else TrackViewChatWallpaperLight)
  ) {
    // 1. Familiar WhatsApp-Style Top App Bar
    Surface(
      color = if (isDark) MaterialTheme.colorScheme.surface else TrackViewDarkGreen,
      contentColor = Color.White,
      shadowElevation = 3.dp,
    ) {
      Column {
        Row(
          modifier =
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("member_detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Home",
              tint = Color.White,
            )
          }

          Row(
            modifier =
              Modifier.weight(1f)
                .clickable { showDetailsCard = !showDetailsCard }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AsyncImage(
              model = member.avatarUrl.ifBlank { Avatars.SARAH },
              contentDescription = "${member.name} avatar",
              modifier =
                Modifier.size(40.dp)
                  .clip(CircleShape)
                  .border(2.dp, TrackViewGreen, CircleShape),
              contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (member.online)
                    "Online • ${member.batteryLevel}% battery • ${if (member.liveSharing) "Sharing GPS" else "Standby"}"
                  else "Last seen ${member.lastActivityTimeLabel} • ${member.batteryLevel}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          IconButton(
            onClick = {
              onNotifyAction("Starting encrypted video check-in with ${member.name}...")
            },
            modifier = Modifier.testTag("member_video_call_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = "Video Call",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = {
              val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:5550192"))
              runCatching { context.startActivity(dialIntent) }
                .onFailure { onNotifyAction("Dialing ${member.name}...") }
            },
            modifier = Modifier.testTag("member_voice_call_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = "Voice Call",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = { showDetailsCard = !showDetailsCard },
            modifier = Modifier.testTag("member_toggle_details_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Toggle Member Safety Details",
              tint = Color.White,
            )
          }
        }

        // Sub-header Live Location & Telemetry Strip
        Surface(
          color = Color.Black.copy(alpha = 0.2f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = TrackViewGreen,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text =
                  if (member.speedMph > 0)
                    "Live: ${member.speedMph} mph • ${member.locationLabel}"
                  else member.locationLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            TextButton(
              onClick = { onOpenMapForMember(member) },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
              modifier = Modifier.height(26.dp).testTag("header_view_on_map_button"),
            ) {
              Text(
                text = "View on Map →",
                style = MaterialTheme.typography.labelSmall,
                color = TrackViewGreen,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // 2. Scrollable Content: Member Safety Details + Chat Messages
    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Expandable Family Member Details Card (Section 4 of User Prompt)
      if (showDetailsCard) {
        item {
          ElevatedCard(
            modifier =
              Modifier.fillMaxWidth().testTag("member_safety_details_card"),
            shape = RoundedCornerShape(18.dp),
            colors =
              CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              // Top Profile Header
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                AsyncImage(
                  model = member.avatarUrl.ifBlank { Avatars.SARAH },
                  contentDescription = "${member.name} large avatar",
                  modifier =
                    Modifier.size(68.dp)
                      .clip(CircleShape)
                      .border(3.dp, TrackViewGreen, CircleShape),
                  contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = member.name,
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                    )
                    Surface(
                      color = TrackViewGreen.copy(alpha = 0.15f),
                      shape = RoundedCornerShape(6.dp),
                    ) {
                      Text(
                        text = member.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrackViewTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text =
                      if (member.online) "Connected • Paired (${member.pairingCode})"
                      else "Standby • Last active ${member.lastActivityTimeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "Last location: ${member.locationLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackViewTeal,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Primary Actions Row: View on Map, Directions, Check-In
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = { onOpenMapForMember(member) },
                  modifier = Modifier.weight(1f).testTag("detail_view_on_map_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("View on Map", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                  onClick = {
                    val geoUri =
                      Uri.parse(
                        "geo:${member.latitude},${member.longitude}?q=${member.latitude},${member.longitude}(${Uri.encode(member.name)})"
                      )
                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                    runCatching { context.startActivity(mapIntent) }
                      .onFailure {
                        onNotifyAction(
                          "Route calculated to ${member.name} (${String.format("%.4f", member.latitude)}, ${String.format("%.4f", member.longitude)})"
                        )
                      }
                  },
                  modifier = Modifier.weight(1f).testTag("detail_directions_button"),
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Directions", style = MaterialTheme.typography.labelMedium)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Telemetry Row: Screen Time + Battery + Speed
              val screenTimeProgress =
                (member.screenTimeMinutesToday.toFloat() /
                    member.screenTimeLimitMinutes.coerceAtLeast(1).toFloat())
                  .coerceIn(0f, 1f)

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TrackViewTeal,
                        modifier = Modifier.size(16.dp),
                      )
                      Text(
                        text = "Screen Time Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    Text(
                      text =
                        "${member.screenTimeMinutesToday / 60}h ${member.screenTimeMinutesToday % 60}m / ${member.screenTimeLimitMinutes / 60}h ${member.screenTimeLimitMinutes % 60}m",
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  LinearProgressIndicator(
                    progress = { screenTimeProgress },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (screenTimeProgress > 0.85f) Color(0xFFF59E0B) else TrackViewGreen,
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Bedtime: ${member.bedtimeSchedule} • Battery: ${member.batteryLevel}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = "Parental Controls →",
                      style = MaterialTheme.typography.labelSmall,
                      color = TrackViewTeal,
                      fontWeight = FontWeight.Bold,
                      modifier =
                        Modifier.clickable { onOpenControlsForMember(member) }
                          .testTag("detail_open_controls_link"),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Location-Sharing Settings Toggles
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Real-Time GPS Sharing",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Mutual family location & battery telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = member.liveSharing,
                  onCheckedChange = onToggleLiveSharing,
                  modifier = Modifier.testTag("detail_live_sharing_switch"),
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "SafeZone Arrival & Departure Alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Instant push notification when crossing school/home boundary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = member.notifyOnSchoolDeparture,
                  onCheckedChange = onToggleSchoolNotification,
                  modifier = Modifier.testTag("detail_safezone_alert_switch"),
                )
              }

              // Recent member alerts if any
              if (memberAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Recent Safety Activity",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                )
                memberAlerts.forEach { alert ->
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "• ${alert.title}",
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Text(
                      text = alert.timeLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Encryption Notice Pill
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Surface(
            color =
              if (isDark) Color(0xFF182229) else Color(0xFFFFF5C4),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(vertical = 4.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF54656F),
              )
              Text(
                text =
                  "Locations & messages are end-to-end encrypted. Only paired family members can view.",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFFD1D7DB) else Color(0xFF54656F),
              )
            }
          }
        }
      }

      // Chat Messages List
      items(messages, key = { it.id.ifBlank { it.text + it.timeLabel } }) { message ->
        when (message.messageType) {
          "GEOFENCE_EVENT" -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              Surface(
                color = TrackViewTeal.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = TrackViewTeal,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "${message.text} • ${message.timeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackViewTeal,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          }
          "LIVE_LOCATION" -> {
            LiveLocationChatBubble(
              message = message,
              member = member,
              isDark = isDark,
              onOpenMap = { onOpenMapForMember(member) },
            )
          }
          else -> {
            StandardChatBubble(message = message, isDark = isDark)
          }
        }
      }
    }

    // 3. Quick Contextual Action Chips Bar
    LazyRow(
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 1.dp,
          modifier =
            Modifier.clickable { onShareLiveLocation() }
              .testTag("quick_chip_share_live_location"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              tint = TrackViewGreen,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "Share my live location",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
      item {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 1.dp,
          modifier =
            Modifier.clickable { onSendEta() }.testTag("quick_chip_send_eta"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = TrackViewTeal,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "Send ETA",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
      item {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 1.dp,
          modifier =
            Modifier.clickable { onRequestCheckIn() }.testTag("quick_chip_request_check_in"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = TrackViewTeal,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "Request SafeCheck",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }

    // 4. Optional Attachment Drawer
    AnimatedVisibility(visible = showAttachmentDrawer) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          AttachmentOptionButton(
            label = "Live Pin",
            icon = Icons.Default.MyLocation,
            tint = TrackViewGreen,
            onClick = {
              showAttachmentDrawer = false
              onShareLiveLocation()
            },
          )
          AttachmentOptionButton(
            label = "Send ETA",
            icon = Icons.Default.Directions,
            tint = TrackViewTeal,
            onClick = {
              showAttachmentDrawer = false
              onSendEta()
            },
          )
          AttachmentOptionButton(
            label = "SafeCheck",
            icon = Icons.Default.CheckCircle,
            tint = Color(0xFF0284C7),
            onClick = {
              showAttachmentDrawer = false
              onRequestCheckIn()
            },
          )
          AttachmentOptionButton(
            label = "Controls",
            icon = Icons.Default.Security,
            tint = Color(0xFF7C3AED),
            onClick = {
              showAttachmentDrawer = false
              onOpenControlsForMember(member)
            },
          )
        }
      }
    }

    // 5. WhatsApp-style Message Input Bar
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { showAttachmentDrawer = !showAttachmentDrawer },
            modifier = Modifier.testTag("chat_attachment_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Share Location or Attachment",
              tint = TrackViewTeal,
            )
          }
          OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("Message ${member.name.substringBefore(" ")}...") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("chat_message_input"),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
              ),
          )
        }
      }

      FloatingActionButton(
        onClick = {
          if (messageInput.isNotBlank()) {
            onSendMessage(messageInput, "TEXT")
            messageInput = ""
          } else {
            onShareLiveLocation()
          }
        },
        shape = CircleShape,
        containerColor = TrackViewGreen,
        contentColor = Color.White,
        modifier = Modifier.size(48.dp).testTag("chat_send_or_mic_button"),
      ) {
        Icon(
          imageVector =
            if (messageInput.isNotBlank()) Icons.AutoMirrored.Filled.Send
            else Icons.Default.Mic,
          contentDescription = if (messageInput.isNotBlank()) "Send Message" else "Send Voice / Quick Location",
        )
      }
    }
  }
}

@Composable
private fun AttachmentOptionButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
  ) {
    Box(
      modifier =
        Modifier.size(46.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = tint)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun StandardChatBubble(message: ChatMessage, isDark: Boolean) {
  val bubbleColor =
    if (message.outgoing) {
      if (isDark) TrackViewOutgoingBubbleDark else TrackViewOutgoingBubbleLight
    } else {
      if (isDark) TrackViewIncomingBubbleDark else TrackViewIncomingBubbleLight
    }

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = if (message.outgoing) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Surface(
      color = bubbleColor,
      shape =
        RoundedCornerShape(
          topStart = 14.dp,
          topEnd = 14.dp,
          bottomStart = if (message.outgoing) 14.dp else 4.dp,
          bottomEnd = if (message.outgoing) 4.dp else 14.dp,
        ),
      shadowElevation = 1.dp,
      modifier = Modifier.widthIn(max = 310.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = message.timeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
          )
          if (message.outgoing) {
            Icon(
              imageVector = Icons.Default.DoneAll,
              contentDescription = "Delivered",
              tint = Color(0xFF53BDEB),
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiveLocationChatBubble(
  message: ChatMessage,
  member: FamilyMember,
  isDark: Boolean,
  onOpenMap: () -> Unit,
) {
  val bubbleColor =
    if (message.outgoing) {
      if (isDark) TrackViewOutgoingBubbleDark else TrackViewOutgoingBubbleLight
    } else {
      if (isDark) TrackViewIncomingBubbleDark else TrackViewIncomingBubbleLight
    }

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = if (message.outgoing) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Surface(
      color = bubbleColor,
      shape = RoundedCornerShape(16.dp),
      shadowElevation = 2.dp,
      modifier =
        Modifier.widthIn(max = 320.dp)
          .clickable { onOpenMap() }
          .testTag("live_location_chat_card"),
    ) {
      Column(modifier = Modifier.padding(8.dp)) {
        // Mini Map Header Preview
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(118.dp)
              .clip(RoundedCornerShape(12.dp))
        ) {
          AsyncImage(
            model = Avatars.MAP_BG,
            contentDescription = "Live Map Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f))
                  )
                )
          )
          Surface(
            color = TrackViewGreen,
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
          ) {
            Text(
              text = "LIVE TELEMETRY • ACTIVE",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }

          AsyncImage(
            model = member.avatarUrl.ifBlank { Avatars.SARAH },
            contentDescription = null,
            modifier =
              Modifier.size(44.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .border(2.5.dp, TrackViewGreen, CircleShape),
            contentScale = ContentScale.Crop,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = message.text,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21),
          modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
          onClick = onOpenMap,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "View Live Map & Route",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
