package com.xprokeey2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.xprokeey2.presentation.navigation.AppNavHost
import com.xprokeey2.presentation.payment.PaymentResult
import com.xprokeey2.presentation.payment.PaymentResults
import com.xprokeey2.presentation.payment.razorpayErrorDescription
import com.xprokeey2.presentation.session.LocalSessionTimeoutMonitor
import com.xprokeey2.presentation.session.SessionTimeoutMonitor
import com.xprokeey2.presentation.theme.XproKeyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Razorpay Checkout reports payments to the activity that opened it, so this is its listener. */
@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var sessionTimeout: SessionTimeoutMonitor

    @Inject
    lateinit var paymentResults: PaymentResults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XproKeyTheme {
                CompositionLocalProvider(LocalSessionTimeoutMonitor provides sessionTimeout) {
                    AppNavHost()
                }
            }
        }
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
