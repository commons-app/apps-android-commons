package fr.free.nrw.commons.upload

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication.Companion.DEFAULT_EDIT_SUMMARY
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.utils.TimeProvider
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwLegacyServiceError
import fr.free.nrw.commons.wikidata.mwapi.MwServiceError
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import java.io.IOException
import java.util.Date

class UploadClientTest {
    @Mock
    private lateinit var uploadInterface: UploadInterface

    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient

    @Mock
    private lateinit var pageContentsCreator: PageContentsCreator

    @Mock
    private lateinit var fileUtilsWrapper: FileUtilsWrapper

    @Mock
    private lateinit var gson: Gson

    @Mock
    private lateinit var timeProvider: TimeProvider

    @Mock
    private lateinit var contributionDao: ContributionDao

    @Mock
    private lateinit var contribution: Contribution

    @Mock
    private lateinit var uploadResult: UploadResult

    @Mock
    private lateinit var uploadResponse: UploadResponse

    private lateinit var dependencies: UploadClientDependencies
    private lateinit var uploadClient: UploadClient

    private val filename = "test.png"
    private val filekey = "filekey"
    private val testToken = "testToken"
    private val createdContent = "createdContent"
    private val uploadJson = JsonObject()
    private val errorResponse = UploadResponse(null)
    private val errorCode = "errorCode"
    private val pageId = "pageId"
    private val expectedChunkSize = 512 * 1024

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        dependencies =
            UploadClientDependencies(
                uploadInterface = uploadInterface,
                csrfTokenClient = csrfTokenClient,
                pageContentsCreator = pageContentsCreator,
                fileUtilsWrapper = fileUtilsWrapper,
                gson = gson,
                timeProvider = timeProvider,
                contributionDao = contributionDao,
            )
        uploadClient = UploadClient(dependencies)
        whenever(csrfTokenClient.getTokenBlocking()).thenReturn(testToken)
        whenever(pageContentsCreator.createFrom(contribution)).thenReturn(createdContent)
        whenever(uploadResponse.upload).thenReturn(uploadResult)
    }

    @Test
    fun testUploadClientDependenciesNotNull() {
        assertNotNull(dependencies)
        assertNotNull(dependencies.uploadInterface)
        assertNotNull(dependencies.csrfTokenClient)
        assertNotNull(dependencies.pageContentsCreator)
        assertNotNull(dependencies.fileUtilsWrapper)
        assertNotNull(dependencies.gson)
        assertNotNull(dependencies.timeProvider)
        assertNotNull(dependencies.contributionDao)
    }

    @Test
    fun testUploadClientInitialization() {
        assertNotNull(uploadClient)
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
                filekey,
            ),
        ).thenReturn(Observable.just(uploadJson))

        val result = uploadClient.uploadFileFromStash(contribution, filename, filekey).test()

        result.assertNoValues()
        val thrown = result.errors()[0]
        assertEquals(errorCode, thrown.message)
        assertTrue(thrown is IOException)
        assertSame(uploadException, thrown.cause)
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
                filekey,
            ),
        ).thenReturn(Observable.error(exception))

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

        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(uploadResponse)
        whenever(
            uploadInterface.uploadFileToStash(
                filenameCaptor.capture(),
                totalFileSizeCaptor.capture(),
                offsetCaptor.capture(),
                fileKeyCaptor.capture(),
                tokenCaptor.capture(),
                fileCaptor.capture(),
            ),
        ).thenReturn(Observable.just(uploadJson))

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
    fun testUploadChunkToStash_SuccessWithRealGson() {
        // Uses the real Gson, to check the JSON Commons sends for an accepted chunk
        val chunkJson =
            Gson().fromJson(
                """{"upload":{"result":"Continue","offset":524288,"filekey":"abc123.jpg"}}""",
                JsonObject::class.java,
            )
        whenever(uploadInterface.uploadFileToStash(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.just(chunkJson))

        val result = uploadClientWithRealGson().uploadChunkToStash(filename, 100, 10, filekey, mock()).test()

        result.assertNoErrors()
        val uploadResult = result.values()[0]
        assertEquals("Continue", uploadResult.result)
        assertEquals(524288, uploadResult.offset)
        assertEquals("abc123.jpg", uploadResult.filekey)
    }

    @Test
    fun testUploadChunkToStash_WithPlaintextErrors() {
        // Uses the real Gson, to check the "errors" JSON Commons sends for a rejected chunk
        val client = uploadClientWithRealGson()
        val errorsJson =
            Gson().fromJson(
                """{"errors":[{"code":"illegal-filename","text":"The filename is not allowed.","module":"upload"}]}""",
                JsonObject::class.java,
            )
        whenever(uploadInterface.uploadFileToStash(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.just(errorsJson))

        val result = client.uploadChunkToStash(filename, 100, 10, filekey, mock()).test()

        result.assertNoValues()
        val thrown = result.errors()[0]
        assertTrue(thrown is IOException)
        assertEquals("illegal-filename", thrown.message)
        assertTrue(thrown.cause is MwException)
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
        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()
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
                eq(expectedChunkSize),
            ),
        ).thenReturn(listOf(mockFile))
        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(uploadResponse)
        whenever(
            uploadInterface.uploadFileToStash(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(Observable.just(uploadJson))

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
                eq(expectedChunkSize),
            ),
        ).thenReturn(listOf(mockFile))

        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(uploadResponse)
        whenever(
            uploadInterface.uploadFileToStash(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(Observable.just(uploadJson))

        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()

        result.assertNoErrors()
        assertEquals(StashUploadState.SUCCESS, result.values()[0].state)
        assertEquals(filekey, result.values()[0].fileKey)
    }

    @Test
    fun uploadFileToStash_handlesNullUploadResultInChunkInfoGracefully() {
        val mockFile = mock<File>()
        val chunkInfo = mock<ChunkInfo>()
        whenever(mockFile.length()).thenReturn(10)
        whenever(chunkInfo.uploadResult).thenReturn(null)
        whenever(chunkInfo.indexOfNextChunkToUpload).thenReturn(0)

        whenever(contribution.isCompleted()).thenReturn(false)
        whenever(contribution.pageId).thenReturn(pageId)
        whenever(contributionDao.getContribution(pageId)).thenReturn(contribution)
        whenever(contribution.chunkInfo).thenReturn(chunkInfo)
        whenever(contribution.dateModified).thenReturn(Date(System.currentTimeMillis()))
        whenever(timeProvider.currentTimeMillis()).thenReturn(System.currentTimeMillis())

        whenever(contribution.localUriPath).thenReturn(mockFile)
        whenever(fileUtilsWrapper.getMimeType(anyOrNull<File>())).thenReturn("image/png")
        whenever(fileUtilsWrapper.getFileChunks(anyOrNull<File>(), eq(expectedChunkSize)))
            .thenReturn(listOf(mockFile))

        whenever(gson.fromJson(uploadJson, UploadResponse::class.java)).thenReturn(uploadResponse)
        whenever(uploadInterface.uploadFileToStash(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.just(uploadJson))

        val result = uploadClient.uploadFileToStash(filename, contribution, mock()).test()
        result.assertNoErrors()
    }

    @Test
    fun testCheckFileName_Blacklisted() {
        whenever(uploadInterface.checkTitleBlacklist("File:Photo.jpg"))
            .thenReturn(Single.just(TitleBlacklistResponse(TitleBlacklistResult("blacklisted"))))

        uploadClient.checkFileName("Photo.jpg").test().assertValue(FileNameCheck.BLACKLISTED)
    }

    @Test
    fun testCheckFileName_Ok() {
        whenever(uploadInterface.checkTitleBlacklist("File:Market in Lagos.jpg"))
            .thenReturn(Single.just(TitleBlacklistResponse(TitleBlacklistResult("ok"))))

        uploadClient.checkFileName("Market in Lagos.jpg").test().assertValue(FileNameCheck.OK)
    }

    @Test
    fun testCheckFileName_ChecksNameAsMediaWikiUploadsIt() {
        // MediaWiki uploads "Market {Lagos}.jpg" as "Market -Lagos-.jpg"
        whenever(uploadInterface.checkTitleBlacklist("File:Market -Lagos-.jpg"))
            .thenReturn(Single.just(TitleBlacklistResponse(TitleBlacklistResult("ok"))))

        uploadClient.checkFileName("Market {Lagos}.jpg").test().assertValue(FileNameCheck.OK)
    }

    @Test
    fun testCheckFileName_InvalidTitle() {
        val error = mock<MwLegacyServiceError> { on { code } doReturn "invalidtitle" }
        whenever(uploadInterface.checkTitleBlacklist("File:A~~~b.jpg"))
            .thenReturn(Single.error(MwIOException("MediaWiki API returned error", error)))

        uploadClient.checkFileName("A~~~b.jpg").test().assertValue(FileNameCheck.INVALID)
    }

    @Test
    fun testCheckFileName_OtherErrorsArePassedOn() {
        val exception = IOException("Unsuccessful response")
        whenever(uploadInterface.checkTitleBlacklist("File:Photo.jpg"))
            .thenReturn(Single.error(exception))

        uploadClient.checkFileName("Photo.jpg").test().assertError(exception)
    }

    private fun uploadClientWithRealGson() =
        UploadClient(
            uploadInterface,
            csrfTokenClient,
            pageContentsCreator,
            fileUtilsWrapper,
            GsonUtil.defaultGson,
            timeProvider,
            contributionDao,
        )

    private fun KArgumentCaptor<RequestBody>.asString(): String = firstValue.asString()

    private fun RequestBody.asString(): String {
        val b = Buffer()
        writeTo(b)
        return b.readUtf8()
    }
}
