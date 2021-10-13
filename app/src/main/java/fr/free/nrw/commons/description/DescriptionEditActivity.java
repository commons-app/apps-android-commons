package fr.free.nrw.commons.description;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.LocationPicker.LocationPickerConstants;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter;
import fr.free.nrw.commons.utils.DialogUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

public class DescriptionEditActivity extends AppCompatActivity implements
    UploadMediaDetailAdapter.EventListener {

    private UploadMediaDetailAdapter uploadMediaDetailAdapter;

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    @BindView(R.id.rv_descriptions_captions)
    RecyclerView rvDescriptions;

    @BindView(R.id.btn_edit_submit)
    AppCompatButton btnSubmit;

    String wikiText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_edit);

        ButterKnife.bind(this, this);
        Bundle bundle = getIntent().getExtras();
        ArrayList<UploadMediaDetail> descriptionAndCaptions = bundle.getParcelableArrayList("mylist");
        wikiText = bundle.getString("wikiText");

        for (UploadMediaDetail d :
            descriptionAndCaptions) {
            Log.d("DescriptionAndCaption", "des1 "+d.getDescriptionText()+" cap1 "
                +d.getCaptionText()+" lan1 "+d.getLanguageCode());
        }

        initRecyclerView(descriptionAndCaptions);
    }

    private void initRecyclerView(ArrayList<UploadMediaDetail> descriptionAndCaptions) {
        uploadMediaDetailAdapter = new UploadMediaDetailAdapter("en",descriptionAndCaptions);
        uploadMediaDetailAdapter.setCallback(this::showInfoAlert);
        uploadMediaDetailAdapter.setEventListener(this);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(this));
        rvDescriptions.setAdapter(uploadMediaDetailAdapter);
    }

    /**
     * show dialog with info
     * @param titleStringID
     * @param messageStringId
     */
    private void showInfoAlert(int titleStringID, int messageStringId) {
        DialogUtil.showAlertDialog(this, getString(titleStringID), getString(messageStringId), getString(android.R.string.ok), null, true);
    }

    @Override
    public void onPrimaryCaptionTextChange(boolean isNotEmpty) {

    }

    @OnClick(R.id.btn_add_description)
    public void onButtonAddDescriptionClicked() {
        UploadMediaDetail uploadMediaDetail = new UploadMediaDetail();
        uploadMediaDetail.setManuallyAdded(true);//This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail);
        rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.getItemCount()-1);
    }


    @OnClick(R.id.btn_edit_submit)
    public void onSubmitButtonClicked(){
        List<UploadMediaDetail> uploadMediaDetails = uploadMediaDetailAdapter.getItems();

        Log.d("DescriptionAndCaption", "clicked");
        for (UploadMediaDetail d :
            uploadMediaDetails) {
            Log.d("DescriptionAndCaption", "des2 "+d.getDescriptionText()+" cap2 "
                +d.getCaptionText()+" lan2 "+d.getLanguageCode());
        }

        updateDescription(uploadMediaDetails);

        finish();
    }

    private void updateDescription(List<UploadMediaDetail> uploadMediaDetails) {
        int descriptionIndex = wikiText.indexOf("description=");
        if(descriptionIndex == -1){
            descriptionIndex = wikiText.indexOf("Description=");
        }
        Log.d("DescriptionAndCaption", "descriptionIndex"+descriptionIndex);

        final StringBuilder buffer = new StringBuilder();

        if( descriptionIndex == -1 ){

        } else {

            String descriptionStart = wikiText.substring(0, descriptionIndex + 12);
            Log.d("DescriptionAndCaption", "descriptionStart"+descriptionStart);
            String descriptionToEnd = wikiText.substring(descriptionIndex+12);
            int descriptionEndIndex = descriptionToEnd.indexOf("\n");
            String descriptionEnd = wikiText.substring(descriptionStart.length()+descriptionEndIndex);
            Log.d("DescriptionAndCaption", "descriptionEnd"+descriptionEnd);

            buffer.append(descriptionStart);
            for (int i=0; i<uploadMediaDetails.size(); i++) {
                UploadMediaDetail uploadDetails = uploadMediaDetails.get(i);
                if(!uploadDetails.getDescriptionText().equals("")) {
                    if (i == uploadMediaDetails.size() - 1) {
                        buffer.append("{{");
                        buffer.append(uploadDetails.getLanguageCode());
                        buffer.append("|1=");
                        buffer.append(uploadDetails.getDescriptionText());
                        buffer.append("}}");
                    } else {
                        buffer.append("{{");
                        buffer.append(uploadDetails.getLanguageCode());
                        buffer.append("|1=");
                        buffer.append(uploadDetails.getDescriptionText());
                        buffer.append("}}, ");
                    }
                }
            }
            buffer.append(descriptionEnd);
        }
        Log.d("EditDes", buffer.toString());
        final Intent returningIntent = new Intent();
        returningIntent.putExtra("updatedWikiText",
            buffer.toString());
        setResult(RESULT_OK, returningIntent);
        finish();
    }
}