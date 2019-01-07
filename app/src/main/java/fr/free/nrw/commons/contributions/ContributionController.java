package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.esafirm.imagepicker.features.ImagePicker;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.BasePermissionListener;

import java.util.ArrayList;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.StringUtils;
import timber.log.Timber;

import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static android.content.Intent.EXTRA_STREAM;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.IS_DIRECT_UPLOAD;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ITEM_LOCATION;

public class ContributionController {

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

    public void initiateCameraPick(Activity activity) {
        boolean useExtStorage = defaultPrefs.getBoolean("useExternalStorage", true);
        if (!useExtStorage) {
            initiateCameraUpload(activity);
            return;
        }

        checkPermissionsAndInitiateUpload(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> initiateCameraUpload(activity), R.string.write_storage_permission_rationale);
    }

    public void initiateGalleryPick(Activity activity, int imageLimit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            initiateGalleryUpload(activity, imageLimit);
        } else {
            checkPermissionsAndInitiateUpload(activity, Manifest.permission.READ_EXTERNAL_STORAGE,
                    () -> initiateGalleryUpload(activity, imageLimit), R.string.read_storage_permission_rationale);
        }
    }

    private void checkPermissionsAndInitiateUpload(Activity activity,
                                                   String permission,
                                                   Runnable onPermissionGranted,
                                                   @StringRes int rationaleMessage) {
        Dexter.withActivity(activity)
                .withPermission(permission)
                .withListener(new BasePermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        onPermissionGranted.run();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            DialogUtil.showAlertDialog(activity,
                                    activity.getString(R.string.storage_permission_title),
                                    activity.getString(rationaleMessage),
                                    activity.getString(R.string.navigation_item_settings),
                                    null,
                                    () -> PermissionUtils.askUserToManuallyEnablePermissionFromSettings(activity),
                                    null);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        DialogUtil.showAlertDialog(activity,
                                activity.getString(R.string.storage_permission_title),
                                activity.getString(rationaleMessage),
                                activity.getString(android.R.string.ok),
                                activity.getString(android.R.string.cancel),
                                token::continuePermissionRequest,
                                token::cancelPermissionRequest);
                    }
                }).check();
    }

    private void initiateGalleryUpload(Activity activity, int imageLimit) {
        ImagePicker imagePicker = ImagePicker.create(activity)
                .showCamera(false)
                .folderMode(true)
                .includeVideo(false)
                .enableLog(true);

        if (imageLimit > 1) {
            imagePicker.multi().limit(imageLimit).start();
        } else {
            imagePicker.single().start();
        }
    }

    private void initiateCameraUpload(Activity activity) {
        ImagePicker.cameraOnly()
                .start(activity);
    }

    public void handleImagesPicked(ArrayList<Uri> uriList) {
        FragmentActivity activity = fragment.getActivity();
        Intent shareIntent = new Intent(activity, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(EXTRA_SOURCE, SOURCE_GALLERY);
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
}
