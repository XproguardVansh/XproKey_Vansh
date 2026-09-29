package com.xprokeey2.presentation.passwords

import androidx.compose.ui.platform.UriHandler
import com.xprokeey2.presentation.passwords.components.displayDomain
import com.xprokeey2.presentation.passwords.components.faviconUrl
import com.xprokeey2.presentation.passwords.components.openLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebsiteLinkTest {

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun showsTheDomain() {
        assertEquals("github.com", displayDomain("https://github.com"))
        assertEquals("github.com", displayDomain("https://www.github.com/login"))
        assertEquals("github.com", displayDomain(" github.com/login "))
    }

    @Test
    fun logoComesFromGoogleFaviconsLikeTheWeb() {
        val favicons = "https://www.google.com/s2/favicons?sz=64&domain="
        assertEquals(favicons + "github.com", faviconUrl("https://github.com", "Github"))
        assertEquals(favicons + "github.com", faviconUrl("GitHub.com/login", "Github"))
        assertEquals(favicons + "www.applock.com", faviconUrl("https://www.applock.com", "AppLock")) // web keeps www
        // No URL: a few known names still get a logo, like on the web.
        assertEquals(favicons + "gmail.com", faviconUrl("", "My Gmail"))
        assertEquals(favicons + "amazon.com", faviconUrl("", "AWS console"))
        // Letter instead: no URL and an unknown name, or a domain without a dot.
        assertNull(faviconUrl("", "Vansh"))
        assertNull(faviconUrl("localhost", "Github"))
    }

    @Test
    fun addressesWithoutHttpOpenAsHttps() {
        val handler = RecordingUriHandler()
        openLink(handler, "github.com")
        openLink(handler, "http://applock.com")
        openLink(handler, " https://github.com/login ")

        assertEquals(listOf("https://github.com", "http://applock.com", "https://github.com/login"), handler.opened)
    }
}
