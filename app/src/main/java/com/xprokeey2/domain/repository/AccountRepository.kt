package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.AccountProfile
import com.xprokeey2.domain.util.Resource

/** The signed-in account on the server. */
interface AccountRepository {

    /** GET /me */
    suspend fun getProfile(): Resource<AccountProfile>

    /** POST /logout: ends the session on the server. */
    suspend fun logOut(): Resource<Unit>
}
