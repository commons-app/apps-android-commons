package fr.free.nrw.commons.filepicker

interface Constants {
    companion object {
        const val DEFAULT_FOLDER_NAME = "CommonsContributions"
        // this is used for truncation logic to cap the selection at 20 files.
        const val MAX_EXTERNAL_UPLOAD_COUNT: Int = 20
    }

    /**
     * Provides the request codes for permission handling
     */
    interface RequestCodes {
        companion object {
            const val LOCATION = 1
            const val STORAGE = 2
        }
    }

    /**
     * Provides locations as string for corresponding operations
     */
    interface BundleKeys {
        companion object {
            const val FOLDER_NAME = "fr.free.nrw.commons.folder_name"
            const val ALLOW_MULTIPLE = "fr.free.nrw.commons.allow_multiple"
            const val COPY_TAKEN_PHOTOS = "fr.free.nrw.commons.copy_taken_photos"
            const val COPY_PICKED_IMAGES = "fr.free.nrw.commons.copy_picked_images"
        }
    }
}
