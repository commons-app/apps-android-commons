package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.util.Log
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.listeners.FragmentCommunicator
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.*
import javax.inject.Inject

class FullScreenModeClient @Inject constructor(

    /**
     * FileProcessor to pre-process the file.
     */
    var fileProcessor: FileProcessor,

    /**
     * File Utils Wrapper for SHA1
     */
    var fileUtilsWrapper: FileUtilsWrapper,

    /**
     * UploadedStatusDao for cache query.
     */
    var uploadedStatusDao: UploadedStatusDao,

    /**
     * NotForUploadDao for database operations
     */
    var notForUploadStatusDao: NotForUploadStatusDao,

    /**
     * Context for coroutine.
     */
    val context: Context
) {
    /**
     * Coroutine Dispatchers and Scope.
     */
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private val scope : CoroutineScope = MainScope()

    fun insertInNFU(it: Image) {
        scope.launch {
            val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                it.uri,
                ioDispatcher,
                fileUtilsWrapper,
                context.contentResolver
            )
            notForUploadStatusDao.insert(
                NotForUploadStatus(
                    imageSHA1,
                    true
                )
            )
            Log.d("haha", "insertInNFU: "+imageSHA1)
        }
    }
}