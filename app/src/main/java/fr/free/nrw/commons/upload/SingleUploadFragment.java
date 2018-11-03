package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTouch;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static android.view.MotionEvent.ACTION_UP;

public class SingleUploadFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.titleEdit) EditText titleEdit;
    @BindView(R.id.rv_descriptions) RecyclerView rvDescriptions;
    @BindView(R.id.titleDescButton) Button titleDescButton;
    @BindView(R.id.share_license_summary) TextView licenseSummaryView;
    @BindView(R.id.licenseSpinner) Spinner licenseSpinner;


    @Inject @Named("default_preferences") SharedPreferences prefs;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;

    private String license;
    private OnUploadActionInitiated uploadActionInitiatedHandler;
    private TitleTextWatcher textWatcher = new TitleTextWatcher();
    private DescriptionsAdapter descriptionsAdapter;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_share, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //What happens when the 'submit' icon is tapped
            case R.id.menu_upload_single:

                if (titleEdit.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), R.string.add_title_toast, Toast.LENGTH_LONG).show();
                    return false;
                }

                String title = titleEdit.getText().toString();
                String descriptionsInVariousLanguages = getDescriptionsInAppropriateFormat();

                //Save the title/desc in short-lived cache so next time this fragment is loaded, we can access these
                prefs.edit()
                        .putString("Title", title)
                        .putString("Desc", new Gson().toJson(descriptionsAdapter
                                .getDescriptions()))//Description, now is not just a string, its a list of description objects
                        .apply();

                uploadActionInitiatedHandler
                        .uploadActionInitiated(title, descriptionsInVariousLanguages);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getDescriptionsInAppropriateFormat() {
        List<Description> descriptions = descriptionsAdapter.getDescriptions();
        StringBuilder descriptionsInAppropriateFormat = new StringBuilder();
        for (Description description : descriptions) {
            String individualDescription = String.format("{{%s|1=%s}}", description.getLanguageId(),
                    description.getDescriptionText());
            descriptionsInAppropriateFormat.append(individualDescription);
        }
        return descriptionsInAppropriateFormat.toString();

    }

    private List<Description> getDescriptions() {
        List<Description> descriptions = descriptionsAdapter.getDescriptions();
        return descriptions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_upload, container, false);
        ButterKnife.bind(this, rootView);

        initRecyclerView();

        Intent activityIntent = getActivity().getIntent();
        if (activityIntent.hasExtra("title")) {
            titleEdit.setText(activityIntent.getStringExtra("title"));
        }
        if (activityIntent.hasExtra("description") && descriptionsAdapter.getDescriptions() != null
                && descriptionsAdapter.getDescriptions().size() > 0) {
            descriptionsAdapter.getDescriptions().get(0)
                    .setDescriptionText(activityIntent.getStringExtra("description"));
            descriptionsAdapter.notifyItemChanged(0);
        }


        ArrayList<String> licenseItems = new ArrayList<>();
        licenseItems.add(getString(R.string.license_name_cc0));
        licenseItems.add(getString(R.string.license_name_cc_by));
        licenseItems.add(getString(R.string.license_name_cc_by_sa));
        licenseItems.add(getString(R.string.license_name_cc_by_four));
        licenseItems.add(getString(R.string.license_name_cc_by_sa_four));

        license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);

        // If this is a direct upload from Nearby, autofill title and desc fields with the Place's values
        boolean isNearbyUpload = ((ShareActivity) getActivity()).isNearbyUpload();

        if (isNearbyUpload) {
            String imageTitle = directPrefs.getString("Title", "");
            String imageDesc = directPrefs.getString("Desc", "");
            String imageCats = directPrefs.getString("Category", "");
            Timber.d("Image title: " + imageTitle + ", image desc: " + imageDesc + ", image categories: " + imageCats);
            titleEdit.setText(imageTitle);
            if (descriptionsAdapter.getDescriptions() != null
                    && descriptionsAdapter.getDescriptions().size() > 0) {
                descriptionsAdapter.getDescriptions().get(0).setDescriptionText(imageDesc);
                descriptionsAdapter.notifyItemChanged(0);
            }
        }

        // check if this is the first time we have uploaded
        if (prefs.getString("Title", "").trim().length() == 0
                && prefs.getString("Desc", "").trim().length() == 0) {
            titleDescButton.setVisibility(View.GONE);
        }

        Timber.d(license);

        ArrayAdapter<String> adapter;
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("theme", false)) {
            // dark theme
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, licenseItems);
        } else {
            // light theme
            adapter = new ArrayAdapter<>(getActivity(), R.layout.light_simple_spinner_dropdown_item, licenseItems);
        }

        licenseSpinner.setAdapter(adapter);

        int position = licenseItems.indexOf(getString(Utils.licenseNameFor(license)));

        // Check position is valid
        if (position < 0) {
            Timber.d("Invalid position: %d. Using default license", position);
            position = 4;
        }

        Timber.d("Position: %d %s", position, getString(Utils.licenseNameFor(license)));
        licenseSpinner.setSelection(position);

        titleEdit.addTextChangedListener(textWatcher);

        titleEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ViewUtil.hideKeyboard(v);
            }
        });

        setLicenseSummary(license);

        return rootView;
    }

    private void initRecyclerView() {
        descriptionsAdapter = new DescriptionsAdapter();
        descriptionsAdapter.setCallback(this::showInfoAlert);
        descriptionsAdapter.setLanguages(getLocaleSupportedByDevice());
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDescriptions.setAdapter(descriptionsAdapter);
    }

    private List<Language> getLocaleSupportedByDevice() {
        List<Language> languages = new ArrayList<>();
        Locale[] localesArray = Locale.getAvailableLocales();
        List<Locale> locales = Arrays.asList(localesArray);
        for (Locale locale : locales) {
            languages.add(new Language(locale));
        }
        return languages;
    }

    @Override
    public void onDestroyView() {
        titleEdit.removeTextChangedListener(textWatcher);
        super.onDestroyView();
    }

    @OnItemSelected(R.id.licenseSpinner)
    void onLicenseSelected(AdapterView<?> parent, View view, int position, long id) {
        String licenseName = parent.getItemAtPosition(position).toString();

        // Set selected color to white because it should be readable on random images.
        TextView selectedText = (TextView) licenseSpinner.getChildAt(0);
        if (selectedText != null) {
            selectedText.setTextColor(Color.WHITE);
            selectedText.setBackgroundColor(Color.TRANSPARENT);
        }

        String license;
        if (getString(R.string.license_name_cc0).equals(licenseName)) {
            license = Prefs.Licenses.CC0;
        } else if (getString(R.string.license_name_cc_by).equals(licenseName)) {
            license = Prefs.Licenses.CC_BY_3;
        } else if (getString(R.string.license_name_cc_by_sa).equals(licenseName)) {
            license = Prefs.Licenses.CC_BY_SA_3;
        } else if (getString(R.string.license_name_cc_by_four).equals(licenseName)) {
            license = Prefs.Licenses.CC_BY_4;
        } else if (getString(R.string.license_name_cc_by_sa_four).equals(licenseName)) {
            license = Prefs.Licenses.CC_BY_SA_4;
        } else {
            throw new IllegalStateException("Unknown licenseName: " + licenseName);
        }

        setLicenseSummary(license);
        prefs.edit()
                .putString(Prefs.DEFAULT_LICENSE, license)
                .apply();
    }


    @OnClick(R.id.titleDescButton)
    void setTitleDescButton() {
        //Retrieve last title and desc entered
        String title = prefs.getString("Title", "");
        String descriptionJson = prefs.getString("Desc", "");
        Timber.d("Title: %s, Desc: %s", title, descriptionJson);

        titleEdit.setText(title);
        Type typeOfDest = new TypeToken<List<Description>>() {
        }.getType();

        List<Description> descriptions = new Gson().fromJson(descriptionJson, typeOfDest);
        descriptionsAdapter.setDescriptions(descriptions);

    }

    /**
     * Copied from https://stackoverflow.com/a/26269435/8065933
     */
    @OnTouch(R.id.titleEdit)
    boolean titleInfo(View view, MotionEvent motionEvent) {
        final int value;
        if (ViewCompat.getLayoutDirection(getView()) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            value = titleEdit.getRight() - titleEdit.getCompoundDrawables()[2].getBounds().width();
            if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() >= value) {
                showInfoAlert(R.string.media_detail_title, R.string.title_info);
                return true;
            }
        }
        else {
            value = titleEdit.getLeft() + titleEdit.getCompoundDrawables()[0].getBounds().width();
            if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() <= value) {
                showInfoAlert(R.string.media_detail_title, R.string.title_info);
                return true;
            }
        }
        return false;
    }

    @SuppressLint("StringFormatInvalid")
    private void setLicenseSummary(String license) {
        String licenseHyperLink = "<a href='" + licenseUrlFor(license)+"'>"+ getString(Utils.licenseNameFor(license)) + "</a><br>";
        licenseSummaryView.setMovementMethod(LinkMovementMethod.getInstance());
        licenseSummaryView.setText(Html.fromHtml(getString(R.string.share_license_summary, licenseHyperLink)));
 }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        uploadActionInitiatedHandler = (OnUploadActionInitiated) getActivity();
    }

    @Override
    public void onStop() {
        super.onStop();

        // FIXME: Stops the keyboard from being shown 'stale' while moving out of this fragment into the next
        View target = getActivity().getCurrentFocus();
        ViewUtil.hideKeyboard(target);
    }

    @NonNull
    private String licenseUrlFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return "https://creativecommons.org/licenses/by/3.0/";
            case Prefs.Licenses.CC_BY_4:
                return "https://creativecommons.org/licenses/by/4.0/";
            case Prefs.Licenses.CC_BY_SA_3:
                return "https://creativecommons.org/licenses/by-sa/3.0/";
            case Prefs.Licenses.CC_BY_SA_4:
                return "https://creativecommons.org/licenses/by-sa/4.0/";
            case Prefs.Licenses.CC0:
                return "https://creativecommons.org/publicdomain/zero/1.0/";
        }
        throw new RuntimeException("Unrecognized license value: " + license);
    }

    public interface OnUploadActionInitiated {

        void uploadActionInitiated(String title, String description);
    }

    private class TitleTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        }
    }


    private void showInfoAlert (int titleStringID, int messageStringID){
        new AlertDialog.Builder(getContext())
                .setTitle(titleStringID)
                .setMessage(messageStringID)
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    @OnClick(R.id.ll_add_description)
    public void onLLAddDescriptionClicked() {
        descriptionsAdapter.addDescription(new Description());
        rvDescriptions.scrollToPosition(descriptionsAdapter.getItemCount() - 1);
    }
}
