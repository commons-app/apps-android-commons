/* Copyright (C) 2012 Yuvi Panda <yuvipanda@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.free.nrw.commons.libs.mediawiki_api;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.http.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.free.nrw.commons.libs.http_fluent.Http.HttpRequestBuilder;

public class ApiResult {
    private Node doc;
    private XPath evaluator;

    ApiResult(Node doc) {
        this.doc = doc;
        this.evaluator = XPathFactory.newInstance().newXPath();
    }

    static ApiResult fromRequestBuilder(HttpRequestBuilder builder, HttpClient client) throws IOException {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(builder.use(client).charset("utf-8").data("format", "xml").asResponse().getEntity().getContent());
            return new ApiResult(doc);
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
    public Node getDocument() {
        return doc;
    }

    public ArrayList<ApiResult> getNodes(String xpath) {
        try {
            ArrayList<ApiResult> results = new ArrayList<ApiResult>();
            NodeList nodes = (NodeList) evaluator.evaluate(xpath, doc, XPathConstants.NODESET);
            for(int i = 0; i < nodes.getLength(); i++) {
                results.add(new ApiResult(nodes.item(i)));
            }
            return results;
        } catch (XPathExpressionException e) {
            return null;
        }
        
    }
    public ApiResult getNode(String xpath) {
        try {
            return new ApiResult((Node) evaluator.evaluate(xpath, doc, XPathConstants.NODE));
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
