package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.io.IOException;

import groovy.lang.Closure;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

/** Create a mac app file structure. */
public class AppBundler extends DefaultTask {

  public AppBundler() {
    setDescription("Create an unsigned mac .app bundle.");
    setGroup("Make Mac App");
  }

  /**
   * The name of the app, without any extension (customizable, auto).
   *
   * <p>Default: "${project.name}"
   *
   * <p>Used in the Info.plist as CFBundleDisplayName and CFBundleName.
   */
  private final Property<String> appName = getProject().getObjects().property(String.class);

  /**
   * The directory to write all the data to (customizable, auto).
   *
   * <p>Default: "${buildDir}/unsignedMacApp"
   */
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  /** The package signature (customizable, autocomputed from appName) for the CFBundleIdentifier. */
  private final Property<String> pkgInfoSignature = getProject().getObjects().property(String.class);

  /** The development region for CFBundleDevelopmentRegion (customizable, optional) (en, de, …). */
  private final Property<String> developmentRegion = getProject().getObjects().property(String.class);

  /** The CFBundleIdentifier (customizable, auto). Computed from the main class’s parent package. */
  private final Property<String> bundleIdentifier = getProject().getObjects().property(String.class);

  /** A copyright string (customizable, optional) for NSHumanReadableCopyright. */
  private final Property<String> copyright = getProject().getObjects().property(String.class);

  /** The path to an icns file to use. */
  private final Property<String> icon = getProject().getObjects().property(String.class);

  /** The path to additional resources to copy into the Resources folder. */
  private final ListProperty<String> additionalResources =
      getProject().getObjects().listProperty(String.class);

  /** A list of document types that can be opened with this app as UTIs (customizable, optional). */
  private final ListProperty<String> viewableDocumentTypes =
      getProject().getObjects().listProperty(String.class);

  private final Property<String> mainClassName = getProject().getObjects().property(String.class);
  private final Property<String> projectName = getProject().getObjects().property(String.class);
  private final Property<String> projectVersion = getProject().getObjects().property(String.class);
  private final Property<Integer> targetJavaVersion = getProject().getObjects().property(Integer.class);
  private final ConfigurableFileCollection javaApplicationStubFiles = getProject().files();
  private final ConfigurableFileCollection mainJarFiles = getProject().files();
  private final ConfigurableFileCollection runtimeClasspath = getProject().files();

  @Input
  public String getAppName() {
    return appName.get();
  }

  public void setAppName(String appName) {
    this.appName.set(appName);
  }

  @Internal
  public String getOutdir() {
    return outdir.get().getAsFile().getAbsolutePath();
  }

  public void setOutdir(String outdir) {
    this.outdir.fileValue(new File(outdir));
  }

  public void setOutdir(File outdir) {
    this.outdir.fileValue(outdir);
  }

  @Input
  public String getPkgInfoSignature() {
    return pkgInfoSignature.get();
  }

  public void setPkgInfoSignature(String pkgInfoSignature) {
    this.pkgInfoSignature.set(pkgInfoSignature);
  }

  @Input
  @Optional
  public String getDevelopmentRegion() {
    return developmentRegion.getOrNull();
  }

  public void setDevelopmentRegion(String developmentRegion) {
    this.developmentRegion.set(developmentRegion);
  }

  @Input
  @Optional
  public String getBundleIdentifier() {
    return bundleIdentifier.getOrNull();
  }

  public void setBundleIdentifier(String bundleIdentifier) {
    this.bundleIdentifier.set(bundleIdentifier);
  }

  @Input
  @Optional
  public String getCopyright() {
    return copyright.getOrNull();
  }

  public void setCopyright(String copyright) {
    this.copyright.set(copyright);
  }

  @Input
  @Optional
  public String getIcon() {
    return icon.getOrNull();
  }

  public void setIcon(String icon) {
    this.icon.set(icon);
  }

  @Input
  @Optional
  public java.util.List<String> getAdditionalResources() {
    return additionalResources.getOrNull();
  }

  public void setAdditionalResources(String[] additionalResources) {
    this.additionalResources.set(java.util.Arrays.asList(additionalResources));
  }

  public void setAdditionalResources(java.util.List<String> additionalResources) {
    this.additionalResources.set(additionalResources);
  }

  @Input
  @Optional
  public java.util.List<String> getViewableDocumentTypes() {
    return viewableDocumentTypes.getOrNull();
  }

  public void setViewableDocumentTypes(String[] viewableDocumentTypes) {
    this.viewableDocumentTypes.set(java.util.Arrays.asList(viewableDocumentTypes));
  }

