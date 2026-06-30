package gmbh.pagina.tools.gradle.mac_app;

/** Built-in Java application stub source presets. */
public final class JavaApplicationStubPresets {

  private JavaApplicationStubPresets() {}

  /** Preset for the shell-script version of universalJavaApplicationStub. */
  public static final JavaApplicationStubSource UNIVERSAL_JAVA_APPLICATION_STUB_SHELL =
      new JavaApplicationStubSource(
          "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub",
          false,
          "universalJavaApplicationStub");

  /** Preset for the precompiled ZIP distribution of universalJavaApplicationStub. */
  public static final JavaApplicationStubSource UNIVERSAL_JAVA_APPLICATION_STUB_PRECOMPILED =
      new JavaApplicationStubSource(
          "https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip",
          true,
          "universalJavaApplicationStub");

  /** Preset for NativeJavaApplicationStub v1 binary download. */
  public static final JavaApplicationStubSource NATIVE_JAVA_APPLICATION_STUB_V1 =
      new JavaApplicationStubSource(
          "https://github.com/paginagmbh/NativeJavaApplicationStub/releases/download/v1.0/NativeJavaApplicationStub",
          false,
          "NativeJavaApplicationStub");
}
