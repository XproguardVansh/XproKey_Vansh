package com.xprokeey2.domain.usecase.support

import com.xprokeey2.domain.model.CreatedTicket
import com.xprokeey2.domain.model.TicketDraft
import com.xprokeey2.domain.repository.SupportRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Sends a new ticket with its text fields trimmed, like the web form. */
class CreateSupportTicketUseCase @Inject constructor(
    private val supportRepository: SupportRepository,
) {
    suspend operator fun invoke(draft: TicketDraft): Resource<CreatedTicket> = supportRepository.createTicket(
        draft.copy(
            name = draft.name.trim(),
            email = draft.email.trim(),
            subject = draft.subject.trim(),
            message = draft.message.trim(),
        )
    )
}
