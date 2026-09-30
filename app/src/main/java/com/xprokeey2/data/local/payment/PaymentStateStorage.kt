package com.xprokeey2.data.local.payment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.xprokeey2.di.PaymentStateDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Razorpay guide's "payment pending" flag: set when Checkout opens, cleared once billing shows the
 * subscription active or the payment was abandoned, so an interrupted payment is checked again later.
 * Nothing about the payment itself is stored.
 */
@Singleton
class PaymentStateStorage @Inject constructor(
    @param:PaymentStateDataStore private val dataStore: DataStore<Preferences>,
) {

    suspend fun isPaymentPending(): Boolean = dataStore.data.first()[PAYMENT_PENDING] ?: false

    suspend fun setPaymentPending(pending: Boolean) {
        dataStore.edit { prefs ->
            if (pending) prefs[PAYMENT_PENDING] = true else prefs.remove(PAYMENT_PENDING)
        }
    }

    private companion object {
        val PAYMENT_PENDING = booleanPreferencesKey("payment_pending")
    }
}
