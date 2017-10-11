package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTouch;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.settings.Prefs;
import timber.log.Timber;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class SingleUploadFragment extends Fragment {

    @BindView(R.id.titleEdit) EditText titleEdit;
    @BindView(R.id.descEdit) EditText descEdit;
    @BindView(R.id.titleDescButton) Button titleDescButton;
    @BindView(R.id.share_license_summary) TextView licenseSummaryView;
    @BindView(R.id.licenseSpinner) Spinner licenseSpinner;

    private SharedPreferences prefs;
    private String license;
    private OnUploadActionInitiated uploadActionInitiatedHandler;
    private TitleTextWatcher textWatcher = new TitleTextWatcher();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_share, menu);
        if (titleEdit != null) {
            menu.findItem(R.id.menu_upload_single).setEnabled(titleEdit.getText().length() != 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //What happens when the 'submit' icon is tapped
            case R.id.menu_upload_single:

                String title = titleEdit.getText().toString();
                String desc = descEdit.getText().toString();

                //Save the title/desc in short-lived cache so next time this fragment is loaded, we can access these
                SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = titleDesc.edit();
                editor.putString("Title", title);
                editor.putString("Desc", desc);
                editor.apply();

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


        ArrayList<String> licenseItems = new ArrayList<>();
        licenseItems.add(getString(R.string.license_name_cc0));
        licenseItems.add(getString(R.string.license_name_cc_by));
        licenseItems.add(getString(R.string.license_name_cc_by_sa));
        licenseItems.add(getString(R.string.license_name_cc_by_four));
        licenseItems.add(getString(R.string.license_name_cc_by_sa_four));

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);

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

        setLicenseSummary(license);

        return rootView;
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
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Prefs.DEFAULT_LICENSE, license);
        editor.commit();
    }

    @OnTouch(R.id.share_license_summary)
    boolean showLicence(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == ACTION_DOWN) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(Utils.licenseUrlFor(license)));
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    @OnClick(R.id.titleDescButton)
    void setTitleDescButton() {
        //Retrieve last title and desc entered
        SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String title = titleDesc.getString("Title", "");
        String desc = titleDesc.getString("Desc", "");
        Timber.d("Title: %s, Desc: %s", title, desc);

        titleEdit.setText(title);
        descEdit.setText(desc);
    }

    /**
     * Copied from https://stackoverflow.com/a/26269435/8065933
     */
    @OnTouch(R.id.titleEdit)
    boolean titleInfo(View view, MotionEvent motionEvent) {
        //Should replace right with end to support different right-to-left languages as well
        final int value = titleEdit.getRight() - titleEdit.getCompoundDrawables()[2].getBounds().width();

        if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() >= value) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.media_detail_title)
                    .setMessage(R.string.title_info)
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                    .create()
                    .show();
            return true;
        }
        return false;
    }

    @OnTouch(R.id.descEdit)
    boolean descriptionInfo(View view, MotionEvent motionEvent) {
        final int value = descEdit.getRight() - descEdit.getCompoundDrawables()[2].getBounds().width();

        if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() >= value) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.media_detail_description)
                    .setMessage(R.string.description_info)
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                    .create()
                    .show();
            return true;
        }
        return false;
    }

    private void setLicenseSummary(String license) {
        licenseSummaryView.setText(getString(R.string.share_license_summary, getString(Utils.licenseNameFor(license))));
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
}
