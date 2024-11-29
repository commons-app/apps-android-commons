package fr.free.nrw.commons.campaigns

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.LayoutCampaginBinding
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.CommonsDateUtil.getIso8601DateFormatShort
import fr.free.nrw.commons.utils.DateUtil.getExtraShortDateString
import fr.free.nrw.commons.utils.SwipableCardView
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import timber.log.Timber
import java.text.ParseException

/**
 * A view which represents a single campaign
 */
class CampaignView : SwipableCardView {
    private var campaign: Campaign? = null
    private var binding: LayoutCampaginBinding? = null
    private var viewHolder: ViewHolder? = null
    private var campaignPreference = CAMPAIGNS_DEFAULT_PREFERENCE

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr) {
        init()
    }

    fun setCampaign(campaign: Campaign?) {
        this.campaign = campaign
        if (campaign != null) {
            if (campaign.isWLMCampaign) {
                campaignPreference = WLM_CARD_PREFERENCE
            }
            visibility = VISIBLE
            viewHolder!!.init()
        } else {
            visibility = GONE
        }
    }

    override fun onSwipe(view: View): Boolean {
        view.visibility = GONE
        (context as BaseActivity).defaultKvStore.putBoolean(CAMPAIGNS_DEFAULT_PREFERENCE, false)
        showLongToast(
            context,
            resources.getString(R.string.nearby_campaign_dismiss_message)
        )
        return true
    }

    private fun init() {
        binding = LayoutCampaginBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        viewHolder = ViewHolder()
        setOnClickListener {
            campaign?.let {
                if (it.isWLMCampaign) {
                    ((context) as MainActivity).showNearby()
                } else {
                    Utils.handleWebUrl(context, Uri.parse(it.link))
                }
            }
        }
    }

    inner class ViewHolder {
        fun init() {
            if (campaign != null) {
                binding!!.ivCampaign.setImageDrawable(
                    ContextCompat.getDrawable(binding!!.root.context, R.drawable.ic_campaign)
                )
                binding!!.tvTitle.text = campaign!!.title
                binding!!.tvDescription.text = campaign!!.description
                try {
                    if (campaign!!.isWLMCampaign) {
                        binding!!.tvDates.text = String.format(
                            "%1s - %2s", campaign!!.startDate,
                            campaign!!.endDate
                        )
                    } else {
                        val startDate = getIso8601DateFormatShort().parse(
                            campaign?.startDate
                        )
                        val endDate = getIso8601DateFormatShort().parse(
                            campaign?.endDate
                        )
                        binding!!.tvDates.text = String.format(
                            "%1s - %2s", getExtraShortDateString(
                                startDate!!
                            ), getExtraShortDateString(endDate!!)
                        )
                    }
                } catch (e: ParseException) {
                    Timber.e(e)
                }
            }
        }
    }

    companion object {
        const val CAMPAIGNS_DEFAULT_PREFERENCE: String = "displayCampaignsCardView"
        const val WLM_CARD_PREFERENCE: String = "displayWLMCardView"
    }
}
