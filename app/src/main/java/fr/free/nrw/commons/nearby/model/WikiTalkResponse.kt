package fr.free.nrw.commons.nearby.model

data class Description(
    val text: String,
    val user: String,
    val time: String
)

data class Title(
    val title: String,
    val descriptions: List<Description>
)