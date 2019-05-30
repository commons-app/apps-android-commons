package fr.free.nrw.commons.category;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryImageUtils {

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
     * Extracts the filename of the uploaded image
     * @param document
     * @return
     */
    private static String getFileName(Node document) {
        Element element = (Element) document;
        return element.getAttribute("title");
    }

}
