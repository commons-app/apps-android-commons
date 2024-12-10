package fr.free.nrw.commons.wikidata

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import fr.free.nrw.commons.wikidata.json.NamespaceTypeAdapter
import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter
import fr.free.nrw.commons.wikidata.json.RequiredFieldsCheckOnReadTypeAdapterFactory
import fr.free.nrw.commons.wikidata.json.UriTypeAdapter
import fr.free.nrw.commons.wikidata.json.WikiSiteTypeAdapter
import fr.free.nrw.commons.wikidata.model.DataValue.Companion.polymorphicTypeAdapter
import fr.free.nrw.commons.wikidata.model.WikiSite
import fr.free.nrw.commons.wikidata.model.page.Namespace

object GsonUtil {
    private const val DATE_FORMAT = "MMM dd, yyyy HH:mm:ss"

    private val DEFAULT_GSON_BUILDER: GsonBuilder by lazy {
        GsonBuilder().setDateFormat(DATE_FORMAT)
            .registerTypeAdapterFactory(polymorphicTypeAdapter)
            .registerTypeHierarchyAdapter(Uri::class.java, UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace::class.java, NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapter(WikiSite::class.java, WikiSiteTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(RequiredFieldsCheckOnReadTypeAdapterFactory())
            .registerTypeAdapterFactory(PostProcessingTypeAdapter())
    }

    val defaultGson: Gson by lazy { DEFAULT_GSON_BUILDER.create() }
}
