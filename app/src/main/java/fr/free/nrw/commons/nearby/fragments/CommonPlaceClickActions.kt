package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ActivityUtils
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.WikidataConstants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class CommonPlaceClickActions
    @Inject
    constructor(
        @Named("default_preferences") private val applicationKvStore: JsonKvStore,
        private val activity: Activity,
        private val contributionController: ContributionController,
    ) {
        fun onCameraClicked(): (Place, ActivityResultLauncher<Array<String>>, ActivityResultLauncher<Intent>) -> Unit =
            { place, launcher, resultLauncher ->
                if (applicationKvStore.getBoolean("login_skipped", false)) {
                    showLoginDialog()
                } else {
                    Timber.d("Camera button tapped. Image title: ${place.name}Image desc: ${place.longDescription}")
                    storeSharedPrefs(place)
                    contributionController.initiateCameraPick(activity, launcher, resultLauncher)
                }
            }

        /**
         * Shows the Label for the Icon when it's long pressed
         **/
        fun onCameraLongPressed(): () -> Boolean =
            {
                Toast.makeText(activity, R.string.menu_from_camera, Toast.LENGTH_SHORT).show()
                true
            }

        fun onGalleryLongPressed(): () -> Boolean =
            {
                Toast.makeText(activity, R.string.menu_from_gallery, Toast.LENGTH_SHORT).show()
                true
            }

        fun onBookmarkLongPressed(): () -> Boolean =
            {
                Toast.makeText(activity, R.string.menu_bookmark, Toast.LENGTH_SHORT).show()
                true
            }

        fun onDirectionsLongPressed(): () -> Boolean =
            {
                Toast.makeText(activity, R.string.nearby_directions, Toast.LENGTH_SHORT).show()
                true
            }

        fun onOverflowLongPressed(): () -> Boolean =
            {
                Toast.makeText(activity, R.string.more, Toast.LENGTH_SHORT).show()
                true
            }

        fun onGalleryClicked(): (Place, ActivityResultLauncher<Intent>) -> Unit =
            {place, galleryPickLauncherForResult ->
                if (applicationKvStore.getBoolean("login_skipped", false)) {
                    showLoginDialog()
                } else {
                    Timber.d("Gallery button tapped. Image title: ${place.name}Image desc: ${place.longDescription}")
                    storeSharedPrefs(place)
                    contributionController.initiateGalleryPick(activity, galleryPickLauncherForResult, false)
                }
            }

        fun onOverflowClicked(): (Place, View) -> Unit =
            { place, view ->
                PopupMenu(view.context, view)
                    .apply {
                        inflate(R.menu.nearby_info_dialog_options)
                        enableBy(R.id.nearby_info_menu_commons_article, place.hasCommonsLink())
                        enableBy(R.id.nearby_info_menu_wikidata_article, place.hasWikidataLink())
                        enableBy(R.id.nearby_info_menu_wikipedia_article, place.hasWikipediaLink())
                        setOnMenuItemClickListener { item: MenuItem ->
                            when (item.itemId) {
                                R.id.nearby_info_menu_commons_article -> openWebView(place.siteLinks!!.commonsUri!!)
                                R.id.nearby_info_menu_wikidata_article -> openWebView(place.siteLinks!!.wikidataUri!!)
                                R.id.nearby_info_menu_wikipedia_article -> openWebView(place.siteLinks!!.wikipediaUri!!)
                                else -> false
                            }
                        }
                    }.show()
            }

        fun onDirectionsClicked(): (Place) -> Unit =
            {
                handleGeoCoordinates(activity, it.location!!)
            }

        private fun storeSharedPrefs(selectedPlace: Place) {
            Timber.d("Store place object %s", selectedPlace.toString())
            applicationKvStore.putJson(WikidataConstants.PLACE_OBJECT, selectedPlace)
        }

        private fun openWebView(link: Uri): Boolean {
            handleWebUrl(activity, link)
            return true
        }

        private fun PopupMenu.enableBy(
            menuId: Int,
            hasLink: Boolean,
        ) {
            menu.findItem(menuId).isEnabled = hasLink
        }

        private fun showLoginDialog() {
            AlertDialog
                .Builder(activity)
                .setMessage(R.string.login_alert_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel){_,_ -> }
                .setPositiveButton(R.string.login) { dialog, which ->
                    setPositiveButton()
                }.show()
        }

        private fun setPositiveButton() {
            ActivityUtils.startActivityWithFlags(
                activity,
                LoginActivity::class.java,
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            applicationKvStore.putBoolean("login_skipped", false)
            activity.finish()
        }
    }
