package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.User

interface UserRepository {

    /** The account saved at login, or null when nobody is signed in. */
    suspend fun getSignedInUser(): User?
}
