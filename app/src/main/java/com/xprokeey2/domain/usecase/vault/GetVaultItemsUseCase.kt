package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class GetVaultItemsUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(): Resource<List<VaultItem>> = vaultRepository.getItems()
}
