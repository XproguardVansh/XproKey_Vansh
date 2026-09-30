package com.xprokeey2.presentation.onboarding.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.model.CheckoutAccess
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.usecase.subscription.AbandonPaymentUseCase
import com.xprokeey2.domain.usecase.subscription.CreateSubscriptionUseCase
import com.xprokeey2.domain.usecase.subscription.GetCheckoutAccessUseCase
import com.xprokeey2.domain.usecase.subscription.StartTrialUseCase
import com.xprokeey2.domain.usecase.subscription.WaitForActiveSubscriptionUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.payment.PaymentResult
import com.xprokeey2.presentation.payment.PaymentResults
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    paymentResults: PaymentResults,
    private val getCheckoutAccess: GetCheckoutAccessUseCase,
    private val startTrial: StartTrialUseCase,
    private val createSubscription: CreateSubscriptionUseCase,
    private val waitForActiveSubscription: WaitForActiveSubscriptionUseCase,
    private val abandonPayment: AbandonPaymentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<PlanEvent>()
    val events = _events.receiveAsFlow()

    /** Razorpay Checkout is open for this screen; results from an earlier visit are ignored. */
    private var awaitingPayment = false

    init {
        viewModelScope.launch {
            when (val access = getCheckoutAccess()) {
                CheckoutAccess.AlreadyActive ->
                    _events.send(PlanEvent.AlreadyActive(UiText.Resource(R.string.checkout_already_active)))
                is CheckoutAccess.Available -> _state.update {
                    it.copy(isCheckingAccess = false, trialEligible = access.trialEligible)
                }
            }
        }
        viewModelScope.launch {
            paymentResults.flow.collect { onAction(PlanAction.PaymentFinished(it)) }
        }
    }

    fun onAction(action: PlanAction) {
        when (action) {
            is PlanAction.PlanSelected -> _state.update { it.copy(selectedPlan = action.plan) }
            PlanAction.Continue -> continueWithPlan()
            is PlanAction.PaymentFinished -> onPaymentFinished(action.result)
        }
    }

    private fun continueWithPlan() {
        val current = _state.value
        val trialEligible = current.trialEligible ?: return
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            if (trialEligible) startFreeTrial(current.selectedPlan) else startPayment(current.selectedPlan)
        }
    }

    private suspend fun startFreeTrial(plan: SubscriptionPlan) {
        when (val result = startTrial(plan)) {
            is Resource.Success -> {
                // The button keeps spinning while the message shows, then the Dashboard opens (web).
                _events.send(PlanEvent.ShowMessage(UiText.Resource(R.string.checkout_trial_started)))
                delay(REDIRECT_DELAY_MILLIS)
                _events.send(PlanEvent.Finished)
            }
            is Resource.Error -> {
                val error = result.error
                if (error is DataError.Server && error.code == HttpURLConnection.HTTP_FORBIDDEN) {
                    // "Trial not available" / "Trial already used": pay instead (Razorpay guide 7.2).
                    _state.update { it.copy(isLoading = false, trialEligible = false) }
                    _events.send(PlanEvent.ShowMessage(error.message.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) } ?: error.asUiText()))
                } else {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(checkoutFailure(error))
                }
            }
        }
    }

    private suspend fun startPayment(plan: SubscriptionPlan) {
        when (val result = createSubscription(plan)) {
            is Resource.Success -> {
                // Stays loading until Razorpay reports back (the web keeps its button busy too).
                awaitingPayment = true
                _events.send(PlanEvent.OpenCheckout(subscriptionId = result.data, description = UiText.Resource(plan.description)))
            }
            is Resource.Error -> {
                _state.update { it.copy(isLoading = false) }
                val error = result.error
                _events.send(
                    if (error is DataError.Server && error.isAlreadyActive()) {
                        PlanEvent.AlreadyActive(UiText.Dynamic(error.message))
                    } else {
                        checkoutFailure(error)
                    }
                )
            }
        }
    }

    private fun onPaymentFinished(result: PaymentResult) {
        if (!awaitingPayment) return
        awaitingPayment = false
        viewModelScope.launch {
            when (result) {
                PaymentResult.Authorized -> {
                    _events.send(PlanEvent.ShowMessage(UiText.Resource(R.string.checkout_payment_success)))
                    if (!waitForActiveSubscription()) {
                        _events.send(PlanEvent.ShowMessage(UiText.Resource(R.string.checkout_still_activating)))
                    }
                    delay(REDIRECT_DELAY_MILLIS)
                    _events.send(PlanEvent.Finished)
                }
                PaymentResult.Cancelled -> {
                    abandonPayment()
                    _state.update { it.copy(isLoading = false) }
                }
                is PaymentResult.Failed -> {
                    abandonPayment()
                    _state.update { it.copy(isLoading = false) }
                    _events.send(
                        PlanEvent.ShowMessage(
                            result.description?.let { UiText.Dynamic(it) } ?: UiText.Resource(R.string.checkout_payment_failed)
                        )
                    )
                }
            }
        }
    }

    private companion object {
        /** How long the success message shows before the Dashboard opens, like the web. */
        const val REDIRECT_DELAY_MILLIS = 2_500L
    }
}

/**
 * Failures of starting the trial or the subscription (Razorpay guide 7.2): a finished session goes to
 * Login, network problems say so, and anything else, including raw payment-provider errors, is
 * "Unable to continue".
 */
internal fun checkoutFailure(error: DataError): PlanEvent = when (error) {
    DataError.SessionExpired, DataError.VaultLocked -> PlanEvent.SignInRequired(error.asUiText())
    DataError.NoInternet, DataError.Timeout -> PlanEvent.ShowMessage(error.asUiText())
    else -> PlanEvent.ShowMessage(UiText.Resource(R.string.checkout_unable_to_continue))
}

/** create-subscription's 400 "You already have an active subscription". */
internal fun DataError.Server.isAlreadyActive(): Boolean =
    code == HttpURLConnection.HTTP_BAD_REQUEST && message.contains("already have an active subscription", ignoreCase = true)

private val SubscriptionPlan.description: Int
    get() = when (this) {
        SubscriptionPlan.QUARTERLY -> R.string.checkout_description_quarterly
        SubscriptionPlan.YEARLY -> R.string.checkout_description_yearly
    }
