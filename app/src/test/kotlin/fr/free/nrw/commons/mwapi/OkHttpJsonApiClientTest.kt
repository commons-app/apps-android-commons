package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.campaigns.CampaignResponseDTO
import fr.free.nrw.commons.explore.depictions.DepictsClient
import io.reactivex.Single
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wikipedia.test.TestFileUtil

class OkHttpJsonApiClientTest() {

    @Mock
    internal var okHttpClient: OkHttpClient? = null

    @Mock
    internal var depictsClient: DepictsClient? = null

    @Mock
    internal var wikiMediaToolforgeUrl: HttpUrl? = null

    @Mock
    internal var gson: Gson? = null

    @Mock
    internal lateinit var campaignResponseDTO: CampaignResponseDTO
    lateinit var campaignsSingle: Single<CampaignResponseDTO>

    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private var mockWebServer = MockWebServer()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        campaignsSingle= Single.just(campaignResponseDTO)
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun whenApiDoesNotHaveCampaigns_getCampaignsShouldReturnNull() {
        val baseUrl: HttpUrl = mockWebServer.url("/api/blah");
        mockWebServer.enqueue(MockResponse().setBody("{}"));

        okHttpJsonApiClient = OkHttpJsonApiClient(okHttpClient, depictsClient, wikiMediaToolforgeUrl, "",
            baseUrl.toString(), gson)

        val campaign: Single<CampaignResponseDTO> = okHttpJsonApiClient!!.campaigns
        campaign.doOnSuccess { assertNull(it) }
    }

    @Test
    fun whenApiHasCampaigns_getCampaignShouldReturnCampaigns() {
        val baseUrl: HttpUrl = mockWebServer.url("/api/blah");

        val json = TestFileUtil.readRawFile("campaigns_beta_active.json")
        mockWebServer.enqueue(MockResponse().setBody(json.toString()));

        okHttpJsonApiClient = OkHttpJsonApiClient(okHttpClient, depictsClient, wikiMediaToolforgeUrl, "",
            baseUrl.toString(), gson)

        val campaign: Single<CampaignResponseDTO> = okHttpJsonApiClient!!.campaigns
        campaign.doOnSuccess { assertNotNull(it) }

    }
}