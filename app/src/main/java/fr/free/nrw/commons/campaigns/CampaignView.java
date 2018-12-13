package fr.free.nrw.commons.campaigns;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.SwipableCardView;
import fr.free.nrw.commons.utils.ViewUtil;

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
        ((MainActivity) getContext()).prefs.edit().putBoolean("displayCampaignsCardView", false).apply();
        ViewUtil.showLongToast(getContext(),
            getResources().getString(R.string.nearby_campaign_dismiss_message));
        return true;
    }

    private void init() {
        View rootView = inflate(getContext(), R.layout.layout_campagin, this);
        viewHolder = new ViewHolder(rootView);
        setOnClickListener(view -> {
                if(campaign!=null){
                    showCampaignInBrowser(campaign.getLink());
                }
        });
    }

    private void showCampaignInBrowser(String link) {
        Intent view = new Intent();
        view.setAction(Intent.ACTION_VIEW);
        view.setData(Uri.parse(link));
        getContext().startActivity(view);
    }

    public class ViewHolder {

        @BindView(R.id.tv_title) TextView tvTitle;
        @BindView(R.id.tv_description) TextView tvDescription;
        @BindView(R.id.tv_start_date) TextView tvStartDate;
        @BindView(R.id.tv_end_date) TextView tvEndDate;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }

        public void init() {
            if (campaign != null) {
                tvTitle.setText(campaign.getTitle());
                tvDescription.setText(campaign.getDescription());
                tvStartDate.setText(campaign.getStartDate());
                tvEndDate.setText(campaign.getEndDate());
            }
        }
    }
}
