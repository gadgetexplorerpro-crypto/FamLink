package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TrackViewRepository
import com.example.ui.theme.TrackViewDarkGreen
import com.example.ui.theme.TrackViewGreen
import com.example.ui.theme.TrackViewTeal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type received.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
    }
  }
}

fun signOutTrackView(
  context: Context,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  val credentialManager = CredentialManager.create(context)
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

const val TRACKVIEW_PRIVACY_URL =
  "https://ais-dev-gxmo6rckjutoh5xrf6xmo3-861509315491.asia-east1.run.app/privacy.html"
const val TRACKVIEW_TERMS_URL =
  "https://ais-dev-gxmo6rckjutoh5xrf6xmo3-861509315491.asia-east1.run.app/terms.html"
const val TRACKVIEW_DELETE_ACCOUNT_URL =
  "https://ais-dev-gxmo6rckjutoh5xrf6xmo3-861509315491.asia-east1.run.app/delete-account.html"
const val TRACKVIEW_DATA_DELETION_URL =
  "https://ais-dev-gxmo6rckjutoh5xrf6xmo3-861509315491.asia-east1.run.app/data-deletion.html"
const val TRACKVIEW_SUPPORT_EMAIL = "gadgetexplorerpro@gmail.com"

enum class LegalTab(val title: String) {
  PRIVACY("Privacy Policy"),
  TERMS("Terms of Service"),
  DELETE_ACCOUNT("Delete Account"),
}

private data class OnboardingStep(
  val title: String,
  val subtitle: String,
  val badge: String,
  val icon: ImageVector,
)

@Composable
fun AuthAndOnboardingScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showPermissionDetails by remember { mutableStateOf(false) }
  var currentStepIndex by remember { mutableIntStateOf(0) }
  var legalDialogTab by remember { mutableStateOf<LegalTab?>(null) }
  var isDeletingAccount by remember { mutableStateOf(false) }

  val steps = remember {
    listOf(
      OnboardingStep(
        title = "Real-Time Family GPS & Chat",
        subtitle =
          "Stay connected with your spouse and children in a familiar, ultra-fast interface with live route cards, ETA updates, and battery telemetry.",
        badge = "Mutual Consent Verified",
        icon = Icons.Default.LocationOn,
      ),
      OnboardingStep(
        title = "SafeZone Geofence Protection",
        subtitle =
          "Receive instant arrival and departure chimes when kids reach Home, School, or Soccer Practice—without constant manual check-ins.",
        badge = "End-to-End Encrypted",
        icon = Icons.Default.Shield,
      ),
      OnboardingStep(
        title = "Respectful Parental Controls",
        subtitle =
          "Manage daily screen time, bedtime schedules, and website safety transparently, backed by an instant Emergency Ghost Mode kill-switch.",
        badge = "Zero Covert Surveillance",
        icon = Icons.Default.VerifiedUser,
      ),
    )
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Brand Header
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier =
            Modifier.size(76.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer,
                  )
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "TrackView Shield Logo",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(42.dp),
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "TrackView",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Privacy-First Family Safety & Parental Controls",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Onboarding Feature Card
      val step = steps[currentStepIndex]
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = step.badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Box(
            modifier =
              Modifier.size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = step.icon,
              contentDescription = step.title,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp),
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = step.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Step Indicators & Next Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              steps.indices.forEach { idx ->
                Box(
                  modifier =
                    Modifier.height(8.dp)
                      .width(if (idx == currentStepIndex) 24.dp else 8.dp)
                      .clip(CircleShape)
                      .background(
                        if (idx == currentStepIndex) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.outlineVariant
                      )
                )
              }
            }
            TextButton(
              onClick = { currentStepIndex = (currentStepIndex + 1) % steps.size },
              modifier = Modifier.testTag("onboarding_next_button"),
            ) {
              Text(if (currentStepIndex < steps.size - 1) "Next Feature" else "Replay Tour")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Transparent Permission & Privacy Disclosure
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Transparent Permissions & Consent",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            TextButton(onClick = { showPermissionDetails = !showPermissionDetails }) {
              Text(if (showPermissionDetails) "Hide" else "Details")
            }
          }

          AnimatedVisibility(visible = showPermissionDetails) {
            Column(
              modifier = Modifier.padding(top = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp).padding(top = 2.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    "Fine Location (GPS): Used only while sharing with approved family members or verifying SafeZone boundaries.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp).padding(top = 2.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    "Safety Notifications: Delivers instant arrival/departure chimes, low battery warnings, and SOS alerts.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Authentication Section (Mandatory Google Sign-In via Credential Manager)
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (errorMessage != null) {
          Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading,
          modifier =
            Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = { showPermissionDetails = !showPermissionDetails },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("review_privacy_button"),
          shape = RoundedCornerShape(50),
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Review Mutual Consent & Encryption Policy")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Policy, Terms of Service, and Delete Account Links Row
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier =
              Modifier.clickable { legalDialogTab = LegalTab.PRIVACY }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("login_privacy_policy_link"),
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
              Modifier.clickable { legalDialogTab = LegalTab.TERMS }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("login_terms_link"),
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
              Modifier.clickable { legalDialogTab = LegalTab.DELETE_ACCOUNT }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("login_delete_account_link"),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Your family chats and live location streams are end-to-end encrypted.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
          textAlign = TextAlign.Center,
        )
      }
    }
  }

  // Legal & Privacy Dialog (Tabs: Privacy Policy, Terms of Service, Delete Account)
  legalDialogTab?.let { tab ->
    LegalAndPrivacyDialog(
      initialTab = tab,
      onDismiss = { legalDialogTab = null },
      actionButtonLabel = "Sign In to Delete Account Now",
      onInitiateAccountDeletion = {
        legalDialogTab = null
        isDeletingAccount = true
        onGoogleSignInClicked(
          context = context,
          credentialManager = credentialManager,
          onAuthSuccess = {
            scope.launch {
              try {
                val repository = TrackViewRepository(context.applicationContext)
                repository.deleteAccountData()
                signOutTrackView(
                  context = context,
                  onSignOutComplete = {
                    isDeletingAccount = false
                    Toast.makeText(
                      context,
                      "TrackView account and all cloud data permanently deleted.",
                      Toast.LENGTH_LONG,
                    ).show()
                  },
                  scope = scope,
                )
              } catch (e: Exception) {
                isDeletingAccount = false
                Toast.makeText(
                  context,
                  "Error deleting data: ${e.message}",
                  Toast.LENGTH_SHORT,
                ).show()
              }
            }
          },
          onAuthError = { err ->
            isDeletingAccount = false
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
          },
          scope = scope,
          onAuthCancelled = { isDeletingAccount = false },
        )
      },
    )
  }
}

