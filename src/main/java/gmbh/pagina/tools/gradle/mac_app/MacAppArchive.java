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

  private final DirectoryProperty sourceDirectory = getProject().getObjects().directoryProperty();
  private final Property<String> appName = getProject().getObjects().property(String.class);
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  public MacAppArchive() {
    setDescription("Create a .tgz archive for the unsigned mac .app bundle.");
    setGroup("Make Mac App");
    outdir.convention(getProject().getLayout().getBuildDirectory().dir("unsignedMacApp"));
  }

  @Internal
  public DirectoryProperty getSourceDirectoryProperty() {
    return sourceDirectory;
  }

  @Input
  public String getAppName() {
    return appName.get();
  }

  public void setAppName(String appName) {
    this.appName.set(appName);
  }

  @Internal
  public Property<String> getAppNameProperty() {
    return appName;
  }

  @Internal
  public DirectoryProperty getOutdirProperty() {
    return outdir;
  }

  @Internal
  public File getOutdir() {
    return outdir.get().getAsFile();
  }

  public void setOutdir(String outdir) {
    this.outdir.fileValue(new File(outdir));
  }

  public void setOutdir(File outdir) {
    this.outdir.fileValue(outdir);
  }

  /** The app bundle that is archived. */
  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getMacApp() {
    return new File(sourceDirectory.get().getAsFile(), getAppName() + ".app");
  }

  /** The generated archive file. */
  @OutputFile
  public File getMacAppTarGz() {
    return new File(getOutdir(), getAppName() + ".tgz");
  }

  @TaskAction
  public void taskAction() {
    File app = getMacApp();
    if (!app.exists()) {
      throw new IllegalStateException("Could not find app bundle to archive: " + app);
    }
    FileUtils.tarGz(app, getMacAppTarGz());
  }
}


