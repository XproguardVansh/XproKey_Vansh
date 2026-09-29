package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class GetVaultCategoriesUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    /** Built-in categories (Personal, Work, …) followed by the user's own ones. */
    suspend operator fun invoke(): Resource<List<String>> = vaultRepository.getCategories()
}
