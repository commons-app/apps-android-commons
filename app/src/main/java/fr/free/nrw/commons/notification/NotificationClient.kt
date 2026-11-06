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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import fr.free.nrw.commons.wikidata.model.notifications.Notification as WikimediaNotification

private const val NOTIFICATION_WIKIS = "wikidatawiki|commonswiki|enwiki"

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
         * Unread count across Commons, Wikidata, and English Wikipedia.
         * Echo answers this with one request and reports at most 100, which means 100 or more.
         */
        fun getUnreadCount(): Single<Int> =
            service
                .getUnreadNotificationCount(wikiList = NOTIFICATION_WIKIS)
                .map { response ->
                    response?.query()?.notifications()?.rawCount() ?: 0
                }.first(0)

        fun markNotificationAsRead(
            notificationId: String?,
            wiki: String,
        ): Observable<Boolean> =
            try {
                service
                    .markRead(
                        token = csrfTokenClient.getTokenBlocking(),
                        readList = notificationId,
                        unreadList = "",
                        wikis = wiki,
                    ).map(MwQueryResponse::success)
            } catch (throwable: InvalidLoginTokenException) {
                Observable.error(throwable)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Failed to mark the notification as read")
                Observable.just(false)
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
                wiki = wiki().ifEmpty { "commonswiki" },
            )
        }
    }
