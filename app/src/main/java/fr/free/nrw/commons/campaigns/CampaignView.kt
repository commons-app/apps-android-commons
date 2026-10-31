package fr.free.nrw.commons.campaigns

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import fr.free.nrw.commons.R
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.LayoutCampaginBinding
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.CommonsDateUtil.getIso8601DateFormatShort
import fr.free.nrw.commons.utils.DateUtil.getExtraShortDateString
import fr.free.nrw.commons.utils.SwipableCardView
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.handleWebUrl
import timber.log.Timber
import java.text.ParseException
import java.util.Locale

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
        context,
        attrs,
        defStyleAttr,
    ) {
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
            resources.getString(R.string.nearby_campaign_dismiss_message),
        )
        return true
    }

    private fun init() {
        binding =
            LayoutCampaginBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )
        viewHolder = ViewHolder()
        setOnClickListener {
            val currentCampaign = campaign ?: return@setOnClickListener

            if (currentCampaign.isWLMCampaign) {
                (context as MainActivity).showNearby()
            } else {
                val link = currentCampaign.link ?: return@setOnClickListener
                handleWebUrl(context, link.toUri())
            }
        }
    }

    inner class ViewHolder {
        fun init() {
            val campaignToDisplay = campaign ?: return
            binding!!.ivCampaign.setImageDrawable(
                ContextCompat.getDrawable(binding!!.root.context, R.drawable.ic_campaign),
            )
            binding!!.tvTitle.text = campaignToDisplay.title
            binding!!.tvDescription.text = campaignToDisplay.description
            try {
                if (campaignToDisplay.isWLMCampaign) {
                    binding!!.tvDates.text =
                        String.format(
                            Locale.getDefault(),
                            "%1s - %2s",
                            campaignToDisplay.startDate,
                            campaignToDisplay.endDate,
                        )
                } else {
                    val startDate =
                        campaignToDisplay.startDate?.let { date ->
                            getIso8601DateFormatShort().parse(date)
                        }
                    val endDate =
                        campaignToDisplay.endDate?.let { date ->
                            getIso8601DateFormatShort().parse(date)
                        }
                    if (startDate == null || endDate == null) {
                        binding!!.tvDates.text = ""
                        return
                    }
                    binding!!.tvDates.text =
                        String.format(
                            Locale.getDefault(),
                            "%1s - %2s",
                            getExtraShortDateString(startDate),
                            getExtraShortDateString(endDate),
                        )
                }
            } catch (e: ParseException) {
                Timber.e(e)
            }
        }
    }

    companion object {
        const val CAMPAIGNS_DEFAULT_PREFERENCE: String = "displayCampaignsCardView"
        const val WLM_CARD_PREFERENCE: String = "displayWLMCardView"
    }
}
