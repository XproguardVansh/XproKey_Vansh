package com.xprokeey2.domain.usecase.card

import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class DeleteCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    /** Returns the server message. */
    suspend operator fun invoke(id: Long): Resource<String> = cardRepository.deleteCard(id)
}
