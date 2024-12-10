package fr.free.nrw.commons.wikidata.mwapi

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter.PostProcessable
import fr.free.nrw.commons.wikidata.model.BaseModel
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import fr.free.nrw.commons.wikidata.model.notifications.Notification
import org.apache.commons.lang3.StringUtils

class MwQueryResult : BaseModel(), PostProcessable {
    private val pages: List<MwQueryPage>? = null
    private val redirects: List<Redirect>? = null
    private val converted: List<ConvertedTitle>? = null

    @SerializedName("userinfo")
    private val userInfo: UserInfo? = null
    private val users: List<ListUserResponse>? = null
    private val tokens: Tokens? = null
    private val notifications: NotificationList? = null

    @SerializedName("allimages")
    private val allImages: List<ImageDetails>? = null

    fun pages(): List<MwQueryPage>? = pages

    fun firstPage(): MwQueryPage? = pages?.firstOrNull()

    fun allImages(): List<ImageDetails> = allImages ?: emptyList()

    fun userInfo(): UserInfo? = userInfo

    fun csrfToken(): String? = tokens?.csrf()

    fun loginToken(): String? = tokens?.login()

    fun notifications(): NotificationList? = notifications

    fun getUserResponse(userName: String): ListUserResponse? =
        users?.find { StringUtils.capitalize(userName) == it.name() }

    fun images() = buildMap {
        pages?.forEach { page ->
            page.imageInfo()?.let {
                put(page.title(), it)
            }
        }
    }

    override fun postProcess() {
        resolveConvertedTitles()
        resolveRedirectedTitles()
    }

    private fun resolveRedirectedTitles() {
        if (redirects == null || pages == null) {
            return
        }

        pages.forEach { page ->
            redirects.forEach { redirect ->
                // TODO: Looks like result pages and redirects can also be matched on the "index"
                // property.  Confirm in the API docs and consider updating.
                if (page.title() == redirect.to()) {
                    page.redirectFrom(redirect.from())
                    if (redirect.toFragment() != null) {
                        page.appendTitleFragment(redirect.toFragment())
                    }
                }
            }
        }
    }

    private fun resolveConvertedTitles() {
        if (converted == null || pages == null) {
            return
        }

        converted.forEach { convertedTitle ->
            pages.forEach { page ->
                if (page.title() == convertedTitle.to()) {
                    page.convertedFrom(convertedTitle.from())
                    page.convertedTo(convertedTitle.to())
                }
            }
        }
    }

    private class Redirect {
        private val index = 0
        private val from: String? = null
        private val to: String? = null

        @SerializedName("tofragment")
        private val toFragment: String? = null

        fun to(): String? = to

        fun from(): String? = from

        fun toFragment(): String? = toFragment
    }

    class ConvertedTitle {
        private val from: String? = null
        private val to: String? = null

        fun to(): String? = to

        fun from(): String? = from
    }

    private class Tokens {
        @SerializedName("csrftoken")
        private val csrf: String? = null

        @SerializedName("createaccounttoken")
        private val createAccount: String? = null

        @SerializedName("logintoken")
        private val login: String? = null

        fun csrf(): String? = csrf

        fun createAccount(): String? = createAccount

        fun login(): String? = login
    }

    class NotificationList {
        private val list: List<Notification>? = null

        fun list(): List<Notification>? = list
    }
}
