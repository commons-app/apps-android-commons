package fr.free.nrw.commons.campaigns;

import fr.free.nrw.commons.MvpView;

/**
 * Interface which defines the view contracts of the campaign view
 */
public interface ICampaignsView extends MvpView {

  void showCampaigns(Campaign campaign);
}
