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
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

/** Create a mac app file structure. */
@CacheableTask
public class AppBundler extends DefaultTask {

  /** Creates the task with default group and description. */
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
  private final Property<String> pkgInfoSignature =
      getProject().getObjects().property(String.class);

  /** The development region for CFBundleDevelopmentRegion (customizable, optional) (en, de, …). */
  private final Property<String> developmentRegion =
      getProject().getObjects().property(String.class);

  /** The CFBundleIdentifier (customizable, auto). Computed from the main class’s parent package. */
  private final Property<String> bundleIdentifier =
      getProject().getObjects().property(String.class);

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

  /** Java system properties for JavaX/Properties (for example <code>-Dfoo=bar</code>). */
  private final ListProperty<String> javaProperties =
      getProject().getObjects().listProperty(String.class);

  /** JavaX VMOptions entries (for example <code>-Xmx512m</code>). */
  private final ListProperty<String> vmOptions =
      getProject().getObjects().listProperty(String.class);

  /** JavaX main class arguments. */
  private final ListProperty<String> mainArguments =
      getProject().getObjects().listProperty(String.class);

  /** Whether JavaX should launch on the macOS main thread. */
  private final Property<Boolean> startOnMainThread =
      getProject().getObjects().property(Boolean.class);

  /** Optional JavaX splash file name from the app Resources folder. */
  private final Property<String> splashFile = getProject().getObjects().property(String.class);

  /** The main class in the format <code>com.example.Main</code>. */
  private final Property<String> mainClassName = getProject().getObjects().property(String.class);

  /** The Gradle project name used for output naming. */
  private final Property<String> projectName = getProject().getObjects().property(String.class);

  /** The project version used in bundle metadata. */
  private final Property<String> projectVersion = getProject().getObjects().property(String.class);

  /** The minimum Java version required by the bundled app launcher metadata. */
  private final Property<Integer> targetJavaVersion =
      getProject().getObjects().property(Integer.class);

  /** The downloaded Java application stub executable produced by the helper task. */
  private final ConfigurableFileCollection javaApplicationStubFiles = getProject().files();

  /** The primary application JAR generated by the <code>jar</code> task. */
  private final ConfigurableFileCollection mainJarFiles = getProject().files();

  /** Runtime classpath entries copied into <code>Contents/Resources/Java</code>. */
  private final ConfigurableFileCollection runtimeClasspath = getProject().files();

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
   * Sets the app name (without extension).
   *
   * @param appName the app name to set
   */
  public void setAppName(String appName) {
    this.appName.set(appName);
  }

  /**
   * Gets the output directory path.
   *
   * @return the output directory absolute path
   */
  @Internal
  public String getOutdir() {
    return outdir.get().getAsFile().getAbsolutePath();
  }

  /**
   * Sets the output directory path.
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
   * Gets the four-character PkgInfo signature.
   *
   * @return the package signature
   */
  @Input
  public String getPkgInfoSignature() {
    return pkgInfoSignature.get();
  }

  /**
   * Sets the package signature for the PkgInfo file.
   *
   * @param pkgInfoSignature the four-character package signature
   */
  public void setPkgInfoSignature(String pkgInfoSignature) {
    this.pkgInfoSignature.set(pkgInfoSignature);
  }

  /**
   * Gets the CFBundle development region.
   *
   * @return the development region, or null when unset
   */
  @Input
  @Optional
  public String getDevelopmentRegion() {
    return developmentRegion.getOrNull();
  }

  /**
   * Sets the CFBundle development region.
   *
   * @param developmentRegion the development region code
   */
  public void setDevelopmentRegion(String developmentRegion) {
    this.developmentRegion.set(developmentRegion);
  }

  /**
   * Gets the bundle identifier.
   *
   * @return the bundle identifier, or null when unset
   */
  @Input
  @Optional
  public String getBundleIdentifier() {
    return bundleIdentifier.getOrNull();
  }

  /**
   * Sets the bundle identifier.
   *
   * @param bundleIdentifier the bundle identifier
   */
  public void setBundleIdentifier(String bundleIdentifier) {
    this.bundleIdentifier.set(bundleIdentifier);
  }

  /**
   * Gets the copyright string.
   *
   * @return the copyright value, or null when unset
   */
  @Input
  @Optional
  public String getCopyright() {
    return copyright.getOrNull();
  }

  /**
   * Sets the copyright string.
   *
   * @param copyright the copyright value
   */
  public void setCopyright(String copyright) {
    this.copyright.set(copyright);
  }

  /**
   * Gets the icon file path.
   *
   * @return the icon file path, or null when unset
   */
  @Internal
  public String getIcon() {
    return icon.getOrNull();
  }

