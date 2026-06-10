package gmbh.pagina.tools.gradle.mac_app;

/** A Java application stub source definition. */
public class JavaApplicationStubSource {
  private final String url;
  private final boolean unzip;
  private final String executableName;

  public JavaApplicationStubSource(String url, boolean unzip, String executableName) {
    this.url = url;
    this.unzip = unzip;
    this.executableName = executableName;
  }

  public String getUrl() {
    return url;
  }

  public boolean isUnzip() {
    return unzip;
  }

  public String getExecutableName() {
    return executableName;
  }
}

