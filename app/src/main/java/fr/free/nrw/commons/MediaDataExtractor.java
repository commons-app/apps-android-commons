package fr.free.nrw.commons;

import android.util.Log;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Fetch additional media data from the network that we don't store locally.
 *
 * This includes things like category lists and multilingual descriptions,
 * which are not intrinsic to the media and may change due to editing.
 */
public class MediaDataExtractor {
    private boolean fetched;
    private boolean processed;

    private String filename;
    private ArrayList<String> categories;
    private Map<String, String> descriptions;
    private String author;
    private Date date;
    private String license;
    private LicenseList licenseList;

    /**
     * @param filename of the target media object, should include 'File:' prefix
     */
    public MediaDataExtractor(String filename, LicenseList licenseList) {
        this.filename = filename;
        categories = new ArrayList<>();
        descriptions = new HashMap<>();
        fetched = false;
        processed = false;
        this.licenseList = licenseList;
    }

    /**
     * Actually fetch the data over the network.
     * todo: use local caching?
     *
     * Warning: synchronous i/o, call on a background thread
     */
    public void fetch() throws IOException {
        if (fetched) {
            throw new IllegalStateException("Tried to call MediaDataExtractor.fetch() again.");
        }

        MWApi api = CommonsApplication.createMWApi();
        ApiResult result = api.action("query")
                .param("prop", "revisions")
                .param("titles", filename)
                .param("rvprop", "content")
                .param("rvlimit", 1)
                .param("rvgeneratexml", 1)
                .get();

        processResult(result);
        fetched = true;
    }

    private void processResult(ApiResult result) throws IOException {

        String wikiSource = result.getString("/api/query/pages/page/revisions/rev");
        String parseTreeXmlSource = result.getString("/api/query/pages/page/revisions/rev/@parsetree");

        // In-page category links are extracted from source, as XML doesn't cover [[links]]
        extractCategories(wikiSource);

        // Description template info is extracted from preprocessor XML
        processWikiParseTree(parseTreeXmlSource);
    }

    /**
     * We could fetch all category links from API, but we actually only want the ones
     * directly in the page source so they're editable. In the future this may change.
     *
     * @param source wikitext source code
     */
    private void extractCategories(String source) {
        Pattern regex = Pattern.compile("\\[\\[\\s*Category\\s*:([^]]*)\\s*\\]\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(source);
        while (matcher.find()) {
            String cat = matcher.group(1).trim();
            categories.add(cat);
        }
    }

    private void processWikiParseTree(String source) throws IOException {
        Document doc;
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.parse(new ByteArrayInputStream(source.getBytes("UTF-8")));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        Node templateNode = findTemplate(doc.getDocumentElement(), "information");
        if (templateNode != null) {
            Node descriptionNode = findTemplateParameter(templateNode, "description");
            descriptions = getMultilingualText(descriptionNode);

            Node authorNode = findTemplateParameter(templateNode, "author");
            author = getFlatText(authorNode);
        }

        /*
        Pull up the license data list...
        look for the templates in two ways:
            * look for 'self' template and check its first parameter
            * if none, look for any of the known templates
         */
        Log.d("Commons", "MediaDataExtractor searching for license");
        Node selfLicenseNode = findTemplate(doc.getDocumentElement(), "self");
        if (selfLicenseNode != null) {
            Node firstNode = findTemplateParameter(selfLicenseNode, 1);
            String licenseTemplate = getFlatText(firstNode);
            License license = licenseList.licenseForTemplate(licenseTemplate);
            if (license == null) {
                Log.d("Commons", "MediaDataExtractor found no matching license for self parameter: " + licenseTemplate + "; faking it");
                this.license = licenseTemplate; // hack hack! For non-selectable licenses that are still in the system.
            } else {
                // fixme: record the self-ness in here too... sigh
                // all this needs better server-side metadata
                this.license = license.getKey();
                Log.d("Commons", "MediaDataExtractor found self-license " + this.license);
            }
        } else {
            for (License license : licenseList.values()) {
                String templateName = license.getTemplate();
                Node template = findTemplate(doc.getDocumentElement(), templateName);
                if (template != null) {
                    // Found!
                    this.license = license.getKey();
                    Log.d("Commons", "MediaDataExtractor found non-self license " + this.license);
                    break;
                }
            }
        }
    }

    private Node findTemplate(Element parentNode, String title) throws IOException {
        String ucTitle= Utils.capitalize(title);
        NodeList nodes = parentNode.getChildNodes();
        for (int i = 0, length = nodes.getLength(); i < length; i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("template")) {
                String foundTitle = getTemplateTitle(node);
                if (Utils.capitalize(foundTitle).equals(ucTitle)) {
                    return node;
                }
            }
        }
        return null;
    }

