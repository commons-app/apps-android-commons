package fr.free.nrw.commons.upload

import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_FAILED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.contributions.ContributionsRepository
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Single
import timber.log.Timber
import java.util.Calendar

/**
 * Checks whether a failed [contribution] already exists on Commons and either queues it as
 * [STATE_QUEUED] or removes a confirmed duplicate from the database.
 *
 * Duplicate-check failures (such as network issues or inaccessible source URIs) fall back to
 * queueing the upload so the user can retry it reliably.
 *
 * @return A [Single] emitting `true` if queued for upload, or `false` if deleted/skipped.
 */
internal fun queueContributionForRetry(
    contribution: Contribution,
    uploadRepository: UploadRepository,
    contributionsRepository: ContributionsRepository,
): Single<Boolean> {
    val shouldCheckDuplicate = contribution.state == STATE_FAILED
    if (shouldCheckDuplicate) {
        contribution.dateUploadStarted = Calendar.getInstance().time
        if (contribution.errorInfo == null) {
            contribution.chunkInfo = null
            contribution.transferred = 0
        }
    }
    val duplicateCheck =
        if (shouldCheckDuplicate) {
            uploadRepository
                .checkDuplicateImage(contribution.contentUri, contribution.localUri)
                .onErrorReturn { throwable ->
                    Timber.e(throwable, "Duplicate check failed during retry; queueing anyway")
                    IMAGE_OK
                }
        } else {
            Single.just(IMAGE_OK)
        }
    return duplicateCheck.flatMap { imageCheckResult ->
        if (imageCheckResult == IMAGE_OK) {
            contribution.errorInfo = null
            contribution.state = STATE_QUEUED
            contributionsRepository
                .save(contribution)
                .toSingleDefault(true)
                .onErrorReturn { throwable ->
                    Timber.e(throwable, "Failed to queue contribution")
                    false
                }
        } else {
            Timber.e("Contribution already exists")
            contributionsRepository
                .deleteContributionFromDB(contribution)
                .toSingleDefault(false)
                .onErrorReturn { throwable ->
                    Timber.e(throwable, "Failed to delete duplicate contribution")
                    false
                }
        }
    }
}
