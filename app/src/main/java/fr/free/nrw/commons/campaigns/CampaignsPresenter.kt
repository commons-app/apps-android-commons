package fr.free.nrw.commons.campaigns

import android.annotation.SuppressLint
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD
import fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.CommonsDateUtil.getIso8601DateFormatShort
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The presenter for the campaigns view, fetches the campaigns from the api and informs the view on
 * success and error
 */
@Singleton
class CampaignsPresenter @Inject constructor(
    private val okHttpJsonApiClient: OkHttpJsonApiClient?,
    @param:Named(IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(MAIN_THREAD) private val mainThreadScheduler: Scheduler
) : BasePresenter<ICampaignsView> {
    private var view: ICampaignsView? = null
    private var disposable: Disposable? = null
    private var campaign: Campaign? = null

    override fun onAttachView(view: ICampaignsView) {
        this.view = view
    }

    override fun onDetachView() {
        view = null
        disposable?.dispose()
    }

    /**
     * make the api call to fetch the campaigns
     */
    @SuppressLint("CheckResult")
    fun getCampaigns() {
        if (view != null && okHttpJsonApiClient != null) {
            //If we already have a campaign, lets not make another call
            if (campaign != null) {
                view!!.showCampaigns(campaign)
                return
            }

            okHttpJsonApiClient.getCampaigns()
                .observeOn(mainThreadScheduler)
                .subscribeOn(ioScheduler)
                .doOnSubscribe { disposable = it }
                .subscribe({ campaignResponseDTO ->
                    val campaigns = campaignResponseDTO?.campaigns?.toMutableList()
                    if (campaigns.isNullOrEmpty()) {
                        Timber.e("The campaigns list is empty")
                        view!!.showCampaigns(null)
                    } else {
                        sortCampaignsByStartDate(campaigns)
                        campaign = findActiveCampaign(campaigns)
                        view!!.showCampaigns(campaign)
                    }
                }, {
                    Timber.e(it, "could not fetch campaigns")
                })
        }
    }

    private fun sortCampaignsByStartDate(campaigns: MutableList<Campaign>) {
        val dateFormat: SimpleDateFormat = getIso8601DateFormatShort()
        campaigns.sortWith(Comparator { campaign: Campaign, other: Campaign ->
            val date1: Date?
            val date2: Date?
            try {
                date1 = campaign.startDate?.let { dateFormat.parse(it) }
                date2 = other.startDate?.let { dateFormat.parse(it) }
            } catch (e: ParseException) {
                Timber.e(e)
                return@Comparator -1
            }
            if (date1 != null && date2 != null) date1.compareTo(date2) else -1
        })
    }

    private fun findActiveCampaign(campaigns: List<Campaign>) : Campaign? {
        val dateFormat: SimpleDateFormat = getIso8601DateFormatShort()
        val currentDate = Date()
        return try {
            campaigns.firstOrNull {
                val campaignStartDate = it.startDate?.let { s -> dateFormat.parse(s) }
                val campaignEndDate = it.endDate?.let { s -> dateFormat.parse(s) }
                campaignStartDate != null && campaignEndDate != null &&
                        campaignEndDate >= currentDate && campaignStartDate <= currentDate
            }
        } catch (e: ParseException) {
            Timber.e(e, "could not find active campaign")
            null
        }
    }
}
