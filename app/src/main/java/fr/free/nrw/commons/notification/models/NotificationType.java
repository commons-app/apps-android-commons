package fr.free.nrw.commons.notification.models;

public enum NotificationType {
    THANK_YOU_EDIT("thank-you-edit"),
    EDIT_USER_TALK("edit-user-talk"),
    MENTION("mention"),
    WELCOME("welcome"),
    UNKNOWN("unknown");
    private String type;

    NotificationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static NotificationType handledValueOf(String name) {
        for (NotificationType e : values()) {
            if (e.getType().equals(name)) {
                return e;
            }
        }
        return UNKNOWN;
    }
}
