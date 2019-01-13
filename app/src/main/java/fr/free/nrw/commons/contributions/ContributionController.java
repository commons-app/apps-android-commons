package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.Fragment;

import com.esafirm.imagepicker.features.ImagePicker;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

@Singleton
public class ContributionController {

    //request codes
    public static final int CAMERA_UPLOAD_REQUEST_CODE = 10011;
    public static final int GALLERY_UPLOAD_REQUEST_CODE = 10012;
    public static final int NEARBY_CAMERA_UPLOAD_REQUEST_CODE = 10013;
    public static final int NEARBY_GALLERY_UPLOAD_REQUEST_CODE = 10014;
    public static final int BOOKMARK_CAMERA_UPLOAD_REQUEST_CODE = 10015;
    public static final int BOOKMARK_GALLERY_UPLOAD_REQUEST_CODE = 10016;

    //upload limits
    public static final int MULTIPLE_UPLOAD_IMAGE_LIMIT = 5;
    public static final int NEARBY_UPLOAD_IMAGE_LIMIT = 1;

    private final Context context;
    private final SharedPreferences defaultPrefs;
    private final SharedPreferences directPrefs;

    @Inject
    public ContributionController(Context context,
                                  @Named("default_preferences") SharedPreferences defaultSharedPrefs,
                                  @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs) {
        this.context = context;
        this.defaultPrefs = defaultSharedPrefs;
        this.directPrefs = directPrefs;
    }

    public void initiateCameraPick(Fragment fragment,
                                   int requestCode) {
        boolean useExtStorage = defaultPrefs.getBoolean("useExternalStorage", true);
        if (!useExtStorage) {
            initiateCameraUpload(fragment, requestCode);
            return;
        }

        PermissionUtils.checkPermissionsAndPerformAction(fragment.getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> initiateCameraUpload(fragment, requestCode),
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale);
    }

    public void initiateGalleryPick(Fragment fragment,
                                    int imageLimit,
                                    int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            initiateGalleryUpload(fragment, imageLimit, requestCode);
        } else {
            PermissionUtils.checkPermissionsAndPerformAction(fragment.getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    () -> initiateGalleryUpload(fragment, imageLimit, requestCode),
                    R.string.storage_permission_title,
                    R.string.read_storage_permission_rationale);
        }
    }

    private void initiateGalleryUpload(Fragment fragment,
                                       int imageLimit,
                                       int requestCode) {
        ImagePicker imagePicker = ImagePicker.ImagePickerWithFragment
                .create(fragment)
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

    private void initiateCameraUpload(Fragment fragment, int requestCode) {
        ImagePicker.cameraOnly()
                .start(fragment, requestCode);
    }

    public Intent handleImagesPicked(ArrayList<Uri> uriList, int requestCode) {
        Intent shareIntent = new Intent(context, UploadActivity.class);
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

        return shareIntent;
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
