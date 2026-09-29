package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.domain.repository.UserFileRepository
import javax.inject.Inject

/** Name and size of the file picked on the Import screen; null when it can't be read. */
class GetPickedFileUseCase @Inject constructor(
    private val userFiles: UserFileRepository,
) {
    suspend operator fun invoke(uri: String): PickedFile? = userFiles.describe(uri)
}
