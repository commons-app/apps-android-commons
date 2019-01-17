package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Intent;

import static fr.free.nrw.commons.contributions.ContributionController.BOOKMARK_CAMERA_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.BOOKMARK_GALLERY_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.CAMERA_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.GALLERY_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.NEARBY_CAMERA_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.NEARBY_GALLERY_UPLOAD_REQUEST_CODE;

public class IntentUtils {

    /**
     * Check if the intent should be handled by nearby list or map fragment
     */
    public static boolean shouldNearbyHandle(int requestCode, int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK
                && (requestCode == NEARBY_CAMERA_UPLOAD_REQUEST_CODE || requestCode == NEARBY_GALLERY_UPLOAD_REQUEST_CODE)
                && data != null;
    }

    /**
     * Check if the intent should be handled by contributions list fragment
     */
    public static boolean shouldContributionsListHandle(int requestCode, int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK
                && (requestCode == GALLERY_UPLOAD_REQUEST_CODE || requestCode == CAMERA_UPLOAD_REQUEST_CODE)
                && data != null;
    }

    /**
     * Check if the intent should be handled by contributions list fragment
     */
    public static boolean shouldBookmarksHandle(int requestCode, int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK
                && (requestCode == BOOKMARK_CAMERA_UPLOAD_REQUEST_CODE || requestCode == BOOKMARK_GALLERY_UPLOAD_REQUEST_CODE)
                && data != null;
    }
}
