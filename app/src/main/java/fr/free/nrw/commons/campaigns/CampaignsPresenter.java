package fr.free.nrw.commons.campaigns;

import android.util.Log;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.MvpView;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CampaignsPresenter implements BasePresenter {
    private final String TAG = "#CampaignsPresenter#";
    private ICampaignsView view;
    private MediaWikiApi mediaWikiApi;
    private Disposable disposable;

    @Override public void onAttachView(MvpView view) {
        this.view = (ICampaignsView) view;
        this.mediaWikiApi = ((ICampaignsView) view).getMediaWikiApi();
    }

    @Override public void onDetachView() {
        this.view = null;
        disposable.dispose();
    }

    public void getCampaigns() {
        if (view != null && mediaWikiApi != null) {
            Single<CampaignResponseDTO> campaigns = mediaWikiApi.getCampaigns();
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
                            Date date1, date2 = null;
                            try {
                                date1 = dateFormat.parse(campaign.getStartDate());
                                date2 = dateFormat.parse(t1.getStartDate());
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            return date1.compareTo(date2);
                        });
                        Date campaignEndDate = null;
                        try {
                            campaignEndDate = dateFormat.parse(campaigns.get(0).getEndDate());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (campaignEndDate == null) {
                            view.showCampaigns(null);
                        } else if (campaignEndDate.compareTo(new Date()) > 0) {
                            view.showCampaigns(campaigns.get(0));
                        } else {
                            Log.e(TAG, "The campaigns has already finished");
                            view.showCampaigns(null);
                        }
                    }

                    @Override public void onError(Throwable e) {
                        Log.e(TAG, "could not fetch campaigns: " + e.getMessage());
                    }
                });
        }
    }
}
