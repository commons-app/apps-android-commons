package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.model.BaseModel;
import org.wikipedia.notifications.Notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MwQueryResult extends BaseModel implements PostProcessingTypeAdapter.PostProcessable {
    @SerializedName("pages") @Nullable private List<MwQueryPage> pages;
    @Nullable private List<Redirect> redirects;
    @Nullable private List<ConvertedTitle> converted;
    @SerializedName("userinfo") private UserInfo userInfo;
    @Nullable private List<ListUserResponse> users;
    @Nullable private Tokens tokens;
    @Nullable private NotificationList notifications;
    @SerializedName("allimages") @Nullable private List<ImageDetails> allImages;

    @Nullable public List<MwQueryPage> pages() {
        return pages;
    }

    @Nullable public MwQueryPage firstPage() {
        if (pages != null && pages.size() > 0) {
            return pages.get(0);
        }
        return null;
    }

    @NonNull
    public List<ImageDetails> allImages() {
        return allImages == null ? Collections.emptyList() : allImages;
    }

    @Nullable public UserInfo userInfo() {
        return userInfo;
    }

    @Nullable public String csrfToken() {
        return tokens != null ? tokens.csrf() : null;
    }

    @Nullable public String loginToken() {
        return tokens != null ? tokens.login() : null;
    }

    @Nullable public NotificationList notifications() {
        return notifications;
    }

    @Nullable public ListUserResponse getUserResponse(@NonNull String userName) {
        if (users != null) {
            for (ListUserResponse user : users) {
                // MediaWiki user names are case sensitive, but the first letter is always capitalized.
                if (StringUtils.capitalize(userName).equals(user.name())) {
                    return user;
                }
            }
        }
        return null;
    }

    @NonNull public Map<String, ImageInfo> images() {
        Map<String, ImageInfo> result = new HashMap<>();
        if (pages != null) {
            for (MwQueryPage page : pages) {
                if (page.imageInfo() != null) {
                    result.put(page.title(), page.imageInfo());
                }
            }
        }
        return result;
    }

    @Override
    public void postProcess() {
        resolveConvertedTitles();
        resolveRedirectedTitles();
    }

    private void resolveRedirectedTitles() {
        if (redirects == null || pages == null) {
            return;
        }
        for (MwQueryPage page : pages) {
            for (MwQueryResult.Redirect redirect : redirects) {
                // TODO: Looks like result pages and redirects can also be matched on the "index"
                // property.  Confirm in the API docs and consider updating.
                if (page.title().equals(redirect.to())) {
                    page.redirectFrom(redirect.from());
                    if (redirect.toFragment() != null) {
                        page.appendTitleFragment(redirect.toFragment());
                    }
                }
            }
        }
    }

    private void resolveConvertedTitles() {
        if (converted == null || pages == null) {
            return;
        }
        // noinspection ConstantConditions
        for (MwQueryResult.ConvertedTitle convertedTitle : converted) {
            // noinspection ConstantConditions
            for (MwQueryPage page : pages) {
                if (page.title().equals(convertedTitle.to())) {
                    page.convertedFrom(convertedTitle.from());
                    page.convertedTo(convertedTitle.to());
                }
            }
        }
    }

    private static class Redirect {
         private int index;
         @Nullable private String from;
         @Nullable private String to;
         @SerializedName("tofragment") @Nullable private String toFragment;

        @Nullable public String to() {
            return to;
        }

        @Nullable public String from() {
            return from;
        }

        @Nullable public String toFragment() {
            return toFragment;
        }
    }

    public static class ConvertedTitle {
         @Nullable private String from;
         @Nullable private String to;

        @Nullable public String to() {
            return to;
        }

        @Nullable public String from() {
            return from;
        }
    }

    private static class Tokens {
        @SuppressWarnings("unused,NullableProblems") @SerializedName("csrftoken")
        @Nullable private String csrf;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("createaccounttoken")
        @Nullable private String createAccount;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("logintoken")
        @Nullable private String login;

        @Nullable private String csrf() {
            return csrf;
        }

        @Nullable private String createAccount() {
            return createAccount;
        }

        @Nullable private String login() {
            return login;
        }
    }

    public static class NotificationList {
        @Nullable
        private List<Notification> list;
        @Nullable
        public List<Notification> list() {
            return list;
        }
    }
}
