package fr.free.nrw.commons.campaigns;

import fr.free.nrw.commons.MvpView;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

public interface ICampaignsView extends MvpView {
    void showMessage(String message);

    MediaWikiApi getMediaWikiApi();

    void showCampaigns(Campaign campaign);
}
