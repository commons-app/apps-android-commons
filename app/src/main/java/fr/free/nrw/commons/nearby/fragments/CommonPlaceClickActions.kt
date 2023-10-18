package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ActivityUtils
import fr.free.nrw.commons.wikidata.WikidataConstants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named


class CommonPlaceClickActions @Inject constructor(
    @Named("default_preferences") private val applicationKvStore: JsonKvStore,
    private val activity: Activity,
    private val contributionController: ContributionController
) {

    fun onCameraClicked(): (Place, ActivityResultLauncher<Array<String>>) -> Unit = { place, launcher ->
        if (applicationKvStore.getBoolean("login_skipped", false)) {
            showLoginDialog()
        } else {
            Timber.d("Camera button tapped. Image title: ${place.getName()}Image desc: ${place.longDescription}")
            storeSharedPrefs(place)
            contributionController.initiateCameraPick(activity, launcher)
        }
    }

    fun onGalleryClicked(): (Place) -> Unit = {
        if (applicationKvStore.getBoolean("login_skipped", false)) {
            showLoginDialog()
        } else {
            Timber.d("Gallery button tapped. Image title: ${it.getName()}Image desc: ${it.getLongDescription()}")
            storeSharedPrefs(it)
            contributionController.initiateGalleryPick(activity, false)
        }
    }

    fun onOverflowClicked(): (Place, View) -> Unit = { place, view ->
        PopupMenu(view.context, view).apply {
            inflate(R.menu.nearby_info_dialog_options)
            enableBy(R.id.nearby_info_menu_commons_article, place.hasCommonsLink())
            enableBy(R.id.nearby_info_menu_wikidata_article, place.hasWikidataLink())
            enableBy(R.id.nearby_info_menu_wikipedia_article, place.hasWikipediaLink())
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.nearby_info_menu_commons_article -> openWebView(place.siteLinks.commonsLink)
                    R.id.nearby_info_menu_wikidata_article -> openWebView(place.siteLinks.wikidataLink)
                    R.id.nearby_info_menu_wikipedia_article -> openWebView(place.siteLinks.wikipediaLink)
                    else -> false
                }
            }
        }.show()
    }

    fun onDirectionsClicked(): (Place) -> Unit = {
        Utils.handleGeoCoordinates(activity, it.getLocation())
    }

    private fun storeSharedPrefs(selectedPlace: Place) {
        Timber.d("Store place object %s", selectedPlace.toString())
        applicationKvStore.putJson(WikidataConstants.PLACE_OBJECT, selectedPlace)
    }

    private fun openWebView(link: Uri): Boolean {
        Utils.handleWebUrl(activity, link)
        return true
    }

    private fun PopupMenu.enableBy(menuId: Int, hasLink: Boolean) {
        menu.findItem(menuId).isEnabled = hasLink
    }

    private fun showLoginDialog() {
        AlertDialog.Builder(activity)
            .setMessage(R.string.login_alert_message)
            .setPositiveButton(R.string.login) { dialog, which ->
                setPositiveButton()
            }
            .show()
    }

    private fun setPositiveButton() {
        ActivityUtils.startActivityWithFlags(
            activity,
            LoginActivity::class.java,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        applicationKvStore.putBoolean("login_skipped", false)
        activity.finish()
    }
}
