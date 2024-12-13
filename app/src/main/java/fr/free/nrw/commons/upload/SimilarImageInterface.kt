package fr.free.nrw.commons.upload

interface SimilarImageInterface {
    fun showSimilarImageFragment(
        originalFilePath: String?,
        possibleFilePath: String?,
        similarImageCoordinates: ImageCoordinates?
    )
}
