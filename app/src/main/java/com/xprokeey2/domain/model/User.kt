package com.xprokeey2.domain.model

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val accountType: String? = null,
    val subscriptionStatus: String? = null,
    val subscriptionExpiresAt: String? = null,
)
