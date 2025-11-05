package fr.free.nrw.commons.campaigns

import org.mockito.kotlin.verify
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.ArrayList

class CampaignsPresenterTest {
    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    private lateinit var view: ICampaignsView

    @Mock
    private lateinit var campaignResponseDTO: CampaignResponseDTO

    @Mock
    private lateinit var campaign: Campaign

    @Mock
    private lateinit var disposable: Disposable

    private lateinit var campaignsPresenter: CampaignsPresenter
    private lateinit var campaignsSingle: Single<CampaignResponseDTO>
    private lateinit var testScheduler: TestScheduler

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        campaignsSingle = Single.just(campaignResponseDTO)
        campaignsPresenter = CampaignsPresenter(okHttpJsonApiClient, testScheduler, testScheduler)
        campaignsPresenter.onAttachView(view)
        Mockito.`when`(okHttpJsonApiClient.getCampaigns()).thenReturn(campaignsSingle)
    }

    @Test
    fun getCampaignsTestNoCampaigns() {
        campaignsPresenter.getCampaigns()
        verify(okHttpJsonApiClient).getCampaigns()
        testScheduler.triggerActions()
        verify(view).showCampaigns(null)
    }

    @Test
    fun getCampaignsTestNonEmptyCampaigns() {
        campaignsPresenter.getCampaigns()
        var campaigns = ArrayList<Campaign>()
        campaigns.add(campaign)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        Mockito.`when`(campaignResponseDTO.campaigns).thenReturn(campaigns)
        var calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -1)
        val startDateString = simpleDateFormat.format(calendar.time).toString()
        calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 3)
        val endDateString = simpleDateFormat.format(calendar.time).toString()
        Mockito.`when`(campaign.endDate).thenReturn(endDateString)
        Mockito.`when`(campaign.startDate).thenReturn(startDateString)
        Mockito.`when`(campaignResponseDTO.campaigns).thenReturn(campaigns)
        verify(okHttpJsonApiClient).getCampaigns()
        testScheduler.triggerActions()
        verify(view).showCampaigns(campaign)
    }

    @Test
    fun testGetCampaignsNonNull() {
        val campaignField: Field =
            CampaignsPresenter::class.java.getDeclaredField("campaign")
        campaignField.isAccessible = true
        campaignField.set(campaignsPresenter, campaign)
        campaignsPresenter.getCampaigns()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDetachViewNull() {
        campaignsPresenter.onDetachView()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDetachViewNonNull() {
        val disposableField: Field =
            CampaignsPresenter::class.java.getDeclaredField("disposable")
        disposableField.isAccessible = true
        disposableField.set(campaignsPresenter, disposable)
        campaignsPresenter.onDetachView()
    }
}
