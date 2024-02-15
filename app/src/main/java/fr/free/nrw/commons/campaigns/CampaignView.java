package fr.free.nrw.commons.campaigns;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.free.nrw.commons.campaigns.models.Campaign;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DateUtil;

import java.text.ParseException;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
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
        final View rootView = inflate(getContext(), R.layout.layout_campagin, this);
        viewHolder = new ViewHolder(rootView);
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

        @BindView(R.id.iv_campaign)
        ImageView ivCampaign;
        @BindView(R.id.tv_title) TextView tvTitle;
        @BindView(R.id.tv_description) TextView tvDescription;
        @BindView(R.id.tv_dates) TextView tvDates;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }

        public void init() {
            if (campaign != null) {
                ivCampaign.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_campaign));

                tvTitle.setText(campaign.getTitle());
                tvDescription.setText(campaign.getDescription());
                try {
                    if (campaign.isWLMCampaign()) {
                        tvDates.setText(
                            String.format("%1s - %2s", campaign.getStartDate(),
                                campaign.getEndDate()));
                    } else {
                        final Date startDate = CommonsDateUtil.getIso8601DateFormatShort()
                            .parse(campaign.getStartDate());
                        final Date endDate = CommonsDateUtil.getIso8601DateFormatShort()
                            .parse(campaign.getEndDate());
                        tvDates.setText(String.format("%1s - %2s", DateUtil.getExtraShortDateString(startDate),
                            DateUtil.getExtraShortDateString(endDate)));
                    }
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
