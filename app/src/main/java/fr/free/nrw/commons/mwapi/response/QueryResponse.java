package fr.free.nrw.commons.mwapi.response;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationType;

import static fr.free.nrw.commons.notification.NotificationType.THANK_YOU_EDIT;
import static fr.free.nrw.commons.notification.NotificationType.UNKNOWN;

@SuppressWarnings({"WeakerAccess", "unused"})
public class QueryResponse {
    @SerializedName("tokens")
    public TokenResponse tokens;
    @SerializedName("userinfo")
    public UserInfoResponse userInfo;
    @SerializedName("searchinfo")
    public SearchInfoResponse searchInfo;
    @SerializedName("pages")
    public Map<String, PageResponse> pages;
    @SerializedName("search")
    public List<SearchResult> searchResults;
    @SerializedName("allcategories")
    public List<Map<String, String>> allCategories;
    @SerializedName("logevents")
    public List<LogEventResponse> logEvents;
    @SerializedName("allimages")
    public List<ImageInfo> allImages;
    @SerializedName("notifications")
    public NotificationQueryResponse notifications;

    @Override
    public String toString() {
        return "QueryResponse{" +
                "tokens=" + tokens +
                ", userInfo=" + userInfo +
                ", searchInfo=" + searchInfo +
                ", pages=" + pages +
                ", searchResults=" + searchResults +
                ", allCategories=" + allCategories +
                '}';
    }

    @NonNull
    public List<NotificationQueryResponse.NotificationResponse> getUsefulNotifications() {
        return notifications != null ? notifications.getUsefulNotifications() : Collections.emptyList();
    }

    @NonNull
    public PageResponse firstPage() {
        return pages != null && pages.size() > 0
                ? new ArrayList<>(pages.values()).get(0)
                : new PageResponse();
    }

    @NonNull
    public List<String> allCategories() {
        if (allCategories == null || allCategories.size() == 0) {
            return Collections.emptyList();
        }
        List<String> categories = new ArrayList<>(searchResults.size());
        for (Map<String, String> map : allCategories) {
            categories.addAll(map.values());
        }
        return categories;
    }

    @NonNull
    public List<String> categories() {
        if (searchResults == null || searchResults.size() == 0) {
            return Collections.emptyList();
        }
        List<String> categories = new ArrayList<>(searchResults.size());
        for (SearchResult result : searchResults) {
            categories.add(result.getCategory());
        }
        return categories;
    }

    public int imageCount() {
        return allImages != null ? allImages.size() : 0;
    }

    public static class TokenResponse {
        @SerializedName("logintoken")
        public String loginToken;
        @SerializedName("csrftoken")
        public String csrfToken;

        @Override
        public String toString() {
            return "TokenResponse{" +
                    "loginToken='" + loginToken + '\'' +
                    ", csrfToken='" + csrfToken + '\'' +
                    '}';
        }
    }

    public class NotificationQueryResponse {
        @SerializedName("list")
        public List<NotificationResponse> list;
        @SerializedName("continue")
        public String continueIndicator;

        public NotificationQueryResponse() {
            list = Collections.emptyList();
        }

        @NonNull
        public List<NotificationResponse> getUsefulNotifications() {
            if (list == null) {
                return Collections.emptyList();
            }
            List<NotificationResponse> nr = new ArrayList<>();
            for (NotificationResponse response : list) {
                if (response.isCommonsNotification()
                        && !UNKNOWN.equals(response.getNotificationType())
                        && !THANK_YOU_EDIT.equals(response.getNotificationType())) {
                    nr.add(response);
                }
            }
            return nr;
        }

        public class NotificationResponse {
            private static final String COMMONS_WIKI = "commonswiki";

            @SerializedName("wiki")
            public String wiki;
            @SerializedName("id")
            public String id;
            @SerializedName("type")
            public String type;
            @SerializedName("category")
            public String category;
            @SerializedName("timestamp")
            public TimestampResponse timestamp;
            @SerializedName("agent")
            public AgentResponse agent;
            @SerializedName("read")
            public String read;
            @SerializedName("title")
            public TitleResponse title;
            @SerializedName("*")
            public ContentResponse content;

            public boolean isCommonsNotification() {
                return COMMONS_WIKI.equals(wiki);
            }

            public NotificationType getNotificationType() {
                return NotificationType.handledValueOf(type);
            }

            @SuppressWarnings("unchecked")
            public Notification toNotification() {
                String link = content.getPrimaryLink();
                String description = title != null && title.text != null ? title.text.trim() : "";
                String icon = String.format("%s%s", BuildConfig.COMMONS_URL, content.iconUrl);
                int notificationTextFormatId = 0;
                String[] notificationTextParameters = new String[0];
                NotificationType notificationType = NotificationType.handledValueOf(type);
                switch (notificationType) {
                    case THANK_YOU_EDIT:
                        notificationTextFormatId = R.string.notifications_thank_you_edit;
                        break;
                    case EDIT_USER_TALK:
                        notificationTextFormatId = R.string.notifications_talk_format;
                        notificationTextParameters = new String[]{content.hasBody()
                                ? content.body.trim()
                                : content.header.trim()};
                        break;
                    case MENTION:
                        notificationTextFormatId = R.string.notifications_mention;
                        notificationTextParameters = new String[]{agent.name};
                        description = content.hasBody() ? content.body.trim() : "";
                        break;
                    case WELCOME:
                        notificationTextFormatId = R.string.notifications_welcome;
                        notificationTextParameters = new String[]{agent.name};
                        break;
                }
                return new Notification(notificationType, timestamp.date, description, link, icon, notificationTextFormatId, notificationTextParameters);
            }

