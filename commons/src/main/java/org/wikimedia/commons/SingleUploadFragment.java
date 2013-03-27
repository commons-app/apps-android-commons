package org.wikimedia.commons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.wikimedia.commons.contributions.Contribution;

public class SingleUploadFragment extends SherlockFragment {

    public interface OnUploadActionInitiated {
        void uploadActionInitiated(String title, String description);
    }

    private EditText titleEdit;
    private EditText descEdit;

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
                uploadActionInitiatedHandler.uploadActionInitiated(titleEdit.getText().toString(), descEdit.getText().toString());
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_upload, null);

        titleEdit = (EditText)rootView.findViewById(R.id.titleEdit);
        descEdit = (EditText)rootView.findViewById(R.id.descEdit);

        TextWatcher uploadEnabler = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            public void afterTextChanged(Editable editable) {
                getSherlockActivity().invalidateOptionsMenu();
            }
        };

        titleEdit.addTextChangedListener(uploadEnabler);

        return rootView;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        uploadActionInitiatedHandler = (OnUploadActionInitiated) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
