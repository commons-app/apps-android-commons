package fr.free.nrw.commons.auth.login

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.stream.MalformedJsonException
import fr.free.nrw.commons.MockWebServerTest
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import fr.free.nrw.commons.wikidata.model.WikiSite
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.json.NamespaceTypeAdapter
import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter
import fr.free.nrw.commons.wikidata.json.UriTypeAdapter
import fr.free.nrw.commons.wikidata.json.WikiSiteTypeAdapter
import fr.free.nrw.commons.wikidata.model.page.Namespace
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class UserExtendedInfoClientTest : MockWebServerTest() {
    private var apiService: LoginInterface? = null
    private val observer = TestObserver<MwQueryResponse>()
    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Uri::class.java, UriTypeAdapter()
            .nullSafe())
        .registerTypeHierarchyAdapter(
            Namespace::class.java, NamespaceTypeAdapter()
            .nullSafe())
        .registerTypeAdapter(
            WikiSite::class.java, WikiSiteTypeAdapter()
            .nullSafe())
        .registerTypeAdapterFactory(PostProcessingTypeAdapter())
        .create()

    @Before
    @Throws(Throwable::class)
    override fun setUp() {
        super.setUp()

        apiService = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(server().url)
            .build()
            .create(LoginInterface::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() = runTest {
        enqueueFromFile("user_extended_info.json")

        val result = apiService!!.getUserInfo("USER")

        val userInfo = result.body()?.query()?.userInfo()
        assertEquals(24531888, userInfo?.id())
        val userResponse = result.body()?.query()?.getUserResponse("USER")?.name()
        assertEquals("USER", userResponse)
    }

    @Test
    fun testRequestResponse404() = runTest {
        enqueue404()

        val result = apiService!!.getUserInfo("USER")

        assertFalse(result.isSuccessful)
        assertEquals(404, result.code())
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() = runTest {
        enqueueMalformed()

        try {
            apiService!!.getUserInfo("USER")
            fail("Exception expected")
        } catch (e: Exception) {
            assertTrue(e is MalformedJsonException)
        }
    }
}
