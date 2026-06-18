package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

  private final Property<String> sourceUrl = getProject().getObjects().property(String.class);
  private final Property<Boolean> unzip = getProject().getObjects().property(Boolean.class);
  private final Property<String> stubExecutableName =
      getProject().getObjects().property(String.class);
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  public JASDownloader() {
    setGroup("Make Mac App");
    setDescription("Download the Java application stub required for the app.");
    getUnzip().convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.isUnzip());
    getStubExecutableName()
        .convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.getExecutableName());
    getSourceUrl().convention(JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1.getUrl());
    getOutdir().convention(getProject().getLayout().getBuildDirectory().dir("javaApplicationStub"));
  }

  @Input
  public Property<String> getSourceUrl() {
    return sourceUrl;
  }

  @Internal
  public String getUrl() {
    return getSourceUrl().get();
  }

  public void setUrl(String url) {
    getSourceUrl().set(url);
  }

  @Input
  public Property<Boolean> getUnzip() {
    return unzip;
  }

  @Input
  public Property<String> getStubExecutableName() {
    return stubExecutableName;
  }

  @Internal
  public String getExecutableName() {
    return getStubExecutableName().get();
  }

  public void setExecutableName(String executableName) {
    getStubExecutableName().set(executableName);
  }

  /** Convenience task DSL: `source = universalJavaApplicationStubShell`. */
  @Internal
  public JavaApplicationStubSource getSource() {
    return new JavaApplicationStubSource(
        getSourceUrl().get(), getUnzip().get(), getStubExecutableName().get());
  }

  /** Convenience task DSL: `source = ...` updates URL, unzip, and executable name together. */
  public void setSource(JavaApplicationStubSource source) {
    getSourceUrl().set(source.getUrl());
    getUnzip().set(source.isUnzip());
    getStubExecutableName().set(source.getExecutableName());
  }

  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubShell() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_SHELL;
  }

  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubProcompiled() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_PROCOMPILED;
  }

  @Internal
  public JavaApplicationStubSource getNativeJavaApplicationStubv1() {
    return JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1;
  }

  @Internal
  public boolean isUnzip() {
    return getUnzip().get();
  }

  public void setUnzip(boolean unzip) {
    getUnzip().set(unzip);
  }

  @Internal
  public DirectoryProperty getOutdir() {
    return outdir;
  }

  /** The output directory used by this task. */
  @Internal
  public File getOutdirFile() {
    return getOutdir().get().getAsFile();
  }

  /** Set the output directory used by this task. */
  public void setOutdir(String outdir) {
    getOutdir().fileValue(new File(outdir));
  }

  /** Set the output directory used by this task. */
  public void setOutdir(File outdir) {
    getOutdir().fileValue(outdir);
  }

  /** The Java application stub file. */
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
    File parent = destination.getParentFile();
    try {
      if (parent != null) Files.createDirectories(parent.toPath());
      URLConnection connection = new URL(getSourceUrl().get()).openConnection();
      connection.setConnectTimeout(30_000);
      connection.setReadTimeout(60_000);
      try (InputStream inputStream = connection.getInputStream()) {
        Files.copy(inputStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not download Java application stub", e);
    }
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
