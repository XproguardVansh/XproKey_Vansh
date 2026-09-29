package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class CreateVaultCategoryUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    /** Returns the new category's name. */
    suspend operator fun invoke(name: String): Resource<String> = vaultRepository.createCategory(name.trim())
}
