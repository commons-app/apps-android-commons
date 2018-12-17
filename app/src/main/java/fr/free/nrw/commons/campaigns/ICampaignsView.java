package fr.free.nrw.commons.campaigns;

import fr.free.nrw.commons.MvpView;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

/**
 * Interface which defines the view contracts of the campaign view
 */
public interface ICampaignsView extends MvpView {
    MediaWikiApi getMediaWikiApi();

    void showCampaigns(Campaign campaign);
}
