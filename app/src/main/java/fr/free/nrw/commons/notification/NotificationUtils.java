package fr.free.nrw.commons.notification;

import android.content.Context;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;

public class NotificationUtils {

    private static final String COMMONS_WIKI = "commonswiki";

    public static boolean isCommonsNotification(Node document) {
        if (document == null || !document.hasAttributes()) {
            return false;
        }
        Element element = (Element) document;
        return COMMONS_WIKI.equals(element.getAttribute("wiki"));
    }

    public static NotificationType getNotificationType(Node document) {
        Element element = (Element) document;
        String type = element.getAttribute("type");
        return NotificationType.handledValueOf(type);
    }

    public static Notification getNotificationFromApiResult(Context context, Node document) {
        NotificationType type = getNotificationType(document);

        String notificationText = "";
        String link = getNotificationLink(document);
        String description = getNotificationDescription(document);
        switch (type) {
            case THANK_YOU_EDIT:
                notificationText = context.getString(R.string.notifications_thank_you_edit);
                break;
            case EDIT_USER_TALK:
                notificationText = getUserTalkMessage(context, document);
                break;
            case MENTION:
                notificationText = getMentionMessage(context, document);
                break;
            case WELCOME:
                notificationText = getWelcomeMessage(context, document);
                break;
        }
        return new Notification(type, notificationText, getTimestamp(document), description, link);
    }

    public static String getMentionMessage(Context context, Node document) {
        String format = context.getString(R.string.notifications_mention);
        return String.format(format, getAgent(document), getNotificationDescription(document));
    }

    public static String getUserTalkMessage(Context context, Node document) {
        String format = context.getString(R.string.notifications_talk_page_message);
        return String.format(format, getAgent(document));
    }

    public static String getWelcomeMessage(Context context, Node document) {
        String welcomeMessageFormat = context.getString(R.string.notifications_welcome);
        return String.format(welcomeMessageFormat, getAgent(document));
    }

    private static String getAgent(Node document) {
        Element agentElement = (Element) getNode(document, "agent");
        if (agentElement != null) {
            return agentElement.getAttribute("name");
        }
        return "";
    }

    private static String getTimestamp(Node document) {
        Element timestampElement = (Element) getNode(document, "timestamp");
        if (timestampElement != null) {
            return timestampElement.getAttribute("date");
        }
        return "";
    }

    private static String getNotificationLink(Node document) {
        String format = "%s%s";
        Element titleElement = (Element) getNode(document, "title");
        if (titleElement != null) {
            String fullName = titleElement.getAttribute("full");
            return String.format(format, BuildConfig.HOME_URL, fullName);
        }
        return "";
    }

    private static String getNotificationDescription(Node document) {
        Element titleElement = (Element) getNode(document, "title");
        if (titleElement != null) {
            return titleElement.getAttribute("text");
        }
        return "";
    }

    @Nullable
    public static Node getNode(Node node, String nodeName) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node nodeItem = childNodes.item(i);
            Element item = (Element) nodeItem;
            if (item.getTagName().equals(nodeName)) {
                return nodeItem;
            }
        }
        return null;
    }
}