            public class TitleResponse {
                @SerializedName("full")
                public String full;
                @SerializedName("namespace")
                public String namespace;
                @SerializedName("namespace-key")
                public String namespaceKey;
                @SerializedName("text")
                public String text;
            }

            public class TimestampResponse {
                @SerializedName("utciso8601")
                public String utciso8601;
                @SerializedName("utcunix")
                public String utcunix;
                @SerializedName("unix")
                public String unix;
                @SerializedName("utcmw")
                public String utcmw;
                @SerializedName("mw")
                public String mw;
                @SerializedName("date")
                public String date;
            }

            public class AgentResponse {
                @SerializedName("id")
                public long id;
                @SerializedName("name")
                public String name;
            }

            public class ContentResponse {
                @SerializedName("header")
                public String header;
                @SerializedName("compactHeader")
                public String compactHeader;
                @SerializedName("body")
                public String body;
                @SerializedName("icon")
                public String icon;
                @SerializedName("iconUrl")
                public String iconUrl;
                @SerializedName("links")
                public Map<String, Object> links;

                public boolean hasBody() {
                    return body != null && !body.trim().isEmpty();
                }

                public boolean hasPrimaryLink() {
                    return links != null && links.get("primary") != null && links.get("primary") instanceof Map;
                }

                @SuppressWarnings("unchecked")
                public String getPrimaryLink() {
                    return hasPrimaryLink() ? (String) ((Map<String, Object>)links.get("primary")).get("url") : "";
                }
            }
        }
    }

    public class UserInfoResponse {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;

        @Override
        public String toString() {
            return "UserInfoResponse{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public class PageResponse {
        @SerializedName("ns")
        public String ns;
        @SerializedName("pageid")
        public String pageId;
        @SerializedName("missing")
        public String missing;
        @SerializedName("known")
        public String known;
        @SerializedName("title")
        public String title;
        @SerializedName("imagerepository")
        public String imageRepository;
        @SerializedName("imageinfo")
        public List<ImageInfo> imageInfo;
        @SerializedName("revisions")
        public List<Revision> revisions;

        public int imageInfoCount() {
            return imageInfo != null ? imageInfo.size() : 0;
        }

        public String thumbUrl() {
            return imageInfo != null && imageInfo.size() > 0
                    ? imageInfo.get(0).thumbUrl
                    : "";
        }

        public String wikiContent() {
            return revisions != null && revisions.size() > 0 ? revisions.get(0).content : "";
        }

        public class Revision {
            @SerializedName("contentformat")
            public String contentFormat;
            @SerializedName("contentmodel")
            public String contentModel;
            @SerializedName("*")
            public String content;
        }
    }

    public class ImageInfo {
        @SerializedName("ns")
        public String ns;
        @SerializedName("title")
        public String title;
        @SerializedName("name")
        public String name;
        @SerializedName("timestamp")
        public String timestamp;
        @SerializedName("user")
        public String user;
        @SerializedName("url")
        public String url;
        @SerializedName("thumburl")
        public String thumbUrl;
        @SerializedName("thumbwidth")
        public int thumbWidth;
        @SerializedName("thumbheight")
        public int thumbHeight;
        @SerializedName("descriptionUrl")
        public String descriptionurl;
        @SerializedName("descriptionshorturl")
        public String descriptionShortUrl;
    }

    public class SearchInfoResponse {
        @SerializedName("searchinfo")
        public SearchInfo searchInfo;

        public class SearchInfo {
            @SerializedName("totalhits")
            int totalhits;
        }
    }

    public class SearchResult {
        private static final String CATEGORY_PREFIX = "Category:";

        @SerializedName("ns")
        public String ns;
        @SerializedName("title")
        public String title;
        @SerializedName("size")
        public int size;
        @SerializedName("wordcount")
        public int wordcount;
        @SerializedName("snippet")
        public String snippet;
        @SerializedName("timestamp")
        public String timestamp;

        public String getCategory() {
            if (title == null || !title.startsWith(CATEGORY_PREFIX)) {
                return null;
            }

            return title.substring(CATEGORY_PREFIX.length(), title.length());
        }
    }

    public class LogEventResponse {
        @SerializedName("logid")
        public String logId;
        @SerializedName("ns")
        public String ns;
        @SerializedName("title")
        public String title;
        @SerializedName("pageid")
        public String pageId;
        @SerializedName("logpage")
        public String logPage;
        @SerializedName("timestamp")
        public String timestamp;

        @Override
        public String toString() {
            return "LogEventResponse{" +
                    "logId='" + logId + '\'' +
                    ", ns='" + ns + '\'' +
                    ", title='" + title + '\'' +
                    ", pageId='" + pageId + '\'' +
                    ", logPage='" + logPage + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    '}';
        }
    }
}
