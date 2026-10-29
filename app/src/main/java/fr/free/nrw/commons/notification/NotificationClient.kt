package fr.free.nrw.commons.notification

import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.notification.models.Notification
import fr.free.nrw.commons.notification.models.NotificationType
import fr.free.nrw.commons.utils.DateUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import fr.free.nrw.commons.wikidata.model.notifications.Notification as WikimediaNotification

private const val NOTIFICATION_WIKIS = "wikidatawiki|commonswiki|enwiki"

// Echo returns at most 50 notifications per response, and its rawcount stops at 100.
// The badge follows notcontinue so an inbox larger than that still drops after a mark-as-read.
private const val MAX_UNREAD_PAGES = 20

@Singleton
class NotificationClient
    @Inject
    constructor(
        @param:Named(NetworkingModule.NAMED_COMMONS_CSRF) private val csrfTokenClient: CsrfTokenClient,
        private val service: NotificationInterface,
    ) {
        fun getNotifications(archived: Boolean): Single<List<Notification>> =
            service
                .getAllNotifications(
                    wikiList = NOTIFICATION_WIKIS,
                    filter = if (archived) "read" else "!read",
                    continueStr = null,
                ).map {
                    it.query()?.notifications()?.list() ?: emptyList()
                }.flatMap {
                    Observable.fromIterable(it)
                }.map {
                    it.toCommonsNotification()
                }.toList()

        /**
         * Counts unread notifications on Commons, Wikidata, and English Wikipedia.
         * One response is not enough: Echo pages them, and the badge has to include every page.
         */
        fun getUnreadCount(): Single<Int> =
            Single.defer {
                var total = 0
                var continueToken: String? = null
                val seenContinueTokens = mutableSetOf<String>()
                var page = 0
                while (page < MAX_UNREAD_PAGES) {
                    val response =
                        service
                            .getAllNotifications(
                                wikiList = NOTIFICATION_WIKIS,
                                filter = "!read",
                                continueStr = continueToken,
                            ).blockingFirst()
                    val notifications = response?.query()?.notifications()
                    total += notifications?.list()?.size ?: 0
                    val next = notifications?.continueToken()
                    page++
                    if (next.isNullOrEmpty() || !seenContinueTokens.add(next)) {
                        break
                    }
                    continueToken = next
                }
                Single.just(total)
            }

        fun markNotificationAsRead(notificationId: String?): Observable<Boolean> =
            try {
                service
                    .markRead(
                        token = csrfTokenClient.getTokenBlocking(),
                        readList = notificationId,
                        unreadList = "",
                    ).map(MwQueryResponse::success)
            } catch (throwable: Throwable) {
                if (throwable is InvalidLoginTokenException) {
                    Observable.error(throwable)
                } else {
                    Observable.just(false)
                }
            }

        private fun WikimediaNotification.toCommonsNotification(): Notification {
            val notificationText = contents?.compactHeader ?: ""
            val notificationType =
                if (notificationText.contains("Sent you an email", ignoreCase = true)) {
                    NotificationType.EMAIL
                } else {
                    NotificationType.UNKNOWN
                }

            return Notification(
                notificationType = notificationType,
                notificationText = notificationText,
                date = DateUtil.getMonthOnlyDateString(getTimestamp()),
                link = contents?.links?.getPrimary()?.url ?: "",
                iconUrl = "",
                notificationId = id().toString(),
            )
        }
    }
