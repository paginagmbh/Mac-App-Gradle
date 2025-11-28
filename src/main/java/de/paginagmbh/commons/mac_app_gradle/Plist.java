package de.paginagmbh.commons.mac_app_gradle;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** A lazy XML builder object that represents an apple property list. */
public class Plist {

  /** The root element */
  private Element root;

  /** The XML document */
  private Document doc;

  /** Create the property list and set up a minimal XML structure. */
  public Plist() {
    try {
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    // Remove the standalone attribute from the XML declaration
    doc.setXmlStandalone(true);

    // Create the plist element
    Element plist = doc.createElement("plist");
    plist.setAttribute("version", "1.0");
    doc.appendChild(plist);

    // Create the root dictionary
    root = doc.createElement("dict");
    plist.appendChild(root);
  }

  /**
   * Save the property list to a target file.
   *
   * @param file The file to write it to.
   */
  public void save(File file) throws ParserConfigurationException, TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(doc), new StreamResult(file));
  }

  /** Add a <code>&lt;string&gt;</code> node to a parent element. */
  public void addString(Node parent, String value) {
    parent.appendChild(textNode("string", value));
  }

  /** Add a <code>&lt;key&gt;</code> node to a parent element. */
  public void addKey(Node parent, String value) {
    parent.appendChild(textNode("key", value));
  }

  /** Add a <code>&lt;string&gt;</code> node to the root dictionary. */
  public void addString(String value) {
    addString(root, value);
  }

  /** Add a <code>&lt;key&gt;</code> node to the root dictionary. */
  public void addKey(String value) {
    addKey(root, value);
  }

  /** Create an entry with a key and a boolean value in a parent node. */
  public void createEntry(Node parent, String key, boolean value) {
    addKey(parent, key);
    parent.appendChild(booleanNode(value));
  }

  /** Create an entry with a key and a boolean value in the root property list. */
  public void createEntry(String key, boolean value) {
    createEntry(root, key, value);
  }

  /** Create an entry with a key and a string value in a parent node. */
  public void createEntry(Node parent, String key, String value) {
    if (value == null) {
      System.out.println("Warning: key '" + key + "' has a value of null, skipping entry.");
    } else {
      addKey(parent, key);
      addString(parent, value);
    }
  }

  /** Create an entry with a key and a string value in the root property list. */
  public void createEntry(String key, String value) {
    createEntry(root, key, value);
  }

  /** Create a node with an element name and a string value and return it. */
  private Node textNode(String key, String value) {
    Element node = doc.createElement(key);
    node.appendChild(doc.createTextNode(value));
    return node;
  }

  /** Create a boolean node (true or false) and return it. */
  private Node booleanNode(boolean value) {
    return doc.createElement(value ? "true" : "false");
  }

  /**
   * Set up the JavaX block to integrate with UniversalJavaApplicationStub.
   *
   * @param mainClass The main class in the format <em>com.package.Class</em>
   * @param jarName The name of the jar file to call.
   * @param minimumVersion The minimum version of Java required.
   */
  public void javaX(String mainClass, String jarName, int minimumVersion) {
    // Root javaX key
    addKey("JavaX");
    Element javaX = doc.createElement("dict");

    createEntry(javaX, "MainClass", mainClass);
    // Adjust the version to the format required by UJAS: 11 → 11.0+
    createEntry(javaX, "JVMVersion", String.valueOf(minimumVersion) + ".0+");

    // Add the elements from the classpath to an array. Assume the live in the java root directory.
    javaX.appendChild(textNode("key", "ClassPath"));
    Element classPath = doc.createElement("array");
    addString(classPath, "$JAVAROOT/*");
    javaX.appendChild(classPath);

    // Add VM options to use UTF 8
    createEntry(javaX, "VMOptions", "-Dfile.encoding=UTF-8");
    root.appendChild(javaX);
  }

  /** Set up document types the document can read/edit. */
  public void documentTypes(DocumentType[] documentTypes) {
    // Add the root array element and corresponding key
    addKey("CFBundleDocumentTypes");
    Element array = doc.createElement("array");

    // Generate the structure for each document type.
    for (DocumentType documentType : documentTypes) {
      // Create a dictionary data structure
      Element dict = doc.createElement("dict");
      // Empty array of icons – this is not yet supporte
      addKey(dict, "CFBundleTypeIconFiles");
      dict.appendChild(doc.createElement("array"));
      // Transfer data form the DocumentType object
      createEntry(dict, "CFBundleTypeName", documentType.name);
      createEntry(dict, "CFBundleTypeRole", documentType.role);
      createEntry(dict, "LSHandlerRank", documentType.handlerRank);
      // Add the content type URIs to a new array
      addKey(dict, "LSItemContentTypes");
      Element typeArray = doc.createElement("array");
      for (String contentType : documentType.contentTypes) addString(typeArray, contentType);
      dict.appendChild(typeArray);

      // Add the new data structure to the document type array.
      array.appendChild(dict);
    }
    // Add the document type array to the root dictionary.
    root.appendChild(array);
  }
}
