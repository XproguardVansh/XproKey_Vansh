package com.xprokeey2.presentation.profile

import com.xprokeey2.domain.model.AccountProfile
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.ProfileOverview
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** The web Profile page's rules. */
class ProfileTest {

    private fun profile(accountType: String = "personal", name: String = "", email: String = "goelv2610@gmail.com") = AccountProfile(
        email = email,
        name = name,
        accountType = accountType,
        joinedAt = null,
        organizationName = null,
        licenseStatus = null,
        licenseExpiresAt = null,
    )

    private fun billing(status: String, autoRenew: Boolean? = null, expiringSoon: Boolean = false) = Billing(
        planType = "yearly",
        subscriptionStatus = status,
        isTrial = status == Billing.STATUS_TRIAL,
        expiresAt = null,
        nextBillingAt = null,
        trialDaysLeft = null,
        isExpiringSoon = expiringSoon,
        autoRenew = autoRenew,
    )

    private fun banner(billing: Billing?, accountType: String = "personal") =
        ProfileBanner.of(ProfileOverview(profile(accountType), billing))

    @Test
    fun bannerFollowsTheWebsOrder() {
        assertEquals(ProfileBanner.BUSINESS, banner(billing = null, accountType = "business"))
        assertEquals(ProfileBanner.CANCELLED, banner(billing("active", autoRenew = false, expiringSoon = true)))
        assertEquals(ProfileBanner.EXPIRING_SOON, banner(billing("active", expiringSoon = true)))
        assertEquals(ProfileBanner.ACTIVE, banner(billing("active", autoRenew = true)))
        assertEquals(ProfileBanner.TRIAL, banner(billing("trial")))
        assertEquals(ProfileBanner.NO_SUBSCRIPTION, banner(billing("inactive")))
        assertEquals(ProfileBanner.NO_SUBSCRIPTION, banner(billing = null))
    }

    @Test
    fun nameFallsBackToTheEmail() {
        assertEquals("goelv2610", profile(name = "").fullName())
        assertEquals("Vansh Goel", profile(name = "Vansh Goel").fullName())
    }

    @Test
    fun initialsLikeTheWeb() {
        assertEquals("GO", profileInitials("goelv2610", "goelv2610@gmail.com"))
        assertEquals("VG", profileInitials("Vansh  Goel Kumar", "x@y.z"))
        assertEquals("V", profileInitials("", "vansh@gmail.com"))
        assertEquals("U", profileInitials("", ""))
    }

    @Test
    fun datesLikeTheWeb() {
        assertEquals("September 25, 2026", joinedDate(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC))
        assertEquals("26/9/2027", validTillDate(Instant.parse("2027-09-26T10:00:00Z"), ZoneOffset.UTC))
    }
}
