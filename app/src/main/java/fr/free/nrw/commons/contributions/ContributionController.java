package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.esafirm.imagepicker.features.ImagePicker;

import java.util.ArrayList;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.StringUtils;
import timber.log.Timber;

import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static android.content.Intent.EXTRA_STREAM;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_EXTERNAL;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.IS_DIRECT_UPLOAD;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ITEM_LOCATION;

public class ContributionController {

    public static final int CAMERA_UPLOAD_REQUEST_CODE = 10011;
    public static final int GALLERY_UPLOAD_REQUEST_CODE = 10012;
    public static final int NEARBY_CAMERA_UPLOAD_REQUEST_CODE = 10013;
    public static final int NEARBY_GALLERY_UPLOAD_REQUEST_CODE = 10014;
    public static final int BOOKMARK_CAMERA_UPLOAD_REQUEST_CODE = 10015;
    public static final int BOOKMARK_GALLERY_UPLOAD_REQUEST_CODE = 10016;

    private Fragment fragment;
    private SharedPreferences defaultPrefs;
    private SharedPreferences directPrefs;

    public ContributionController(Fragment fragment,
                                  SharedPreferences defaultSharedPrefs,
                                  SharedPreferences directPrefs) {
        this.fragment = fragment;
        this.defaultPrefs = defaultSharedPrefs;
        this.directPrefs = directPrefs;
    }

    public void initiateCameraPick(Activity activity,
                                   int requestCode) {
        boolean useExtStorage = defaultPrefs.getBoolean("useExternalStorage", true);
        if (!useExtStorage) {
            initiateCameraUpload(activity, requestCode);
            return;
        }

        PermissionUtils.checkPermissionsAndPerformAction(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> initiateCameraUpload(activity, requestCode),
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale);
    }

    public void initiateGalleryPick(Activity activity,
                                    int imageLimit,
                                    int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            initiateGalleryUpload(activity, imageLimit, requestCode);
        } else {
            PermissionUtils.checkPermissionsAndPerformAction(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    () -> initiateGalleryUpload(activity, imageLimit, requestCode),
                    R.string.storage_permission_title,
                    R.string.read_storage_permission_rationale);
        }
    }

    private void initiateGalleryUpload(Activity activity,
                                       int imageLimit,
                                       int requestCode) {
        ImagePicker imagePicker = ImagePicker.create(activity)
                .showCamera(false)
                .folderMode(true)
                .includeVideo(false)
                .enableLog(true);

        if (imageLimit > 1) {
            imagePicker.multi().limit(imageLimit).start(requestCode);
        } else {
            imagePicker.single().start(requestCode);
        }
    }

    private void initiateCameraUpload(Activity activity, int requestCode) {
        ImagePicker.cameraOnly()
                .start(activity, requestCode);
    }

    public void handleImagesPicked(ArrayList<Uri> uriList, int requestCode) {
        FragmentActivity activity = fragment.getActivity();
        Intent shareIntent = new Intent(activity, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(EXTRA_SOURCE, getSourceFromRequestCode(requestCode));
        shareIntent.putExtra(EXTRA_STREAM, uriList);
        shareIntent.setType("image/jpeg");

        boolean isDirectUpload = directPrefs.getBoolean(IS_DIRECT_UPLOAD, false);

        shareIntent.putExtra("isDirectUpload", isDirectUpload);
        Timber.d("Put extras into image intent, isDirectUpload is " + isDirectUpload);

        String wikiDataEntityId = directPrefs.getString(WIKIDATA_ENTITY_ID_PREF, null);
        String wikiDataItemLocation = directPrefs.getString(WIKIDATA_ITEM_LOCATION, null);

        if (!StringUtils.isNullOrWhiteSpace(wikiDataEntityId)) {
            shareIntent.putExtra(WIKIDATA_ENTITY_ID_PREF, wikiDataEntityId);
            shareIntent.putExtra(WIKIDATA_ITEM_LOCATION, wikiDataItemLocation);
        }

        if (activity != null) {
            activity.startActivity(shareIntent);
        }
    }

    private String getSourceFromRequestCode(int requestCode) {
        switch (requestCode) {
            case CAMERA_UPLOAD_REQUEST_CODE:
            case NEARBY_CAMERA_UPLOAD_REQUEST_CODE:
            case BOOKMARK_CAMERA_UPLOAD_REQUEST_CODE:
                return SOURCE_CAMERA;
            case GALLERY_UPLOAD_REQUEST_CODE:
            case NEARBY_GALLERY_UPLOAD_REQUEST_CODE:
            case BOOKMARK_GALLERY_UPLOAD_REQUEST_CODE:
                return SOURCE_GALLERY;
        }

        return SOURCE_EXTERNAL;
    }
}
