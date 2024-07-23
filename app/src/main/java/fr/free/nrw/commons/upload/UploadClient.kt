package fr.free.nrw.commons.upload

import com.google.gson.Gson
import com.google.gson.JsonObject
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.upload.worker.UploadWorker.NotificationUpdateProgressListener
import fr.free.nrw.commons.wikidata.mwapi.MwException
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadClient @Inject constructor(
    private val uploadInterface: UploadInterface,
    private val csrfTokenClient: CsrfTokenClient,
    private val pageContentsCreator: PageContentsCreator,
    private val fileUtilsWrapper: FileUtilsWrapper,
    private val gson: Gson, private val timeProvider: TimeProvider,
    private val contributionDao: ContributionDao
) {
    private val CHUNK_SIZE = 512 * 1024 // 512 KB

    //This is maximum duration for which a stash is persisted on MediaWiki
    // https://www.mediawiki.org/wiki/Manual:$wgUploadStashMaxAge
    private val MAX_CHUNK_AGE = 6 * 3600 * 1000 // 6 hours
    private val compositeDisposable = CompositeDisposable()

    /**
     * Upload file to stash in chunks of specified size. Uploading files in chunks will make
     * handling of large files easier. Also, it will be useful in supporting pause/resume of
     * uploads
     */
    @Throws(IOException::class)
    fun uploadFileToStash(
        filename: String, contribution: Contribution,
        notificationUpdater: NotificationUpdateProgressListener
    ): Observable<StashUploadResult> {
        if (contribution.isCompleted()) {
            return Observable.just(
                StashUploadResult(StashUploadState.SUCCESS, contribution.fileKey,null)
            )
        }

        val file = contribution.localUriPath
        val fileChunks = fileUtilsWrapper.getFileChunks(file, CHUNK_SIZE)
        val mediaType = fileUtilsWrapper.getMimeType(file).toMediaTypeOrNull()

        val chunkInfo = AtomicReference<ChunkInfo?>()
        if (isStashValid(contribution)) {
            chunkInfo.set(contribution.chunkInfo)
            Timber.d(
                "Chunk: Next Chunk: %s, Total Chunks: %s",
                contribution.chunkInfo!!.indexOfNextChunkToUpload,
                contribution.chunkInfo!!.totalChunks
            )
        }

        val index = AtomicInteger()
        val failures = AtomicBoolean()
        val errorMessage = AtomicReference<String>()
        compositeDisposable.add(
            Observable.fromIterable(fileChunks).forEach { chunkFile: File ->
                if (canProcess(contributionDao, contribution, failures)) {
                    if (contributionDao.getContribution(contribution.pageId) == null) {
                        compositeDisposable.clear()
                        return@forEach
                    } else {
                        processChunk(
                            filename,
                            contribution,
                            notificationUpdater,
                            chunkFile,
                            failures,
                            chunkInfo,
                            index,
                            errorMessage,
                            mediaType!!,
                            file!!,
                            fileChunks.size
                        )
                    }
                }
            }
        )

        return when {
            contributionDao.getContribution(contribution.pageId) == null -> {
                return Observable.just(StashUploadResult(StashUploadState.CANCELLED, null, "Upload cancelled"))
            }
            contributionDao.getContribution(contribution.pageId).state == Contribution.STATE_PAUSED
                    || CommonsApplication.isPaused -> {
                Timber.d("Upload stash paused %s", contribution.pageId)
                Observable.just(StashUploadResult(StashUploadState.PAUSED, null, null))
            }
            failures.get() -> {
                Timber.d("Upload stash contains failures %s", contribution.pageId)
                Observable.just(StashUploadResult(StashUploadState.FAILED, null, errorMessage.get()))
            }
            chunkInfo.get() != null -> {
                Timber.d("Upload stash success %s", contribution.pageId)
                Observable.just(
                    StashUploadResult(
                        StashUploadState.SUCCESS,
                        chunkInfo.get()!!.uploadResult!!.filekey,
                        "success"
                    )
                )
            }
            else -> {
                Timber.d("Upload stash failed %s", contribution.pageId)
                Observable.just(StashUploadResult(StashUploadState.FAILED, null,null))
            }
        }
    }

    private fun processChunk(
        filename: String, contribution: Contribution,
        notificationUpdater: NotificationUpdateProgressListener, chunkFile: File,
        failures: AtomicBoolean, chunkInfo: AtomicReference<ChunkInfo?>, index: AtomicInteger,
        errorMessage : AtomicReference<String>, mediaType: MediaType, file: File, totalChunks: Int
    ) {
        if (shouldSkip(chunkInfo, index)) {
            index.incrementAndGet()
            Timber.d("Chunk: Increment and return: %s", index.get())
            return
        }

        index.getAndIncrement()

        val offset = if (chunkInfo.get() != null) chunkInfo.get()!!.uploadResult!!.offset else 0
        Timber.d("Chunk: Sending Chunk number: %s, offset: %s", index.get(), offset)

        val filekey = chunkInfo.get()?.let { it.uploadResult!!.filekey }
        val requestBody = chunkFile.asRequestBody(mediaType)
        val listener = { transferred: Long, total: Long ->
            notificationUpdater.onProgress(transferred, total)
        }
        val countingRequestBody = CountingRequestBody(requestBody, listener, offset.toLong(), file.length())

        compositeDisposable.add(
            uploadChunkToStash(
                filename, file.length(), offset.toLong(), filekey, countingRequestBody
            ).subscribe(
                { uploadResult: UploadResult ->
                    Timber.d(
                        "Chunk: Received Chunk number: %s, offset: %s",
                        index.get(),
                        uploadResult.offset
                    )
                    chunkInfo.set(ChunkInfo(uploadResult, index.get(), totalChunks))
                    notificationUpdater.onChunkUploaded(contribution, chunkInfo.get())
                }, { throwable: Throwable? ->
                    Timber.e(throwable, "Received error in chunk upload")
                    errorMessage.set(throwable?.message)
                    failures.set(true)
                }
            )
        )
    }

    /**
     * Stash is valid for 6 hours. This function checks the validity of stash
     *
     * @param contribution
     * @return
     */
    private fun isStashValid(contribution: Contribution): Boolean {
        return contribution.chunkInfo != null &&
                contribution.dateModified!!.after(Date(
                    timeProvider.currentTimeMillis() - MAX_CHUNK_AGE))
    }

    /**
     * Uploads a file chunk to stash
     *
     * @param filename            The name of the file being uploaded
     * @param fileSize            The total size of the file
     * @param offset              The offset returned by the previous chunk upload
     * @param fileKey             The filekey returned by the previous chunk upload
     * @param countingRequestBody Request body with chunk file
     * @return
     */
    fun uploadChunkToStash(
        filename: String?,
        fileSize: Long,
        offset: Long,
        fileKey: String?,
        countingRequestBody: CountingRequestBody
    ): Observable<UploadResult> {
        val filePart: MultipartBody.Part
        return try {
            filePart = MultipartBody.Part.createFormData(
                "chunk",
                URLEncoder.encode(filename, "utf-8"),
                countingRequestBody
            )
            uploadInterface.uploadFileToStash(
                toRequestBody(filename),
                toRequestBody(fileSize.toString()),
                toRequestBody(offset.toString()),
                toRequestBody(fileKey),
                toRequestBody(csrfTokenClient.getTokenBlocking()),
                filePart
            ).map(UploadResponse::upload)
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Failed to upload chunk to stash")
            Observable.error(throwable)
        }
    }

    /**
     * Converts string value to request body
     */
    private fun toRequestBody(value: String?): RequestBody? {
        return value?.toRequestBody(MultipartBody.FORM)
    }

    fun uploadFileFromStash(
        contribution: Contribution?,
        uniqueFileName: String?,
        fileKey: String?
    ): Observable<UploadResult?> {
        return try {
            uploadInterface.uploadFileFromStash(
                    csrfTokenClient.getTokenBlocking(),
                    pageContentsCreator.createFrom(contribution),
                    CommonsApplication.DEFAULT_EDIT_SUMMARY,
                    uniqueFileName!!,
                    fileKey!!
                ).map { uploadResponse: JsonObject? ->
                    val uploadResult = gson.fromJson(uploadResponse, UploadResponse::class.java)
                    if (uploadResult.upload == null) {
                        val exception = gson.fromJson(uploadResponse, MwException::class.java)
                        Timber.e(exception, "Error in uploading file from stash")
                        throw Exception(exception.getErrorCode())
                    }
                    uploadResult.upload
                }
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Exception occurred in uploading file from stash")
            Observable.error(throwable)
        }
    }

    fun interface TimeProvider {
        fun currentTimeMillis(): Long
    }
}

private fun canProcess(
    contributionDao: ContributionDao,
    contribution: Contribution,
    failures: AtomicBoolean
): Boolean {
    // As long as the contribution hasn't been paused and there are no errors,
    // we can process the current chunk.
    return !(contributionDao.getContribution(contribution.pageId).state == Contribution.STATE_PAUSED
            || failures.get() || CommonsApplication.isPaused)
}

private fun shouldSkip(
    chunkInfo: AtomicReference<ChunkInfo?>,
    index: AtomicInteger
): Boolean {
    return chunkInfo.get() != null && index.get() < chunkInfo.get()!!.indexOfNextChunkToUpload
}
