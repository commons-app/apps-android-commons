package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName

data class PlaceBindings(
    @SerializedName("item") val item: Item,
    @SerializedName("label") val label: Label,
    @SerializedName("location") val location: Location,
    @SerializedName("class") val clas: Clas
)

data class ItemsClass(
    @SerializedName("head") val head: Head,
    @SerializedName("results") val results: Results
)

data class Label(
    @SerializedName("xml:lang") val xml: String,
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

data class Location(
    @SerializedName("datatype") val datatype: String,
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

data class Results(
    @SerializedName("bindings") val bindings: List<PlaceBindings>
)

data class Item(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

data class Head(
    @SerializedName("vars") val vars: List<String>
)


data class Clas(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)