@Composable
fun LegalAndPrivacyDialog(
  initialTab: LegalTab,
  onDismiss: () -> Unit,
  onInitiateAccountDeletion: () -> Unit,
  actionButtonLabel: String = "Delete Account & Wipe Cloud Data Now",
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var selectedTab by remember { mutableStateOf(initialTab) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.fillMaxWidth().testTag("legal_privacy_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = TrackViewGreen,
            modifier = Modifier.size(24.dp),
          )
          Text(
            text = "TrackView Legal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close dialog")
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
          selectedTabIndex = selectedTab.ordinal,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = TrackViewGreen,
        ) {
          LegalTab.entries.forEach { tab ->
            Tab(
              selected = selectedTab == tab,
              onClick = { selectedTab = tab },
              text = {
                Text(
                  text =
                    when (tab) {
                      LegalTab.PRIVACY -> "Privacy"
                      LegalTab.TERMS -> "Terms"
                      LegalTab.DELETE_ACCOUNT -> "Delete"
                    },
                  fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 12.sp,
                  maxLines = 1,
                )
              },
              icon = {
                Icon(
                  imageVector =
                    when (tab) {
                      LegalTab.PRIVACY -> Icons.Default.Shield
                      LegalTab.TERMS -> Icons.Default.Description
                      LegalTab.DELETE_ACCOUNT -> Icons.Default.DeleteForever
                    },
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint =
                    if (selectedTab == tab) {
                      if (tab == LegalTab.DELETE_ACCOUNT) MaterialTheme.colorScheme.error
                      else TrackViewGreen
                    } else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              },
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(380.dp)
              .verticalScroll(rememberScrollState()),
        ) {
          when (selectedTab) {
            LegalTab.PRIVACY ->
              PrivacyPolicySection(
                onOpenBrowser = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TRACKVIEW_PRIVACY_URL))
                  context.startActivity(intent)
                },
                onCopyUrl = {
                  clipboardManager.setText(AnnotatedString(TRACKVIEW_PRIVACY_URL))
                  Toast.makeText(
                    context,
                    "Privacy Policy URL copied to clipboard",
                    Toast.LENGTH_SHORT,
                  ).show()
                },
              )

            LegalTab.TERMS ->
              TermsOfServiceSection(
                onOpenBrowser = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TRACKVIEW_TERMS_URL))
                  context.startActivity(intent)
                },
                onCopyUrl = {
                  clipboardManager.setText(AnnotatedString(TRACKVIEW_TERMS_URL))
                  Toast.makeText(
                    context,
                    "Terms of Service URL copied to clipboard",
                    Toast.LENGTH_SHORT,
                  ).show()
                },
              )

            LegalTab.DELETE_ACCOUNT ->
              DeleteAccountSection(
                onInitiateAccountDeletion = onInitiateAccountDeletion,
                actionButtonLabel = actionButtonLabel,
                onOpenBrowser = {
                  val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(TRACKVIEW_DELETE_ACCOUNT_URL))
                  context.startActivity(intent)
                },
                onCopyUrl = {
                  clipboardManager.setText(AnnotatedString(TRACKVIEW_DELETE_ACCOUNT_URL))
                  Toast.makeText(
                    context,
                    "Account Deletion URL copied to clipboard",
                    Toast.LENGTH_SHORT,
                  ).show()
                },
                onEmailSupport = {
                  val intent =
                    Intent(Intent.ACTION_SENDTO).apply {
                      data = Uri.parse("mailto:$TRACKVIEW_SUPPORT_EMAIL")
                      putExtra(Intent.EXTRA_SUBJECT, "TrackView Account Deletion Request")
                    }
                  try {
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    clipboardManager.setText(AnnotatedString(TRACKVIEW_SUPPORT_EMAIL))
                    Toast.makeText(
                      context,
                      "Support email copied: $TRACKVIEW_SUPPORT_EMAIL",
                      Toast.LENGTH_SHORT,
                    ).show()
                  }
                },
              )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = TrackViewGreen),
      ) {
        Text("Done")
      }
    },
  )
}

