package fr.free.nrw.commons.upload

import android.content.SharedPreferences
import fr.free.nrw.commons.caching.CacheController
import fr.free.nrw.commons.mwapi.CategoryApi
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import javax.inject.Inject
import javax.inject.Named

class FileProcessorTest {

    @Mock
    internal var cacheController: CacheController? = null
    @Mock
    internal var gpsCategoryModel: GpsCategoryModel? = null
    @Mock
    internal var apiCall: CategoryApi? = null
    @Mock
    @field:[Inject Named("default_preferences")]
    internal var prefs: SharedPreferences? = null

    @InjectMocks
    var fileProcessor: FileProcessor? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun processFileCoordinates() {

    }
}