package fr.free.nrw.commons.campaigns;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    Campaign campaign = null;
    private ViewHolder viewHolder;

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

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
        if (campaign != null) {
            this.setVisibility(View.VISIBLE);
            viewHolder.init();
        } else {
            this.setVisibility(View.GONE);
        }
    }

    @Override public boolean onSwipe(View view) {
        view.setVisibility(View.GONE);
        ((MainActivity) getContext()).defaultKvStore
                .putBoolean("displayCampaignsCardView", false);
        ViewUtil.showLongToast(getContext(),
            getResources().getString(R.string.nearby_campaign_dismiss_message));
        return true;
    }

    private void init() {
        View rootView = inflate(getContext(), R.layout.layout_campagin, this);
        viewHolder = new ViewHolder(rootView);
        setOnClickListener(view -> {
            if (campaign != null) {
                Utils.handleWebUrl(getContext(), Uri.parse(campaign.getLink()));
            }
        });
    }

    public class ViewHolder {

        @BindView(R.id.tv_title) TextView tvTitle;
        @BindView(R.id.tv_description) TextView tvDescription;
        @BindView(R.id.tv_dates) TextView tvDates;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }

        public void init() {
            if (campaign != null) {
                tvTitle.setText(campaign.getTitle());
                tvDescription.setText(campaign.getDescription());
                try {
                    Date startDate = CommonsDateUtil.getIso8601DateFormatShort().parse(campaign.getStartDate());
                    Date endDate = CommonsDateUtil.getIso8601DateFormatShort().parse(campaign.getEndDate());
                    tvDates.setText(String.format("%1s - %2s", DateUtil.getExtraShortDateString(startDate),
                            DateUtil.getExtraShortDateString(endDate)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
