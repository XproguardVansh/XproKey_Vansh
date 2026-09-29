package com.xprokeey2.presentation.support.newticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.model.TicketDraft
import com.xprokeey2.domain.usecase.support.CreateSupportTicketUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.validation.ValidateTicketFormUseCase
import com.xprokeey2.domain.usecase.validation.ValidationError
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewTicketViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val validateForm: ValidateTicketFormUseCase,
    private val createTicket: CreateSupportTicketUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewTicketUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<NewTicketEvent>()
    val events = _events.receiveAsFlow()

    init {
        // The web fills these from the account (`name || prev.name`); fields already typed are kept.
        viewModelScope.launch {
            val user = getSignedInUser() ?: return@launch
            _state.update {
                it.copy(
                    name = it.name.ifEmpty { user.name },
                    email = it.email.ifEmpty { user.email },
                )
            }
        }
    }

    fun onAction(action: NewTicketAction) {
        when (action) {
            is NewTicketAction.NameChanged -> _state.update { it.copy(name = action.value, nameError = null) }
            is NewTicketAction.EmailChanged -> _state.update { it.copy(email = action.value, emailError = null) }
            is NewTicketAction.CategorySelected -> _state.update { it.copy(category = action.category) }
            is NewTicketAction.SubjectChanged -> _state.update { it.copy(subject = action.value, subjectError = null) }
            is NewTicketAction.MessageChanged -> _state.update { it.copy(message = action.value, messageError = null) }
            NewTicketAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val errors = validateForm(current.name, current.email, current.subject, current.message)
        if (errors.hasErrors) {
            _state.update {
                it.copy(
                    nameError = errors.name?.let { UiText.Resource(R.string.ticket_error_name) },
                    emailError = when (val emailError = errors.email) {
                        null -> null
                        ValidationError.REQUIRED -> UiText.Resource(R.string.ticket_error_email)
                        else -> emailError.asUiText()
                    },
                    subjectError = errors.subject?.let { UiText.Resource(R.string.ticket_error_subject) },
                    messageError = errors.message?.let { UiText.Resource(R.string.ticket_error_message) },
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = createTicket(
                TicketDraft(
                    name = current.name,
                    email = current.email,
                    category = current.category,
                    subject = current.subject,
                    message = current.message,
                )
            )
            _state.update { it.copy(isSubmitting = false) }
            val event = when (result) {
                is Resource.Success -> NewTicketEvent.Created(
                    ticketId = result.data.ticket?.id,
                    // `res.message || "Support ticket created successfully!"`
                    message = result.data.message?.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
                        ?: UiText.Resource(R.string.ticket_created),
                )
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked -> NewTicketEvent.SignInRequired(error.asUiText())
                    DataError.NoInternet, DataError.Timeout -> NewTicketEvent.ShowMessage(error.asUiText())
                    // The server's own error text, else the web's fallback.
                    is DataError.Server -> NewTicketEvent.ShowMessage(
                        error.message.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
                            ?: UiText.Resource(R.string.ticket_submit_failed)
                    )
                    else -> NewTicketEvent.ShowMessage(UiText.Resource(R.string.ticket_submit_failed))
                }
            }
            _events.send(event)
        }
    }
}
