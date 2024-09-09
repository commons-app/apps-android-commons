package fr.free.nrw.commons.upload

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication.DEFAULT_EDIT_SUMMARY
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.upload.UploadClient.TimeProvider
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwServiceError
import io.reactivex.Observable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.commons.annotation.Testable
import java.io.File
import java.util.Date


class UploadClientTest {

    private val contribution = mock<Contribution>()
    private val uploadResult = mock<UploadResult>()
    private val uploadInterface = mock<UploadInterface>()
    private val csrfTokenClient = mock<CsrfTokenClient>()
    private val pageContentsCreator = mock<PageContentsCreator>()
    private val fileUtilsWrapper = mock<FileUtilsWrapper>()
    private val gson = mock<Gson>()
    private val contributionDao = mock<ContributionDao> { }
    private val timeProvider = mock<TimeProvider>()
    private val uploadClient = UploadClient(
        uploadInterface,
        csrfTokenClient,
        pageContentsCreator,
        fileUtilsWrapper,
        gson,
        timeProvider,
        contributionDao
    )

    private val expectedChunkSize = 512 * 1024
    private val testToken = "test-token"
    private val createdContent = "content"
    private val filename = "test.jpg"
    private val filekey = "the-key"
    private val pageId = "page-id"
    private val errorCode = "the-code"
    private val uploadJson = Gson().fromJson("{\"foo\" = 1}", JsonObject::class.java)

    private val uploadResponse = UploadResponse(uploadResult)
    private val errorResponse = UploadResponse(null)

    @Before
    fun setUp() {
        whenever(csrfTokenClient.getTokenBlocking()).thenReturn(testToken)
        whenever(pageContentsCreator.createFrom(contribution)).thenReturn(createdContent)
    }

    @Test
    fun testUploadFileFromStash_NoErrors() {
        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(uploadResponse)
        whenever(
            uploadInterface.uploadFileFromStash(
                testToken,
                createdContent,
                DEFAULT_EDIT_SUMMARY,
                filename,
                filekey
            )
        ).thenReturn(Observable.just(uploadJson))

        val result = uploadClient.uploadFileFromStash(contribution, filename, filekey).test()

        result.assertNoErrors()
        assertSame(uploadResult, result.values()[0])
    }

    @Test
    fun testUploadFileFromStash_WithError() {
        val error = mock<MwServiceError>()
        whenever(error.code).thenReturn(errorCode)
        val uploadException = MwException(error, null)

        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(errorResponse)
        whenever(gson.fromJson(uploadJson, MwException::class.java)).thenReturn(uploadException)
        whenever(
            uploadInterface.uploadFileFromStash(
                testToken,
                createdContent,
                DEFAULT_EDIT_SUMMARY,
                filename,
                filekey
            )
        ).thenReturn(Observable.just(uploadJson))

        val result = uploadClient.uploadFileFromStash(contribution, filename, filekey).test()

        result.assertNoValues()
        assertEquals(errorCode, result.errors()[0].message)
    }

    @Test
    fun testUploadFileFromStash_Failure() {
        val exception = Exception("test")
        whenever(
            uploadInterface.uploadFileFromStash(
                testToken,
                createdContent,
                DEFAULT_EDIT_SUMMARY,
                filename,
                filekey
            )
        )
            .thenReturn(Observable.error(exception))

        val result = uploadClient.uploadFileFromStash(contribution, filename, filekey).test()

        result.assertNoValues()
        assertEquals(exception, result.errors()[0])
    }

    @Test
    fun testUploadChunkToStash_Success() {
        val fileContent = "content"
        val requestBody: RequestBody = fileContent.toRequestBody("text/plain".toMediaType())
        val countingRequestBody =
            CountingRequestBody(requestBody, mock(), 0, fileContent.length.toLong())

        val filenameCaptor: KArgumentCaptor<RequestBody> = argumentCaptor<RequestBody>()
        val totalFileSizeCaptor = argumentCaptor<RequestBody>()
        val offsetCaptor = argumentCaptor<RequestBody>()
        val fileKeyCaptor = argumentCaptor<RequestBody>()
        val tokenCaptor = argumentCaptor<RequestBody>()
        val fileCaptor = argumentCaptor<MultipartBody.Part>()

        whenever(
            uploadInterface.uploadFileToStash(
                filenameCaptor.capture(), totalFileSizeCaptor.capture(), offsetCaptor.capture(),
                fileKeyCaptor.capture(), tokenCaptor.capture(), fileCaptor.capture()
            )
        ).thenReturn(Observable.just(uploadResponse))

        val result =
            uploadClient.uploadChunkToStash(filename, 100, 10, filekey, countingRequestBody).test()

        result.assertNoErrors()
        assertSame(uploadResult, result.values()[0])

        assertEquals(filename, filenameCaptor.asString())
        assertEquals("100", totalFileSizeCaptor.asString())
        assertEquals("10", offsetCaptor.asString())
        assertEquals(filekey, fileKeyCaptor.asString())
        assertEquals(testToken, tokenCaptor.asString())
        assertEquals(fileContent, fileCaptor.firstValue.body.asString())
    }

