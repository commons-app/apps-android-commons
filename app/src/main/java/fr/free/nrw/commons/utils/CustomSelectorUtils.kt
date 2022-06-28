package fr.free.nrw.commons.utils

import android.content.ContentResolver
import android.net.Uri
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Util Class for Custom Selector
 */
class CustomSelectorUtils {
    companion object {
        /**
         * Get image sha1 from uri, used to retrieve the original image sha1.
         */
        suspend fun getImageSHA1(uri: Uri,
                                 ioDispatcher : CoroutineDispatcher,
                                 fileUtilsWrapper: FileUtilsWrapper,
                                 contentResolver: ContentResolver
        ): String {
            return withContext(ioDispatcher) {

                try {
                    val result = fileUtilsWrapper.getSHA1(contentResolver.openInputStream(uri))
                    result
                } catch (e: FileNotFoundException){
                    e.printStackTrace()
                    ""
                }
            }
        }
    }
}