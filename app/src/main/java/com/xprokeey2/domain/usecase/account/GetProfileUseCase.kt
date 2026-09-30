package com.xprokeey2.domain.usecase.account

import com.xprokeey2.domain.model.ProfileOverview
import com.xprokeey2.domain.repository.AccountRepository
import com.xprokeey2.domain.usecase.subscription.GetBillingUseCase
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * The Profile page, like the web: the account (GET /me), and for a Personal account its billing too.
 * A failed billing call only leaves the billing out.
 */
class GetProfileUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getBilling: GetBillingUseCase,
) {
    suspend operator fun invoke(): Resource<ProfileOverview> {
        val profile = when (val result = accountRepository.getProfile()) {
            is Resource.Success -> result.data
            is Resource.Error -> return result
        }
        val billing = if (profile.isPersonal) (getBilling() as? Resource.Success)?.data else null
        return Resource.Success(ProfileOverview(profile = profile, billing = billing))
    }
}
