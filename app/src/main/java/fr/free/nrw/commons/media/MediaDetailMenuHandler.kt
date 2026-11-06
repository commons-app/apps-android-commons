package fr.free.nrw.commons.media

import android.app.Activity
import android.graphics.Bitmap
import android.view.Menu
import android.view.MenuItem
import androidx.core.net.toUri
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.ImageUtils.setAvatarFromImageUrl
import fr.free.nrw.commons.utils.ImageUtils.setWallpaperFromImageUrl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * Handles options menu actions and item configurations for media details.
 */

class MediaDetailMenuHandler
    @Inject
    constructor(
        private val bookmarkDao: BookmarkPicturesDao,
        private val sessionManager: SessionManager,
        private val okHttpJsonApiClient: OkHttpJsonApiClient,
    ) {
        private val compositeDisposable = CompositeDisposable()

        /**
         * Set the media as the device's wallpaper if the imageUrl is not null
         * Fails silently if setting the wallpaper fails
         * @param activity
         * @param media
         */
        fun setWallpaper(
            activity: Activity,
            media: Media,
        ) {
            if (media.imageUrl == null || media.imageUrl!!.isEmpty()) {
                Timber.d("Media URL not present")
                return
            }
            setWallpaperFromImageUrl(activity, media.imageUrl!!.toUri())
        }

        /**
         * Set the media as user's leaderboard avatar
         * @param activity
         * @param media
         */
        fun setAvatar(
            activity: Activity,
            media: Media,
        ) {
            if (media.imageUrl == null || media.imageUrl!!.isEmpty()) {
                Timber.d("Media URL not present")
                return
            }
            setAvatarFromImageUrl(
                activity,
                media.imageUrl!!,
                sessionManager.currentAccount!!.name,
                okHttpJsonApiClient,
                compositeDisposable,
            )
        }

        /**
         * Decide wether or not we should display the background color menu items
         * We display them if the image is transparent
         * @param getBitmap
         * @param menu
         */
        fun handleBackgroundColorMenuItems(
            getBitmap: Callable<Bitmap>,
            menu: Menu,
        ) {
            Observable
                .fromCallable(
                    getBitmap,
                ).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { image: Bitmap ->
                        if (image.hasAlpha()) {
                            menu
                                .findItem(R.id.menu_view_set_white_background)
                                .setVisible(true)
                                .setEnabled(true)
                            menu
                                .findItem(R.id.menu_view_set_black_background)
                                .setVisible(true)
                                .setEnabled(true)
                        }
                    },
                )
        }

        /**
         * Updates the bookmark state and icon for the given menu item.
         *
         * @param item the bookmark menu item to update
         * @param bookmark the current bookmark entity
         * @param currentItem the current pager position
         * @param removedItems list of positions that have been unbookmarked in this session
         */
        fun updateBookmarkState(
            item: MenuItem,
            bookmark: Bookmark?,
            currentItem: Int,
            removedItems: ArrayList<Int>,
        ) {
            val isBookmarked = bookmarkDao.findBookmark(bookmark)
            if (isBookmarked) {
                if (removedItems.contains(currentItem)) {
                    removedItems.remove(currentItem)
                }
            } else {
                if (!removedItems.contains(currentItem)) {
                    removedItems.add(currentItem)
                }
            }

            item.setIcon(
                if (isBookmarked) {
                    R.drawable.menu_ic_round_star_filled_24px
                } else {
                    R.drawable.menu_ic_round_star_border_24px
                },
            )
        }
    }
