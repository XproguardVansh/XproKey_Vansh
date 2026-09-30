package com.xprokeey2.domain.model

/** Where the app opens. */
enum class LaunchDestination {
    /** Nobody is signed in, or the session timeout logged the user out. */
    LOGIN,

    /** Signed in: straight into the app, like the web. */
    APP,

    /** Signed in, but the "Lock" session timeout passed while the app wasn't used. */
    LOCK,
}
