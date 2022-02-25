package fr.free.nrw.commons.campaigns

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.data.models.Campaign
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
import java.util.*
import kotlin.collections.ArrayList

class CampaignsPresenterTest {
    @Mock
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    lateinit var campaignsPresenter: CampaignsPresenter

    @Mock
    internal lateinit var view: ICampaignsView

    @Mock
    internal lateinit var campaignResponseDTO: CampaignResponseDTO
    lateinit var campaignsSingle: Single<CampaignResponseDTO>

    @Mock
    lateinit var campaign: Campaign

    lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var disposable: Disposable

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler=TestScheduler()
        campaignsSingle= Single.just(campaignResponseDTO)
        campaignsPresenter= CampaignsPresenter(okHttpJsonApiClient,testScheduler,testScheduler)
        campaignsPresenter.onAttachView(view)
        Mockito.`when`(okHttpJsonApiClient.campaigns).thenReturn(campaignsSingle)
    }

    @Test
    fun getCampaignsTestNoCampaigns() {
        campaignsPresenter.getCampaigns()
        verify(okHttpJsonApiClient).campaigns
        testScheduler.triggerActions()
        verify(view).showCampaigns(null)
    }

    @Test
    fun getCampaignsTestNonEmptyCampaigns() {
        campaignsPresenter.getCampaigns()
        var campaigns= ArrayList<Campaign>()
        campaigns.add(campaign)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        Mockito.`when`(campaignResponseDTO.campaigns).thenReturn(campaigns)
        var calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE,-1)
        val startDateString = simpleDateFormat.format(calendar.time).toString()
        calendar= Calendar.getInstance()
        calendar.add(Calendar.DATE,3)
        val endDateString= simpleDateFormat.format(calendar.time).toString()
        Mockito.`when`(campaign.endDate).thenReturn(endDateString)
        Mockito.`when`(campaign.startDate).thenReturn(startDateString)
        Mockito.`when`(campaignResponseDTO.campaigns).thenReturn(campaigns)
        verify(okHttpJsonApiClient).campaigns
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
