package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class FileProcessorTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var gpsCategoryModel: GpsCategoryModel

    @Mock
    private lateinit var depictsModel: DepictModel

    @Mock
    private lateinit var apiCall: CategoryApi

    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private lateinit var dependencies: FileProcessorDependencies

    private lateinit var fileProcessor: FileProcessor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        dependencies =
            FileProcessorDependencies(
                gpsCategoryModel = gpsCategoryModel,
                depictsModel = depictsModel,
                apiCall = apiCall,
                okHttpJsonApiClient = okHttpJsonApiClient,
            )
        fileProcessor = FileProcessor(context, contentResolver, defaultKvStore, dependencies)
    }

    @Test
    fun testFileProcessorDependenciesNotNull() {
        assertNotNull(dependencies)
        assertNotNull(dependencies.gpsCategoryModel)
        assertNotNull(dependencies.depictsModel)
        assertNotNull(dependencies.apiCall)
        assertNotNull(dependencies.okHttpJsonApiClient)
    }

    @Test
    fun testFileProcessorInitialization() {
        assertNotNull(fileProcessor)
    }
}
