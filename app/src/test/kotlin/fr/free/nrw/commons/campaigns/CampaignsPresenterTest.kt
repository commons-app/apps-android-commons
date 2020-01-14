package fr.free.nrw.commons.campaigns

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CampaignsPresenterTest {
    @Mock
    var okHttpJsonApiClient: OkHttpJsonApiClient? = null

    lateinit var campaignsPresenter: CampaignsPresenter

    @Mock
    internal var view: ICampaignsView? = null

    @Mock
    internal var campaignResponseDTO: CampaignResponseDTO? = null
    lateinit var campaignsSingle: Single<CampaignResponseDTO>

    @Mock
    var campaign: Campaign? = null

    lateinit var testScheduler: TestScheduler

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
        campaignsPresenter?.onAttachView(view)
        Mockito.`when`(okHttpJsonApiClient?.campaigns).thenReturn(campaignsSingle)
    }

    @Test
    fun getCampaignsTestNoCampaigns() {
        campaignsPresenter.getCampaigns()
        verify(okHttpJsonApiClient)?.campaigns
        testScheduler.triggerActions()
        verify(view)?.showCampaigns(null)
    }

    @Test
    fun getCampaignsTestNonEmptyCampaigns() {
        campaignsPresenter.getCampaigns()
        var campaigns= ArrayList<Campaign>()
        campaigns.add(campaign!!)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        Mockito.`when`(campaignResponseDTO?.campaigns).thenReturn(campaigns)
        var calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE,-1)
        val startDateString = simpleDateFormat.format(calendar.time).toString()
        calendar= Calendar.getInstance()
        calendar.add(Calendar.DATE,3)
        val endDateString= simpleDateFormat.format(calendar.time).toString()
        Mockito.`when`(campaign?.endDate).thenReturn(endDateString)
        Mockito.`when`(campaign?.startDate).thenReturn(startDateString)
        Mockito.`when`(campaignResponseDTO?.campaigns).thenReturn(campaigns)
        verify(okHttpJsonApiClient)?.campaigns
        testScheduler.triggerActions()
        verify(view)?.showCampaigns(campaign)
    }
}