    private String getTemplateTitle(Node templateNode) throws IOException {
        NodeList nodes = templateNode.getChildNodes();
        for (int i = 0, length = nodes.getLength(); i < length; i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("title")) {
                return node.getTextContent().trim();
            }
        }
        throw new IOException("Template has no title element.");
    }

    private static abstract class TemplateChildNodeComparator {
        abstract public boolean match(Node node);
    }

    private Node findTemplateParameter(Node templateNode, String name) throws IOException {
        final String theName = name;
        return findTemplateParameter(templateNode, new TemplateChildNodeComparator() {
            @Override
            public boolean match(Node node) {
                return (Utils.capitalize(node.getTextContent().trim()).equals(Utils.capitalize(theName)));
            }
        });
    }

    private Node findTemplateParameter(Node templateNode, int index) throws IOException {
        final String theIndex = "" + index;
        return findTemplateParameter(templateNode, new TemplateChildNodeComparator() {
            @Override
            public boolean match(Node node) {
                Element el = (Element)node;
                if (el.getTextContent().trim().equals(theIndex)) {
                    return true;
                } else if (el.getAttribute("index") != null && el.getAttribute("index").trim().equals(theIndex)) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private Node findTemplateParameter(Node templateNode, TemplateChildNodeComparator comparator) throws IOException {
        NodeList nodes = templateNode.getChildNodes();
        for (int i = 0, length = nodes.getLength(); i < length; i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("part")) {
                NodeList childNodes = node.getChildNodes();
                for (int j = 0, childNodesLength = childNodes.getLength(); j < childNodesLength; j++) {
                    Node childNode = childNodes.item(j);
                    if (childNode.getNodeName().equals("name") && comparator.match(childNode)) {
                        // yay! Now fetch the value node.
                        for (int k = j + 1; k < childNodesLength; k++) {
                            Node siblingNode = childNodes.item(k);
                            if (siblingNode.getNodeName().equals("value")) {
                                return siblingNode;
                            }
                        }
                        throw new IOException("No value node found for matched template parameter.");
                    }
                }
            }
        }
        throw new IOException("No matching template parameter node found.");
    }

    private String getFlatText(Node parentNode) throws IOException {
        return parentNode.getTextContent();
    }

    // Extract a dictionary of multilingual texts from a subset of the parse tree.
    // Texts are wrapped in things like {{en|foo} or {{en|1=foo bar}}.
    // Text outside those wrappers is stuffed into a 'default' faux language key if present.
    private Map<String, String> getMultilingualText(Node parentNode) throws IOException {
        Map<String, String> texts = new HashMap<>();
        StringBuilder localText = new StringBuilder();

        NodeList nodes = parentNode.getChildNodes();
        for (int i = 0, length = nodes.getLength(); i < length; i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("template")) {
                // process a template node
                String title = getTemplateTitle(node);
                if (title.length() < 3) {
                    // Hopefully a language code. Nasty hack!
                    String lang = title;
                    Node valueNode = findTemplateParameter(node, 1);
                    String value = valueNode.getTextContent(); // hope there's no subtemplates or formatting for now
                    texts.put(lang, value);
                }
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                localText.append(node.getTextContent());
            }
        }

        // Some descriptions don't list multilingual variants
        String defaultText = localText.toString().trim();
        if (defaultText.length() > 0) {
            texts.put("default", localText.toString());
        }
        return texts;
    }

    /**
     * Take our metadata and inject it into a live Media object.
     * Media object might contain stale or cached data, or emptiness.
     * @param media
     */
    public void fill(Media media) {
        if (!fetched) {
            throw new IllegalStateException("Tried to call MediaDataExtractor.fill() before fetch().");
        }

        media.setCategories(categories);
        media.setDescriptions(descriptions);
        if (license != null) {
            media.setLicense(license);
        }

        // add author, date, etc fields
    }
}
