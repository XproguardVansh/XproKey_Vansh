package com.xprokeey2.presentation.payment

import android.app.Activity
import android.content.Context
import com.razorpay.Checkout
import com.xprokeey2.BuildConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** What Razorpay Checkout reported to [com.xprokeey2.MainActivity]. */
sealed interface PaymentResult {
    /** Payment authorised. Access still waits for billing: the server's webhook activates the subscription. */
    data object Authorized : PaymentResult

    /** The payment sheet was closed. */
    data object Cancelled : PaymentResult

    data class Failed(val description: String?) : PaymentResult
}

/** Hands Razorpay's results from the activity to the checkout screen. */
@Singleton
class PaymentResults @Inject constructor() {
    private val results = Channel<PaymentResult>(Channel.BUFFERED)

    val flow: Flow<PaymentResult> = results.receiveAsFlow()

    fun report(result: PaymentResult) {
        results.trySend(result)
    }
}

/** Razorpay Checkout for a subscription the server created. Only the public key ID is in the app. */
object RazorpayCheckout {

    /** Loads Checkout ahead of time so the payment sheet opens faster. */
    fun preload(context: Context) = Checkout.preload(context.applicationContext)

    /** The result comes back to the activity, which must be a PaymentResultWithDataListener. */
    fun open(activity: Activity, subscriptionId: String, description: String) {
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
        val options = JSONObject()
            .put("name", MERCHANT_NAME)
            .put("description", description)
            .put("subscription_id", subscriptionId)
            .put("theme", JSONObject().put("color", THEME_COLOR))
        checkout.open(activity, options)
    }

    /** At sign-out: Razorpay forgets the customer details it saved on this device. */
    fun clearUserData(context: Context) = Checkout.clearUserData(context.applicationContext)

    private const val MERCHANT_NAME = "XPROSHIELD GLOBAL PRIVATE LIMITED"
    private const val THEME_COLOR = "#2563EB"
}

/** Razorpay's error response, e.g. `{"error": {"description": "…"}}`: the description the web shows. */
internal fun razorpayErrorDescription(response: String?): String? = response?.let {
    runCatching {
        Json.parseToJsonElement(it).jsonObject["error"]?.jsonObject?.get("description")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}?.takeIf { it.isNotBlank() }
