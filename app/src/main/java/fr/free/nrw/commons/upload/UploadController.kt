package fr.free.nrw.commons.upload

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadController @Inject constructor(
    private val sessionManager: SessionManager,
    private val context: Context,
    private val store: JsonKvStore
) {
    /**
     * Starts a new upload task.
     *
     * @param contribution the contribution object
     */
    @SuppressLint("StaticFieldLeak")
    fun prepareMedia(contribution: Contribution) {
        //Set creator, desc, and license

        // If author name is enabled and set, use it

        val media = contribution.media
        if (store.getBoolean("useAuthorName", false)) {
            val authorName = store.getString("authorName", "")
            media.author = authorName
        }

        if (media.author.isNullOrEmpty()) {
            val currentAccount = sessionManager.currentAccount
            if (currentAccount == null) {
                Timber.d("Current account is null")
                showLongToast(context, context.getString(R.string.user_not_logged_in))
                sessionManager.forceLogin(context)
                return
            }
            media.author = sessionManager.userName
        }

        if (media.fallbackDescription == null) {
            media.fallbackDescription = ""
        }

        val license = store.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3)
        media.license = license

        buildUpload(contribution)
    }

    private fun buildUpload(contribution: Contribution) {
        val contentResolver = context.contentResolver

        contribution.dataLength = resolveDataLength(contentResolver, contribution)

        val mimeType = resolveMimeType(contentResolver, contribution)

        if (mimeType != null) {
            Timber.d("MimeType is: %s", mimeType)
            contribution.mimeType = mimeType
            if (mimeType.startsWith("image/") && contribution.dateCreated == null) {
                contribution.dateCreated = resolveDateTakenOrNow(contentResolver, contribution)
            }
        }
    }

    private fun resolveMimeType(
        contentResolver: ContentResolver,
        contribution: Contribution
    ): String? {
        val mimeType: String? = contribution.mimeType
        return if (mimeType.isNullOrEmpty() || mimeType.endsWith("*")) {
            contentResolver.getType(contribution.localUri!!)
        } else {
            mimeType
        }
    }

    private fun resolveDataLength(
        contentResolver: ContentResolver,
        contribution: Contribution
    ): Long {
        try {
            if (contribution.dataLength <= 0) {
                Timber.d(
                    "UploadController/doInBackground, contribution.getLocalUri():%s",
                    contribution.localUri
                )

                contentResolver.openAssetFileDescriptor(
                    Uri.fromFile(File(contribution.localUri!!.path!!)), "r"
                )?.use {
                    return if (it.length != -1L) it.length
                    else countBytes(contentResolver.openInputStream(contribution.localUri))
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Exception occurred while uploading image")
        } catch (e: NullPointerException) {
            Timber.e(e, "Exception occurred while uploading image")
        } catch (e: SecurityException) {
            Timber.e(e, "Exception occurred while uploading image")
        }
        return contribution.dataLength
    }

    private fun resolveDateTakenOrNow(
        contentResolver: ContentResolver,
        contribution: Contribution
    ): Date {
        Timber.d("local uri   %s", contribution.localUri)
        dateTakenCursor(contentResolver, contribution).use { cursor ->
            if (cursor != null && cursor.count != 0 && cursor.columnCount != 0) {
                cursor.moveToFirst()
                val dateCreated = Date(cursor.getLong(0))
                if (dateCreated.after(Date(0))) {
                    return dateCreated
                }
            }
            return Date()
        }
    }

    private fun dateTakenCursor(
        contentResolver: ContentResolver,
        contribution: Contribution
    ): Cursor? = contentResolver.query(
        contribution.localUri!!,
        arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN), null, null, null
    )

    /**
     * Counts the number of bytes in `stream`.
     *
     * @param stream the stream
     * @return the number of bytes in `stream`
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun countBytes(stream: InputStream?): Long {
        var count: Long = 0
        val bis = BufferedInputStream(stream)
        while (bis.read() != -1) {
            count++
        }
        return count
    }
}
