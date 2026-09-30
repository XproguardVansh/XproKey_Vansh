package com.xprokeey2.domain.security

import com.xprokeey2.domain.model.AccessBlock
import kotlinx.coroutines.flow.Flow

/** Reports every request the server's access guard refused, wherever in the app it was made. */
interface AccessGate {
    val blocks: Flow<AccessBlock>
}
