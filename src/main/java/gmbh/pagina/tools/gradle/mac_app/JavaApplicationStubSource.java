package gmbh.pagina.tools.gradle.mac_app;

/** A Java application stub source definition. */
public class JavaApplicationStubSource {
  /** Source URL used to download the Java application stub. */
  private final String url;

  /** Whether the downloaded artifact must be unzipped. */
  private final boolean unzip;

  /** Executable name used from the downloaded artifact. */
  private final String executableName;

  /**
   * Constructs a JavaApplicationStubSource with the provided configuration.
   *
   * @param url the URL to download the stub from
   * @param unzip whether to unzip the downloaded file
   * @param executableName the name of the executable file
   */
  public JavaApplicationStubSource(String url, boolean unzip, String executableName) {
    this.url = url;
    this.unzip = unzip;
    this.executableName = executableName;
  }

  /**
   * Gets the URL to download the stub from.
   *
   * @return the stub URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Gets whether the downloaded file should be unzipped.
   *
   * @return true if the file should be unzipped, false otherwise
   */
  public boolean isUnzip() {
    return unzip;
  }

  /**
   * Gets the name of the executable file.
   *
   * @return the executable name
   */
  public String getExecutableName() {
    return executableName;
  }
}
