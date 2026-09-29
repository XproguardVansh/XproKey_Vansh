package com.xprokeey2.domain.usecase.support

import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.repository.SupportRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class GetSupportTicketUseCase @Inject constructor(
    private val supportRepository: SupportRepository,
) {
    suspend operator fun invoke(id: String): Resource<SupportTicket> = supportRepository.getTicket(id)
}
