package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.upload.UploadActivity;
import timber.log.Timber;

import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static android.content.Intent.EXTRA_STREAM;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ITEM_LOCATION;

public class ContributionController {

    public static final int SELECT_FROM_GALLERY = 1;
    public static final int SELECT_FROM_CAMERA = 2;
    public static final int PICK_IMAGE_MULTIPLE = 3;

    private Fragment fragment;

    public ContributionController(Fragment fragment) {
        this.fragment = fragment;
    }

    // See http://stackoverflow.com/a/5054673/17865 for why this is done
    private Uri lastGeneratedCaptureUri;

    private Uri reGenerateImageCaptureUriInCache() {
        File photoFile = new File(fragment.getContext().getCacheDir() + "/images",
                new Date().getTime() + ".jpg");
        photoFile.getParentFile().mkdirs();
        Context applicationContext = fragment.getActivity().getApplicationContext();
        return FileProvider.getUriForFile(
                fragment.getContext(),
                applicationContext.getPackageName() + ".provider",
                photoFile);
    }

    private static void requestWritePermission(Context context, Intent intent, Uri uri) {

        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public void startCameraCapture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        lastGeneratedCaptureUri = reGenerateImageCaptureUriInCache();

        // Intent.setFlags doesn't work for API level <20
        requestWritePermission(fragment.getContext(), takePictureIntent, lastGeneratedCaptureUri);

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastGeneratedCaptureUri);
        if (!fragment.isAdded()) {
            return;
        }
        fragment.startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
    }

    public void startGalleryPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            startMultipleGalleryPick();
        } else {
            startSingleGalleryPick();
        }
    }

    public void startSingleGalleryPick() {
        //FIXME: Starts gallery (opens Google Photos)
        Intent pickImageIntent = new Intent(ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        // See https://stackoverflow.com/questions/22366596/android-illegalstateexception-fragment-not-attached-to-activity-webview
        if (!fragment.isAdded()) {
            Timber.d("Fragment is not added, startActivityForResult cannot be called");
            return;
        }
        Timber.d("startSingleGalleryPick() called with pickImageIntent");

        fragment.startActivityForResult(pickImageIntent, SELECT_FROM_GALLERY);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startMultipleGalleryPick() {
        Intent pickImageIntent = new Intent(ACTION_GET_CONTENT);
        pickImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageIntent.setType("image/*");
        if (!fragment.isAdded()) {
            Timber.d("Fragment is not added, startActivityForResult cannot be called");
            return;
        }
        Timber.d("startMultipleGalleryPick() called with pickImageIntent");

        fragment.startActivityForResult(pickImageIntent, PICK_IMAGE_MULTIPLE);
    }

    public void handleImagesPicked(int requestCode, @Nullable ArrayList<Uri> uri) {
        FragmentActivity activity = fragment.getActivity();
        Intent shareIntent = new Intent(activity, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(EXTRA_SOURCE, SOURCE_GALLERY);
        shareIntent.putExtra(EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        if (activity != null) {
            activity.startActivity(shareIntent);
        }
    }

    public void handleImagePicked(int requestCode, @Nullable Uri uri, boolean isDirectUpload, String wikiDataEntityId, String wikidateItemLocation) {
        FragmentActivity activity = fragment.getActivity();
        Timber.d("handleImagePicked() called with onActivityResult(). Boolean isDirectUpload: " + isDirectUpload + "String wikiDataEntityId: " + wikiDataEntityId);
        Intent shareIntent = new Intent(activity, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND);
        switch (requestCode) {
            case SELECT_FROM_GALLERY:
                //Handles image picked from gallery
                Uri imageData = uri;
                shareIntent.setType(activity.getContentResolver().getType(imageData));
                shareIntent.putExtra(EXTRA_STREAM, imageData);
                shareIntent.putExtra(EXTRA_SOURCE, SOURCE_GALLERY);
                break;
            case SELECT_FROM_CAMERA:
                //FIXME: Find out appropriate mime type
                // AFAIK this is the right type for a JPEG image
                // https://developer.android.com/training/sharing/send.html#send-binary-content
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(EXTRA_STREAM, lastGeneratedCaptureUri);
                shareIntent.putExtra(EXTRA_SOURCE, SOURCE_CAMERA);
                break;
            default:
                break;
        }

        Timber.i("Image selected");
        shareIntent.putExtra("isDirectUpload", isDirectUpload);
        Timber.d("Put extras into image intent, isDirectUpload is " + isDirectUpload);

        try {
            if (wikiDataEntityId != null && !wikiDataEntityId.equals("")) {
                shareIntent.putExtra(WIKIDATA_ENTITY_ID_PREF, wikiDataEntityId);
                shareIntent.putExtra(WIKIDATA_ITEM_LOCATION, wikidateItemLocation);
            }
        } catch (SecurityException e) {
            Timber.e(e, "Security Exception");
        }

        if (activity != null) {
            activity.startActivity(shareIntent);
        }
    }

    void saveState(Bundle outState) {
        if (outState != null) {
            outState.putParcelable("lastGeneratedCaptureURI", lastGeneratedCaptureUri);
        }
    }

    void loadState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            lastGeneratedCaptureUri = savedInstanceState.getParcelable("lastGeneratedCaptureURI");
        }
    }
}
