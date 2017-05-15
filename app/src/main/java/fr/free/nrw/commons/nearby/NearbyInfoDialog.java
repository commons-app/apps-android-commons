package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.ui.widget.OverlayDialog;
import fr.free.nrw.commons.utils.DialogUtil;

public class NearbyInfoDialog extends OverlayDialog {

    private final static String ARG_TITLE = "placeTitle";
    private final static String ARG_DESC = "placeDesc";
    private final static String ARG_LATITUDE = "latitude";
    private final static String ARG_LONGITUDE = "longitude";
    private final static String ARG_ARTICLE_LINK = "articleLink";
    private final static String ARG_WIKI_DATA_LINK = "wikiDataLink";

    @BindView(R.id.link_preview_title)
    TextView placeTitle;
    @BindView(R.id.link_preview_extract)
    TextView placeDescription;

    @BindView(R.id.link_preview_go_button)
    TextView goToButton;

    private Unbinder unbinder;

    private LatLng location;
    private Uri articleLink;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_nearby_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        initUi();
        return view;
    }

    private void initUi() {
        Bundle bundle = getArguments();
        placeTitle.setText(bundle.getString(ARG_TITLE));
        placeDescription.setText(bundle.getString(ARG_DESC));
        location = new LatLng(bundle.getDouble(ARG_LATITUDE), bundle.getDouble(ARG_LONGITUDE));
        getArticleLink(bundle);
    }

    private void getArticleLink(Bundle bundle) {
        String articleLink = bundle.getString(ARG_ARTICLE_LINK);
        articleLink = articleLink.replace("<", "").replace(">", "");

        if (Utils.isNullOrWhiteSpace(articleLink) || articleLink == "\n") {
            articleLink = bundle.getString(ARG_WIKI_DATA_LINK).replace("<", "").replace(">", "");
        }

        if (!Utils.isNullOrWhiteSpace(articleLink) && articleLink != "\n") {
            this.articleLink = Uri.parse(articleLink);
        } else {
            goToButton.setVisibility(View.GONE);
        }
    }

    public static void showYourself(FragmentActivity fragmentActivity, Place place) {
        NearbyInfoDialog mDialog = new NearbyInfoDialog();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TITLE, place.name);
        bundle.putString(ARG_DESC, place.description);
        bundle.putDouble(ARG_LATITUDE, place.location.latitude);
        bundle.putDouble(ARG_LONGITUDE, place.location.longitude);
        bundle.putString(ARG_ARTICLE_LINK, place.siteLink.toString());
        bundle.putString(ARG_WIKI_DATA_LINK, place.wikiDataLink.toString());
        mDialog.setArguments(bundle);
        DialogUtil.showSafely(fragmentActivity, mDialog);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.link_preview_directions_button)
    void onDirectionsClick() {
        //Open map app at given position
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + location.latitude + "," + location.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    @OnClick(R.id.link_preview_go_button)
    void onReadArticleClick() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, articleLink);
        startActivity(browserIntent);
    }

    @OnClick(R.id.emptyLayout)
    void onCloseClicked() {
        dismissAllowingStateLoss();
    }
}
