package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Create a tar.gz archive for a generated mac app bundle. */
public class MacAppArchive extends DefaultTask {

  /** Directory that contains the generated app bundle to archive. */
  private final DirectoryProperty sourceDirectory = getProject().getObjects().directoryProperty();

  /** App name used to resolve the bundle and archive file names. */
  private final Property<String> appName = getProject().getObjects().property(String.class);

  /** Output directory where the tar.gz archive is written. */
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  /** Creates the task with default group, description, and output directory convention. */
  public MacAppArchive() {
    setDescription("Create a .tgz archive for the unsigned mac .app bundle.");
    setGroup("Make Mac App");
    outdir.convention(getProject().getLayout().getBuildDirectory().dir("unsignedMacApp"));
  }

    /**
     * Returns the source directory property.
     *
     * @return source directory property
     */
  @Internal
  public DirectoryProperty getSourceDirectoryProperty() {
    return sourceDirectory;
  }

  /**
   * Gets the app name.
   *
   * @return the app name
   */
  @Input
  public String getAppName() {
    return appName.get();
  }

  /**
   * Sets the app name.
   *
   * @param appName the app name to set
   */
  public void setAppName(String appName) {
    this.appName.set(appName);
  }

    /**
     * Returns the app name property.
     *
     * @return app name property
     */
  @Internal
  public Property<String> getAppNameProperty() {
    return appName;
  }

    /**
     * Returns the output directory property.
     *
     * @return output directory property
     */
  @Internal
  public DirectoryProperty getOutdirProperty() {
    return outdir;
  }

  /**
   * Gets the output directory.
   *
   * @return the output directory
   */
  @Internal
  public File getOutdir() {
    return outdir.get().getAsFile();
  }

  /**
   * Sets the output directory as a string.
   *
   * @param outdir the output directory path
   */
  public void setOutdir(String outdir) {
    this.outdir.fileValue(new File(outdir));
  }

  /**
   * Sets the output directory.
   *
   * @param outdir the output directory
   */
  public void setOutdir(File outdir) {
    this.outdir.fileValue(outdir);
  }

  /**
   * Gets the app bundle directory that will be archived.
   *
   * @return app bundle directory
   */
  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getMacApp() {
    return new File(sourceDirectory.get().getAsFile(), getAppName() + ".app");
  }

  /**
   * Gets the generated tar.gz archive file.
   *
   * @return archive file path
   */
  @OutputFile
  public File getMacAppTarGz() {
    return new File(getOutdir(), getAppName() + ".tgz");
  }

  /** Creates the tar.gz archive for the generated unsigned app bundle. */
  @TaskAction
  public void taskAction() {
    File app = getMacApp();
    if (!app.exists()) {
      throw new IllegalStateException("Could not find app bundle to archive: " + app);
    }
    FileUtils.tarGz(app, getMacAppTarGz());
  }
}
