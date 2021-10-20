package fr.free.nrw.commons.description;

import static fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION;
import static fr.free.nrw.commons.description.EditDescriptionConstants.UPDATED_WIKITEXT;
import static fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter;
import fr.free.nrw.commons.utils.DialogUtil;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Activity for populating and editing existing description and caption
 */
public class DescriptionEditActivity extends AppCompatActivity implements
    UploadMediaDetailAdapter.EventListener {

    /**
     * Adapter for showing UploadMediaDetail in the activity
     */
    private UploadMediaDetailAdapter uploadMediaDetailAdapter;

    /**
     * For getting default preference
     */
    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    /**
     * Recyclerview for recycling data in views
     */
    @BindView(R.id.rv_descriptions_captions)
    RecyclerView rvDescriptions;

    /**
     * Submit button for submitting new wikiText
     */
    @BindView(R.id.btn_edit_submit)
    AppCompatButton btnSubmit;

//    @BindView(R.id.mapbox_place_picker_toolbar_back_button)
//    ImageView backButton;


    /**
     * Current wikitext
     */
    String wikiText;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_edit);

        ButterKnife.bind(this, this);
        final Bundle bundle = getIntent().getExtras();
        final ArrayList<UploadMediaDetail> descriptionAndCaptions
            = bundle.getParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION);
        wikiText = bundle.getString(WIKITEXT);

        initRecyclerView(descriptionAndCaptions);
    }

    /**
     * Initializes the RecyclerView
     * @param descriptionAndCaptions list of description and caption
     */
    private void initRecyclerView(final ArrayList<UploadMediaDetail> descriptionAndCaptions) {
        uploadMediaDetailAdapter
            = new UploadMediaDetailAdapter("en",descriptionAndCaptions);
        uploadMediaDetailAdapter.setCallback(this::showInfoAlert);
        uploadMediaDetailAdapter.setEventListener(this);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(this));
        rvDescriptions.setAdapter(uploadMediaDetailAdapter);
    }

    /**
     * show dialog with info
     * @param titleStringID Title ID
     * @param messageStringId Message ID
     */
    private void showInfoAlert(final int titleStringID, final int messageStringId) {
        DialogUtil.showAlertDialog(this, getString(titleStringID),
            getString(messageStringId), getString(android.R.string.ok),
            null, true);
    }

    @Override
    public void onPrimaryCaptionTextChange(final boolean isNotEmpty) {

    }

    @Optional
    @OnClick(R.id.mapbox_place_picker_toolbar_back_button)
    public void onBackButtonClicked(){
        onBackPressed();
    }

    @OnClick(R.id.btn_add_description)
    public void onButtonAddDescriptionClicked() {
        final UploadMediaDetail uploadMediaDetail = new UploadMediaDetail();
        uploadMediaDetail.setManuallyAdded(true);//This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail);
        rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.getItemCount()-1);
    }


    @OnClick(R.id.btn_edit_submit)
    public void onSubmitButtonClicked(){
        showLoggingProgressBar();
        final List<UploadMediaDetail> uploadMediaDetails = uploadMediaDetailAdapter.getItems();
        updateDescription(uploadMediaDetails);
        finish();
    }

    /**
     * Updates newly added descriptions in the wikiText and send to calling fragment
     * @param uploadMediaDetails descriptions and captions
     */
    private void updateDescription(final List<UploadMediaDetail> uploadMediaDetails) {
        int descriptionIndex = wikiText.indexOf("description=");
        if (descriptionIndex == -1){
            descriptionIndex = wikiText.indexOf("Description=");
        }

        final StringBuilder buffer = new StringBuilder();

        if (descriptionIndex != -1) {

            final String descriptionStart = wikiText.substring(0, descriptionIndex + 12);
            final String descriptionToEnd = wikiText.substring(descriptionIndex+12);
            final int descriptionEndIndex = descriptionToEnd.indexOf("\n");
            final String descriptionEnd = wikiText.substring(descriptionStart.length()
                +descriptionEndIndex);

            buffer.append(descriptionStart);
            for (int i=0; i<uploadMediaDetails.size(); i++) {
                final UploadMediaDetail uploadDetails = uploadMediaDetails.get(i);
                if (!uploadDetails.getDescriptionText().equals("")) {
                    buffer.append("{{");
                    buffer.append(uploadDetails.getLanguageCode());
                    buffer.append("|1=");
                    buffer.append(uploadDetails.getDescriptionText());
                    buffer.append("}}, ");
                }
            }
            buffer.deleteCharAt(buffer.length()-1);
            buffer.deleteCharAt(buffer.length()-1);
            buffer.append(descriptionEnd);
        }
        final Intent returningIntent = new Intent();
        returningIntent.putExtra(UPDATED_WIKITEXT, buffer.toString());
        returningIntent.putParcelableArrayListExtra(LIST_OF_DESCRIPTION_AND_CAPTION,
            (ArrayList<? extends Parcelable>) uploadMediaDetails);
        setResult(RESULT_OK, returningIntent);
        finish();
    }

    private void showLoggingProgressBar() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(getString(R.string.updating_caption_title));
        progressDialog.setMessage(getString(R.string.updating_caption_message));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }
}