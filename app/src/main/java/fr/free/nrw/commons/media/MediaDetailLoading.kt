package fr.free.nrw.commons.media

import android.view.View
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.ContributionsFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Objects

/** Loads each file usage list when it has not been requested yet. */
internal fun MediaDetailFragment.fetchFileUsages(fileName: String) {
    if (viewModel.commonsContainerState.value == MediaDetailViewModel.FileUsagesContainerState.Initial) {
        viewModel.loadFileUsagesCommons(fileName)
    }

    if (viewModel.globalContainerState.value == MediaDetailViewModel.FileUsagesContainerState.Initial) {
        viewModel.loadGlobalFileUsages(fileName)
    }
}

/**
 * Retrieves the ContributionsFragment that is potentially the parent, grandparent, etc
 * fragment of this fragment.
 *
 * @return The ContributionsFragment instance. If the ContributionsFragment instance could not
 * be found, null is returned.
 */
internal fun MediaDetailFragment.getContributionsFragmentParent(): ContributionsFragment? {
    var fragment: Fragment? = this

    while (fragment != null && fragment !is ContributionsFragment) {
        fragment = fragment.parentFragment
    }

    if (fragment == null) {
        return null
    }

    return fragment as ContributionsFragment
}

/** Renders the supplied media and refreshes its details. */
internal fun MediaDetailFragment.displayMediaDetails() {
    setTextFields(media!!)
    viewDisposables.addAll(
        mediaDataExtractor
            .refresh(media!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { media: Media -> onMediaRefreshed(media) },
                { t: Throwable? ->
                    if (t.isFileDeletedOnCommons()) {
                        onFileDeletedFromCommons()
                    } else {
                        Timber.e(t)
                    }
                },
            ),
        mediaDataExtractor
            .getCurrentWikiText(
                Objects.requireNonNull(media?.filename!!),
            ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { s: String? -> updateCategoryList(s!!) },
                { t: Throwable? -> Timber.e(t) },
            ),
        mediaDataExtractor
            .checkDeletionRequestExists(media!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { deletionPageExists: Boolean -> onDeletionPageExists(deletionPageExists) },
                { t: Throwable? -> Timber.e(t) },
            ),
        mediaDataExtractor
            .fetchDiscussion(media!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { discussion: String -> onDiscussionLoaded(discussion) },
                { t: Throwable? -> Timber.e(t) },
            ),
    )
}

/** Renders the refreshed media and requests its depictions. */
internal fun MediaDetailFragment.onMediaRefreshed(media: Media) {
    media.categories = this.media!!.categories
    this.media = media
    setTextFields(media)
    viewDisposables.addAll(
        mediaDataExtractor
            .fetchDepictionIdsAndLabels(media)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { idAndCaptions: List<IdAndLabels> -> onDepictionsLoaded(idAndCaptions) },
                { t: Throwable? -> Timber.e(t) },
            ),
    )
}

/** Renders the loaded file discussion. */
internal fun MediaDetailFragment.onDiscussionLoaded(discussion: String) {
    binding.mediaDetailDisc.text = prettyDiscussion(discussion.trim { it <= ' ' })
}

/** Renders the loaded depictions and restores the edit control. */
internal fun MediaDetailFragment.onDepictionsLoaded(idAndCaptions: List<IdAndLabels>) {
    binding.depictsLayout.visibility = View.VISIBLE
    binding.depictionsEditButton.visibility = View.VISIBLE
    buildDepictionList(idAndCaptions)
}

/**
 * The media load this screen already makes reported the file page as missing.
 * Explain the deletion once, and keep the row so the gallery can show it as deleted.
 */
internal fun MediaDetailFragment.onFileDeletedFromCommons() {
    val currentMedia = media
    if (currentBinding != null && currentMedia != null) {
        val alreadyShown = fileWasDeletedFromCommons
        fileWasDeletedFromCommons = true
        revealMediaDetailWithoutImage()
        binding.deletedFileBanner.visibility = View.VISIBLE
        binding.nominateDeletion.visibility = View.GONE
        binding.nominatedDeletionBanner.visibility = View.GONE
        val filename = currentMedia.filename
        if (!alreadyShown && !filename.isNullOrEmpty()) {
            contributionsRepository
                .markCompletedContributionDeleted(filename)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {
                        // No action needed after successful deletion
                    },
                    { t: Throwable -> Timber.e(t, "Failed to mark contribution deleted") },
                )
            if (isAdded) {
                DeletedFileActivity.start(requireContext(), filename)
            }
        }
    }
}
