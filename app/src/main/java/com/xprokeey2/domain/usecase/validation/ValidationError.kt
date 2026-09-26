package com.xprokeey2.domain.usecase.validation

enum class ValidationError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    TERMS_NOT_ACCEPTED,
    INVALID_OTP,
    INVALID_CARD_NUMBER,
    INVALID_EXPIRY,
    INVALID_CVC,
}
