package fr.free.nrw.commons.category;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.util.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import fr.free.nrw.commons.Media;
import timber.log.Timber;

public class CategoryImageUtils {

    /**
     * The method iterates over the child nodes to return a list of Media objects
     * @param childNodes
     * @return
     */
    public static List<Media> getMediaList(NodeList childNodes) {
        List<Media> categoryImages = new ArrayList<>();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (getFileName(node).substring(0, 5).equals("File:")) {
                categoryImages.add(getMediaFromPage(node));
            }
        }

        return categoryImages;
    }

    /**
     * The method iterates over the child nodes to return a list of Subcategory name
     * sorted alphabetically
     * @param childNodes
     * @return
     */
    public static List<String> getSubCategoryList(NodeList childNodes) {
        List<String> subCategories = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            subCategories.add(getFileName(node));
        }
        Collections.sort(subCategories);
        return subCategories;
    }

    /**
     * Creates a new Media object from the XML response as received by the API
     * @param node
     * @return
     */
    public static Media getMediaFromPage(Node node) {
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

    /**
     * Extracts the filename of the uploaded image
     * @param document
     * @return
     */
    private static String getFileName(Node document) {
        Element element = (Element) document;
        return element.getAttribute("title");
    }

    /**
     * Extracts the image description for that particular upload
     * @param document
     * @return
     */
    private static String getDescription(Node document) {
        return getMetaDataValue(document, "ImageDescription");
    }

    /**
     * Extracts license information from the image meta data
     * @param document
     * @return
     */
    private static String getLicense(Node document) {
        return getMetaDataValue(document, "License");
    }

    /**
     * Returns the parsed value of artist from the response
     * The artist information is returned as a HTML string from the API. Using HTML parser to parse the HTML
     * @param document
     * @return
     */
    @NonNull
    private static String getCreator(Node document) {
        String artist = getMetaDataValue(document, "Artist");
        if (StringUtils.isBlank(artist)) {
            return "";
        }
        return StringUtil.fromHtml(artist).toString();
    }

    /**
     * Returns the parsed date of creation of the image
     * @param document
     * @return
     */
    private static Date getDateCreated(Node document) {
        String dateTime = getMetaDataValue(document, "DateTime");
        if (dateTime != null && !dateTime.equals("")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                return format.parse(dateTime);
            } catch (ParseException e) {
                Timber.d("Error occurred while parsing date %s", dateTime);
                return new Date();
            }
        }
        return new Date();
    }

    /**
     * @param document
     * @return Returns the url attribute from the imageInfo node
     */
    private static String getImageUrl(Node document) {
        Element element = (Element) getImageInfo(document);
        if (element != null) {
            return element.getAttribute("url");
        }
        return null;
    }

    /**
     * Takes the node document and gives out the attribute length from the node document
     * @param document
     * @return
     */
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

    /**
     * Generic method to get the value of any meta as returned by the getMetaData function
     * @param document node document as returned by API
     * @param metaName the name of meta node to be returned
     * @return
     */
    private static String getMetaDataValue(Node document, String metaName) {
        Element metaData = getMetaData(document, metaName);
        if (metaData != null) {
            return metaData.getAttribute("value");
        }
        return null;
    }

    /**
     * Generic method to return an element taking the node document and metaName as input
     * @param document node document as returned by API
     * @param metaName the name of meta node to be returned
     * @return
     */
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

    /**
     * Extracts extmetadata from the response XML
     * @param document
     * @return
     */
    @Nullable
    private static Node getExtraMetaData(Node document) {
        Node imageInfo = getImageInfo(document);
        if (imageInfo != null) {
            return getNode(imageInfo, "extmetadata");
        }
        return null;
    }

    /**
     * Extracts the ii node from the imageinfo node
     * @param document
     * @return
     */
    @Nullable
    private static Node getImageInfo(Node document) {
        Node imageInfo = getNode(document, "imageinfo");
        if (imageInfo != null) {
            return getNode(imageInfo, "ii");
        }
        return null;
    }

    /**
     * Takes a parent node as input and returns a child node if present
     * @param node parent node
     * @param nodeName child node name
     * @return
     */
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
