package fr.free.nrw.commons.media.model

data class PageMediaListResponse(
    val revision: String,
    val tid: String,
    val items: List<Item>
)

data class Item(val title: String)
