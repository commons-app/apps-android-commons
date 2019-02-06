package fr.free.nrw.commons.campaigns;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.MvpView;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The presenter for the campaigns view, fetches the campaigns from the api and informs the view on
 * success and error
 */
@Singleton
public class CampaignsPresenter implements BasePresenter {
    private final OkHttpJsonApiClient okHttpJsonApiClient;

    private final String TAG = "#CampaignsPresenter#";
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
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        if (campaigns == null || campaigns.isEmpty()) {
                            Log.e(TAG, "The campaigns list is empty");
                            view.showCampaigns(null);
                        }
                        Collections.sort(campaigns, (campaign, t1) -> {
                            Date date1, date2;
                            try {
                                date1 = dateFormat.parse(campaign.getStartDate());
                                date2 = dateFormat.parse(t1.getStartDate());
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
                                campaignEndDate = dateFormat.parse(aCampaign.getEndDate());
                                campaignStartDate =
                                    dateFormat.parse(aCampaign.getStartDate());
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
                        Log.e(TAG, "could not fetch campaigns: " + e.getMessage());
                    }
                });
        }
    }
}
