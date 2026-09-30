package com.xprokeey2.data.remote

import com.xprokeey2.data.remote.auth.accessBlockOf
import com.xprokeey2.domain.model.AccessBlock
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 403 bodies of the server's access guard (Razorpay guide 7.3). */
class AccessGuardTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun nextActionTellsWhereTheUserHasToGo() {
        assertEquals(AccessBlock.PAYMENT, accessBlockOf(json, """{"next_action":"payment"}"""))
        assertEquals(AccessBlock.TRIAL_EXPIRED, accessBlockOf(json, """{"next_action":"trial_expired","message":"Trial ended"}"""))
        assertEquals(AccessBlock.ACTIVATE_LICENSE, accessBlockOf(json, """{"next_action":"activate_license"}"""))
    }

    @Test
    fun otherForbiddenAnswersAreNotBlocks() {
        assertNull(accessBlockOf(json, """{"error":"User not found"}"""))
        assertNull(accessBlockOf(json, """{"next_action":"already_active"}"""))
        assertNull(accessBlockOf(json, "<html>Forbidden</html>"))
        assertNull(accessBlockOf(json, ""))
    }
}
