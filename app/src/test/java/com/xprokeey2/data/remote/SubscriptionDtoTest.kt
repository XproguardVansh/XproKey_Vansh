package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toDto
import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import com.xprokeey2.data.remote.dto.payment.CreateSubscriptionResponseDto
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The response shapes from the Razorpay guide (7.1, 7.4) and the web's Subscription page. */
class SubscriptionDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun billingFromTheGuide() {
        val billing = json.decodeFromString<BillingDto>(
            """{"plan_type":"yearly","subscription_status":"trial","is_trial":true,"expires_at":"2026-10-02T09:30:00+05:30",
               "next_billing_at":null,"trial_days_left":3,"is_expiring_soon":true}"""
        ).toDomain()

        assertEquals("yearly", billing.planType)
        assertEquals("trial", billing.subscriptionStatus)
        assertTrue(billing.isTrial)
        assertEquals(Instant.parse("2026-10-02T04:00:00Z"), billing.expiresAt)
        assertNull(billing.nextBillingAt)
        assertEquals(3, billing.trialDaysLeft)
        assertTrue(billing.isExpiringSoon)
        assertNull(billing.autoRenew)
        assertFalse(billing.isActive)
    }

    @Test
    fun billingBeforeAnythingWasBought() {
        val billing = json.decodeFromString<BillingDto>("""{"plan_type":null,"subscription_status":"inactive"}""").toDomain()

        assertNull(billing.planType)
        assertFalse(billing.isTrial)
        assertFalse(billing.isExpiringSoon)
    }

    @Test
    fun summaryOfBothAccountTypes() {
        val personal = json.decodeFromString<DashboardSummaryDto>(
            """{"account_type":"personal","vault_count":2,"card_count":1,"status":"trial","days_left":3,"missing_fields_count":0}"""
        ).toDomain()
        val business = json.decodeFromString<DashboardSummaryDto>(
            """{"account_type":"business","organization":"Xproguard","license_status":"active","license_expires_at":"2027-09-26T00:00:00Z"}"""
        ).toDomain()

        assertTrue(personal.isPersonal)
        assertEquals("trial", personal.status)
        assertEquals(3, personal.daysLeft)
        assertFalse(business.isPersonal)
        assertEquals("active", business.licenseStatus)
        assertEquals(Instant.parse("2027-09-26T00:00:00Z"), business.licenseExpiresAt)
    }

    @Test
    fun securitySettingsBothWays() {
        val server = json.decodeFromString<SecuritySettingsDto>("""{"timeout_duration":"15","timeout_action":"lock"}""").toDomain()
        assertEquals(TimeoutDuration.FIFTEEN_MINUTES, server.duration)
        assertEquals(TimeoutAction.LOCK, server.action)

        val body = json.encodeToString(SecuritySettingsDto.serializer(), SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOGOUT).toDto())
        assertEquals("""{"timeout_duration":"never","timeout_action":"logout"}""", body)
    }

    @Test
    fun createdSubscription() {
        val created = json.decodeFromString<CreateSubscriptionResponseDto>("""{"success":true,"subscription_id":"sub_Q1w2E3","plan":"yearly"}""")

        assertEquals("sub_Q1w2E3", created.subscriptionId)
    }
}
