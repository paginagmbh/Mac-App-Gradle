package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

/** Create a mac app file structure. */
public class AppBundler extends DefaultTask {

  public String getDescription() {
    return "Create an unsigned mac .app bundle.";
  }

  public String getGroup() {
    return "Make Mac App";
  }

  /** Get the {@link Project} in slightly less time, effort, and characters. */
  private Project project = getProject();

  /** The application extension object to read data from later. */
  private JavaApplication javaApplication =
      project.getExtensions().getByType(JavaApplication.class);

  /** Get the name of the main class of the project. */
  private String getMainClass() {
    return javaApplication.getMainClass().get();
  }

  /**
   * The name of the app, without any extension (customizable, auto).
   *
   * <p>Default: "${project.name}"
   *
   * <p>Used in the Info.plist as CFBundleDisplayName and CFBundleName.
   */
  public String appName = project.getName();

  /**
   * The directory to write all the data to (customizable, auto).
   *
   * <p>Default: "${buildDir}/unsignedMacApp"
   */
  public String outdir =
      getProject()
          .getLayout()
          .getBuildDirectory()
          .file("unsignedMacApp")
          .get()
          .getAsFile()
          .getAbsolutePath();

  /** The package signature (customizable, autocomputed from appName) for the CFBundleIdentifier. */
  public String pkgInfoSignature = computePkgInfoSignature(appName);

  /** The development region for CFBundleDevelopmentRegion (customizable, optional) (en, de, …). */
  public String developmentRegion = null;

  /** The CFBundleIdentifier (customizable, auto). Computed from the main class’s parent package. */
  public String bundleIdentifier = null;

  /** A copyright string (customizable, optional) for NSHumanReadableCopyright. */
  public String copyright = null;

  /** The path to an icns file to use. */
  public String icon = null;

  /** The path to additional resources to copy into the Resources folder. */
  public String[] additionalResources = null;

  /** A list of document types that can be opened with this app as UTIs (customizable, optional). */
  public String[] viewableDocumentTypes = null;

  /** The logger to use for file logging */
  @SuppressWarnings("unused")
  private final Logger logger = Logging.getLogger(getClass());

  /** The app bundle that is generated. */
  @OutputDirectory
  public File getMacApp() {
    return new File(outdir, appName + ".app");
  }

  /** The app bundle that is generated. */
  @OutputFile
  public File getMacAppTarGz() {
    return new File(outdir, appName + ".tgz");
  }

  /** The object to use for constructing the Info.plist file. */
  private Plist infoPlist = new Plist();

  /**
   * Compute a code to use for the app PkgInfo signature:
   * https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html
   *
   * @param name The app name to process.
   * @return An exactly four digit code that is stable with respect to the passed name.
   */
  private String computePkgInfoSignature(String name) {
    String[] words =
        name.replaceAll("-", " ")
            .replaceAll("_", " ")
            .replaceAll("\\.", " ")
            .replaceAll(",", " ")
            .replaceAll("\\+", " ")
            .replaceAll("(.)(\\p{Upper}|\\d)", "$1 $2") // CamelCaseSplitting
            .replaceAll(" +", " ")
            .strip()
            .split(" ");

    String candidate = "";
    if (words.length >= 4) {
      // Use the first character of the first four words in the name.
      candidate =
          words[0].substring(0, 1)
              + words[1].substring(0, 1)
              + words[2].substring(0, 1)
              + words[3].substring(0, 1);
    } else if (words.length == 3 && words[0].length() > 1) {
      // Use the first two letters from the first word and the first letter of 2 and 3.
      candidate = words[0].substring(0, 2) + words[1].charAt(0) + words[2].charAt(0);
    } else if (words.length == 2 && words[0].length() > 1 && words[1].length() > 1) {
      // Use the first two letters of both words.
      candidate = words[0].substring(0, 2) + words[1].substring(0, 2);
    } else if (words.length == 2 && words[0].length() >= 3) {
      // Use the first three letters from the first word.
      candidate = words[0].substring(0, 3) + words[1].substring(0, 1);
    } else {
      // Just use the first word.
      candidate = words[0];
    }

    // Fill the remaining characters with Xs.
    return (candidate + "XXXX").substring(0, 4);
  }

  /** Compute a bundle identifier from the main class’s parent package. */
  private String computeBundleIdentifier() {
    String bundleIdentifier = "";
    String nextWord = "";

    // Keep adding words except the last one.
    for (char c : getMainClass().toCharArray()) {
      if (c == '.') {
        bundleIdentifier = (bundleIdentifier.isEmpty() ? "" : bundleIdentifier + ".") + nextWord;
        nextWord = "";
      } else {
        nextWord += c;
      }
    }
    return bundleIdentifier;
  }

