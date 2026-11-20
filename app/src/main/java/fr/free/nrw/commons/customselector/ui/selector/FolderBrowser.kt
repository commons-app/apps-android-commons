package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.os.Build
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.FolderDeletionHelper
import fr.free.nrw.commons.kvstore.JsonKvStore
import java.io.File

/**
 * Keeps track of the folder that is browsed in the custom selector. It shows the folder list or
 * the images of a folder, keeps the toolbar in sync with them and deletes the opened folder.
 */
class FolderBrowser(
    /**
     * Activity that hosts the folder and image fragments and the toolbar.
     */
    private val activity: AppCompatActivity,
    /**
     * View model, used to reload the folders once the opened folder is deleted.
     */
    private val viewModel: CustomSelectorViewModel,
    /**
     * Key value store, tells whether the folder deletion button is displayed.
     */
    private val defaultKvStore: JsonKvStore,
    /**
     * Waits for confirmation of delete folder
     */
    private val folderDeletionLauncher: ActivityResultLauncher<IntentSenderRequest>,
    /**
     * Called when the user clicks on the upload limit error button of the toolbar.
     */
    private val onUploadLimitErrorClick: () -> Unit,
) {
    /**
     * isImageFragmentOpen is true when the image fragment is in view.
     */
    var isImageFragmentOpen = false
        private set

    /**
     * Current ImageFragment attributes.
     */
    var bucketId: Long = 0L
        private set
    lateinit var bucketName: String
        private set

    /**
     * Show delete button in folder
     */
    private var showOverflowMenu = false

    /**
     * Set up view, default folder view.
     */
    fun setUpViews() {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, FolderFragment.newInstance())
            .commit()
        setUpToolbar()
    }

    /**
     * Opens the images of a folder, changes the toolbar title and makes the overflow menu visible.
     *
     * @param folderId The id of the folder to open.
     * @param folderName The name of the folder to open.
     * @param lastItemId The last scroll position in the folder.
     */
    fun openFolder(
        folderId: Long,
        folderName: String,
        lastItemId: Long,
    ) {
        activity.supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folderId, lastItemId))
            .addToBackStack(null)
            .commit()

        changeTitle(folderName, 0)

        bucketId = folderId
        bucketName = folderName
        isImageFragmentOpen = true

        // show the overflow menu only when a folder is clicked
        showOverflowMenu = true
        setUpToolbar()
    }

    /**
     * Updates the folder state and the toolbar once the user navigated back.
     */
    fun onNavigatedBack() {
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null && fragment is FolderFragment) {
            isImageFragmentOpen = false
            changeTitle(activity.getString(R.string.custom_selector_title), 0)
        }

        // hide overflow menu when not in folder
        showOverflowMenu = false
        setUpToolbar()
    }

    /**
     * Change the title of the toolbar.
     */
    fun changeTitle(
        title: String,
        selectedImageCount: Int,
    ) {
        if (title.isNotEmpty()) {
            val titleText = activity.findViewById<TextView>(R.id.title)
            var titleWithAppendedImageCount = title
            if (selectedImageCount > 0) {
                titleWithAppendedImageCount += " (${activity.resources.getQuantityString(
                    R.plurals.custom_picker_images_selected_title_appendix,
                    selectedImageCount,
                    selectedImageCount,
                )})"
            }
            if (titleText != null) {
                titleText.text = titleWithAppendedImageCount
            }
        }
    }

    /**
     * Handles the result of the folder deletion confirmation, goes back to the folder list when
     * the folder got deleted.
     *
     * @param result The result of the folder deletion confirmation.
     */
    fun onDeleteFolderResultReceived(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            FolderDeletionHelper.showSuccess(activity, "Folder deleted successfully", bucketName)
            navigateToCustomSelector()
        }
    }

    /**
     * Set up the toolbar, back listener, done listener.
     */
    private fun setUpToolbar() {
        val back: ImageButton = activity.findViewById(R.id.back)
        back.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }

        val limitError: ImageButton = activity.findViewById(R.id.image_limit_error)
        limitError.visibility = View.INVISIBLE
        limitError.setOnClickListener { onUploadLimitErrorClick() }

        val overflowMenu: ImageButton = activity.findViewById(R.id.menu_overflow)
        if (defaultKvStore.getBoolean("displayDeletionButton")) {
            overflowMenu.visibility = if (showOverflowMenu) View.VISIBLE else View.INVISIBLE
            overflowMenu.setOnClickListener { showPopupMenu(overflowMenu) }
        } else {
            overflowMenu.visibility = View.GONE
        }
    }

    /**
     * Shows the popup menu of the toolbar, that allows to delete the opened folder.
     *
     * @param anchorView The view the popup menu is anchored to.
     */
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(activity, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_custom_selector, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_folder -> {
                    deleteFolder()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    /**
     * Deletes folder based on Android API version.
     */
    private fun deleteFolder() {
        val folderPath =
            FolderDeletionHelper.getFolderPath(activity, bucketId) ?: run {
                FolderDeletionHelper.showError(activity, "Failed to retrieve folder path", bucketName)
                return
            }

        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            FolderDeletionHelper.showError(activity, "Folder not found or is not a directory", bucketName)
            return
        }

        FolderDeletionHelper.confirmAndDeleteFolder(activity, folder, folderDeletionLauncher) { success ->
            if (success) {
                // for API 30+, navigation is handled in 'onDeleteFolderResultReceived'
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    FolderDeletionHelper.showSuccess(activity, "Folder deleted successfully", bucketName)
                    navigateToCustomSelector()
                }
            } else {
                FolderDeletionHelper.showError(activity, "Failed to delete folder", bucketName)
            }
        }
    }

    /**
     * Navigates back to the main `FolderFragment`, refreshes the MediaStore, resets UI states,
     * and reloads folder data.
     */
    private fun navigateToCustomSelector() {
        val folderPath = FolderDeletionHelper.getFolderPath(activity, bucketId) ?: ""
        val folder = File(folderPath)

        activity.supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )

        // refresh MediaStore for the deleted folder path to ensure metadata updates
        FolderDeletionHelper.refreshMediaStore(activity, folder)

        // replace the current fragment with FolderFragment to go back to the main screen
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, FolderFragment.newInstance())
            .commitAllowingStateLoss()

        // reset toolbar and flags
        isImageFragmentOpen = false
        showOverflowMenu = false
        setUpToolbar()
        changeTitle(activity.getString(R.string.custom_selector_title), 0)

        // fetch updated folder data
        viewModel.fetchImages()
    }
}