  /**
   * Sets the icon file path.
   *
   * @param icon the icon file path
   */
  public void setIcon(String icon) {
    this.icon.set(icon);
  }

  /**
   * Gets additional resource paths to copy into the app bundle.
   *
   * @return additional resource paths, or null when unset
   */
  @Internal
  public java.util.List<String> getAdditionalResources() {
    return additionalResources.getOrNull();
  }

  /**
   * Sets additional resources using an array.
   *
   * @param additionalResources additional resource paths
   */
  public void setAdditionalResources(String[] additionalResources) {
    this.additionalResources.set(java.util.Arrays.asList(additionalResources));
  }

  /**
   * Sets additional resources using a list.
   *
   * @param additionalResources additional resource paths
   */
  public void setAdditionalResources(java.util.List<String> additionalResources) {
    this.additionalResources.set(additionalResources);
  }

  /**
   * Gets viewable document UTI values.
   *
   * @return viewable document types, or null when unset
   */
  @Input
  @Optional
  public java.util.List<String> getViewableDocumentTypes() {
    return viewableDocumentTypes.getOrNull();
  }

  /**
   * Sets viewable document types using an array.
   *
   * @param viewableDocumentTypes document UTI values
   */
  public void setViewableDocumentTypes(String[] viewableDocumentTypes) {
    this.viewableDocumentTypes.set(java.util.Arrays.asList(viewableDocumentTypes));
  }

  /**
   * Sets viewable document types using a list.
   *
   * @param viewableDocumentTypes document UTI values
   */
  public void setViewableDocumentTypes(java.util.List<String> viewableDocumentTypes) {
    this.viewableDocumentTypes.set(viewableDocumentTypes);
  }

  /**
   * Gets Java system property arguments.
   *
   * @return Java property arguments, or null when unset
   */
  @Input
  @Optional
  public java.util.List<String> getJavaProperties() {
    return javaProperties.getOrNull();
  }

  /**
   * Sets Java system property arguments using an array.
   *
   * @param javaProperties Java property arguments
   */
  public void setJavaProperties(String[] javaProperties) {
    this.javaProperties.set(java.util.Arrays.asList(javaProperties));
  }

  /**
   * Sets Java system property arguments using a list.
   *
   * @param javaProperties Java property arguments
   */
  public void setJavaProperties(java.util.List<String> javaProperties) {
    this.javaProperties.set(javaProperties);
  }

  /**
   * Gets JVM option arguments.
   *
   * @return JVM options, or null when unset
   */
  @Input
  @Optional
  public java.util.List<String> getVmOptions() {
    return vmOptions.getOrNull();
  }

  /**
   * Sets JVM option arguments using an array.
   *
   * @param vmOptions JVM options
   */
  public void setVmOptions(String[] vmOptions) {
    this.vmOptions.set(java.util.Arrays.asList(vmOptions));
  }

  /**
   * Sets JVM option arguments using a list.
   *
   * @param vmOptions JVM options
   */
  public void setVmOptions(java.util.List<String> vmOptions) {
    this.vmOptions.set(vmOptions);
  }

  /**
   * Gets main-class arguments.
   *
   * @return main arguments, or null when unset
   */
  @Input
  @Optional
  public java.util.List<String> getMainArguments() {
    return mainArguments.getOrNull();
  }

  /**
   * Sets main-class arguments using an array.
   *
   * @param mainArguments main arguments
   */
  public void setMainArguments(String[] mainArguments) {
    this.mainArguments.set(java.util.Arrays.asList(mainArguments));
  }

  /**
   * Sets main-class arguments using a list.
   *
   * @param mainArguments main arguments
   */
  public void setMainArguments(java.util.List<String> mainArguments) {
    this.mainArguments.set(mainArguments);
  }

  /**
   * Gets the main-thread startup flag.
   *
   * @return true/false value, or null when unset
   */
  @Input
  @Optional
  public Boolean getStartOnMainThread() {
    return startOnMainThread.getOrNull();
  }

  /**
   * Sets whether launch must happen on the macOS main thread.
   *
   * @param startOnMainThread startup flag
   */
  public void setStartOnMainThread(Boolean startOnMainThread) {
    this.startOnMainThread.set(startOnMainThread);
  }

  /**
   * Gets the splash file path.
   *
   * @return splash file path, or null when unset
   */
  @Internal
  public String getSplashFile() {
    return splashFile.getOrNull();
  }

  /**
   * Sets the splash file path.
   *
   * @param splashFile splash file path
   */
  public void setSplashFile(String splashFile) {
    this.splashFile.set(splashFile);
  }

  /**
   * Gets the fully-qualified main class name.
   *
   * @return the main class name
   */
  @Input
  public String getMainClassName() {
    return mainClassName.get();
  }