  /** The main action: Execute the plugin. */
  @TaskAction
  public void taskAction() throws IOException {
    // Fill in the bundle identifier now from the current main class.
    if (bundleIdentifier == null) bundleIdentifier = computeBundleIdentifier();

    // Generate the app directory
    File app = getMacApp();
    if (app.exists()) app.delete();
    app.mkdirs();

    File contents = new File(app, "Contents");
    contents.mkdirs();

    // Write the PkgInfo file.
    FileUtils.writeToFile(new File(contents, "PkgInfo"), "APPL" + pkgInfoSignature + "\n");

    File macOS = new File(contents, "MacOS");
    macOS.mkdirs();

    // Copy the universal java application stub and mark it as executable.
    Task ujasTask = project.getTasks().findByPath("universalJavaApplicationStub");
    File universalJavaApplicationStub = ujasTask.getOutputs().getFiles().getSingleFile();
    FileUtils.copyToDir(universalJavaApplicationStub, macOS);
    FileUtils.setExecutable(new File(macOS, universalJavaApplicationStub.getName()));

    File resources = new File(contents, "Resources");
    resources.mkdirs();

    File javaDir = new File(resources, "Java");
    javaDir.mkdirs();

    // Copy the icon file to the resources directory
    String iconName = null;
    if (icon != null) {
      File iconFile = new File(icon);
      String fileName = iconFile.getName();
      iconName =
          fileName.endsWith(".icns") ? fileName.substring(0, fileName.length() - 5) : fileName;
      FileUtils.copyToDir(iconFile, resources);
    }

    // Copy additional resources to the resources directory
    if (additionalResources != null) {
      for (String resourcePath : additionalResources) {
        File resourceFile = new File(resourcePath);
        FileUtils.copyToDir(resourceFile, resources);
      }
    }

    // Get the jar task
    Jar jarTask = (Jar) getProject().getTasks().getByName("jar");

    // Get runtime classpath configuration
    Configuration runtimeClasspath = getProject().getConfigurations().getByName("runtimeClasspath");

    // Copy the main jar and mark it as executable
    File mainJar = new File(javaDir, getProject().getName() + ".jar");
    FileUtils.copyToFile(jarTask.getOutputs().getFiles().getSingleFile(), mainJar);
    FileUtils.setExecutable(mainJar);

    for (File file : runtimeClasspath.getFiles()) FileUtils.copyToDir(file, javaDir);

    // Setup the info.plist object with all the app metadata
    infoPlist.createEntry("CFBundleInfoDictionaryVersion", "6.0");
    infoPlist.createEntry("CFBundleAllowMixedLocalizations", true);
    infoPlist.createEntry("CFBundleDisplayName", appName);
    infoPlist.createEntry("CFBundlePackageType", "APPL");
    infoPlist.createEntry("CFBundleName", appName);
    infoPlist.createEntry("CFBundleIdentifier", bundleIdentifier);
    infoPlist.createEntry("CFBundleExecutable", universalJavaApplicationStub.getName());
    infoPlist.createEntry("CFBundleVersion", (String) project.getVersion());
    infoPlist.createEntry("CFBundleShortVersionString", (String) project.getVersion());
    infoPlist.createEntry("CFBundleDevelopmentRegion", developmentRegion);
    infoPlist.createEntry("NSHumanReadableCopyright", copyright);
    infoPlist.createEntry("CFBundleIconFile", iconName);
    infoPlist.createEntry("CFBundleIconName", iconName);
    infoPlist.createEntry("NSHighResolutionCapable", true);
    // Settings for files that can be opened in said app.
    if (viewableDocumentTypes != null)
      infoPlist.documentTypes(
          new DocumentType[] {
            new DocumentType(appName, "Viewer", "Alternate", viewableDocumentTypes)
          });
    // Settings for the universal java application stub.
    infoPlist.javaX(
        getMainClass(),
        mainJar.getName(),
        Integer.parseInt(
            ((JavaVersion) project.getProperties().get("targetCompatibility")).getMajorVersion()));
    // Save the Info.plist object.
    try {
      infoPlist.save(new File(contents, "Info.plist"));
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }

    // Create a zip file for distribution
    FileUtils.tarGz(app, getMacAppTarGz());
  }
}
