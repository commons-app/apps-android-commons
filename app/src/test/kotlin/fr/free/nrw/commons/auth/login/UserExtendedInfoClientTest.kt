package fr.free.nrw.commons.auth.login

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.stream.MalformedJsonException
import fr.free.nrw.commons.MockWebServerTest
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.json.NamespaceTypeAdapter
import org.wikipedia.json.PostProcessingTypeAdapter
import org.wikipedia.json.UriTypeAdapter
import org.wikipedia.json.WikiSiteTypeAdapter
import org.wikipedia.page.Namespace
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class UserExtendedInfoClientTest : MockWebServerTest() {
    private var apiService: LoginInterface? = null
    private val observer = TestObserver<MwQueryResponse>()
    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Uri::class.java, UriTypeAdapter().nullSafe())
        .registerTypeHierarchyAdapter(Namespace::class.java, NamespaceTypeAdapter().nullSafe())
        .registerTypeAdapter(WikiSite::class.java, WikiSiteTypeAdapter().nullSafe())
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
    fun testRequestSuccess() {
        enqueueFromFile("user_extended_info.json")

        apiService!!.getUserInfo("USER").subscribe(observer)

        observer
            .assertComplete()
            .assertNoErrors()
            .assertValue { result: MwQueryResponse ->
                result.query()!!
                    .userInfo()!!.id() == 24531888 && result.query()!!.getUserResponse("USER")!!
                    .name() == "USER"
            }
    }

    @Test
    fun testRequestResponse404() {
        enqueue404()

        apiService!!.getUserInfo("USER").subscribe(observer)

        observer.assertError(Exception::class.java)
    }

    @Test
    fun testRequestResponseMalformed() {
        enqueueMalformed()

        apiService!!.getUserInfo("USER").subscribe(observer)

        observer.assertError(MalformedJsonException::class.java)
    }
}
