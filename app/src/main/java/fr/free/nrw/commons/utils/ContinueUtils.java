package fr.free.nrw.commons.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import fr.free.nrw.commons.category.QueryContinue;

public class ContinueUtils {

    public static QueryContinue getQueryContinue(Node document) {
        Element continueElement = (Element) document;
        return new QueryContinue(continueElement.getAttribute("continue"),
                continueElement.getAttribute("gcmcontinue"));
    }
}
