package gmbh.pagina.tools.gradle.mac_app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Plist}, focusing on {@code additionalPlistEntries} and {@code
 * additionalJavaXEntries} behavior.
 */
class PlistTest {

  private static final String MAIN_CLASS = "com.example.Main";

  private Plist plist;
  private File tempFile;

  @BeforeEach
  void setUp() throws Exception {
    plist = new Plist();
    tempFile = File.createTempFile("PlistTest-", ".plist");
    tempFile.deleteOnExit();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Save the current plist and re-parse into a DOM document. */
  private Document saveAndParse() throws Exception {
    plist.save(tempFile);
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(tempFile);
  }

  /** Return the root {@code <dict>} element of a plist document. */
  private static Element rootDict(Document doc) {
    Node child = doc.getDocumentElement().getFirstChild();
    while (child != null && !(child instanceof Element)) {
      child = child.getNextSibling();
    }
    return (Element) child;
  }

  /** Return the JavaX {@code <dict>} element from a plist document. */
  private static Element javaXDict(Document doc) {
    return valueForKey(rootDict(doc), "JavaX");
  }

  /**
   * Walk a {@code <dict>} element's key/value pairs and return the value {@link Element} for the
   * given key name, or {@code null} if not found.
   */
  private static Element valueForKey(Element dict, String key) {
    NodeList children = dict.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Element
          && "key".equals(((Element) child).getTagName())
          && key.equals(child.getTextContent().trim())) {
        Node sibling = child.getNextSibling();
        while (sibling != null && !(sibling instanceof Element)) {
          sibling = sibling.getNextSibling();
        }
        return sibling instanceof Element ? (Element) sibling : null;
      }
    }
    return null;
  }

  /** Count how many {@code <key>} elements with the given text appear in a dict. */
  private static int countKeyOccurrences(Element dict, String key) {
    int count = 0;
    NodeList children = dict.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Element
          && "key".equals(((Element) child).getTagName())
          && key.equals(child.getTextContent().trim())) {
        count++;
      }
    }
    return count;
  }

  /** Build a minimal JavaX block, forwarding custom entries. */
  private void buildJavaX(Map<String, ?> customEntries) {
    plist.javaX(MAIN_CLASS, 21, null, null, null, null, null, customEntries);
  }

  // ---------------------------------------------------------------------------
  // additionalPlistEntries – value types
  // ---------------------------------------------------------------------------

  @Test
  void additionalPlistEntries_stringValue() throws Exception {
    plist.createEntries(Map.of("NSCameraUsageDescription", "Camera needed"));

    Element value = valueForKey(rootDict(saveAndParse()), "NSCameraUsageDescription");

    assertNotNull(value, "Key must be present");
    assertEquals("string", value.getTagName());
    assertEquals("Camera needed", value.getTextContent());
  }

  @Test
  void additionalPlistEntries_booleanTrue() throws Exception {
    plist.createEntries(Map.of("LSUIElement", true));

    Element value = valueForKey(rootDict(saveAndParse()), "LSUIElement");

    assertNotNull(value);
    assertEquals("true", value.getTagName());
  }

  @Test
  void additionalPlistEntries_booleanFalse() throws Exception {
    plist.createEntries(Map.of("SomeBoolKey", false));

    Element value = valueForKey(rootDict(saveAndParse()), "SomeBoolKey");

    assertNotNull(value);
    assertEquals("false", value.getTagName());
  }

  @Test
  void additionalPlistEntries_integerValue() throws Exception {
    plist.createEntries(Map.of("MyCount", 42));

    Element value = valueForKey(rootDict(saveAndParse()), "MyCount");

    assertNotNull(value);
    assertEquals("integer", value.getTagName());
    assertEquals("42", value.getTextContent());
  }

  @Test
  void additionalPlistEntries_longValue() throws Exception {
    plist.createEntries(Map.of("BigCount", 9_000_000_000L));

    Element value = valueForKey(rootDict(saveAndParse()), "BigCount");

    assertNotNull(value);
    assertEquals("integer", value.getTagName());
    assertEquals("9000000000", value.getTextContent());
  }

  @Test
  void additionalPlistEntries_doubleValue() throws Exception {
    plist.createEntries(Map.of("MyRatio", 1.5));

    Element value = valueForKey(rootDict(saveAndParse()), "MyRatio");

    assertNotNull(value);
    assertEquals("real", value.getTagName());
    assertEquals("1.5", value.getTextContent());
  }

  @Test
  void additionalPlistEntries_listValue() throws Exception {
    plist.createEntries(Map.of("MyList", Arrays.asList("alpha", "beta", "gamma")));

    Element value = valueForKey(rootDict(saveAndParse()), "MyList");

    assertNotNull(value);
    assertEquals("array", value.getTagName());
    NodeList strings = value.getElementsByTagName("string");
    assertEquals(3, strings.getLength());
    assertEquals("alpha", strings.item(0).getTextContent());
    assertEquals("beta", strings.item(1).getTextContent());
    assertEquals("gamma", strings.item(2).getTextContent());
  }

  @Test
  void additionalPlistEntries_mapValue() throws Exception {
    Map<String, String> inner = new LinkedHashMap<>();
    inner.put("innerKey", "innerVal");
    plist.createEntries(Map.of("MyDict", inner));

    Element value = valueForKey(rootDict(saveAndParse()), "MyDict");

    assertNotNull(value);
    assertEquals("dict", value.getTagName());
    Element innerValue = valueForKey(value, "innerKey");
    assertNotNull(innerValue);
    assertEquals("innerVal", innerValue.getTextContent());
  }

  @Test
  void additionalPlistEntries_nestedListOfMaps() throws Exception {
    Map<String, String> item = Collections.singletonMap("k", "v");
    plist.createEntries(Map.of("Items", List.of(item)));

    Element value = valueForKey(rootDict(saveAndParse()), "Items");

    assertNotNull(value);
    assertEquals("array", value.getTagName());
    NodeList dicts = value.getElementsByTagName("dict");
    assertEquals(1, dicts.getLength());
  }

  // ---------------------------------------------------------------------------
  // additionalPlistEntries – additive behavior and overrides
  // ---------------------------------------------------------------------------

  @Test
  void additionalPlistEntries_multipleEntriesAreAllAdded() throws Exception {
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("KeyA", "valueA");
    entries.put("KeyB", "valueB");
    plist.createEntries(entries);

    Document doc = saveAndParse();
    assertEquals("valueA", valueForKey(rootDict(doc), "KeyA").getTextContent());
    assertEquals("valueB", valueForKey(rootDict(doc), "KeyB").getTextContent());
  }

  @Test
  void additionalPlistEntries_areAddedAlongsideGeneratedKeys() throws Exception {
    // Simulate generated keys written first
    plist.createEntry("CFBundleName", "My App");
    plist.createEntry("CFBundleVersion", "1.0");
    plist.createEntries(Map.of("NSCameraUsageDescription", "Camera"));

    Document doc = saveAndParse();
    assertEquals("My App", valueForKey(rootDict(doc), "CFBundleName").getTextContent());
    assertEquals("1.0", valueForKey(rootDict(doc), "CFBundleVersion").getTextContent());
    assertNotNull(
        valueForKey(rootDict(doc), "NSCameraUsageDescription"),
        "Additional entry must be present alongside generated ones");
  }

  @Test
  void additionalPlistEntries_canOverrideGeneratedKey() throws Exception {
    plist.createEntry("CFBundleName", "OriginalName");
    plist.createEntries(Map.of("CFBundleName", "OverriddenName"));

    Document doc = saveAndParse();
    Element value = valueForKey(rootDict(doc), "CFBundleName");

    assertNotNull(value);
    assertEquals(
        "OverriddenName",
        value.getTextContent(),
        "Additional entry must override the generated value");
    assertEquals(
        1,
        countKeyOccurrences(rootDict(doc), "CFBundleName"),
        "Key must appear exactly once after override");
  }

  @Test
  void additionalPlistEntries_nullMapIsNoOp() throws Exception {
    plist.createEntry("Existing", "kept");
    plist.createEntries((Map<String, ?>) null);

    assertEquals("kept", valueForKey(rootDict(saveAndParse()), "Existing").getTextContent());
  }

  @Test
  void additionalPlistEntries_emptyMapIsNoOp() throws Exception {
    plist.createEntry("Existing", "kept");
    plist.createEntries(Map.of());

    assertEquals("kept", valueForKey(rootDict(saveAndParse()), "Existing").getTextContent());
  }

  @Test
  void additionalPlistEntries_nullValueIsSkipped() throws Exception {
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("NullKey", null);
    entries.put("PresentKey", "present");
    plist.createEntries(entries);

    Document doc = saveAndParse();
    assertNull(valueForKey(rootDict(doc), "NullKey"), "Null-valued key must be skipped");
    assertEquals("present", valueForKey(rootDict(doc), "PresentKey").getTextContent());
  }

  // ---------------------------------------------------------------------------
  // additionalJavaXEntries – via javaX()
  // ---------------------------------------------------------------------------

  @Test
  void additionalJavaXEntries_stringValue() throws Exception {
    buildJavaX(Map.of("WorkingDirectory", "$APP_ROOT/Contents"));

    Element value = valueForKey(javaXDict(saveAndParse()), "WorkingDirectory");

    assertNotNull(value, "JavaX custom key must be present");
    assertEquals("string", value.getTagName());
    assertEquals("$APP_ROOT/Contents", value.getTextContent());
  }

  @Test
  void additionalJavaXEntries_booleanValue() throws Exception {
    buildJavaX(Map.of("SomeFlag", true));

    Element value = valueForKey(javaXDict(saveAndParse()), "SomeFlag");

    assertNotNull(value);
    assertEquals("true", value.getTagName());
  }

  @Test
  void additionalJavaXEntries_preservesAllGeneratedKeys() throws Exception {
    buildJavaX(Map.of("ExtraKey", "extraValue"));

    Element javaX = javaXDict(saveAndParse());

    assertNotNull(valueForKey(javaX, "MainClass"), "MainClass must still be present");
    assertEquals(MAIN_CLASS, valueForKey(javaX, "MainClass").getTextContent());
    assertNotNull(valueForKey(javaX, "JVMVersion"), "JVMVersion must still be present");
    assertNotNull(valueForKey(javaX, "ClassPath"), "ClassPath must still be present");
    assertNotNull(valueForKey(javaX, "ExtraKey"), "Additional key must be present");
    assertEquals("extraValue", valueForKey(javaX, "ExtraKey").getTextContent());
  }

  @Test
  void additionalJavaXEntries_canOverrideJVMVersion() throws Exception {
    buildJavaX(Map.of("JVMVersion", "21+"));

    Element javaX = javaXDict(saveAndParse());
    Element version = valueForKey(javaX, "JVMVersion");

    assertNotNull(version);
    assertEquals(
        "21+", version.getTextContent(), "Custom JVMVersion must override the generated value");
    assertEquals(
        1,
        countKeyOccurrences(javaX, "JVMVersion"),
        "JVMVersion must appear exactly once after override");
  }

  @Test
  void additionalJavaXEntries_canOverrideMainClass() throws Exception {
    buildJavaX(Map.of("MainClass", "com.example.Other"));

    Element javaX = javaXDict(saveAndParse());
    Element mainClass = valueForKey(javaX, "MainClass");

    assertNotNull(mainClass);
    assertEquals("com.example.Other", mainClass.getTextContent());
    assertEquals(1, countKeyOccurrences(javaX, "MainClass"));
  }

  @Test
  void additionalJavaXEntries_nullIsNoOp() throws Exception {
    buildJavaX(null);

    Element javaX = javaXDict(saveAndParse());
    assertNotNull(valueForKey(javaX, "MainClass"), "Generated keys must be intact");
  }

  @Test
  void additionalJavaXEntries_emptyIsNoOp() throws Exception {
    buildJavaX(Map.of());

    Element javaX = javaXDict(saveAndParse());
    assertNotNull(valueForKey(javaX, "MainClass"), "Generated keys must be intact");
  }

  @Test
  void additionalJavaXEntries_appearsInsideJavaXNotAtRootLevel() throws Exception {
    buildJavaX(Map.of("CustomJavaXKey", "javaXOnly"));

    Document doc = saveAndParse();
    assertNotNull(
        valueForKey(javaXDict(doc), "CustomJavaXKey"), "Custom key must be inside the JavaX dict");
    assertNull(
        valueForKey(rootDict(doc), "CustomJavaXKey"), "Custom key must NOT appear at root level");
  }

  // ---------------------------------------------------------------------------
  // Combined: both additionalPlistEntries and additionalJavaXEntries together
  // ---------------------------------------------------------------------------

  @Test
  void bothAdditional_doNotInterfereWithEachOther() throws Exception {
    plist.createEntry("CFBundleName", "App");
    buildJavaX(Map.of("ExtraJavaX", "jxVal"));
    plist.createEntries(Map.of("ExtraPlist", "plVal"));

    Document doc = saveAndParse();

    assertEquals("App", valueForKey(rootDict(doc), "CFBundleName").getTextContent());
    assertEquals("plVal", valueForKey(rootDict(doc), "ExtraPlist").getTextContent());
    assertEquals("jxVal", valueForKey(javaXDict(doc), "ExtraJavaX").getTextContent());
    assertNull(valueForKey(rootDict(doc), "ExtraJavaX"), "JavaX-only key must not leak to root");
    assertNull(valueForKey(javaXDict(doc), "ExtraPlist"), "Root-only key must not leak to JavaX");
  }
}
