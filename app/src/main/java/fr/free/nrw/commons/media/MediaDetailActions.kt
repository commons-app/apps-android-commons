package fr.free.nrw.commons.media

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.locationpicker.LocationPicker
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage.Revision
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber

private const val DEFAULT_PICKER_LATITUDE = 37.773972
private const val DEFAULT_PICKER_LONGITUDE = -122.431297
private const val DEFAULT_PICKER_ZOOM = 16.0

/** Shows the license details. */
internal fun MediaDetailFragment.onMediaDetailLicenceClicked() {
    val url: String? = media!!.licenseUrl
    if (!StringUtils.isBlank(url) && activity != null) {
        handleWebUrl(requireContext(), Uri.parse(url))
    } else {
        viewUtil.showShortToast(requireActivity(), getString(R.string.null_url))
    }
}

/** Opens the file coordinates. */
internal fun MediaDetailFragment.onMediaDetailCoordinatesClicked() {
    if (media!!.coordinates != null && activity != null) {
        handleGeoCoordinates(requireContext(), media!!.coordinates!!)
    }
}

/** Copies a wikitext link for the current file. */
internal fun MediaDetailFragment.onCopyWikicodeClicked() {
    val item = media ?: return
    val filename = item.filename ?: return
    val data = formatMediaWikitext(filename, item.fallbackDescription)
    requireContext().copyToClipboard("wikiCode", data)
    Timber.d("Generated wikidata copy code: %s", data)

    Toast
        .makeText(requireContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT)
        .show()
}

/**
 * Sends thanks to author if the author is not the user
 */
internal fun MediaDetailFragment.sendThanksToAuthor() {
    val fileName: String? = media!!.filename
    if (TextUtils.isEmpty(fileName)) {
        Toast
            .makeText(
                context,
                getString(R.string.error_sending_thanks),
                Toast.LENGTH_SHORT,
            ).show()
        return
    }
    viewDisposables.add(
        reviewHelper
            .getFirstRevisionOfFile(fileName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { revision: Revision? ->
                sendThanks(
                    requireContext(),
                    revision,
                )
            },
    )
}

/**
 * Api call for sending thanks to the author when the author is not the user
 * and display toast depending on the result
 * @param context context
 * @param firstRevision the revision id of the image
 */
@SuppressLint("CheckResult", "StringFormatInvalid")
internal fun MediaDetailFragment.sendThanks(
    context: Context,
    firstRevision: Revision?,
) {
    showShortToast(
        context,
        context.getString(R.string.send_thank_toast, media!!.displayTitle),
    )

    if (firstRevision == null) {
        return
    }

    Observable
        .defer {
            thanksClient.thank(
                firstRevision.revisionId(),
            )
        }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            { result: Boolean ->
                displayThanksToast(
                    requireContext(),
                    result,
                )
            },
            { throwable: Throwable? ->
                if (throwable is InvalidLoginTokenException) {
                    val username: String? = sessionManager.userName
                    val logoutListener: CommonsApplication.BaseLogoutListener =
                        CommonsApplication.BaseLogoutListener(
                            requireActivity(),
                            requireActivity().getString(R.string.invalid_login_message),
                            username,
                        )

                    instance.clearApplicationData(
                        requireActivity(),
                        logoutListener,
                    )
                } else {
                    Timber.e(throwable)
                }
            },
        )
}

/**
 * Method to display toast when api call to thank the author is completed
 * @param context context
 * @param result true if success, false otherwise
 */
@SuppressLint("StringFormatInvalid")
internal fun MediaDetailFragment.displayThanksToast(
    context: Context,
    result: Boolean,
) {
    val message: String =
        if (result) {
            context.getString(
                R.string.send_thank_success_message,
                media!!.user,
            )
        } else {
            context.getString(
                R.string.send_thank_failure_message,
                media!!.user,
            )
        }

    showShortToast(context, message)
}

/** Starts editing the media coordinates. */
internal fun MediaDetailFragment.onUpdateCoordinatesClicked() {
    goToLocationPickerActivity()
}

/**
 * Start location picker activity with a request code and get the coordinates from the activity.
 */
internal fun MediaDetailFragment.goToLocationPickerActivity() {
    /*
    If location is not provided in media this coordinates will act as a placeholder in
    location picker activity
     */
    var defaultLatitude = DEFAULT_PICKER_LATITUDE
    var defaultLongitude: Double = DEFAULT_PICKER_LONGITUDE
    if (media!!.coordinates != null) {
        defaultLatitude = media!!.coordinates!!.latitude
        defaultLongitude = media!!.coordinates!!.longitude
    } else {
        if (locationManager.getLastLocation() != null) {
            defaultLatitude = locationManager.getLastLocation()!!.latitude
            defaultLongitude = locationManager.getLastLocation()!!.longitude
        } else {
            val lastLocation: Array<String>? =
                applicationKvStore
                    .getString(
                        UploadMediaDetailFragment.LAST_LOCATION,
                        ("$defaultLatitude,$defaultLongitude"),
                    )?.split(",".toRegex())
                    ?.dropLastWhile { it.isEmpty() }
                    ?.toTypedArray()

            if (lastLocation != null) {
                defaultLatitude = lastLocation[0].toDouble()
                defaultLongitude = lastLocation[1].toDouble()
            }
        }
    }

    startActivity(
        LocationPicker
            .IntentBuilder()
            .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, DEFAULT_PICKER_ZOOM))
            .activityKey("MediaActivity")
            .media(media!!)
            .build(requireActivity()),
    )
}

/** Opens the Commons deletion request. */
internal fun MediaDetailFragment.onSeeMoreClicked() {
    if (binding.nominatedDeletionBanner.visibility == View.VISIBLE && activity != null) {
        handleWebUrl(requireContext(), Uri.parse(media!!.pageTitle.mobileUri))
    }
}

/** Opens the author profile. */
internal fun MediaDetailFragment.onAuthorViewClicked() {
    if (media == null || media!!.user == null) {
        return
    }
    if (sessionManager.userName == null) {
        val userProfileLink: String = BuildConfig.COMMONS_URL + "/wiki/User:" + media!!.user
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(userProfileLink))
        startActivity(browserIntent)
        return
    }
    ProfileActivity.startYourself(
        requireActivity(), // Ensure this is a non-null Activity context
        media?.user ?: "", // Provide a fallback value if media?.user is null
        sessionManager.userName != media?.user, // This can remain as is, null check will apply
    )
}
