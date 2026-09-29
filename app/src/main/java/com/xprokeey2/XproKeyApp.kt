package com.xprokeey2

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.xprokeey2.presentation.util.siteLogoImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XproKeyApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader = siteLogoImageLoader(context)
}