  /**
   * Sets the fully-qualified main class name.
   *
   * @param mainClassName the main class name
   */
  public void setMainClassName(String mainClassName) {
    this.mainClassName.set(mainClassName);
  }

  /**
   * Gets the project name.
   *
   * @return project name
   */
  @Input
  public String getProjectName() {
    return projectName.get();
  }

  /**
   * Sets the project name.
   *
   * @param projectName project name
   */
  public void setProjectName(String projectName) {
    this.projectName.set(projectName);
  }

  /**
   * Gets the project version.
   *
   * @return project version
   */
  @Input
  public String getProjectVersion() {
    return projectVersion.get();
  }

  /**
   * Sets the project version.
   *
   * @param projectVersion project version
   */
  public void setProjectVersion(String projectVersion) {
    this.projectVersion.set(projectVersion);
  }

  /**
   * Gets the minimum Java version for launcher metadata.
   *
   * @return target Java version
   */
  @Input
  public Integer getTargetJavaVersion() {
    return targetJavaVersion.get();
  }

  /**
   * Sets the minimum Java version for launcher metadata.
   *
   * @param targetJavaVersion target Java version
   */
  public void setTargetJavaVersion(Integer targetJavaVersion) {
    this.targetJavaVersion.set(targetJavaVersion);
  }

  /**
   * Gets the Java application stub input files.
   *
   * @return configured stub input files
   */
  @InputFiles
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public ConfigurableFileCollection getJavaApplicationStubFiles() {
    return javaApplicationStubFiles;
  }

  /**
   * Gets the main JAR input files.
   *
   * @return configured main JAR input files
   */
  @InputFiles
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public ConfigurableFileCollection getMainJarFiles() {
    return mainJarFiles;
  }

  /**
   * Gets runtime classpath input files.
   *
   * @return configured runtime classpath files
   */
  @Classpath
  public ConfigurableFileCollection getRuntimeClasspath() {
    return runtimeClasspath;
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
   * Returns the icon property.
   *
   * @return icon property
   */
  @Internal
  public Property<String> getIconProperty() {
    return icon;
  }

  /**
   * Returns the main class property.
   *
   * @return main class property
   */
  @Internal
  public Property<String> getMainClassNameProperty() {
    return mainClassName;
  }

  /**
   * Returns the project name property.
   *
   * @return project name property
   */
  @Internal
  public Property<String> getProjectNameProperty() {
    return projectName;
  }

  /**
   * Returns the project version property.
   *
   * @return project version property
   */
  @Internal
  public Property<String> getProjectVersionProperty() {
    return projectVersion;
  }

  /**
   * Returns the target Java version property.
   *
   * @return target Java version property
   */
  @Internal
  public Property<Integer> getTargetJavaVersionProperty() {
    return targetJavaVersion;
  }

  /**
   * Returns the Java properties list property.
   *
   * @return Java properties list property
   */
  @Internal
  public ListProperty<String> getJavaPropertiesProperty() {
    return javaProperties;
  }

  /**
   * Returns the VM options list property.
   *
   * @return VM options list property
   */
  @Internal
  public ListProperty<String> getVmOptionsProperty() {
    return vmOptions;
  }

  /**
   * Returns the main arguments list property.
   *
   * @return main arguments list property
   */
  @Internal
  public ListProperty<String> getMainArgumentsProperty() {
    return mainArguments;
  }

  /**
   * Returns the main-thread startup property.
   *
   * @return main-thread startup property
   */
  @Internal
  public Property<Boolean> getStartOnMainThreadProperty() {
    return startOnMainThread;
  }

  /**
   * Returns the splash file property.
   *
   * @return splash file property
   */
  @Internal
  public Property<String> getSplashFileProperty() {
    return splashFile;
  }

  /**
   * Gets the icon file input for Gradle incremental checks.
   *
   * @return icon file, or null when no icon is configured
   */
  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getIconFile() {
    String iconPath = getIcon();
    return iconPath == null ? null : new File(iconPath);
  }

  /**
   * Gets additional resource file inputs for Gradle incremental checks.
   *
   * @return additional resource file collection, possibly empty
   */
  @Optional
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getAdditionalResourceFiles() {
    java.util.List<String> resources = getAdditionalResources();
    if (resources == null) return getProject().files();
    return getProject().files((Object[]) resources.toArray(new String[0]));
  }

  /**
   * Gets the configured splash image file input.
   *
   * @return splash image file, or null when no splash is configured
   */
  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getSplashFileInput() {
    String splashPath = getSplashFile();
    return splashPath == null ? null : new File(splashPath);
  }

  /**
   * Gets the optional <code>@2x</code> splash image file next to the configured splash image.
   *
   * @return retina splash image file, or null when not present
   */
  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getSplashFile2xInput() {
    File splash = getSplashFileInput();
    if (splash == null) return null;

    String splashName = splash.getName();
    int dot = splashName.lastIndexOf('.');
    String basename = dot >= 0 ? splashName.substring(0, dot) : splashName;
    String extension = dot >= 0 ? splashName.substring(dot) : "";
    String splash2xName = basename + "@2x" + extension;
    File splash2x =
        splash.getParent() == null
            ? new File(splash2xName)
            : new File(splash.getParent(), splash2xName);
    return splash2x.isFile() ? splash2x : null;
  }

  /** The logger to use for file logging */
  @SuppressWarnings("unused")
  private final Logger logger = Logging.getLogger(getClass());

  {
    appName.convention(getProject().getName());
    outdir.convention(getProject().getLayout().getBuildDirectory().dir("unsignedMacApp"));
    pkgInfoSignature.convention(appName.map(this::computePkgInfoSignature));
    bundleIdentifier.convention(mainClassName.map(this::computeBundleIdentifier));
  }

  /**
   * Configures the <code>javaApplicationStub</code> task from this task.
   *
   * @param source combined source descriptor for URL, unzip mode, and executable name
   */
  public void setJavaApplicationStub(JavaApplicationStubSource source) {
    ((JASDownloader) getProject().getTasks().getByName("javaApplicationStub")).setSource(source);
  }

  /**
   * Configures the <code>javaApplicationStub</code> task using a delegated closure.
   *
   * @param closure configuration closure delegated to <code>JASDownloader</code>
   */
  public void javaApplicationStub(Closure<?> closure) {
    JASDownloader jas = (JASDownloader) getProject().getTasks().getByName("javaApplicationStub");
    closure.setDelegate(jas);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }

  /**
   * Gets the shell-script preset source for universalJavaApplicationStub.
   *
   * @return shell-script preset source
   */
  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubShell() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_SHELL;
  }

