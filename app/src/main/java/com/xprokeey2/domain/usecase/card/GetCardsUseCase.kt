package com.xprokeey2.domain.usecase.card

import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class GetCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(): Resource<List<Card>> = cardRepository.getCards()
}
