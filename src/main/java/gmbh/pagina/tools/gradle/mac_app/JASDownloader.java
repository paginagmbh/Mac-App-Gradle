package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Download the configured Java application stub. */
@CacheableTask
public class JASDownloader extends DefaultTask {

  /** URL of the Java application stub to download. */
  private final Property<String> sourceUrl = getProject().getObjects().property(String.class);

  /** Whether the downloaded artifact should be unzipped before use. */
  private final Property<Boolean> unzip = getProject().getObjects().property(Boolean.class);

  /** Executable name expected inside the downloaded artifact. */
  private final Property<String> stubExecutableName =
      getProject().getObjects().property(String.class);

  /** Output directory where downloaded and extracted files are stored. */
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  /** Creates the task and wires default values from the built-in native stub preset. */
  public JASDownloader() {
    setGroup("Make Mac App");
    setDescription("Download the Java application stub required for the app.");
    getUnzip().convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.isUnzip());
    getStubExecutableName()
        .convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.getExecutableName());
    getSourceUrl().convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.getUrl());
    getOutdir().convention(getProject().getLayout().getBuildDirectory().dir("javaApplicationStub"));
  }

  /**
   * Returns the configured source URL property.
   *
   * @return source URL property
   */
  @Input
  public Property<String> getSourceUrl() {
    return sourceUrl;
  }

  /**
   * Gets the URL to download the stub from.
   *
   * @return the source URL
   */
  @Internal
  public String getUrl() {
    return getSourceUrl().get();
  }

  /**
   * Sets the URL to download the stub from.
   *
   * @param url the source URL to set
   */
  public void setUrl(String url) {
    getSourceUrl().set(url);
  }

  /**
   * Returns the unzip configuration property.
   *
   * @return unzip property
   */
  @Input
  public Property<Boolean> getUnzip() {
    return unzip;
  }

  /**
   * Returns the executable-name configuration property.
   *
   * @return executable-name property
   */
  @Input
  public Property<String> getStubExecutableName() {
    return stubExecutableName;
  }

  /**
   * Gets the executable name used for the final stub file.
   *
   * @return executable file name
   */
  @Internal
  public String getExecutableName() {
    return getStubExecutableName().get();
  }

  /**
   * Sets the executable name used for the final stub file.
   *
   * @param executableName executable file name
   */
  public void setExecutableName(String executableName) {
    getStubExecutableName().set(executableName);
  }

  /**
   * Gets the combined source descriptor used by task DSL assignment.
   *
   * @return combined Java-application-stub source descriptor
   */
  @Internal
  public JavaApplicationStubSource getSource() {
    return new JavaApplicationStubSource(
        getSourceUrl().get(), getUnzip().get(), getStubExecutableName().get());
  }

  /**
   * Updates URL, unzip mode, and executable name from a combined source descriptor.
   *
   * @param source source descriptor
   */
  public void setSource(JavaApplicationStubSource source) {
    getSourceUrl().set(source.getUrl());
    getUnzip().set(source.isUnzip());
    getStubExecutableName().set(source.getExecutableName());
  }

  /**
   * Gets the preset source for universalJavaApplicationStub shell script.
   *
   * @return shell-script preset source
   */
  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubShell() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_SHELL;
  }

  /**
   * Gets the preset source for universalJavaApplicationStub precompiled binary.
   *
   * @return precompiled preset source
   */
  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubPrecompiled() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_PRECOMPILED;
  }

  /**
   * Gets the preset source for NativeJavaApplicationStub v1.
   *
   * @return native v1 preset source
   */
  @Internal
  public JavaApplicationStubSource getNativeJavaApplicationStubv1() {
    return JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1;
  }

  /**
   * Gets whether the downloaded artifact should be unzipped.
   *
   * @return true when unzip mode is enabled
   */
  @Internal
  public boolean isUnzip() {
    return getUnzip().get();
  }

  /**
   * Sets whether the downloaded file should be unzipped.
   *
   * @param unzip true to unzip, false otherwise
   */
  public void setUnzip(boolean unzip) {
    getUnzip().set(unzip);
  }

  /**
   * Returns the output directory property.
   *
   * @return output directory property
   */
  @Internal
  public DirectoryProperty getOutdir() {
    return outdir;
  }

  /**
   * Gets the output directory as a file.
   *
   * @return output directory
   */
  @Internal
  public File getOutdirFile() {
    return getOutdir().get().getAsFile();
  }

  /**
   * Sets the output directory from a path string.
   *
   * @param outdir output directory path
   */
  public void setOutdir(String outdir) {
    getOutdir().fileValue(new File(outdir));
  }

  /**
   * Sets the output directory.
   *
   * @param outdir output directory
   */
  public void setOutdir(File outdir) {
    getOutdir().fileValue(outdir);
  }

  /**
   * Gets the target Java-application-stub file produced by this task.
   *
   * @return target stub executable file
   */
  @OutputFile
  public File getTargetFile() {
    if (isUnzip())
      return new File(
          new File(getOutdirFile(), getDownloadedBaseName()), getStubExecutableName().get());
    return new File(getOutdirFile(), getStubExecutableName().get());
  }

  /** The unzipped archive file. Does not exist if shell version is used. */
  private File getUnzippedFile() {
    return new File(getOutdirFile(), getDownloadedBaseName());
  }

  /** The downloaded zip file. Does not exist if shell version is used. */
  private File getZipFile() {
    return new File(getOutdirFile(), getDownloadName());
  }

  /** The filename stem of downloaded archive or file. */
  private String getDownloadedBaseName() {
    String download = getDownloadName();
    return download.split("\\.").length == 1
        ? download
        : download.substring(0, download.lastIndexOf("."));
  }

  /** Get the name of the zip archive */
  private String getDownloadName() {
    String[] components = getSourceUrl().get().split("/");
    return components[components.length - 1];
  }

  /** Download the zip or shell file. */
  private void download() {
    File destination = isUnzip() ? getZipFile() : getTargetFile();
    DownloadUtils.downloadHttpToFile(
        getSourceUrl().get(), destination.toPath(), "Java application stub");
  }

  /** Unzip the zip file. */
  private void unzip() {
    FileUtils.unzip(getZipFile(), getUnzippedFile());
  }

  /** Perform the plugin action. */
  @TaskAction
  public void taskAction() {
    // Ensure the output directory exists
    File outDir = getOutdirFile();
    if (!outDir.exists() && !outDir.mkdirs()) {
      throw new IllegalStateException(
          "Could not create output directory: " + outDir.getAbsolutePath());
    }
    // Download the file. Gradle's up-to-date checks and build cache avoid redundant downloads.
    download();
    // Unzip it, if an archive is configured. Non-zipped sources are downloaded directly.
    if (isUnzip()) unzip();
  }
}
