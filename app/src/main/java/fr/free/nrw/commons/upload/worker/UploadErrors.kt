package fr.free.nrw.commons.upload.worker

import java.io.IOException

/**
 * Returns the error code of a failed upload,
 * or the exception message if there is no code.
 *
 * UploadClient throws an IOException containing the server error code,
 * which may be wrapped in a RuntimeException by blockingSingle().
 */
internal fun Throwable.uploadErrorCode(): String? {
    val cause = cause
    return if (this !is IOException && cause is IOException) cause.message else message
}

/**
 * Server error codes that retrying the same file with the same name can never fix.
 * Most of them come from UploadBase::convertVerifyErrorToStatus() in MediaWiki.
 * verification-error is left out, because a chunk damaged during the upload can cause it,
 * and uploading the file again can then succeed.
 */
private val permanentUploadErrors =
    setOf(
        "filename-tooshort",
        "illegal-filename",
        "filename-toolong",
        "filetype-missing",
        "windows-nonascii-filename",
        "empty-file",
        "file-too-large",
        "filetype-banned",
        // From the TitleBlacklist extension on Commons, for example a caption that is only "§".
        "titleblacklist-forbidden",
    )

/**
 * Returns true if the upload failed for a reason that retrying cannot fix.
 */
internal fun isPermanentUploadError(errorInfo: String?): Boolean = errorInfo in permanentUploadErrors
