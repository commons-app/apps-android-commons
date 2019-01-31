package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.esafirm.imagepicker.features.ImagePicker;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.FrescoImageLoader;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.PermissionUtils;

import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static android.content.Intent.EXTRA_STREAM;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_EXTERNAL;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

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
    private final BasicKvStore defaultKvStore;
    private final JsonKvStore directKvStore;

    @Inject
    public ContributionController(Context context,
                                  @Named("default_preferences") BasicKvStore defaultKvStore,
                                  @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore) {
        this.context = context;
        this.defaultKvStore = defaultKvStore;
        this.directKvStore = directKvStore;
    }

    public void initiateCameraPick(Activity activity,
                                   int requestCode) {
        boolean useExtStorage = defaultKvStore.getBoolean("useExternalStorage", true);
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
        ImagePicker imagePicker = ImagePicker.ImagePickerWithFragment
                .create(activity)
                .showCamera(false)
                .folderMode(true)
                .includeVideo(false)
                .imageLoader(new FrescoImageLoader())
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

    public Intent handleImagesPicked(ArrayList<Uri> uriList, int requestCode) {
        Intent shareIntent = new Intent(context, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(EXTRA_SOURCE, getSourceFromRequestCode(requestCode));
        shareIntent.putExtra(EXTRA_STREAM, uriList);
        shareIntent.setType("image/jpeg");
        Place place = directKvStore.getJson(PLACE_OBJECT, Place.class);
        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place);
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
