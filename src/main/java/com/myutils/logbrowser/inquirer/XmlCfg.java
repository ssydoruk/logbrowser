/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author ssydoruk
 */
public final class XmlCfg {

    private Document doc; // XML doc for the CFG
    private Element root;
    private long lastModified = 0;
    private final String xmlFile;

    public XmlCfg(String filePath) throws Exception {
        this.xmlFile = filePath;
        loadFile();
    }

    public ArrayList<Element> getLayouts() {
        ArrayList<Element> ret = new ArrayList<>();

        if (root != null) {
            NodeList nl = root.getChildNodes();
            inquirer.logger.debug("getLayouts " + nl);
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    inquirer.logger.debug("added layout " + i + " node " + nl.item(i));
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(i);
                        inquirer.logger.debug(el.getNodeName());
                        if (el.getNodeName().equalsIgnoreCase("MsgTypes")) {
                            AddSpecs(ret, el);
                        }
                    }
                }
            }
        }
        return ret;
    }

    private void AddSpecs(ArrayList<Element> l, Element el) {
        inquirer.logger.debug("AddSpec " + el);
        NodeList nl = el.getChildNodes();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element spec = (Element) nl.item(i);
                    if (spec.getNodeName().equalsIgnoreCase("spec")) {
                        l.add(spec);
                    }
                }
            }
        }
    }

    public String getXmlFile() {
        return xmlFile;
    }

    /**
     *
     * @return true if file was reloaded
     * @throws ParserConfigurationException
     * @throws Exception
     */
    public boolean loadFile() throws ParserConfigurationException, Exception {
        File fXmlFile = new File(xmlFile);
        if (lastModified != fXmlFile.lastModified()) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            try {

                try {
                    doc = dBuilder.parse(fXmlFile);
                } catch (SAXParseException ex) {
//                    inquirer.ExceptionHandler.handleException("Cannot parse [" + xmlFile + "], line: " + ex.getLineNumber() + " col: " + ex.getColumnNumber(), ex);
                    throw new Exception(ex);
                }
                //optional, but recommended
                //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                doc.getDocumentElement().normalize();

                root = doc.getDocumentElement();
                if (root == null || !root.getNodeName().equalsIgnoreCase("OutputSpec")) {
                    throw new Exception("bad root XML element: " + root);
                }

            } catch (Exception e) {
                throw e;
            }
            this.lastModified = fXmlFile.lastModified();
            return true;
        }
        return false;
    }
}
