package com.xprokeey2.presentation.settings

import com.xprokeey2.R
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.presentation.settings.changepassword.toChangePasswordMessage
import com.xprokeey2.presentation.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

/** The web page's `getErrorMessage` for failed "Send OTP" / "Change Password" calls. */
class ChangePasswordMessagesTest {

    private fun DataError.messageId() = (toChangePasswordMessage() as UiText.Resource).id

    @Test
    fun knownServerErrorsGetTheWebTexts() {
        assertEquals(R.string.change_password_otp_expired, DataError.Server(400, "OTP has expired").messageId())
        assertEquals(R.string.change_password_invalid_otp, DataError.Server(400, "Invalid OTP").messageId())
        assertEquals(R.string.change_password_invalid_email_or_otp, DataError.Server(400, "Invalid email or OTP").messageId())
        assertEquals(R.string.change_password_mismatch, DataError.Server(400, "Passwords do not match").messageId())
    }

    @Test
    fun otherServerErrorsAreShownAsSent() {
        assertEquals(UiText.Dynamic("User not found"), DataError.Server(404, "  User not found ").toChangePasswordMessage())
    }

    @Test
    fun failuresWithoutAServerTextGetTheFallback() {
        assertEquals(R.string.change_password_failed, DataError.Server(500, " ").messageId())
        assertEquals(R.string.change_password_failed, DataError.Unknown("Unexpected JSON").messageId())
    }
}
