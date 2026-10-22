package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FamilyMember
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TrackViewRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addAndObserveFamilyMember_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TrackViewRepository(firestore, auth)
    val docId = "member_${UUID.randomUUID().toString().take(8)}"

    val member =
      FamilyMember(
        userId = uid,
        name = "Lucas Miller",
        role = "CHILD",
        avatarUrl = "https://example.com/lucas.jpg",
        batteryLevel = 91,
      )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repo.addFamilyMember(member, docId) }
    assertTrue(result.isSuccess)

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMembers(uid).first { list -> list.any { it.id == docId } }
      }
    assertTrue(observed.any { it.id == docId && it.name == "Lucas Miller" })
  }

  @Test
  fun getFamilyMemberById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TrackViewRepository(firestore, auth)
    val docId = "member_${UUID.randomUUID().toString().take(8)}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo
        .addFamilyMember(
          FamilyMember(userId = aliceUid, name = "Sarah Miller", role = "SPOUSE"),
          docId,
        )
        .getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = TrackViewRepository(firestore, auth)
    val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getFamilyMemberById(aliceUid, docId) }
    assertTrue("Expected cross-user read to fail, but got: $crossResult", crossResult.isFailure)
    val ex = crossResult.exceptionOrNull() as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun observeMembers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = TrackViewRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeMembers(aliceUid).first() }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: Throwable) {
      val fse = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fse?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@trackview.test"
    const val BOB_EMAIL = "bob@trackview.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
