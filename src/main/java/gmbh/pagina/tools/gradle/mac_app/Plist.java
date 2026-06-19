package gmbh.pagina.tools.gradle.mac_app;

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
   * @throws ParserConfigurationException If XML transformer setup fails.
   * @throws TransformerException If writing the XML document fails.
   */
  public void save(File file) throws ParserConfigurationException, TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(doc), new StreamResult(file));
  }

  /**
   * Add a <code>&lt;string&gt;</code> node to a parent element.
   *
   * @param parent The parent XML node to append to.
   * @param value The string value to store.
   */
  public void addString(Node parent, String value) {
    parent.appendChild(textNode("string", value));
  }

  /**
   * Add a <code>&lt;key&gt;</code> node to a parent element.
   *
   * @param parent The parent XML node to append to.
   * @param value The key value to store.
   */
  public void addKey(Node parent, String value) {
    parent.appendChild(textNode("key", value));
  }

  /**
   * Add a <code>&lt;string&gt;</code> node to the root dictionary.
   *
   * @param value The string value to store.
   */
  public void addString(String value) {
    addString(root, value);
  }

  /**
   * Add a <code>&lt;key&gt;</code> node to the root dictionary.
   *
   * @param value The key value to store.
   */
  public void addKey(String value) {
    addKey(root, value);
  }

  /**
   * Create an entry with a key and a boolean value in a parent node.
   *
   * @param parent The parent XML node.
   * @param key The plist key.
   * @param value The boolean value.
   */
  public void createEntry(Node parent, String key, boolean value) {
    createEntry(parent, key, Boolean.valueOf(value));
  }

  /**
   * Create an entry with a key and a boolean value in the root property list.
   *
   * @param key The plist key.
   * @param value The boolean value.
   */
  public void createEntry(String key, boolean value) {
    createEntry(root, key, value);
  }

  /**
   * Create an entry with a key and a string value in a parent node.
   *
   * @param parent The parent XML node.
   * @param key The plist key.
   * @param value The string value.
   */
  public void createEntry(Node parent, String key, String value) {
    createEntry(parent, key, (Object) value);
  }

  /**
   * Create or replace an entry with a key and a plist-compatible value in a parent dictionary.
   *
   * @param parent The parent XML dictionary node.
   * @param key The plist key.
   * @param value The plist-compatible value.
   */
  public void createEntry(Node parent, String key, Object value) {
    if (value == null) {
      System.out.println("Warning: key '" + key + "' has a value of null, skipping entry.");
      return;
    }

    Node valueNode = valueNode(value);
    if (valueNode == null) {
      System.out.println(
          "Warning: key '"
              + key
              + "' has an unsupported value type '"
              + value.getClass().getName()
              + "', skipping entry.");
      return;
    }

    removeEntry(parent, key);
    addKey(parent, key);
    parent.appendChild(valueNode);
  }

  /**
   * Create an entry with a key and a string value in the root property list.
   *
   * @param key The plist key.
   * @param value The string value.
   */
  public void createEntry(String key, String value) {
    createEntry(root, key, value);
  }

  /**
   * Create or replace an entry with a key and a plist-compatible value in the root dictionary.
   *
   * @param key The plist key.
   * @param value The plist-compatible value.
   */
  public void createEntry(String key, Object value) {
    createEntry(root, key, value);
  }

  /**
   * Create or replace multiple entries in a parent dictionary.
   *
   * @param parent The parent XML dictionary node.
   * @param entries Key/value entries to apply.
   */
  public void createEntries(Node parent, java.util.Map<String, ?> entries) {
    if (entries == null || entries.isEmpty()) return;
    for (java.util.Map.Entry<String, ?> entry : entries.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isEmpty()) continue;
      createEntry(parent, entry.getKey(), entry.getValue());
    }
  }

  /**
   * Create or replace multiple entries in the root dictionary.
   *
   * @param entries Key/value entries to apply.
   */
  public void createEntries(java.util.Map<String, ?> entries) {
    createEntries(root, entries);
  }

  /** Create a node with an element name and a string value and return it. */
  private Node textNode(String key, String value) {
    Element node = doc.createElement(key);
    node.appendChild(doc.createTextNode(value));
    return node;
  }

  /** Create a boolean node (true or false) and return it. */
  private Node booleanNode(boolean value) {
    return doc.createElement(Boolean.toString(value));
  }

  /** Create an integer node and return it. */
  private Node integerNode(long value) {
    return textNode("integer", String.valueOf(value));
  }

  /** Create a real node and return it. */
  private Node realNode(double value) {
    return textNode("real", String.valueOf(value));
  }

  /** Convert a Java value to a plist value node. */
  private Node valueNode(Object value) {
    if (value instanceof String) return textNode("string", (String) value);
    if (value instanceof Boolean) return booleanNode(((Boolean) value).booleanValue());
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      return integerNode(((Number) value).longValue());
    }
    if (value instanceof Float || value instanceof Double) {
      return realNode(((Number) value).doubleValue());
    }
    if (value instanceof java.util.Map<?, ?>) {
      Element dict = doc.createElement("dict");
      for (java.util.Map.Entry<?, ?> entry : ((java.util.Map<?, ?>) value).entrySet()) {
        if (!(entry.getKey() instanceof String)) continue;
        Object childValue = entry.getValue();
        if (childValue == null) continue;
        Node childNode = valueNode(childValue);
        if (childNode == null) continue;
        addKey(dict, (String) entry.getKey());
        dict.appendChild(childNode);
      }
      return dict;
    }
    if (value instanceof java.lang.Iterable<?>) {
      Element array = doc.createElement("array");
      for (Object item : (java.lang.Iterable<?>) value) {
        if (item == null) continue;
        Node itemNode = valueNode(item);
        if (itemNode != null) array.appendChild(itemNode);
      }
      return array;
    }
    if (value instanceof Object[]) {
      Element array = doc.createElement("array");
      for (Object item : (Object[]) value) {
        if (item == null) continue;
        Node itemNode = valueNode(item);
        if (itemNode != null) array.appendChild(itemNode);
      }
      return array;
    }
    return null;
  }

  /** Remove an existing key and its following value node from a dictionary when present. */
  private void removeEntry(Node parent, String key) {
    Node child = parent.getFirstChild();
    while (child != null) {
      Node next = child.getNextSibling();
      if (child instanceof Element
          && "key".equals(((Element) child).getTagName())
          && key.equals(child.getTextContent())) {
        parent.removeChild(child);
        Node valueNode = next;
        while (valueNode != null && !(valueNode instanceof Element)) {
          valueNode = valueNode.getNextSibling();
        }
        if (valueNode != null) parent.removeChild(valueNode);
        return;
      }
      child = next;
    }
  }

  /** Add an array entry with string values to a parent dictionary. */
  private void createArrayEntry(Node parent, String key, java.util.List<String> values) {
    if (values == null || values.isEmpty()) return;
    addKey(parent, key);
    Element array = doc.createElement("array");
    for (String value : values) addString(array, value);
    parent.appendChild(array);
  }

  /** Add a Properties dictionary entry from JVM args like <code>-Dkey=value</code>. */
  private void createPropertiesEntry(Node parent, java.util.List<String> properties) {
    if (properties == null || properties.isEmpty()) return;
    Element propertyDict = doc.createElement("dict");

    for (String property : properties) {
      if (property == null || !property.startsWith("-D") || property.length() <= 2) continue;

      String body = property.substring(2);
      int separator = body.indexOf('=');
      String propertyKey = separator >= 0 ? body.substring(0, separator) : body;
      String propertyValue = separator >= 0 ? body.substring(separator + 1) : "";

      if (!propertyKey.isEmpty()) {
        addKey(propertyDict, propertyKey);
        addString(propertyDict, propertyValue);
      }
    }

    if (propertyDict.hasChildNodes()) {
      addKey(parent, "Properties");
      parent.appendChild(propertyDict);
    }
  }

  /**
   * Set up the JavaX block to integrate with UniversalJavaApplicationStub.
   *
   * @param mainClass The main class in the format <em>com.package.Class</em>
   * @param minimumVersion The minimum version of Java required.
   * @param properties Java system properties in <code>-Dkey=value</code> form.
   * @param vmOptions JVM options passed to the launcher.
   * @param startOnMainThread Whether to launch on the macOS main thread.
   * @param mainArguments Arguments passed to the Java main class.
   * @param splashFile Optional splash file name.
   */
  public void javaX(
      String mainClass,
      int minimumVersion,
      java.util.List<String> properties,
      java.util.List<String> vmOptions,
      Boolean startOnMainThread,
      java.util.List<String> mainArguments,
      String splashFile,
      java.util.Map<String, ?> customEntries) {
    // Root javaX key
    addKey("JavaX");
    Element javaX = doc.createElement("dict");

    createEntry(javaX, "MainClass", mainClass);
    createEntry(javaX, "JVMVersion", String.valueOf(minimumVersion) + "+");
    createEntry(javaX, "SplashFile", splashFile);
    if (startOnMainThread != null) createEntry(javaX, "StartOnMainThread", startOnMainThread);

    // Add the elements from the classpath to an array. Assume the live in the java root
    // directory.
    javaX.appendChild(textNode("key", "ClassPath"));
    Element classPath = doc.createElement("array");
    addString(classPath, "$JAVAROOT/*");
    javaX.appendChild(classPath);

    // Additional JavaX parameters supported by universalJavaApplicationStub.
    createPropertiesEntry(javaX, properties);
    createArrayEntry(javaX, "VMOptions", vmOptions);
    createArrayEntry(javaX, "Arguments", mainArguments);
    createEntries(javaX, customEntries);
    root.appendChild(javaX);
  }

  /**
   * Set up document types the document can read/edit.
   *
   * @param documentTypes Document type definitions to write into <code>CFBundleDocumentTypes</code>
   *     .
   */
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
