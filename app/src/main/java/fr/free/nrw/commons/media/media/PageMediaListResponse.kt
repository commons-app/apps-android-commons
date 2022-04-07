package fr.free.nrw.commons.media.media

data class PageMediaListResponse(
    val revision: String,
    val tid: String,
    val items: List<PageMediaListItem>
)

data class PageMediaListItem(val title: String)
