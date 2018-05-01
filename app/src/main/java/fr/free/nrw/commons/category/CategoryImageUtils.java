package fr.free.nrw.commons.category;

import android.content.Context;

import org.jsoup.Jsoup;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import fr.free.nrw.commons.Media;
import timber.log.Timber;

public class CategoryImageUtils {

    public static List<Media> getMediaList(Context context, NodeList childNodes) {
        List<Media> categoryImages = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            categoryImages.add(getMediaFromPage(context, node));
        }

        return categoryImages;
    }

    private static Media getMediaFromPage(Context context, Node node) {
        Media media = new Media(null,
                getImageUrl(node),
                getFileName(node),
                getDescription(node),
                getDataLength(node),
                getDateCreated(node),
                getDateCreated(node),
                getCreator(node)
        );

        media.setLicense(getLicense(node));

        return media;
    }

    private static String getFileName(Node document) {
        Element element = (Element) document;
        return element.getAttribute("title");
    }

    private static String getDescription(Node document) {
        return getMetaDataValue(document, "ImageDescription");
    }

    private static String getLicense(Node document) {
        return getMetaDataValue(document, "License");
    }

    private static String getCreator(Node document) {
        String artist = getMetaDataValue(document, "Artist");
        if (artist != null) {
            return Jsoup.parse(artist).text();
        }
        return null;
    }

    private static Date getDateCreated(Node document) {
        String dateTime = getMetaDataValue(document, "DateTime");
        Timber.d("Date time is %s", dateTime);
        if (dateTime != null && !dateTime.equals("")) {
            return new Date(dateTime);
        }
        return new Date();
    }

    private static String getImageUrl(Node document) {
        Element element = (Element) getImageInfo(document);
        if (element != null) {
            return element.getAttribute("url");
        }
        return null;
    }

    private static long getDataLength(Node document) {
        Element element = (Element) document;
        if (element != null) {
            String length = element.getAttribute("length");
            if (length != null && !length.equals("")) {
                return Long.parseLong(length);
            }
        }
        return 0L;
    }

    private static String getMetaDataValue(Node document, String metaName) {
        Element metaData = getMetaData(document, metaName);
        if (metaData != null) {
            return metaData.getAttribute("value");
        }
        return null;
    }

    @Nullable
    private static Element getMetaData(Node document, String metaName) {
        Node extraMetaData = getExtraMetaData(document);
        if (extraMetaData != null) {
            Node node = getNode(extraMetaData, metaName);
            if (node != null) {
                return (Element) node;
            }
        }
        return null;
    }

    @Nullable
    private static Node getExtraMetaData(Node document) {
        Node imageInfo = getImageInfo(document);
        if (imageInfo != null) {
            return getNode(imageInfo, "extmetadata");
        }
        return null;
    }

    @Nullable
    private static Node getImageInfo(Node document) {
        Node imageInfo = getNode(document, "imageInfo");
        if (imageInfo != null) {
            return getNode(imageInfo, "ii");
        }
        return null;
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
