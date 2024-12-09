package fr.free.nrw.commons.fileusages

/**
 * Show where file is being used on Commons and oher wikis.
 */
data class FileUsagesUiModel(
    val title: String,
    val link: String?
)

fun FileUsage.toUiModel(): FileUsagesUiModel {
    return FileUsagesUiModel(title = title, link = "https://commons.wikimedia.org/wiki/$title")
}

fun GlobalFileUsage.toUiModel(): FileUsagesUiModel {
    // link is associated with sub items under wiki group (which is not used ATM)
    return FileUsagesUiModel(title = wiki, link = null)
}
