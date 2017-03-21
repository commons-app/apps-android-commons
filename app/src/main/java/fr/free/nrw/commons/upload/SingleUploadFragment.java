package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTouch;
import fr.free.nrw.commons.Prefs;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;

public class SingleUploadFragment extends Fragment {
    private SharedPreferences prefs;
    private String license;

    public interface OnUploadActionInitiated {
        void uploadActionInitiated(String title, String description);
    }

    @BindView(R.id.titleEdit) EditText titleEdit;
    @BindView(R.id.descEdit) EditText descEdit;
    @BindView(R.id.titleDescButton) Button titleDescButton;
    @BindView(R.id.share_license_summary) TextView licenseSummaryView;
    @BindView(R.id.licenseSpinner) Spinner licenseSpinner;

    private OnUploadActionInitiated uploadActionInitiatedHandler;

    private static final String TAG = SingleUploadFragment.class.getName();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_share, menu);
        if(titleEdit != null) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_upload, null);
        ButterKnife.bind(this, rootView);

        titleEdit = (EditText)rootView.findViewById(R.id.titleEdit);
        descEdit = (EditText)rootView.findViewById(R.id.descEdit);
        Button titleDescButton = (Button) rootView.findViewById(R.id.titleDescButton);
        licenseSpinner = (Spinner) rootView.findViewById(R.id.licenseSpinner);
        licenseSummaryView = (TextView)rootView.findViewById(R.id.share_license_summary);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.FALLBACK_LICENSE);

        Log.d("Single Upload fragment", license);

        int position = 4;
        TypedArray license_entries = getResources().obtainTypedArray(R.array.pref_defaultLicense_entries);
        for(int i = 0; i < license_entries.length(); i++) {
            if(license.equals(license_entries.getString(i))) {
                position = i;
                break;
            }
        }
        license_entries.recycle();

        Log.d("Single Upload fragment", "Spinner Position: "+ position +", License: "+license);
        licenseSpinner.setSelection(position);

        TextWatcher uploadEnabler = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if(getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        };

        titleEdit.addTextChangedListener(uploadEnabler);

        setLicenseSummary(license);

        return rootView;
    }

    @OnItemSelected(R.id.licenseSpinner) void onLicenseSelected(AdapterView<?> parent, View view, int position, long id) {
        String licenseName = parent.getItemAtPosition(position).toString();

        String license = Prefs.FALLBACK_LICENSE; // default value
        if(getString(R.string.license_name_cc0).equals(licenseName)) {
            license = getString(R.string.license_name_cc0);
        } else if(getString(R.string.license_name_cc_by).equals(licenseName)) {
            license = getString(R.string.license_name_cc_by_3_0);
        } else if(getString(R.string.license_name_cc_by_sa).equals(licenseName)) {
            license = getString(R.string.license_name_cc_by_sa_3_0);
        } else if(getString(R.string.license_name_cc_by_four).equals(licenseName)) {
            license = getString(R.string.license_name_cc_by_4_0);
        } else if(getString(R.string.license_name_cc_by_sa_four).equals(licenseName)) {
            license = getString(R.string.license_name_cc_by_sa_4_0);
        }

        setLicenseSummary(license);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Prefs.DEFAULT_LICENSE, license);
        editor.apply();
    }

    @OnTouch(R.id.share_license_summary) boolean showLicence(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(Utils.licenseUrlFor(license, getContext())));
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    @OnClick(R.id.titleDescButton) void setTitleDescButton() {
        //Retrieve last title and desc entered
        SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String title = titleDesc.getString("Title", "");
        String desc = titleDesc.getString("Desc", "");
        Log.d(TAG, "Title: " + title + ", Desc: " + desc);

        titleEdit.setText(title);
        descEdit.setText(desc);
    }

    private void setLicenseSummary(String license) {
        licenseSummaryView.setText(getString(R.string.share_license_summary, license));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        uploadActionInitiatedHandler = (OnUploadActionInitiated) activity;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
