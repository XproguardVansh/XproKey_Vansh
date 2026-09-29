package com.xprokeey2.data.local.files

import android.content.Context
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.xprokeey2.domain.model.ImportFileProblem
import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.domain.repository.UserFileRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** [UserFileRepository] over the content URIs that Android's "Open" and "Save as" screens return. */
@Singleton
class ContentUriFileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : UserFileRepository {

    private val resolver get() = context.contentResolver

    override suspend fun describe(uri: String): PickedFile? = withContext(Dispatchers.IO) {
        try {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            resolver.query(uri.toUri(), columns, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(0) ?: uri.toUri().lastPathSegment.orEmpty()
                val size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
                PickedFile(uri = uri, name = name, sizeBytes = size)
            }
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun read(uri: String): Resource<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val bytes = resolver.openInputStream(uri.toUri())?.use { it.readBytes() }
            if (bytes != null) Resource.Success(bytes) else Unreadable
        } catch (e: IOException) {
            Unreadable
        } catch (e: SecurityException) {
            Unreadable
        }
    }

    override suspend fun write(uri: String, bytes: ByteArray): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            // "wt" truncates an existing file; a few providers only know "w".
            val stream = try {
                resolver.openOutputStream(uri.toUri(), "wt")
            } catch (e: FileNotFoundException) {
                resolver.openOutputStream(uri.toUri(), "w")
            } catch (e: IllegalArgumentException) {
                resolver.openOutputStream(uri.toUri(), "w")
            } ?: return@withContext Resource.Error(DataError.Unknown("No output stream"))
            stream.use { it.write(bytes) }
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error(DataError.Unknown(e.message))
        } catch (e: SecurityException) {
            Resource.Error(DataError.Unknown(e.message))
        }
    }

    override suspend fun delete(uri: String) {
        withContext(Dispatchers.IO) {
            try {
                DocumentsContract.deleteDocument(resolver, uri.toUri())
            } catch (e: FileNotFoundException) {
                // Already gone.
            } catch (e: SecurityException) {
                // Not allowed by this provider: an empty file stays behind.
            } catch (e: UnsupportedOperationException) {
                // Provider can't delete.
            } catch (e: IllegalArgumentException) {
                // Not a document URI.
            }
        }
    }

    private companion object {
        val Unreadable = Resource.Error(DataError.ImportFile(ImportFileProblem.UNREADABLE))
    }
}
