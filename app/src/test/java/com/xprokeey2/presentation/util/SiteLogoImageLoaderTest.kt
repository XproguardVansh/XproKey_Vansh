package com.xprokeey2.presentation.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class SiteLogoImageLoaderTest {

    /** [AcceptImageOn404] in front of a fake server answering [code] with [contentType]. */
    private fun fetch(code: Int, contentType: String): Int {
        val client = OkHttpClient.Builder()
            .addInterceptor(AcceptImageOn404)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("")
                    .header("Content-Type", contentType)
                    .body("image".toResponseBody(contentType.toMediaType()))
                    .build()
            }
            .build()
        val request = Request.Builder().url("https://t2.gstatic.com/faviconV2?url=http://unknown-site.com").build()
        return client.newCall(request).execute().use { it.code }
    }

    @Test
    fun googlesDefaultGlobeIsShownLikeInTheBrowser() {
        assertEquals(200, fetch(404, "image/png"))
    }

    @Test
    fun otherFailuresStayFailures() {
        assertEquals(404, fetch(404, "text/html"))
        assertEquals(500, fetch(500, "image/png"))
    }
}
