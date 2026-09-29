package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.repository.WeakVaultItemRepository
import javax.inject.Inject

/** Web `getWeakVaultItemIds`: the items marked weak when their password was saved on this device. */
class GetWeakVaultItemIdsUseCase @Inject constructor(
    private val weakItems: WeakVaultItemRepository,
) {
    suspend operator fun invoke(): Set<Long> = weakItems.getWeakItemIds()
}