@Composable
private fun PrivacyPolicySection(
  onOpenBrowser: () -> Unit,
  onCopyUrl: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Surface(
      color = Color(0xFFE8F5E9),
      shape = RoundedCornerShape(12.dp),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TrackViewDarkGreen)
        Text(
          text = "Zero Covert Surveillance Guarantee\nAll location, audio, and video streams require explicit mutual consent and display continuous visible indicators.",
          style = MaterialTheme.typography.bodySmall,
          color = TrackViewDarkGreen,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    LegalCard(
      title = "1. Information We Collect",
      body = "• Google Account profile (User ID, display name, email, avatar URL)\n• Precise GPS coordinates (latitude, longitude, speed, accuracy)\n• Connected device hardware tokens, battery levels & online states\n• Custom SafeZones & geofence perimeter coordinates\n• End-to-end encrypted family messages and ETA notifications",
    )

    LegalCard(
      title = "2. Camera & Microphone Safeguards",
      body = "Remote video and voice calls NEVER start silently or automatically. The receiving device displays a clear incoming consent prompt. When active, a pulsing 'LIVE ACTIVE' indicator and instant 'Stop' button remain visible. No streams are recorded or stored on servers.",
    )

    LegalCard(
      title = "3. Children's Privacy (COPPA & GDPR-K)",
      body = "Designed for family safety with required parental consent. Minor children's accounts are managed under parental supervision. Location data is never monetized, sold, or shared with third-party data brokers.",
    )

    LegalCard(
      title = "4. Zero-Trust Security & Rights",
      body = "All communication is encrypted using TLS and Firebase zero-trust security rules. You have the right to revoke device links, pause GPS via Ghost Mode, export data to JSON, or wipe all records at any time.",
    )

    LegalCard(
      title = "Public Privacy Policy URL",
      body = TRACKVIEW_PRIVACY_URL,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = onOpenBrowser,
        modifier = Modifier.weight(1f).testTag("privacy_open_browser_btn"),
      ) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Web View", style = MaterialTheme.typography.labelMedium)
      }
      OutlinedButton(
        onClick = onCopyUrl,
        modifier = Modifier.weight(1f).testTag("privacy_copy_url_btn"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Copy URL", style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun TermsOfServiceSection(
  onOpenBrowser: () -> Unit,
  onCopyUrl: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    LegalCard(
      title = "1. Permitted Family Safety Usage",
      body = "TrackView is strictly for consensual family coordination, child safety, and device recovery. Stalking, harassment, or unauthorized tracking of non-consenting adults is strictly prohibited and grounds for immediate termination.",
    )

    Surface(
      color = Color(0xFFFEF3C7),
      shape = RoundedCornerShape(12.dp),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
        Text(
          text = "Emergency 911 Disclaimer:\nTrackView is a supplemental communication tool and is NOT a replacement for 911 or official emergency services.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF92400E),
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    LegalCard(
      title = "2. Device Pairing & Parental Responsibility",
      body = "Parents and guardians are responsible for supervising minors using the application. Pairing codes and QR tokens must be kept confidential within your trusted family circle.",
    )

    LegalCard(
      title = "3. Public Terms URL",
      body = TRACKVIEW_TERMS_URL,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = onOpenBrowser,
        modifier = Modifier.weight(1f).testTag("terms_open_browser_btn"),
      ) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Web View", style = MaterialTheme.typography.labelMedium)
      }
      OutlinedButton(
        onClick = onCopyUrl,
        modifier = Modifier.weight(1f).testTag("terms_copy_url_btn"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Copy URL", style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun DeleteAccountSection(
  onInitiateAccountDeletion: () -> Unit,
  onOpenBrowser: () -> Unit,
  onCopyUrl: () -> Unit,
  onEmailSupport: () -> Unit,
  actionButtonLabel: String = "Delete Account & Wipe Cloud Data Now",
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.errorContainer,
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Default.DeleteForever,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Text(
            text = "Permanent Account & Cloud Data Wipe",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Deleting your account permanently erases your user profile, registered phones, pairing tokens, GPS location history, SafeZones, and chat messages from Firebase Firestore. This cannot be undone.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }

    LegalCard(
      title = "Option 1: In-App Immediate Deletion",
      body = "Tap the button below to execute instant cloud data deletion. All your records will be immediately purged from our servers, and your session will be signed out.",
    )

    Button(
      onClick = onInitiateAccountDeletion,
      modifier = Modifier.fillMaxWidth().testTag("in_app_delete_account_btn"),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
      Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(actionButtonLabel)
    }

    LegalCard(
      title = "Option 2: Web Deletion Portal",
      body = "You can also submit an account deletion request through our web portal: $TRACKVIEW_DELETE_ACCOUNT_URL",
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = onOpenBrowser,
        modifier = Modifier.weight(1f).testTag("delete_open_browser_btn"),
      ) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Portal Web", style = MaterialTheme.typography.labelMedium)
      }
      OutlinedButton(
        onClick = onCopyUrl,
        modifier = Modifier.weight(1f).testTag("delete_copy_url_btn"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Copy Link", style = MaterialTheme.typography.labelMedium)
      }
    }

    LegalCard(
      title = "Option 3: Email Deletion Request",
      body = "Send an email request from your registered account to $TRACKVIEW_SUPPORT_EMAIL. Requests are processed within 5 business days.",
    )

    OutlinedButton(
      onClick = onEmailSupport,
      modifier = Modifier.fillMaxWidth().testTag("delete_email_support_btn"),
    ) {
      Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Email $TRACKVIEW_SUPPORT_EMAIL")
    }
  }
}

@Composable
private fun LegalCard(title: String, body: String) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
