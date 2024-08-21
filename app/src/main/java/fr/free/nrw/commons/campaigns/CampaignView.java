package fr.free.nrw.commons.campaigns;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.free.nrw.commons.campaigns.models.Campaign;
import fr.free.nrw.commons.databinding.LayoutCampaginBinding;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DateUtil;

import java.text.ParseException;
import java.util.Date;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import fr.free.nrw.commons.utils.SwipableCardView;
import fr.free.nrw.commons.utils.ViewUtil;

/**
 * A view which represents a single campaign
 */
public class CampaignView extends SwipableCardView {
    Campaign campaign;
    private LayoutCampaginBinding binding;
    private ViewHolder viewHolder;

    public static final String CAMPAIGNS_DEFAULT_PREFERENCE = "displayCampaignsCardView";
    public static final String WLM_CARD_PREFERENCE = "displayWLMCardView";

    private String campaignPreference = CAMPAIGNS_DEFAULT_PREFERENCE;

    public CampaignView(@NonNull Context context) {
        super(context);
        init();
    }

    public CampaignView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CampaignView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
        if (campaign != null) {
            if (campaign.isWLMCampaign()) {
                campaignPreference = WLM_CARD_PREFERENCE;
            }
            setVisibility(View.VISIBLE);
            viewHolder.init();
        } else {
            this.setVisibility(View.GONE);
        }
    }

    @Override public boolean onSwipe(final View view) {
        view.setVisibility(View.GONE);
        ((BaseActivity) getContext()).defaultKvStore
            .putBoolean(CAMPAIGNS_DEFAULT_PREFERENCE, false);
        ViewUtil.showLongToast(getContext(),
            getResources().getString(R.string.nearby_campaign_dismiss_message));
        return true;
    }

    private void init() {
        binding = LayoutCampaginBinding.inflate(LayoutInflater.from(getContext()), this, true);
        viewHolder = new ViewHolder();
        setOnClickListener(view -> {
            if (campaign != null) {
                if (campaign.isWLMCampaign()) {
                    ((MainActivity)(getContext())).showNearby();
                } else {
                    Utils.handleWebUrl(getContext(), Uri.parse(campaign.getLink()));
                }
            }
        });
    }

    public class ViewHolder {
        public void init() {
            if (campaign != null) {
                binding.ivCampaign.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_campaign));

                binding.tvTitle.setText(campaign.getTitle());
                binding.tvDescription.setText(campaign.getDescription());
                try {
                    if (campaign.isWLMCampaign()) {
                        binding.tvDates.setText(
                            String.format("%1s - %2s", campaign.getStartDate(),
                                campaign.getEndDate()));
                    } else {
                        final Date startDate = CommonsDateUtil.getIso8601DateFormatShort()
                            .parse(campaign.getStartDate());
                        final Date endDate = CommonsDateUtil.getIso8601DateFormatShort()
                            .parse(campaign.getEndDate());
                        binding.tvDates.setText(String.format("%1s - %2s", DateUtil.getExtraShortDateString(startDate),
                            DateUtil.getExtraShortDateString(endDate)));
                    }
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
