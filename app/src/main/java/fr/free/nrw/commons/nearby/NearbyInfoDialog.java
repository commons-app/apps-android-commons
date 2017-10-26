package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.ui.widget.OverlayDialog;
import fr.free.nrw.commons.utils.DialogUtil;

public class NearbyInfoDialog extends OverlayDialog {

    private final static String ARG_TITLE = "placeTitle";
    private final static String ARG_DESC = "placeDesc";
    private final static String ARG_LATITUDE = "latitude";
    private final static String ARG_LONGITUDE = "longitude";
    private final static String ARG_SITE_LINK = "sitelink";

    @BindView(R.id.link_preview_title) TextView placeTitle;
    @BindView(R.id.link_preview_extract) TextView placeDescription;
    @BindView(R.id.link_preview_go_button) TextView goToButton;
    @BindView(R.id.link_preview_overflow_button) ImageView overflowButton;

    private Unbinder unbinder;
    private LatLng location;
    private Sitelinks sitelinks;

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
        location = new LatLng(bundle.getDouble(ARG_LATITUDE), bundle.getDouble(ARG_LONGITUDE), 0);
        getArticleLink(bundle);
    }

    private void getArticleLink(Bundle bundle) {
        this.sitelinks = bundle.getParcelable(ARG_SITE_LINK);

        if (sitelinks == null || Uri.EMPTY.equals(sitelinks.getWikipediaLink())) {
            goToButton.setVisibility(View.GONE);
        }

        overflowButton.setVisibility(showMenu() ? View.VISIBLE : View.GONE);

        overflowButton.setOnClickListener(v -> popupMenuListener());
    }

    private void popupMenuListener() {
        PopupMenu popupMenu = new PopupMenu(getActivity(), overflowButton);
        popupMenu.inflate(R.menu.nearby_info_dialog_options);

        MenuItem commonsArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_commons_article);
        MenuItem wikiDataArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_wikidata_article);

        commonsArticle.setEnabled(!sitelinks.getCommonsLink().equals(Uri.EMPTY));
        wikiDataArticle.setEnabled(!sitelinks.getWikidataLink().equals(Uri.EMPTY));

        popupMenu.setOnMenuItemClickListener(menuListener);
        popupMenu.show();
    }

    private boolean showMenu() {
        return !sitelinks.getCommonsLink().equals(Uri.EMPTY)
                || !sitelinks.getWikidataLink().equals(Uri.EMPTY);
    }

    private final PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu
            .OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nearby_info_menu_commons_article:
                    openWebView(sitelinks.getCommonsLink());
                    return true;
                case R.id.nearby_info_menu_wikidata_article:
                    openWebView(sitelinks.getWikidataLink());
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    public static void showYourself(FragmentActivity fragmentActivity, Place place) {
        NearbyInfoDialog mDialog = new NearbyInfoDialog();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TITLE, place.name);
        bundle.putString(ARG_DESC, place.getDescription().getText());
        bundle.putDouble(ARG_LATITUDE, place.location.getLatitude());
        bundle.putDouble(ARG_LONGITUDE, place.location.getLongitude());
        bundle.putParcelable(ARG_SITE_LINK, place.siteLinks);
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
        Uri gmmIntentUri = Uri.parse(
                "geo:0,0?q=" + location.getLatitude() + "," + location.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    @OnClick(R.id.link_preview_go_button)
    void onReadArticleClick() {
        openWebView(sitelinks.getWikipediaLink());
    }

    private void openWebView(Uri link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, link);
        startActivity(browserIntent);
    }

    @OnClick(R.id.emptyLayout)
    void onCloseClicked() {
        dismissAllowingStateLoss();
    }
}
