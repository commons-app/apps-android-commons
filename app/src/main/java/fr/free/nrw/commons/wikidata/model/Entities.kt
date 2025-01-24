package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.mwapi.MwResponse
import org.apache.commons.lang3.StringUtils

class Entities : MwResponse() {
    private val entities: Map<String, Entity>? = null
    val success: Int = 0

    fun entities(): Map<String, Entity> = entities ?: emptyMap()

    private val first : Entity?
        get() = entities?.values?.iterator()?.next()

    override fun postProcess() {
        first?.let {
            if (it.isMissing()) throw RuntimeException("The requested entity was not found.")
        }
    }

    class Entity {
        private val type: String? = null
        private val id: String? = null
        private val labels: Map<String, Label>? = null
        private val descriptions: Map<String, Label>? = null
        private val sitelinks: Map<String, SiteLink>? = null

        @SerializedName(value = "statements", alternate = ["claims"])
        val statements: Map<String, List<StatementPartial>>? = null
        private val missing: String? = null

        fun id(): String =
            StringUtils.defaultString(id)

        fun labels(): Map<String, Label> =
            labels ?: emptyMap()

        fun descriptions(): Map<String, Label> =
            descriptions ?: emptyMap()

        fun sitelinks(): Map<String, SiteLink> =
            sitelinks ?: emptyMap()

        fun isMissing(): Boolean =
            "-1" == id && missing != null
    }

    class Label(private val language: String?, private val value: String?) {
        fun language(): String =
            StringUtils.defaultString(language)

        fun value(): String =
            StringUtils.defaultString(value)
    }

    class SiteLink {
        val site: String? = null
            get() = StringUtils.defaultString(field)

        private val title: String? = null
            get() = StringUtils.defaultString(field)
    }
}
