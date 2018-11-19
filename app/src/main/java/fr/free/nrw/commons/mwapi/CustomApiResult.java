package fr.free.nrw.commons.mwapi;

import org.apache.http.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import in.yuvi.http.fluent.Http;
import timber.log.Timber;

public class CustomApiResult {
    private Node doc;
    private XPath evaluator;

    CustomApiResult(Node doc) {
        this.doc = doc;
        this.evaluator = XPathFactory.newInstance().newXPath();
    }

    static CustomApiResult fromRequestBuilder(Http.HttpRequestBuilder builder, HttpClient client) throws IOException {

        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(builder.use(client).charset("utf-8").data("format", "xml").asResponse().getEntity().getContent());
            printStringFromDocument(doc);
            return new CustomApiResult(doc);
        } catch (ParserConfigurationException e) {
            // I don't know wtf I can do about this on...
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            // So, this should never actually happen - since we assume MediaWiki always generates valid json
            // So the only thing causing this would be a network truncation
            // Sooo... I can throw IOError
            // Thanks Java, for making me spend significant time on shit that happens once in a bluemoon
            // I surely am writing Nuclear Submarine controller code
            throw new IOError(e);
        } catch (SAXException e) {
            // See Rant above
            throw new IOError(e);
        }
    }

    public static void printStringFromDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            Timber.d("API response is\n %s", writer.toString());
        }
        catch(TransformerException ex)
        {
            Timber.d("Error occurred in transforming", ex);
        }
    }

    public Node getDocument() {
        return doc;
    }

    public ArrayList<CustomApiResult> getNodes(String xpath) {
        try {
            ArrayList<CustomApiResult> results = new ArrayList<CustomApiResult>();
            NodeList nodes = (NodeList) evaluator.evaluate(xpath, doc, XPathConstants.NODESET);
            for(int i = 0; i < nodes.getLength(); i++) {
                results.add(new CustomApiResult(nodes.item(i)));
            }
            return results;
        } catch (XPathExpressionException e) {
            return null;
        }

    }
    public CustomApiResult getNode(String xpath) {
        try {
            return new CustomApiResult((Node) evaluator.evaluate(xpath, doc, XPathConstants.NODE));
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public Double getNumber(String xpath) {
        try {
            return (Double) evaluator.evaluate(xpath, doc, XPathConstants.NUMBER);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public String getString(String xpath) {
        try {
            return (String) evaluator.evaluate(xpath, doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
