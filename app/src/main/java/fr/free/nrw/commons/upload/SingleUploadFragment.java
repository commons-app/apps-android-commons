package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    private OnUploadActionInitiated uploadActionInitiatedHandler;

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
            case R.id.menu_upload_single:

                String title = titleEdit.getText().toString();
                String desc = descEdit.getText().toString();

                //TODO: Save the values of these fields in short-lived cache so next time this fragment is loaded, we can access these
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

        //TODO: Add button here for 'copy from previous upload'
        licenseSummaryView = (TextView)rootView.findViewById(R.id.share_license_summary);

        TextWatcher uploadEnabler = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            public void afterTextChanged(Editable editable) {
                if(getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        };

        titleEdit.addTextChangedListener(uploadEnabler);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA);
        licenseSummaryView.setText(getString(R.string.share_license_summary, getString(Utils.licenseNameFor(license))));

        // Open license page on touch
        licenseSummaryView.setOnTouchListener(new View.OnTouchListener() {
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
