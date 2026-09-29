package com.xprokeey2.presentation.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Coil's image loader, used for the site logos (Google's favicon service, like the web). It has its
 * own HTTP client: logo requests must never go through the API client, which adds login tokens.
 */
fun siteLogoImageLoader(context: PlatformContext): ImageLoader {
    val client = OkHttpClient.Builder()
        .addInterceptor(AcceptImageOn404)
        .build()
    return ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
        .build()
}

/**
 * For sites it doesn't know, Google answers 404 with its default globe image. Browsers still show
 * that image, so the web does; this lets Coil show it too instead of falling back to the letter.
 */
internal val AcceptImageOn404 = Interceptor { chain ->
    val response = chain.proceed(chain.request())
    val isImage = response.header("Content-Type")?.startsWith("image/") == true
    if (response.code == 404 && isImage) response.newBuilder().code(200).build() else response
}
