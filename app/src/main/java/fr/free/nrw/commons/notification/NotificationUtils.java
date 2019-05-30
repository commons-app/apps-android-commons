package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;

import static fr.free.nrw.commons.notification.NotificationType.UNKNOWN;

public class NotificationUtils {

    private static final String COMMONS_WIKI = "commonswiki";
    private static final String WIKIDATA_WIKI = "wikidatawiki";
    private static final String WIKIPEDIA_WIKI = "enwiki";

    /**
     * Returns true if the wiki attribute corresponds to commonswiki
     * @param document
     * @return boolean representing whether the wiki attribute corresponds to commonswiki
     */
    public static boolean isCommonsNotification(Node document) {
        if (document == null || !document.hasAttributes()) {
            return false;
        }
        Element element = (Element) document;
        return COMMONS_WIKI.equals(element.getAttribute("wiki"));
    }

    /**
     * Returns true if the wiki attribute corresponds to wikidatawiki
     * @param document
     * @return boolean representing whether the wiki attribute corresponds to wikidatawiki
     */
    public static boolean isWikidataNotification(Node document) {
        if (document == null || !document.hasAttributes()) {
            return false;
        }
        Element element = (Element) document;
        return WIKIDATA_WIKI.equals(element.getAttribute("wiki"));
    }

    /**
     * Returns true if the wiki attribute corresponds to enwiki
     * @param document
     * @return
     */
    public static boolean isWikipediaNotification(Node document) {
        if (document == null || !document.hasAttributes()) {
            return false;
        }
        Element element = (Element) document;
        return WIKIPEDIA_WIKI.equals(element.getAttribute("wiki"));
    }

    /**
     * Returns document notification type
     * @param document
     * @return the document's NotificationType
     */
    public static NotificationType getNotificationType(Node document) {
        Element element = (Element) document;
        String type = element.getAttribute("type");
        return NotificationType.handledValueOf(type);
    }

    public static String getNotificationId(Node document) {
        Element element = (Element) document;
        return element.getAttribute("id");
    }

    public static List<Notification> getNotificationsFromBundle(Context context, Node document) {
        Element bundledNotifications = getBundledNotifications(document);
        NodeList childNodes = bundledNotifications.getChildNodes();

        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (isUsefulNotification(node)) {
                notifications.add(getNotificationFromApiResult(context, node));
            }
        }
        return notifications;
    }

    @NonNull
    public static List<Notification> getNotificationsFromList(Context context, NodeList childNodes) {
        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (isUsefulNotification(node)) {
                if (isBundledNotification(node)) {
                    notifications.addAll(getNotificationsFromBundle(context, node));
                } else {
                    notifications.add(getNotificationFromApiResult(context, node));
                }
            }
        }

        return notifications;
    }

    /**
     * Currently the app is interested in showing notifications just from the following three wikis: commons, wikidata, wikipedia
     * This function returns true only if the notification belongs to any of the above wikis and is of a known notification type
     * @param node
     * @return whether a notification is from one of Commons, Wikidata or Wikipedia
     */
    private static boolean isUsefulNotification(Node node) {
        return (isCommonsNotification(node)
                || isWikidataNotification(node)
                || isWikipediaNotification(node))
                && !getNotificationType(node).equals(UNKNOWN);
    }

    public static boolean isBundledNotification(Node document) {
        Element bundleElement = getBundledNotifications(document);
        if (bundleElement == null) {
            return false;
        }

        return bundleElement.getChildNodes().getLength() > 0;
    }

    private static Element getBundledNotifications(Node document) {
        return (Element) getNode(document, "bundledNotifications");
    }

    public static Notification getNotificationFromApiResult(Context context, Node document) {
        NotificationType type = getNotificationType(document);

        String notificationText = "";
        String link = getPrimaryLink(document);
        String description = getNotificationDescription(document);
        String iconUrl = getNotificationIconUrl(document);

        switch (type) {
            case THANK_YOU_EDIT:
                notificationText = getThankYouEditDescription(document);
                break;
            case EDIT_USER_TALK:
                notificationText = getNotificationText(document);
                break;
            case MENTION:
                notificationText = getMentionMessage(context, document);
                description = getMentionDescription(document);
                break;
            case WELCOME:
                notificationText = getWelcomeMessage(context, document);
                break;
        }
        return new Notification(type, notificationText, getTimestamp(document), description, link, iconUrl, getTimestampWithYear(document),
                getNotificationId(document));
    }

    private static String getNotificationText(Node document) {
        String notificationBody = getNotificationBody(document);
        if (notificationBody == null || notificationBody.trim().equals("")) {
            return getNotificationHeader(document);
        }
        return notificationBody;
    }

    private static String getNotificationHeader(Node document) {
        Node body = getNode(getModel(document), "header");
        if (body != null) {
            String textContent = body.getTextContent();
            return textContent.replace("<strong>", "").replace("</strong>", "");
        } else {
            return "";
        }
    }

    private static String getNotificationBody(Node document) {
        Node body = getNode(getModel(document), "body");
        if (body != null) {
            String textContent = body.getTextContent();
            return textContent.replace("<strong>", "").replace("</strong>", "");
        } else {
            return "";
        }
    }

    private static String getMentionDescription(Node document) {
        Node body = getNode(getModel(document), "body");
        return body != null ? body.getTextContent() : "";
    }

    /**
     * Gets the header node returned in the XML document to form the description for thank you edits
     * @param document
     * @return
     */
    private static String getThankYouEditDescription(Node document) {
        Node body = getNode(getModel(document), "header");
        return body != null ? body.getTextContent() : "";
    }

    private static String getNotificationIconUrl(Node document) {
        String format = "%s%s";
        Node iconUrl = getNode(getModel(document), "iconUrl");
        if (iconUrl == null) {
            return null;
        } else {
            String url = iconUrl.getTextContent();
            return String.format(format, BuildConfig.COMMONS_URL, url);
        }
    }

    public static String getMentionMessage(Context context, Node document) {
        String format = context.getString(R.string.notifications_mention);
        return String.format(format, getAgent(document), getNotificationDescription(document));
    }

    @SuppressLint("StringFormatMatches")
    public static String getUserTalkMessage(Context context, Node document) {
        String format = context.getString(R.string.notifications_talk_page_message);
        return String.format(format, getAgent(document));
    }

    @SuppressLint("StringFormatInvalid")
    public static String getWelcomeMessage(Context context, Node document) {
        String welcomeMessageFormat = context.getString(R.string.notifications_welcome);
        return String.format(welcomeMessageFormat, getAgent(document));
    }

    private static String getPrimaryLink(Node document) {
        Node links = getNode(getModel(document), "links");
        Element primaryLink = (Element) getNode(links, "primary");
        if (primaryLink != null) {
            return primaryLink.getAttribute("url");
        }
        return "";
    }

    private static Node getModel(Node document) {
        return getNode(document, "_.2A.");
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

    private static String getTimestampWithYear(Node document) {
        Element timestampElement = (Element) getNode(document, "timestamp");
        if (timestampElement != null) {
            return timestampElement.getAttribute("utcunix");
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
