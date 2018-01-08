package fr.free.nrw.commons.notification;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NotificationObject {
    public static final String TYPE_EDIT_USER_TALK = "edit-user-talk";
    public static final String TYPE_REVERTED = "reverted";
    public static final String TYPE_EDIT_THANK = "edit-thank";
    public static final String WIKIDATA_WIKI = "wikidatawiki";

    @SuppressWarnings("unused,NullableProblems") @NonNull
    private String wiki;
    @SuppressWarnings("unused") private int id;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String type;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String category;
    @SuppressWarnings("unused") private int revid;

    @SuppressWarnings("unused,NullableProblems") @NonNull private Title title;
    @SuppressWarnings("unused,NullableProblems") @NonNull private Agent agent;

    @NonNull public String wiki() {
        return wiki;
    }

    public int id() {
        return id;
    }

    @NonNull public String type() {
        return type;
    }

    @NonNull public Agent agent() {
        return agent;
    }

    @NonNull public Title title() {
        return title;
    }

    public int revID() {
        return revid;
    }

    public boolean isFromWikidata() {
        return wiki.equals(WIKIDATA_WIKI);
    }

    @Override public String toString() {
        return Integer.toString(id);
    }

    public static class Title {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String full;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String text;
        @SuppressWarnings("unused") @Nullable
        private String namespace;
        @SuppressWarnings("unused") @SerializedName("namespace-key") private int namespaceKey;

        @NonNull public String text() {
            return text;
        }

        @NonNull public String full() {
            return full;
        }

        public boolean isMainNamespace() {
            return namespaceKey == 0;
        }

        public void setFull(@NonNull String title) {
            full = title;
        }
    }

    public static class Agent {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String name;

        @NonNull public String name() {
            return name;
        }
    }

    public static class NotificationList {
        @SuppressWarnings("unused,NullableProblems") @NonNull private List<Notification> list;

        @NonNull public List<Notification> getNotifications() {
            return list;
        }
    }

    public static class QueryNotifications {
        @SuppressWarnings("unused,NullableProblems") @NonNull private NotificationList notifications;

        @NonNull public List<Notification> get() {
            return notifications.getNotifications();
        }
    }
}