package com.xprokeey2

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.xprokeey2.presentation.navigation.AppNavHost
import com.xprokeey2.presentation.payment.PaymentResult
import com.xprokeey2.presentation.payment.PaymentResults
import com.xprokeey2.presentation.payment.razorpayErrorDescription
import com.xprokeey2.presentation.session.LocalSessionTimeoutMonitor
import com.xprokeey2.presentation.session.SessionTimeoutMonitor
import com.xprokeey2.presentation.theme.DarkXpColors
import com.xprokeey2.presentation.theme.LightXpColors
import com.xprokeey2.presentation.theme.LocalThemeToggle
import com.xprokeey2.presentation.theme.ThemeViewModel
import com.xprokeey2.presentation.theme.XproKeyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** androidx's default navigation bar scrims (EdgeToEdge.kt), for phones with 3-button navigation. */
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/** Razorpay Checkout reports payments to the activity that opened it, so this is its listener. */
@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var sessionTimeout: SessionTimeoutMonitor

    @Inject
    lateinit var paymentResults: PaymentResults

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        drawOnceThemeIsKnown()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            themeMode?.let { mode -> ThemedApp(darkTheme = mode.isDark(isSystemInDarkTheme())) }
        }
    }

    @Composable
    private fun ThemedApp(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            applyWindowTheme(darkTheme)
            onDispose { }
        }
        XproKeyTheme(darkTheme = darkTheme) {
            CompositionLocalProvider(
                LocalSessionTimeoutMonitor provides sessionTimeout,
                LocalThemeToggle provides themeViewModel::onToggleTheme,
            ) {
                AppNavHost()
            }
        }
    }

    /**
     * Nothing is drawn until the saved theme is read (a few milliseconds), so the app never opens in
     * the wrong colours; the web's next-themes runs a script before the page paints for the same reason.
     */
    private fun drawOnceThemeIsKnown() {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (themeViewModel.themeMode.value == null) return false
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    /**
     * The status and navigation bar icons and the window behind the screens follow the app's theme,
     * which can differ from the phone's. The window colours are the ones in themes.xml.
     */
    private fun applyWindowTheme(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
        )
        val windowColor = if (darkTheme) DarkXpColors.surface else LightXpColors.surface
        window.setBackgroundDrawable(ColorDrawable(windowColor.toArgb()))
    }

    /** Any touch or key press keeps the session from timing out. */
    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionTimeout.onUserActivity()
    }

    // The payment ID isn't needed (the server's webhook activates the subscription) and is never logged.
    override fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        paymentResults.report(PaymentResult.Authorized)
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        paymentResults.report(
            if (code == Checkout.PAYMENT_CANCELED) {
                PaymentResult.Cancelled
            } else {
                PaymentResult.Failed(razorpayErrorDescription(response))
            }
        )
    }
}
