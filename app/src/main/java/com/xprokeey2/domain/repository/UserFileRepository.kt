package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.domain.util.Resource

/** Files the user picked or created with Android's file screens, addressed by their content URI. */
interface UserFileRepository {

    /** Name and size; null when the file can't be read. */
    suspend fun describe(uri: String): PickedFile?

    suspend fun read(uri: String): Resource<ByteArray>

    /** Replaces the file's content. */
    suspend fun write(uri: String, bytes: ByteArray): Resource<Unit>

    /** Best effort: removes a file created for an export that then failed. */
    suspend fun delete(uri: String)
}