  public void setViewableDocumentTypes(java.util.List<String> viewableDocumentTypes) {
    this.viewableDocumentTypes.set(viewableDocumentTypes);
  }

  @Input
  public String getMainClassName() {
    return mainClassName.get();
  }

  public void setMainClassName(String mainClassName) {
    this.mainClassName.set(mainClassName);
  }

  @Input
  public String getProjectName() {
    return projectName.get();
  }

  public void setProjectName(String projectName) {
    this.projectName.set(projectName);
  }

  @Input
  public String getProjectVersion() {
    return projectVersion.get();
  }

  public void setProjectVersion(String projectVersion) {
    this.projectVersion.set(projectVersion);
  }

  @Input
  public Integer getTargetJavaVersion() {
    return targetJavaVersion.get();
  }

  public void setTargetJavaVersion(Integer targetJavaVersion) {
    this.targetJavaVersion.set(targetJavaVersion);
  }

  @InputFiles
  public ConfigurableFileCollection getJavaApplicationStubFiles() {
    return javaApplicationStubFiles;
  }

  @InputFiles
  public ConfigurableFileCollection getMainJarFiles() {
    return mainJarFiles;
  }

  @Classpath
  public ConfigurableFileCollection getRuntimeClasspath() {
    return runtimeClasspath;
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
  public Property<String> getIconProperty() {
    return icon;
  }

  @Internal
  public Property<String> getMainClassNameProperty() {
    return mainClassName;
  }

  @Internal
  public Property<String> getProjectNameProperty() {
    return projectName;
  }

  @Internal
  public Property<String> getProjectVersionProperty() {
    return projectVersion;
  }

  @Internal
  public Property<Integer> getTargetJavaVersionProperty() {
    return targetJavaVersion;
  }

  /** The logger to use for file logging */
  @SuppressWarnings("unused")
  private final Logger logger = Logging.getLogger(getClass());

  {
    appName.convention(getProject().getName());
    outdir.convention(getProject().getLayout().getBuildDirectory().dir("unsignedMacApp"));
    pkgInfoSignature.convention(appName.map(this::computePkgInfoSignature));
  }

  /** Configure the javaApplicationStub task from the macApp task. */
  public void setJavaApplicationStub(JavaApplicationStubSource source) {
    ((JASDownloader) getProject().getTasks().getByName("javaApplicationStub")).setSource(source);
  }

  /** Configure the javaApplicationStub task from the macApp task. */
  public void javaApplicationStub(Closure<?> closure) {
    JASDownloader jas = (JASDownloader) getProject().getTasks().getByName("javaApplicationStub");
    closure.setDelegate(jas);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
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
  public JavaApplicationStubSource getNativeJavaApplicationStubv0_9() {
    return JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V0_9;
  }

  /** The app bundle that is generated. */
  @OutputDirectory
  public File getMacApp() {
    return new File(getOutdir(), getAppName() + ".app");
  }

  /** The app bundle that is generated. */
  @OutputFile
  public File getMacAppTarGz() {
    return new File(getOutdir(), getAppName() + ".tgz");
  }

  /**
   * Compute a code to use for the app PkgInfo signature:
   * https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html
   *
   * @param name The app name to process.
   * @return An exactly four digit code that is stable with respect to the passed name.
   */
  private String computePkgInfoSignature(String name) {
    String[] words =
        name.replace("-", " ")
            .replace("_", " ")
            .replaceAll("\\.", " ")
            .replace(",", " ")
            .replaceAll("\\+", " ")
            .replaceAll("(.)(\\p{Upper}|\\d)", "$1 $2") // CamelCaseSplitting
            .replaceAll(" +", " ")
            .strip()
            .split(" ");

    String candidate;
    if (words.length >= 4) {
      // Use the first character of the first four words in the name.
      candidate =
          "" + words[0].charAt(0) + words[1].charAt(0) + words[2].charAt(0) + words[3].charAt(0);
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
    StringBuilder nextWord = new StringBuilder();

    // Keep adding words except the last one.
    for (char c : getMainClassName().toCharArray()) {
      if (c == '.') {
        bundleIdentifier =
            (bundleIdentifier.isEmpty() ? "" : bundleIdentifier + ".") + nextWord.toString();
        nextWord.setLength(0);
      } else {
        nextWord.append(c);
      }
    }
    return bundleIdentifier;
  }

  /** The main action: Execute the plugin. */
  @TaskAction
  public void taskAction() throws IOException {
    Plist infoPlist = new Plist();

    // Fill in the bundle identifier now from the current main class.
    if (getBundleIdentifier() == null) setBundleIdentifier(computeBundleIdentifier());

    // Generate the app directory
    File app = getMacApp();
    if (app.exists()) FileUtils.deleteRecursively(app);
    if (!app.mkdirs()) {
      throw new IllegalStateException("Could not create app directory: " + app);
    }

    File contents = new File(app, "Contents");
    if (!contents.mkdirs()) {
      throw new IllegalStateException("Could not create Contents directory: " + contents);
    }

    // Write the PkgInfo file.
    FileUtils.writeToFile(new File(contents, "PkgInfo"), "APPL" + getPkgInfoSignature() + "\n");

    File macOS = new File(contents, "MacOS");
    if (!macOS.mkdirs()) {
      throw new IllegalStateException("Could not create MacOS directory: " + macOS);
    }

    // Copy the Java application stub and mark it as executable.
    File javaApplicationStub = getJavaApplicationStubFiles().getSingleFile();
    FileUtils.copyToDir(javaApplicationStub, macOS);
    FileUtils.setExecutable(new File(macOS, javaApplicationStub.getName()));

    File resources = new File(contents, "Resources");
    if (!resources.mkdirs()) {
      throw new IllegalStateException("Could not create Resources directory: " + resources);
    }

    File javaDir = new File(resources, "Java");
    if (!javaDir.mkdirs()) {
      throw new IllegalStateException("Could not create Resources/Java directory: " + javaDir);
    }

    // Copy the icon file to the resources directory
    String iconName = null;
    if (getIcon() != null) {
      File iconFile = new File(getIcon());
      String fileName = iconFile.getName();
      iconName =
          fileName.endsWith(".icns") ? fileName.substring(0, fileName.length() - 5) : fileName;
      FileUtils.copyToDir(iconFile, resources);
    }

    // Copy additional resources to the resources directory
    if (getAdditionalResources() != null) {
      for (String resourcePath : getAdditionalResources()) {
        File resourceFile = new File(resourcePath);
        FileUtils.copyToDir(resourceFile, resources);
      }
    }

    // Get the jar task
    // Copy the main jar and mark it as executable
    File mainJar = new File(javaDir, getProjectName() + ".jar");
    FileUtils.copyToFile(getMainJarFiles().getSingleFile(), mainJar);
    FileUtils.setExecutable(mainJar);

    for (File file : getRuntimeClasspath().getFiles()) FileUtils.copyToDir(file, javaDir);

    // Setup the info.plist object with all the app metadata
    infoPlist.createEntry("CFBundleInfoDictionaryVersion", "6.0");
    infoPlist.createEntry("CFBundleAllowMixedLocalizations", true);
    infoPlist.createEntry("CFBundleDisplayName", getAppName());
    infoPlist.createEntry("CFBundlePackageType", "APPL");
    infoPlist.createEntry("CFBundleName", getAppName());
    infoPlist.createEntry("CFBundleIdentifier", getBundleIdentifier());
    infoPlist.createEntry("CFBundleExecutable", javaApplicationStub.getName());
    infoPlist.createEntry("CFBundleVersion", getProjectVersion());
    infoPlist.createEntry("CFBundleShortVersionString", getProjectVersion());
    infoPlist.createEntry("CFBundleDevelopmentRegion", getDevelopmentRegion());
    infoPlist.createEntry("NSHumanReadableCopyright", getCopyright());
    infoPlist.createEntry("CFBundleIconFile", iconName);
    infoPlist.createEntry("CFBundleIconName", iconName);
    infoPlist.createEntry("NSHighResolutionCapable", true);
    // Settings for files that can be opened in said app.
    if (getViewableDocumentTypes() != null)
      infoPlist.documentTypes(
          new DocumentType[] {
            new DocumentType(
                getAppName(),
                "Viewer",
                "Alternate",
                getViewableDocumentTypes().toArray(new String[0]))
          });
    // Settings for the universal java application stub.
    infoPlist.javaX(
        getMainClassName(),
        mainJar.getName(),
        getTargetJavaVersion());
    // Save the Info.plist object.
    try {
      infoPlist.save(new File(contents, "Info.plist"));
    } catch (ParserConfigurationException | TransformerException e) {
      throw new IllegalStateException("Could not write Info.plist", e);
    }

    // Create a zip file for distribution
    FileUtils.tarGz(app, getMacAppTarGz());
  }
}
