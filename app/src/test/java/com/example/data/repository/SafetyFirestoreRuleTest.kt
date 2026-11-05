package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SafetyFirestoreRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createEmergencyAlert_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val alertId = "SOS-${UUID.randomUUID().toString().take(8)}"

    val alertData = hashMapOf(
      "id" to alertId,
      "userId" to uid,
      "userName" to "Alice Adeleke",
      "userPhone" to "+2348011223344",
      "status" to "active",
      "crisisMode" to "KIDNAP_SILENT",
      "battery" to 90,
      "latitude" to 6.4281,
      "longitude" to 3.4219,
      "address" to "Victoria Island, Lagos"
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("emergency_alerts").document(alertId).set(alertData).await()
    }

    val snapshot = withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("emergency_alerts").document(alertId).get().await()
    }
    assertTrue(snapshot.exists())
    assertEquals("active", snapshot.getString("status"))
  }

  @Test
  fun createEmergencyAlert_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val alertId = "SOS-${UUID.randomUUID().toString().take(8)}"

    val alertData = hashMapOf(
      "id" to alertId,
      "userId" to "unauth_user",
      "status" to "active",
      "crisisMode" to "ACCIDENT"
    )

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("emergency_alerts").document(alertId).set(alertData).await()
      }
      fail("Expected PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun updateEmergencyAlert_crossUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val alertId = "SOS-${UUID.randomUUID().toString().take(8)}"

    val alertData = hashMapOf(
      "id" to alertId,
      "userId" to aliceUid,
      "status" to "active",
      "crisisMode" to "MEDICAL"
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("emergency_alerts").document(alertId).set(alertData).await()
    }

    // Sign in as Bob
    signInTestUser(BOB_EMAIL)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("emergency_alerts").document(alertId).update("status", "resolved").await()
      }
      fail("Bob should not be able to resolve Alice's alert")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@ajiya.test"
    const val BOB_EMAIL = "bob@ajiya.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}
