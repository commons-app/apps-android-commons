package fr.free.nrw.commons.delete

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.actions.PageEditClient
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Requests the speedy deletion of an upload under criterion G7 of Commons: the author or
 * uploader asks for the deletion of unused content that is less than 7 days old.
 * https://commons.wikimedia.org/wiki/Commons:Criteria_for_speedy_deletion#G7
 *
 * Files that do not meet the criterion need a regular deletion request instead, see
 * [DeleteHelper].
 */
@Singleton
class SpeedyDeletionHelper
    @Inject
    constructor(
        private val speedyDeletionInterface: SpeedyDeletionInterface,
        @param:Named("commons-page-edit") private val pageEditClient: PageEditClient,
    ) {
        /**
         * Checks with Commons that the file is not used anywhere, then adds the G7 template to
         * the file page.
         *
         * @param media the upload to delete, which must be the user's own upload
         * @param reason the reason shown on the file page, in English
         * @return true if the file page was tagged. Fails with [FileInUseException] if the file is
         * used, and with [FileUsageUnknownException] if Commons could not tell.
         */
        fun requestDeletion(
            media: Media,
            reason: String,
        ): Single<Boolean> {
            val filename = media.filename ?: return Single.error(FileUsageUnknownException())
            return speedyDeletionInterface
                .getFileUsage(filename)
                .map { response -> checkUnused(response) }
                .flatMap {
                    pageEditClient
                        .prependEdit(
                            filename,
                            "{{G7|reason=${templateSafe(reason)}}}\n",
                            "Requesting speedy deletion of [[$filename]]: author or uploader request (G7)",
                        ).firstOrError()
                }
        }

        private fun checkUnused(response: FileUsageResponse) {
            val page = response.query?.pages?.firstOrNull()
            // Without "globalusage", Commons did not say where the file is used
            if (page == null || page.missing == true || page.globalusage == null) {
                throw FileUsageUnknownException()
            }
            if (page.globalusage.isNotEmpty() || !page.fileusage.isNullOrEmpty()) {
                throw FileInUseException()
            }
        }

        /**
         * Removes characters that would end the template parameter early
         */
        private fun templateSafe(reason: String): String = reason.replace("|", " ").replace("{{", "").replace("}}", "")

        companion object {
            /** G7 applies to content that is less than 7 days old */
            val MAX_AGE_MILLIS: Long = TimeUnit.DAYS.toMillis(7)

            /**
             * Whether an upload is recent enough for G7. An upload without a known date is not.
             *
             * @param dateUploaded when the file was uploaded
             * @param now the current time
             */
            fun isWithinTimeLimit(
                dateUploaded: Date?,
                now: Date = Date(),
            ): Boolean {
                if (dateUploaded == null) {
                    return false
                }
                val age = now.time - dateUploaded.time
                return age in 0 until MAX_AGE_MILLIS
            }
        }
    }

/**
 * The file is used on a wiki, so it needs a regular deletion request instead of G7
 */
class FileInUseException : Exception("The file is in use")

/**
 * Commons did not tell whether the file is used, so G7 cannot be requested safely
 */
class FileUsageUnknownException : Exception("Could not check where the file is used")
