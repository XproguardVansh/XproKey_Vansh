package com.xprokeey2.domain.usecase.validation

enum class ValidationError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    TERMS_NOT_ACCEPTED,
    INVALID_OTP,

    // Card form (card validation spec §4)
    CARD_LABEL_REQUIRED,
    CARD_NUMBER_REQUIRED,
    INVALID_CARD_NUMBER,
    EXPIRY_MONTH_INVALID,
    EXPIRY_YEAR_REQUIRED,
    EXPIRY_YEAR_TWO_DIGITS,
    CARD_EXPIRED,
    CVC_REQUIRED,
    CVC_MUST_BE_3_DIGITS,
    CVC_MUST_BE_4_DIGITS,
}
