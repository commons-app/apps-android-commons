package fr.free.nrw.commons.campaigns

import android.annotation.SuppressLint
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.CommonsDateUtil.getIso8601DateFormatShort
import io.reactivex.Scheduler
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.text.ParseException
import java.util.Collections
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
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(
        CommonsApplicationModule.MAIN_THREAD
    ) private val mainThreadScheduler: Scheduler
) :
    BasePresenter<ICampaignsView?> {
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
            val campaigns = okHttpJsonApiClient.campaigns
            campaigns.observeOn(mainThreadScheduler)
                .subscribeOn(ioScheduler)
                .subscribeWith(object : SingleObserver<CampaignResponseDTO> {
                    override fun onSubscribe(d: Disposable) {
                        disposable = d
                    }

                    override fun onSuccess(campaignResponseDTO: CampaignResponseDTO) {
                        val campaigns = campaignResponseDTO.campaigns
                        if (campaigns == null || campaigns.isEmpty()) {
                            Timber.e("The campaigns list is empty")
                            view!!.showCampaigns(null)
                            return
                        }
                        val dateFormat = getIso8601DateFormatShort()
                        Collections.sort(campaigns) { campaign: Campaign, t1: Campaign ->
                            val date1: Date
                            val date2: Date
                            try {
                                date1 = dateFormat.parse(campaign.startDate)
                                date2 = dateFormat.parse(t1.startDate)
                            } catch (e: ParseException) {
                                Timber.e(e)
                                return@sort -1
                            }
                            date1.compareTo(date2)
                        }
                        var campaignEndDate: Date
                        var campaignStartDate: Date
                        val currentDate = Date()
                        try {
                            for (aCampaign in campaigns) {
                                campaignEndDate = dateFormat.parse(aCampaign.endDate)
                                campaignStartDate = dateFormat.parse(aCampaign.startDate)
                                if (
                                    campaignEndDate.compareTo(currentDate) >= 0 &&
                                    campaignStartDate.compareTo(currentDate) <= 0
                                ) {
                                    campaign = aCampaign
                                    break
                                }
                            }
                        } catch (e: ParseException) {
                            Timber.e(e)
                        }
                        view!!.showCampaigns(campaign)
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e, "could not fetch campaigns")
                    }
                })
        }
    }
}
