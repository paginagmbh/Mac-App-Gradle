package gmbh.pagina.tools.gradle.mac_app;

/** Built-in Java application stub source presets. */
public final class JavaApplicationStubPresets {

  private JavaApplicationStubPresets() {}

  public static final JavaApplicationStubSource UNIVERSAL_JAVA_APPLICATION_STUB_SHELL =
      new JavaApplicationStubSource(
          "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub",
          false,
          "universalJavaApplicationStub");

  public static final JavaApplicationStubSource UNIVERSAL_JAVA_APPLICATION_STUB_PROCOMPILED =
      new JavaApplicationStubSource(
          "https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip",
          true,
          "universalJavaApplicationStub");

  public static final JavaApplicationStubSource NATIVE_JAVA_APPLICATION_STUB_V0_9 =
      new JavaApplicationStubSource(
          "https://github.com/paginagmbh/NativeJavaApplicationStub/releases/download/v0.9/NativeJavaApplicationStub",
          false,
          "NativeJavaApplicationStub");
}

