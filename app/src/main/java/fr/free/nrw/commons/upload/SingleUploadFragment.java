package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.Prefs;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;

public class SingleUploadFragment extends Fragment {

    public interface OnUploadActionInitiated {
        void uploadActionInitiated(String title, String description);
    }

    private EditText titleEdit;
    private EditText descEdit;
    private TextView licenseSummaryView;
    private Spinner licenseSpinner;

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

        titleEdit = (EditText)rootView.findViewById(R.id.titleEdit);
        descEdit = (EditText)rootView.findViewById(R.id.descEdit);
        Button titleDescButton = (Button) rootView.findViewById(R.id.titleDescButton);
        licenseSpinner = (Spinner) rootView.findViewById(R.id.licenseSpinner);
        licenseSummaryView = (TextView)rootView.findViewById(R.id.share_license_summary);

        ArrayList<String> licenseItems = new ArrayList<>();
        licenseItems.add(getString(R.string.license_name_cc0));
        licenseItems.add(getString(R.string.license_name_cc_by));
        licenseItems.add(getString(R.string.license_name_cc_by_sa));

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA);

        Log.d("Single Upload fragment", license);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, licenseItems);
        licenseSpinner.setAdapter(adapter);

        int position = licenseItems.indexOf(getString(Utils.licenseNameFor(license)));
        Log.d("Single Upload fragment", "Position:"+position+" "+getString(Utils.licenseNameFor(license)));
        licenseSpinner.setSelection(position);

        licenseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String licenseName = parent.getItemAtPosition(position).toString();

                String license = Prefs.Licenses.CC_BY_SA; // default value
                if(getString(R.string.license_name_cc0).equals(licenseName)) {
                    license = Prefs.Licenses.CC0;
                } else if(getString(R.string.license_name_cc_by).equals(licenseName)) {
                    license = Prefs.Licenses.CC_BY;
                } else if(getString(R.string.license_name_cc_by_sa).equals(licenseName)) {
                    license = Prefs.Licenses.CC_BY_SA;
                }

                setLicenseSummary(license);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Prefs.DEFAULT_LICENSE, license);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        titleDescButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Retrieve last title and desc entered
                SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String title = titleDesc.getString("Title", "");
                String desc = titleDesc.getString("Desc", "");
                Log.d(TAG, "Title: " + title + ", Desc: " + desc);

                titleEdit.setText(title);
                descEdit.setText(desc);
            }
        });

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

        // Open license page on touch
        licenseSummaryView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Utils.licenseUrlFor(license)));
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            }
        });

        return rootView;
    }

    private void setLicenseSummary(String license) {
        licenseSummaryView.setText(getString(R.string.share_license_summary, getString(Utils.licenseNameFor(license))));
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