  /**
   * Gets the precompiled preset source for universalJavaApplicationStub.
   *
   * @return precompiled preset source
   */
  @Internal
  public JavaApplicationStubSource getUniversalJavaApplicationStubProcompiled() {
    return JavaApplicationStubPresets.UNIVERSAL_JAVA_APPLICATION_STUB_PROCOMPILED;
  }

  /**
   * Gets the NativeJavaApplicationStub v1 preset source.
   *
   * @return native v1 preset source
   */
  @Internal
  public JavaApplicationStubSource getNativeJavaApplicationStubv1() {
    return JavaApplicationStubPresets.NATIVE_JAVA_APPLICATION_STUB_V1;
  }

  /**
   * The app bundle that is generated.
   *
   * @return generated app bundle directory
   */
  @OutputDirectory
  public File getMacApp() {
    return new File(getOutdir(), getAppName() + ".app");
  }

  /**
   * The archive path traditionally generated alongside the app bundle.
   *
   * @return generated tar.gz archive file path
   */
  @Internal
  public File getMacAppTarGz() {
    return new File(getOutdir(), getAppName() + ".tgz");
  }

  /**
   * Compute a code to use for the app PkgInfo signature.
   *
   * <p>Reference: <a href="https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html">Apple Runtime Configuration documentation</a>
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
  private String computeBundleIdentifier(String mainClassName) {
    String bundleIdentifier = "";
    StringBuilder nextWord = new StringBuilder();

    // Keep adding words except the last one.
    for (char c : mainClassName.toCharArray()) {
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

  /**
   * The main action: execute the task and generate the unsigned app bundle.
   *
   * @throws IOException If required file operations fail.
   */
  @TaskAction
  public void taskAction() throws IOException {
    Plist infoPlist = new Plist();

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

    // Copy splash file to the resources directory when configured.
    String splashFileName = null;
    if (getSplashFile() != null) {
      File splash = new File(getSplashFile());
      splashFileName = splash.getName();
      FileUtils.copyToDir(splash, resources);

      // Copy the optional retina variant when it sits next to the base splash image.
      File splash2x = getSplashFile2xInput();
      if (splash2x != null) FileUtils.copyToDir(splash2x, resources);
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
        getTargetJavaVersion(),
        getJavaProperties(),
        getVmOptions(),
        getStartOnMainThread(),
        getMainArguments(),
        splashFileName);
    // Save the Info.plist object.
    try {
      infoPlist.save(new File(contents, "Info.plist"));
    } catch (ParserConfigurationException | TransformerException e) {
      throw new IllegalStateException("Could not write Info.plist", e);
    }
  }
}
