package fr.nrw.free.commons.contributions;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import com.actionbarsherlock.app.SherlockFragment;
import fr.nrw.free.commons.campaigns.Campaign;
import fr.nrw.free.commons.upload.ShareActivity;
import fr.nrw.free.commons.upload.UploadService;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ContributionController {
    private SherlockFragment fragment;
    private Activity activity;
    private Campaign campaign;

    private final static int SELECT_FROM_GALLERY = 1;
    private final static int SELECT_FROM_CAMERA = 2;

    public ContributionController(SherlockFragment fragment, Campaign campaign) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.campaign = campaign;
    }

    // See http://stackoverflow.com/a/5054673/17865 for why this is done
    private Uri lastGeneratedCaptureURI;

    private Uri reGenerateImageCaptureURI() {
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)) {

            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Commons/images/" + new Date().getTime() + ".jpg";
            File _photoFile = new File(path);
            try {
                if(_photoFile.exists() == false) {
                    _photoFile.getParentFile().mkdirs();
                    _photoFile.createNewFile();
                }

            } catch (IOException e) {
                Log.e("Commons", "Could not create file: " + path, e);
            }

            return Uri.fromFile(_photoFile);
        }   else {
            throw new RuntimeException("No external storage found!");
        }
    }

    public void startCameraCapture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        lastGeneratedCaptureURI = reGenerateImageCaptureURI();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastGeneratedCaptureURI);
        fragment.startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
    }

    public void startGalleryPick() {
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        fragment.startActivityForResult(pickImageIntent, SELECT_FROM_GALLERY);
    }

    public void handleImagePicked(int requestCode, Intent data) {
        Intent shareIntent = new Intent(activity, ShareActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(UploadService.EXTRA_CAMPAIGN, campaign);
        switch(requestCode) {
            case SELECT_FROM_GALLERY:
                shareIntent.setType(activity.getContentResolver().getType(data.getData()));
                shareIntent.putExtra(Intent.EXTRA_STREAM, data.getData());
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_GALLERY);
                break;
            case SELECT_FROM_CAMERA:
                shareIntent.setType("image/jpeg"); //FIXME: Find out appropriate mime type
                shareIntent.putExtra(Intent.EXTRA_STREAM, lastGeneratedCaptureURI);
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_CAMERA);
                break;
        }
        activity.startActivity(shareIntent);
    }

    public void saveState(Bundle outState) {
        outState.putParcelable("lastGeneratedCaptureURI", lastGeneratedCaptureURI);
        outState.putSerializable("campaign", campaign);
    }

    public void loadState(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            lastGeneratedCaptureURI = (Uri) savedInstanceState.getParcelable("lastGeneratedCaptureURI");
            campaign = (Campaign) savedInstanceState.getSerializable("campaign");
        }
    }

}
