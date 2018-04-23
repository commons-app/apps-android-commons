package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

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
import timber.log.Timber;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class SingleUploadFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.titleEdit) EditText titleEdit;
    @BindView(R.id.descEdit) EditText descEdit;
    @BindView(R.id.titleDescButton) Button titleDescButton;
    @BindView(R.id.share_license_summary) TextView licenseSummaryView;
    @BindView(R.id.licenseSpinner) Spinner licenseSpinner;

    @Inject @Named("default_preferences") SharedPreferences prefs;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;

    private String license;
    private OnUploadActionInitiated uploadActionInitiatedHandler;
    private TitleTextWatcher textWatcher = new TitleTextWatcher();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_share, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //What happens when the 'submit' icon is tapped
            case R.id.menu_upload_single:

                if (titleEdit.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), R.string.add_title_toast, Toast.LENGTH_LONG).show();
                    return false;
                }

                String title = titleEdit.getText().toString();
                String desc = descEdit.getText().toString();

                //Save the title/desc in short-lived cache so next time this fragment is loaded, we can access these
                prefs.edit()
                        .putString("Title", title)
                        .putString("Desc", desc)
                        .apply();

                uploadActionInitiatedHandler.uploadActionInitiated(title, desc);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_upload, container, false);
        ButterKnife.bind(this, rootView);

        Intent activityIntent = getActivity().getIntent();
        if (activityIntent.hasExtra("title")) {
            titleEdit.setText(activityIntent.getStringExtra("title"));
        }
        if (activityIntent.hasExtra("description")) {
            descEdit.setText(activityIntent.getStringExtra("description"));
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
            descEdit.setText(imageDesc);
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
                hideKeyboard(v);
            }
        });

        descEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){
                hideKeyboard(v);
            }
        });

        setLicenseSummary(license);

        return rootView;
    }

    public void hideKeyboard(View view) {
        Log.i("hide", "hideKeyboard: ");
        InputMethodManager inputMethodManager =(InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
                .commit();
    }


    @OnClick(R.id.titleDescButton)
    void setTitleDescButton() {
        //Retrieve last title and desc entered
        String title = prefs.getString("Title", "");
        String desc = prefs.getString("Desc", "");
        Timber.d("Title: %s, Desc: %s", title, desc);

        titleEdit.setText(title);
        descEdit.setText(desc);
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

    @OnTouch(R.id.descEdit)
    boolean descriptionInfo(View view, MotionEvent motionEvent) {
        final int value;
        if (ViewCompat.getLayoutDirection(getView()) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            value = descEdit.getRight() - descEdit.getCompoundDrawables()[2].getBounds().width();
            if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() >= value) {
                showInfoAlert(R.string.media_detail_description,R.string.description_info);
                return true;
            }
        }
        else{
            value = descEdit.getLeft() + descEdit.getCompoundDrawables()[0].getBounds().width();
            if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() <= value) {
                showInfoAlert(R.string.media_detail_description,R.string.description_info);
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
        View target = getView().findFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
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
}
