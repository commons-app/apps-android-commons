package fr.free.nrw.commons.contributions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.upload.ShareActivity;
import fr.free.nrw.commons.upload.UploadService;
import timber.log.Timber;

public class ContributionController {
    private Fragment fragment;
    private Activity activity;

    private final static int SELECT_FROM_GALLERY = 1;
    private final static int SELECT_FROM_CAMERA = 2;

    public ContributionController(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    // See http://stackoverflow.com/a/5054673/17865 for why this is done
    private Uri lastGeneratedCaptureUri;

    private Uri reGenerateImageCaptureUriInCache() {
        File photoFile = new File(fragment.getContext().getCacheDir() + "/images",
                new Date().getTime() + ".jpg");
        photoFile.getParentFile().mkdirs();
        return FileProvider.getUriForFile(
                fragment.getContext(),
                fragment.getActivity().getApplicationContext().getPackageName() + ".provider",
                photoFile);
    }

    public void startCameraCapture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        lastGeneratedCaptureUri = reGenerateImageCaptureUriInCache();

        // Intent.setFlags doesn't work for API level <20
        requestWritePermission(fragment.getContext(), takePictureIntent, lastGeneratedCaptureUri);

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastGeneratedCaptureUri);
        fragment.startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
    }

    private static void requestWritePermission(Context context, Intent intent, Uri uri){

        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public void startGalleryPick() {
        //FIXME: Starts gallery (opens Google Photos)
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        fragment.startActivityForResult(pickImageIntent, SELECT_FROM_GALLERY);
    }

    public void handleImagePicked(int requestCode, Intent data) {
        Intent shareIntent = new Intent(activity, ShareActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        switch(requestCode) {
            case SELECT_FROM_GALLERY:
                //Handles image picked from gallery
                Uri imageData = data.getData();
                shareIntent.setType(activity.getContentResolver().getType(imageData));
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageData);
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_GALLERY);
                break;
            case SELECT_FROM_CAMERA:
                shareIntent.setType("image/jpeg"); //FIXME: Find out appropriate mime type
                shareIntent.putExtra(Intent.EXTRA_STREAM, lastGeneratedCaptureUri);
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_CAMERA);
                break;
        }
        Timber.i("Image selected");
        try {
            activity.startActivity(shareIntent);
        } catch (SecurityException e) {
            Timber.e(e, "Security Exception");
        }
    }

    public void saveState(Bundle outState) {
        outState.putParcelable("lastGeneratedCaptureURI", lastGeneratedCaptureUri);
    }

    public void loadState(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            lastGeneratedCaptureUri = savedInstanceState.getParcelable("lastGeneratedCaptureURI");
        }
    }

}
