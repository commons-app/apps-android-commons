package fr.free.nrw.commons.notification.models

enum class NotificationType(private val type: String) {
    THANK_YOU_EDIT("thank-you-edit"),

    EDIT_USER_TALK("edit-user-talk"),

    MENTION("mention"),

    EMAIL("email"),

    WELCOME("welcome"),

    UNKNOWN("unknown");

    // Getter for the type property
    fun getType(): String {
        return type
    }

    companion object {
        // Returns the corresponding NotificationType for a given name or UNKNOWN
        // if no match is found
        fun handledValueOf(name: String): NotificationType {
            for (e in values()) {
                if (e.type == name) {
                    return e
                }
            }
            return UNKNOWN
        }
    }
}
