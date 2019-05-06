package fr.free.nrw.commons.campaigns;

import android.annotation.SuppressLint;

import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.MvpView;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * The presenter for the campaigns view, fetches the campaigns from the api and informs the view on
 * success and error
 */
@Singleton
public class CampaignsPresenter implements BasePresenter {
    private final OkHttpJsonApiClient okHttpJsonApiClient;

    private ICampaignsView view;
    private Disposable disposable;
    private Campaign campaign;

    @Inject
    public CampaignsPresenter(OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }

    @Override public void onAttachView(MvpView view) {
        this.view = (ICampaignsView) view;
    }

    @Override public void onDetachView() {
        this.view = null;
        if (disposable != null) {
            disposable.dispose();
        }
    }

    /**
     * make the api call to fetch the campaigns
     */
    @SuppressLint("CheckResult")
    public void getCampaigns() {
        if (view != null && okHttpJsonApiClient != null) {
            //If we already have a campaign, lets not make another call
            if (this.campaign != null) {
                view.showCampaigns(campaign);
                return;
            }
            Single<CampaignResponseDTO> campaigns = okHttpJsonApiClient.getCampaigns();
            campaigns.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(new SingleObserver<CampaignResponseDTO>() {

                    @Override public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override public void onSuccess(CampaignResponseDTO campaignResponseDTO) {
                        List<Campaign> campaigns = campaignResponseDTO.getCampaigns();
                        if (campaigns == null || campaigns.isEmpty()) {
                            Timber.e("The campaigns list is empty");
                            view.showCampaigns(null);
                        }
                        Collections.sort(campaigns, (campaign, t1) -> {
                            Date date1, date2;
                            try {

                                date1 = CommonsDateUtil.getIso8601DateFormatShort().parse(campaign.getStartDate());
                                date2 = CommonsDateUtil.getIso8601DateFormatShort().parse(t1.getStartDate());
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            return date1.compareTo(date2);
                        });
                        Date campaignEndDate, campaignStartDate;
                        Date currentDate = new Date();
                        try {
                            for (Campaign aCampaign : campaigns) {
                                campaignEndDate = CommonsDateUtil.getIso8601DateFormatShort().parse(aCampaign.getEndDate());
                                campaignStartDate = CommonsDateUtil.getIso8601DateFormatShort().parse(aCampaign.getStartDate());
                                if (campaignEndDate.compareTo(currentDate) >= 0
                                    && campaignStartDate.compareTo(currentDate) <= 0) {
                                    campaign = aCampaign;
                                    break;
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        view.showCampaigns(campaign);
                    }

                    @Override public void onError(Throwable e) {
                        Timber.e(e, "could not fetch campaigns");
                    }
                });
        }
    }
}
