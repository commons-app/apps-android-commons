package fr.free.nrw.commons.mwapi.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.utils.StringUtils;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    @SuppressWarnings("unused")
    @Nullable
    private String code;
    @SuppressWarnings("unused")
    @Nullable
    private String info;
    @SuppressWarnings("unused")
    @Nullable
    private String docref;
    @SuppressWarnings("unused")
    @NonNull
    private List<Message> messages = Collections.emptyList();

    @Override
    @NonNull
    public String getTitle() {
        return StringUtils.defaultString(code);
    }

    @Override
    @NonNull
    public String getDetails() {
        return StringUtils.defaultString(info);
    }

    @Nullable
    public String getDocRef() {
        return docref;
    }

    public boolean badToken() {
        return "badtoken".equals(code);
    }

    public boolean badLoginState() {
        return "assertuserfailed".equals(code);
    }

    public boolean hasMessageName(@NonNull String messageName) {
        for (Message msg : messages) {
            if (messageName.equals(msg.name)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public String getMessageHtml(@NonNull String messageName) {
        for (Message msg : messages) {
            if (messageName.equals(msg.name)) {
                return msg.html();
            }
        }
        return null;
    }

    private static final class Message {
        @SuppressWarnings("unused")
        @Nullable
        private String name;
        @SuppressWarnings("unused")
        @Nullable
        private String html;

        @NonNull
        private String html() {
            return StringUtils.defaultString(html);
        }
    }
}