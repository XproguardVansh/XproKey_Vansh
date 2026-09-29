package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.repository.WeakVaultItemRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Deletes one or more items. The API deletes one item per request, so they go one after another,
 * stopping at the first failure. Each deleted item leaves the weak list (`removeVaultItemWeakness`).
 */
class DeleteVaultItemsUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val weakItems: WeakVaultItemRepository,
) {
    /** Returns how many items were deleted. */
    suspend operator fun invoke(ids: Collection<Long>): Resource<Int> {
        var deleted = 0
        for (id in ids) {
            when (val result = vaultRepository.deleteItem(id)) {
                is Resource.Success -> {
                    weakItems.remove(id)
                    deleted++
                }
                is Resource.Error -> return result
            }
        }
        return Resource.Success(deleted)
    }
}
