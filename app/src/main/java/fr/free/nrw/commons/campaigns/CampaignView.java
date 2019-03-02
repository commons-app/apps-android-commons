package fr.free.nrw.commons.campaigns;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.utils.SwipableCardView;
import fr.free.nrw.commons.utils.ViewUtil;

/**
 * A view which represents a single campaign
 */
public class CampaignView extends SwipableCardView {
    Campaign campaign = null;
    private ViewHolder viewHolder;
    private BasicKvStore defaultKvStore;

    public CampaignView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CampaignView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CampaignView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
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

    private void init(Context context) {
        View rootView = inflate(getContext(), R.layout.layout_campagin, this);
        defaultKvStore = new BasicKvStore(context, "default_preferences");
        viewHolder = new ViewHolder(rootView);
        setOnClickListener(view -> {
            if (campaign != null) {
                showCampaignInBrowser(campaign.getLink());
            }
        });
    }

    /**
     * open the url associated with the campaign in the system's default browser
     */
    private void showCampaignInBrowser(String link) {
        Intent view = new Intent();
        view.setAction(Intent.ACTION_VIEW);
        view.setData(Uri.parse(link));
        getContext().startActivity(view);
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
                SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd MMM");
                try {
                    Date startDate = inputDateFormat.parse(campaign.getStartDate());
                    Date endDate = inputDateFormat.parse(campaign.getEndDate());
                    tvDates.setText(String.format("%1s - %2s", outputDateFormat.format(startDate),
                        outputDateFormat.format(endDate)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
