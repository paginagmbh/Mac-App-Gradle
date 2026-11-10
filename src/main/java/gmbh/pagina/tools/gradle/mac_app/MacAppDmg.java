package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/** Creates an unsigned disk image without signing or notarization. */
@DisableCachingByDefault(because = "DMG creation depends on macOS tools and optional Finder automation")
public abstract class MacAppDmg extends DefaultTask {
  private java.util.function.Supplier<DmgOptions> dmg;

  /** Creates the task with its default metadata and output directory. */
  public MacAppDmg() {
    DmgOptions defaults = getProject().getObjects().newInstance(DmgOptions.class);
    dmg = () -> defaults;
    setDescription("Create an unsigned .dmg for the unsigned mac .app bundle.");
    setGroup("Make Mac App");
    getOutdirProperty().convention(
        getProject().getLayout().getBuildDirectory().dir("unsignedMacApp"));
    getProjectVersion().convention(
        getProject().provider(() -> String.valueOf(getProject().getVersion())));
  }

  /**
   * Returns the directory containing the unsigned bundle.
   *
   * @return source directory property
   */
  @Internal
  public abstract DirectoryProperty getSourceDirectoryProperty();

  /**
   * Returns the app name used for the bundle and DMG filenames.
   *
   * @return app name property
   */
  @Input
  public abstract Property<String> getAppName();

  /**
   * Returns the version used in the volume name.
   *
   * @return project version property
   */
  @Input
  public abstract Property<String> getProjectVersion();

  /**
   * Returns the DMG output directory.
   *
   * @return output directory property
   */
  @Internal
  public abstract DirectoryProperty getOutdirProperty();

  /**
   * Returns the unsigned app bundle to package.
   *
   * @return unsigned app bundle
   */
  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getMacApp() {
    return new File(getSourceDirectoryProperty().get().getAsFile(), getAppName().get() + ".app");
  }

  /**
   * Returns the unsigned disk image output.
   *
   * @return DMG output file
   */
  @OutputFile
  public File getMacAppDmg() {
    return new File(getOutdirProperty().get().getAsFile(), getAppName().get() + ".dmg");
  }

  /**
   * Returns the optional Finder layout settings.
   *
   * @return DMG layout options
   */
  @Nested
  public DmgOptions getDmg() {
    return dmg.get();
  }

  void useDmg(java.util.function.Supplier<DmgOptions> options) {
    dmg = options;
  }

  /** Packages the unsigned bundle without invoking signing or notarization. */
  @TaskAction
  public void taskAction() {
    DmgLayout.packageApp(getDmg(), getMacApp(), getMacAppDmg(),
        getAppName().get(), getProjectVersion().get(), getTemporaryDir());
  }
}
