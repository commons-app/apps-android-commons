package fr.free.nrw.commons.contributions

import android.view.View
import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.campaigns.ICampaignsView
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.utils.WLM_END_DATE
import fr.free.nrw.commons.utils.WLM_START_DATE
import fr.free.nrw.commons.utils.isMonumentsEnabled

/** Owns campaign selection and display on the contributions screen. */
class ContributionsCampaignController(
    private val fragment: ContributionsFragment,
) : ICampaignsView,
    FragmentManager.OnBackStackChangedListener {
    private var wlmCampaign: Campaign? = null

    /**
     * Initialise the campaign object for WML
     */
    fun initWLMCampaign() {
        wlmCampaign =
            Campaign(
                fragment.getString(R.string.wlm_campaign_title),
                fragment.getString(R.string.wlm_campaign_description),
                WLM_START_DATE,
                WLM_END_DATE,
                NearbyParentFragment.WLM_URL,
                true,
            )
    }

    /**
     * As the home screen has limited space, we have choosen to show either campaigns or WLM card.
     * The WLM Card gets the priority over monuments, so if the WLM is going on we show that instead
     * of campaigns on the campaigns card
     */
    fun fetchCampaigns() {
        if (isMonumentsEnabled) {
            if (fragment.binding != null) {
                fragment.binding!!.campaignsView.setCampaign(wlmCampaign)
                fragment.binding!!.campaignsView.visibility = View.VISIBLE
            }
        } else if (fragment.store!!.getBoolean(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE, true)) {
            fragment.presenter!!.getCampaigns()
        } else {
            if (fragment.binding != null) {
                fragment.binding!!.campaignsView.visibility = View.GONE
            }
        }
    }

    /** Displays a fetched campaign unless this screen belongs to a user profile. */
    override fun showCampaigns(campaign: Campaign?) {
        if (campaign != null && !fragment.isUserProfile) {
            if (fragment.binding != null) {
                fragment.binding!!.campaignsView.setCampaign(campaign)
            }
        }
    }

    /** Refreshes campaign content after a child fragment transaction. */
    override fun onBackStackChanged() {
        fetchCampaigns()
    }
}
