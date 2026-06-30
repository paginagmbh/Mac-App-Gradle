package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModelAndPresetTest {

  @Test
  void documentType_storesAllConstructorValues() {
    String[] contentTypes = new String[] {"public.text", "public.data"};
    DocumentType type = new DocumentType("Text", "Viewer", "Alternate", contentTypes);

    assertEquals("Text", type.name);
    assertEquals("Viewer", type.role);
    assertEquals("Alternate", type.handlerRank);
    assertArrayEquals(contentTypes, type.contentTypes);
  }

  @Test
  void javaApplicationStubSource_exposesConfiguredValues() {
    JavaApplicationStubSource source =
        new JavaApplicationStubSource("https://example.com/stub", true, "myStub");

    assertEquals("https://example.com/stub", source.getUrl());
    assertTrue(source.isUnzip());
    assertEquals("myStub", source.getExecutableName());
  }

  @Test
  void javaApplicationStubPresets_areWired() {
    JavaApplicationStubSource shell =
        JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_SHELL;
    JavaApplicationStubSource precompiled =
        JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_PRECOMPILED;
    JavaApplicationStubSource nativeV1 = JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1;

    assertNotNull(shell);
    assertNotNull(precompiled);
    assertNotNull(nativeV1);

    assertFalse(shell.isUnzip());
    assertTrue(precompiled.isUnzip());
    assertFalse(nativeV1.isUnzip());

    assertEquals("universalJavaApplicationStub", shell.getExecutableName());
    assertEquals("universalJavaApplicationStub", precompiled.getExecutableName());
    assertEquals("NativeJavaApplicationStub", nativeV1.getExecutableName());
  }
}
