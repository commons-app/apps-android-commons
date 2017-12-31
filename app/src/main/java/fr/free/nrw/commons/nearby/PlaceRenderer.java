package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.support.transition.TransitionManager;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

class PlaceRenderer extends Renderer<Place> {

    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.buttonLayout) LinearLayout buttonLayout;
    @BindView(R.id.cameraButton) LinearLayout cameraButton;
    @BindView(R.id.galeryButton) LinearLayout galeryButton;
    @BindView(R.id.directionsButton) LinearLayout directionsButton;
    @BindView(R.id.iconOverflow) LinearLayout iconOverflow;
    @BindView(R.id.cameraButtonText) TextView cameraButtonText;
    @BindView(R.id.galeryButtonText) TextView galeryButtonText;
    @BindView(R.id.directionsButtonText) TextView directionsButtonText;
    @BindView(R.id.iconOverflowText) TextView iconOverflowText;

    private View view;
    private static ArrayList<LinearLayout> openedItems;
    private Place place;


    PlaceRenderer(){
        openedItems = new ArrayList<>();
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        view = layoutInflater.inflate(R.layout.item_place, viewGroup, false);
        return view;
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {

        final View.OnClickListener listener = view12 -> {
            Log.d("Renderer", "clicked");
            TransitionManager.beginDelayedTransition(buttonLayout);

            if(buttonLayout.isShown()){
                closeLayout(buttonLayout);
            }else {
                openLayout(buttonLayout);
            }

        };
        view.setOnClickListener(listener);
        view.requestFocus();
        view.setOnFocusChangeListener((view1, hasFocus) -> {
            if (!hasFocus && buttonLayout.isShown()) {
                closeLayout(buttonLayout);
            } else if (hasFocus && !buttonLayout.isShown()) {
                listener.onClick(view1);
            }
        });

    }

    private void closeLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.GONE);
    }

    private void openLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void render() {
        place = getContent();
        tvName.setText(place.name);
        String descriptionText = place.getLabel().getText();
        if (descriptionText.equals("?")) {
            descriptionText = getContext().getString(R.string.no_description_found);
        }
        tvDesc.setText(descriptionText);
        distance.setText(place.distance);
        icon.setImageResource(place.getLabel().getIcon());

        directionsButton.setOnClickListener(view -> {
            //Open map app at given position
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, place.location.getGmmIntentUri());
            if (mapIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                view.getContext().startActivity(mapIntent);
            }
        });

        //TODO: Insert onClickListeners for galeryButton and cameraButton

        iconOverflow.setVisibility(showMenu() ? View.VISIBLE : View.GONE);
        iconOverflow.setOnClickListener(v -> popupMenuListener());
    }

    private void popupMenuListener() {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), iconOverflow);
        popupMenu.inflate(R.menu.nearby_info_dialog_options);

        MenuItem commonsArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_commons_article);
        MenuItem wikiDataArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_wikidata_article);
        MenuItem wikipediaArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_wikipedia_article);

        commonsArticle.setEnabled(place.hasCommonsLink());
        wikiDataArticle.setEnabled(place.hasWikidataLink());
        wikipediaArticle.setEnabled(place.hasWikipediaLink());

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.nearby_info_menu_commons_article:
                    openWebView(place.siteLinks.getCommonsLink());
                    return true;
                case R.id.nearby_info_menu_wikidata_article:
                    openWebView(place.siteLinks.getWikidataLink());
                    return true;
                case R.id.nearby_info_menu_wikipedia_article:
                    openWebView(place.siteLinks.getWikipediaLink());
                    return true;
                default:
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void openWebView(Uri link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, link);
        view.getContext().startActivity(browserIntent);
    }

    private boolean showMenu() {
        return place.hasCommonsLink() || place.hasWikidataLink();
    }

}