    @Test
    fun testUploadChunkToStash_Failure() {
        val exception = Exception("expected")
        whenever(uploadInterface.uploadFileToStash(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.error(exception))

        val result = uploadClient.uploadChunkToStash(filename, 100, 10, filekey, mock()).test()

        result.assertNoValues()
        assertSame(exception, result.errors()[0])
    }

    @Test
    fun uploadFileToStash_completedContribution() {
        whenever(contribution.isCompleted()).thenReturn(true)
        whenever(contribution.fileKey).thenReturn(filekey)

        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()

        result.assertNoErrors()
        val stashResult = result.values()[0]
        assertEquals(filekey, stashResult.fileKey)
        assertEquals(StashUploadState.SUCCESS, stashResult.state)
    }

    @Test
    fun uploadFileToStash_returnsFailureIfNothingToUpload() {
        val tempFile = File.createTempFile("tempFile", ".tmp")
        tempFile.deleteOnExit()
        whenever(contribution.isCompleted()).thenReturn(false)
        whenever(contribution.fileKey).thenReturn(filekey)
        whenever(contribution.pageId).thenReturn(pageId)
        whenever(contributionDao.getContribution(pageId)).thenReturn(contribution)
        whenever(contribution.localUriPath).thenReturn(tempFile)
        whenever(fileUtilsWrapper.getMimeType(anyOrNull<File>())).thenReturn("image/png")
        whenever(fileUtilsWrapper.getFileChunks(anyOrNull<File>(), eq(expectedChunkSize))).thenReturn(emptyList())
        val result = uploadClient.uploadFileToStash(filename, contribution, mock() ).test()
        result.assertNoErrors()
        assertEquals(StashUploadState.FAILED, result.values()[0].state)
    }

    @Test
    fun uploadFileToStash_returnsFailureIfAnyChunkFails() {
        val mockFile = mock<File>()
        whenever(mockFile.length()).thenReturn(1)
        whenever(contribution.localUriPath).thenReturn(mockFile)
        whenever(contribution.isCompleted()).thenReturn(false)
        whenever(contribution.pageId).thenReturn(pageId)
        whenever(contributionDao.getContribution(pageId)).thenReturn(contribution)
        whenever(contribution.fileKey).thenReturn(filekey)
        whenever(fileUtilsWrapper.getMimeType(anyOrNull<File>())).thenReturn("image/png")
        whenever(
            fileUtilsWrapper.getFileChunks(
                anyOrNull<File>(),
                eq(expectedChunkSize)
            )
        ).thenReturn(listOf(mockFile))
        whenever(
            uploadInterface.uploadFileToStash(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(Observable.just(uploadResponse))

        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()

        result.assertNoErrors()
        assertEquals(StashUploadState.FAILED, result.values()[0].state)
    }

    @Test
    fun uploadFileToStash_successWithOneChunk() {
        val mockFile = mock<File>()
        val chunkInfo = mock<ChunkInfo>()
        whenever(mockFile.length()).thenReturn(10)
        whenever(chunkInfo.uploadResult).thenReturn(uploadResult)

        whenever(uploadResult.offset).thenReturn(1)
        whenever(uploadResult.filekey).thenReturn(filekey)

        whenever(contribution.localUriPath).thenReturn(mockFile)
        whenever(contribution.chunkInfo).thenReturn(chunkInfo)
        whenever(contribution.isCompleted()).thenReturn(false)
        whenever(contribution.dateModified).thenReturn(Date(100))
        whenever(timeProvider.currentTimeMillis()).thenReturn(200)
        whenever(contribution.fileKey).thenReturn(filekey)
        whenever(contribution.pageId).thenReturn(pageId)
        whenever(contributionDao.getContribution(pageId)).thenReturn(contribution)

        whenever(fileUtilsWrapper.getMimeType(anyOrNull<File>())).thenReturn("image/png")
        whenever(
            fileUtilsWrapper.getFileChunks(
                anyOrNull<File>(),
                eq(expectedChunkSize)
            )
        ).thenReturn(listOf(mockFile))

        whenever(
            uploadInterface.uploadFileToStash(
                anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull()
            )
        ).thenReturn(Observable.just(uploadResponse))

        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()

        result.assertNoErrors()
        assertEquals(StashUploadState.SUCCESS, result.values()[0].state)
        assertEquals(filekey, result.values()[0].fileKey)
    }


    private fun KArgumentCaptor<RequestBody>.asString(): String =
        firstValue.asString()

    private fun RequestBody.asString(): String {
        val b = Buffer()
        writeTo(b)
        return b.readUtf8()
    }
}