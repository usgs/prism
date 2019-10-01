/*******************************************************************************
 * Name: Java class PrismXMLReader.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * Written by: Jeanne Jones, USGS, jmjones@usgs.gov
 * 
 * This software is in the public domain because it contains materials that 
 * originally came from the United States Geological Survey, an agency of the 
 * United States Department of Interior. For more information, see the official 
 * USGS copyright policy at 
 * https://www.usgs.gov/information-policies-and-instructions/copyrights-and-credits#copyright
 * 
 * This software has been approved for release by the U.S. Geological Survey (USGS). 
 * Although the software has been subjected to rigorous review, the USGS reserves 
 * the right to update the software as needed pursuant to further analysis and 
 * review. No warranty, expressed or implied, is made by the USGS or the U.S. 
 * Government as to the functionality of the software and related material nor 
 * shall the fact of release constitute any such warranty. Furthermore, the 
 * software is released on condition that neither the USGS nor the U.S. Government 
 * shall be held liable for any damages resulting from its authorized or unauthorized use.
 * 
 * Version 1.0 release: Feb. 2015
 * Version 2.0 release: Oct. 2019
 ******************************************************************************/
package SmUtilities;

import static SmConstants.SmConfigConstants.CONFIG_XSD_VALIDATOR;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is a customized XML parser for the PRISM Strong Motion project.
 * It only looks at the document object part of the xml file, and only checks for
 * element and text nodes.  It uses recursion to walk each unique path to each
 * text element and makes a tag trail of the element tags along the way.  It
 * builds the tag path by concatenating tag names separated by "/" (forward slash).
 * The actual text value is appended at the end of this string using the 
 * separator "///".  When all text entries have been discovered and their paths
 * saved, each tag trail is separated into a key (all the element tag names separated
 * by "/") and a value (the text value), and put into the config reader object
 * as a key-value pair.
 * Example: PRISM/DataUnitsForCountConversion/DataUnitsCode/DataUnitCode///04
 * would get stored as 
 * key: PRISM/DataUnitsForCountConversion/DataUnitsCode/DataUnitCode
 * value: 04
 * @author jmjones
 */
public class PrismXMLReader {
    private final boolean ignoreWhitespace = true;
    private final boolean ignoreComments = true;
    private final boolean putCDATAIntoText = true;
    private final boolean createEntityRefs = true;
    private final DocumentBuilderFactory dbFactory;
    private final DocumentBuilder dBuilder;
/**
 * This constructor for PrismXMLReader creates a document builder factory and a new
 * document builder using configuration parameters to ignore white space and comments.
 * @throws ParserConfigurationException if unable to create the builder
 */    
    public PrismXMLReader() throws ParserConfigurationException {
        //Set up and configure a document builder
        dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setIgnoringComments(ignoreComments);
        dbFactory.setIgnoringElementContentWhitespace(ignoreWhitespace);
        dbFactory.setExpandEntityReferences(createEntityRefs);
        dbFactory.setCoalescing(!putCDATAIntoText);
        dBuilder = dbFactory.newDocumentBuilder();
    }
/**
 * This method reads in the xml file given in the filename AS STRING
 * @param filename The xml file to read and parse
 * @throws IOException if unable to read in the file
 * @throws ParserConfigurationException if parser configuration is incorrect
 * @throws SAXException if unable to correctly parse the xml
 */
    public void readFile( String filename ) throws IOException, 
                                    ParserConfigurationException,SAXException {

        Document doc = dBuilder.parse(new File(filename));
        parseFileContents(doc);
    }
/**
 * This method reads in the xml file given in the filename AS INPUTSTREAM
 * @param ins The xml file to read and parse, as input stream
 * @throws IOException if unable to read in the file
 * @throws SAXException if unable to correctly parse the xml
 */
    public void readFile( InputStream ins ) throws IOException,SAXException {
        
        Document doc = dBuilder.parse(ins);
        parseFileContents(doc);
    }
/**
 * This method validates the document, pulls out the unique
 * paths to each text node, and enters each key-value pair in the config reader
 * object.
 * @param doc The xml file to parse
 * @throws IOException if unable to read in the file
 */
    private void parseFileContents( Document doc ) throws IOException {
        ArrayList<String> tagtrail = new ArrayList<>();
        String[] keyvalue;
        
        //Parse the xml document directly from the file
        validateXMLSchema(CONFIG_XSD_VALIDATOR, doc);
        Element top = doc.getDocumentElement();
        String home = top.getTagName();
        
        //Walk the paths until each text node is reached
        findSubNode(home, top, tagtrail);
        
        //Separate each tag trail into a key, value pair and enter into the 
        //config reader
        ConfigReader config = ConfigReader.INSTANCE;
        for (String each : tagtrail) {
            keyvalue = each.split("///");
            config.setConfigValue(keyvalue[0], keyvalue[1]);
        }
        tagtrail.clear();
    }
/**
 * Recursive method to find all the child nodes of each node until a text node
 * is found.  At each level the tag name is appended to the tag path to keep track
 * of the order that nodes are traversed.
 * @param name The current tag path, starting at the document node
 * @param inNode The current node being traversed
 * @param trail The array list to hold each tag trail as it is found
 */
    private void findSubNode( String name, Node inNode, ArrayList<String> trail) {
        StringBuilder result = new StringBuilder();
        String value;
        
        //Skip the node if it has no children
        if ( inNode.hasChildNodes()) {
            NodeList list = inNode.getChildNodes();
            
            //Check each subnode of the current node
            for (int i=0; i<list.getLength(); i++) {
                Node subnode = list.item(i);
                result.setLength(0);
                
                //If it's an element node, continue walking down after saving 
                //the current node name in the key string being built.
                if (subnode.getNodeType() == Node.ELEMENT_NODE) {
                    result.append(name).append("/").append(subnode.getNodeName());
                    findSubNode(result.toString(), subnode, trail);
                } 
                
                //If it's a text node, it's the end of the line - append
                //the text entry as the value in the key-value pair.
                else if (subnode.getNodeType() == Node.TEXT_NODE) {
                    value = subnode.getTextContent().trim();
                    if (!value.isEmpty()) {
                        trail.add(result.append(name).append("///")
                                                    .append(value).toString());
                    }
                }
            }
        }
    }
/**
 * Performs validation on the configuration xml file using an xsd file
 * @param xsdname the name of the xsd file for validation
 * @param domdoc the configuration file, parsed into a document object
 * @return true if xml validated successfully
 * @throws IOException if a validation error occurred
 */
    public boolean validateXMLSchema(String xsdname, Document domdoc) throws IOException  {
            boolean test = true;
            try {
                URL xsdurl = PrismXMLReader.class.getResource(xsdname);
                SchemaFactory factory = SchemaFactory.newInstance(
                                            XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(xsdurl);
                Validator validator = schema.newValidator();
                validator.validate(new DOMSource(domdoc));
            } catch (IOException | SAXException e) {
                throw new IOException("Invalid XML schema: " + e.getMessage());
            }
            return test;
    }
}
