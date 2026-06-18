package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/** The task that signs and notarizes the mac app. Only works on macOS. */
@DisableCachingByDefault(
    because = "Signing/notarization depends on external services and keychain state")
public class SignAndNotarize extends DefaultTask {

  public SignAndNotarize() {
    setDescription("Sign and notarize the mac .app bundle.");
    setGroup("Make Mac App");
    // Signing/notarization has external side effects and must not be considered up-to-date.
    getOutputs().upToDateWhen(task -> false);
  }

  /**
   * The name of the keychain used to hold the variable (customizable, auto). If not overwritten, it
   * generates a default one with a unique name that is deleted after use. However, one can also set
   * it to login.keychain – which will not be deleted after use.
   */
  private final Property<String> keychainName = getProject().getObjects().property(String.class);

  /** The password for the keychain. Has to be the user password if the login keychain is used. */
  private final Property<String> keychainPassword =
      getProject().getObjects().property(String.class);

  /**
   * The path to the signing certificate (customizable, auto). Defaults to one read from environment
   * variables. Either a path in <em>$APPLE_SIGNING_P12</em> or the base64 encoded contents of a p12
   * file in <em>$APPLE_SIGNING_P12_BASE64</em>. If these variables don’t exist and this property is
   * not set, an error will be thrown.
   *
   * <p>If the variable is null, it will be assigned at task-runtime since the outdir can be changed
   * by the user before then..
   */
  private final Property<String> certificate = getProject().getObjects().property(String.class);

  /**
   * The password to the certificate (customizable, auto). Defaults to one read from the environment
   * variables. Either directly from <em>$APPLE_SIGNING_PASSWORD</em> or using a (not) “secure” base
   * 64 encoded variable <em>$APPLE_SIGNING_PASSWORD_BASE64</em>.
   */
  private final Property<String> certificatePassword =
      getProject().getObjects().property(String.class);

  /**
   * The AppleID used for code signing (customizable, auto). Defaults to one found in an environment
   * variable named <em>$APPLE_SIGN_ID</em>. This has a format lie <em>Developer ID Application: The
   * Company (ASDF213FDSA)</em>.
   */
  private final Property<String> appleSignID = getProject().getObjects().property(String.class);

  /**
   * The AppleID used for code signing (customizable, auto). Defaults to one found in an environment
   * variable named <em>$APPLE_SIGN_ID</em>. This is usually an e-mail address.
   */
  private final Property<String> appleIDUser = getProject().getObjects().property(String.class);

  /**
   * The password to the AppleID used for code signing (customizable, auto). By default it is pulled
   * from the environment variable named <em>$APPLE_ID_PASSWORD</em>.
   */
  private final Property<String> appleIDPassword = getProject().getObjects().property(String.class);

  /**
   * The TeamID of the AppleID used for signing and notarizing the app (customizable, auto). Default
   * is read from the environment variable <em>$APPLE_ID_TEAM_ID</em>. The parenthesized part of the
   * {@link #appleSignID} should be the same.
   */
  private final Property<String> appleIDTeamID = getProject().getObjects().property(String.class);

  /** The directory that is used for outputting the file (customizable, auto). */
  private final DirectoryProperty outdir = getProject().getObjects().directoryProperty();

  private final Property<String> appName = getProject().getObjects().property(String.class);
  private final DirectoryProperty unsignedMacAppDirectory =
      getProject().getObjects().directoryProperty();
  private final DirectoryProperty existingMacAppBundle = getProject().getObjects().directoryProperty();
  private final Property<String> macAppIcon = getProject().getObjects().property(String.class);
  private final Property<String> projectVersion = getProject().getObjects().property(String.class);

  {
    keychainName.convention("TemporaryPaginaSigningKeychain.keychain");
    keychainPassword.convention("TotallySecretPassword");
    certificatePassword.convention(loadCertificatePasswordFromEnv());
    appleSignID.convention(getProject().getProviders().environmentVariable("APPLE_SIGN_ID"));
    appleIDUser.convention(getProject().getProviders().environmentVariable("APPLE_ID_USER"));
    appleIDPassword.convention(getProject().getProviders().environmentVariable("APPLE_ID_PASSWORD"));
    appleIDTeamID.convention(getProject().getProviders().environmentVariable("APPLE_ID_TEAM_ID"));
    outdir.convention(getProject().getLayout().getBuildDirectory().dir("signedMacApp"));
  }

  @Input
  public String getKeychainName() {
    return keychainName.get();
  }

  public void setKeychainName(String keychainName) {
    this.keychainName.set(keychainName);
  }

  @Input
  public String getKeychainPassword() {
    return keychainPassword.get();
  }

  public void setKeychainPassword(String keychainPassword) {
    this.keychainPassword.set(keychainPassword);
  }

  @Input
  @Optional
  public String getCertificate() {
    return certificate.getOrNull();
  }

  public void setCertificate(String certificate) {
    this.certificate.set(certificate);
  }

  @Input
  @Optional
  public String getCertificatePassword() {
    return certificatePassword.getOrNull();
  }

  public void setCertificatePassword(String certificatePassword) {
    this.certificatePassword.set(certificatePassword);
  }

  @Input
  @Optional
  public String getAppleSignID() {
    return appleSignID.getOrNull();
  }

  public void setAppleSignID(String appleSignID) {
    this.appleSignID.set(appleSignID);
  }

  @Input
  @Optional
  public String getAppleIDUser() {
    return appleIDUser.getOrNull();
  }

  public void setAppleIDUser(String appleIDUser) {
    this.appleIDUser.set(appleIDUser);
  }

  @Input
  @Optional
  public String getAppleIDPassword() {
    return appleIDPassword.getOrNull();
  }

  public void setAppleIDPassword(String appleIDPassword) {
    this.appleIDPassword.set(appleIDPassword);
  }

  @Input
  @Optional
  public String getAppleIDTeamID() {
    return appleIDTeamID.getOrNull();
  }

  public void setAppleIDTeamID(String appleIDTeamID) {
    this.appleIDTeamID.set(appleIDTeamID);
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

  @Input
  public String getAppName() {
    if (appName.isPresent()) return appName.get();
    return getInputAppBundleName();
  }


  @Internal
  public DirectoryProperty getUnsignedMacAppDirectoryProperty() {
    return unsignedMacAppDirectory;
  }

  /** Determine the bundle name from the configured input app path. */
  private String getInputAppBundleName() {
    File inputApp = existingMacAppBundle.isPresent() ? existingMacAppBundle.get().getAsFile() : null;
    if (inputApp == null) {
      if (!unsignedMacAppDirectory.isPresent()) {
        throw new InvalidUserDataException(
            "Cannot determine appName: set existingMacAppBundle or configure unsignedMacAppDirectory.");
      }
      inputApp = new File(getMacAppDirectory(), getAppNameFromUnsignedBundleFallback());
    }
    String name = inputApp.getName();
    if (name.endsWith(".app")) return name.substring(0, name.length() - 4);
    return name;
  }

  /** Derive the unsigned bundle name from the task state when appName is not configured. */
  private String getAppNameFromUnsignedBundleFallback() {
    if (appName.isPresent()) return appName.get();
    throw new InvalidUserDataException(
        "Cannot determine appName: configure appName for unsigned signing, or set existingMacAppBundle.");
  }

  /**
   * Optional path to an existing .app bundle to sign/notarize directly.
   *
   * <p>If this is set, the task uses this bundle as input instead of resolving
   * {@code unsignedMacAppDirectory/appName.app}.
   */
  @InputDirectory
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getExistingMacAppBundle() {
    return existingMacAppBundle.isPresent() ? existingMacAppBundle.get().getAsFile() : null;
  }

  public void setExistingMacAppBundle(String existingMacAppBundle) {
    this.existingMacAppBundle.fileValue(new File(existingMacAppBundle));
  }

  public void setExistingMacAppBundle(File existingMacAppBundle) {
    this.existingMacAppBundle.fileValue(existingMacAppBundle);
  }

  @Internal
  public DirectoryProperty getExistingMacAppBundleProperty() {
    return existingMacAppBundle;
  }

  @Input
  @Optional
  public String getMacAppIcon() {
    return macAppIcon.getOrNull();
  }

  public void setMacAppIcon(String macAppIcon) {
    this.macAppIcon.set(macAppIcon);
  }

  @Input
  public String getProjectVersion() {
    return projectVersion.get();
  }

  public void setProjectVersion(String projectVersion) {
    this.projectVersion.set(projectVersion);
  }

  @Internal
  public Property<String> getAppNameProperty() {
    return appName;
  }

  @Internal
  public Property<String> getMacAppIconProperty() {
    return macAppIcon;
  }

  @Internal
  public Property<String> getProjectVersionProperty() {
    return projectVersion;
  }

  @Internal
  public DirectoryProperty getOutdirProperty() {
    return outdir;
  }

  /** The signed mac app bundle. */
  @OutputDirectory
  public File getSignedAndNotarizedMacApp() {
    return new File(getOutdir(), getAppName() + ".app");
  }

  /** The notarized installation disk image. */
  @OutputFile
  public File getNotarizedDMG() {
    return new File(getOutdir(), getAppName() + ".dmg");
  }

  /** A tar.gz file containing the signed app. */
  @Internal
  public File getSignedAndNotarizedMacAppTarGz() {
    return new File(getOutdir(), getAppName() + ".tgz");
  }

  // ===============================================================================================
  // Methods To Fill Variables At Runtime
  // ===============================================================================================

  /**
   * Delete the certificate after use. This is set when loading the certificate from base64 data. It
   * will not be deleted if the file already existed before.
   */
  private boolean deleteCertificateAfter = false;

  /**
   * Get the path to the certificate file. Might be {@link null}. If the <em>$APPLE_SIGNING_P12</em>
   * variable is set in the environment, it‘ll be returned and interpreted as a path. Alternatively,
   * the environment variable <em>$APPLE_SIGNING_P12_BASE64</em> is base64 decoded and stored inside
   * a temp file in the output folder. The file will be deleted after, for now its path is returned.
   */
  private String getCertificateFilePath() {
    // Try to read the path variable
    String path;
    if ((path = System.getenv("APPLE_SIGNING_P12")) != null) return path;

    // Read the base 64 variable
    String data;
    if ((data = System.getenv("APPLE_SIGNING_P12_BASE64")) != null) {
      // Mark the temporary certificate to be deleted after use.
      deleteCertificateAfter = true;
      // Decode and write the file.
      File outfile = new File(getOutdir(), "certificate.p12");
      File parent = outfile.getParentFile();
      if (parent != null && !parent.exists() && !parent.mkdirs()) {
        throw new IllegalStateException("Could not create output directory: " + parent);
      }
      byte[] decodedBytes = Base64.getDecoder().decode(data);
      try (FileOutputStream outputStream = new FileOutputStream(outfile)) {
        outputStream.write(decodedBytes);
      } catch (IOException e) {
        throw new GradleScriptException("IOException", e);
      }
      return outfile.getAbsolutePath();
    }

    // No variables set.
    return null;
  }

  /**
   * Get the password to the signing cert from the <em>$APPLE_SIGNING_PASSWORD</em> environment var.
   * If it does not exist, decode it from <em>$APPLE_SIGNING_PASSWORD_BASE64</em>. If this does also
   * not exist, return {@link null}.
   */
  private String loadCertificatePasswordFromEnv() {
    String pw;
    if ((pw = System.getenv("APPLE_SIGNING_PASSWORD")) != null) return pw.strip();

    String data;
    if ((data = System.getenv("APPLE_SIGNING_PASSWORD_BASE64")) != null) {
      // Strip is important here since I encountered an issue, where there was a trailing newline or
      // space which broke the entire thing.
      return new String(Base64.getDecoder().decode(data)).strip();
    }

    // No variable set.
    return null;
  }

  /** Get the directory that the mac app is in. */
  private File getMacAppDirectory() {
    return unsignedMacAppDirectory.get().getAsFile();
  }

  /** Get the path to the unsigned app. */
  private File getUnsignedMacApp() {
    if (existingMacAppBundle.isPresent()) {
      return existingMacAppBundle.get().getAsFile();
    }
    return new File(getMacAppDirectory(), getAppName() + ".app");
  }

  // ===============================================================================================
  // Output Helper
  // ===============================================================================================

  /** The logger object that will be used for printing. */
  private final Logger logger = Logging.getLogger(getClass());

  /** Print a message in a consistent headline format in the info log level. */
  private void headline(String message) {
    logger.info("\033[34;1m" + message + "\033[0m");
  }

  /** Print a message in a consistent sub-headline format in the info log level. */
  private void subHeadline(String message) {
    logger.info("\033[1m• " + message + "\033[0m");
  }

  // ===============================================================================================
  // Auxiliary Shell Tasks
  // ===============================================================================================

  /** Update the search list of keychains, either adding or removing the custom one. */
  private void updateKeychains(boolean addInCustomKeychain) {
    // Get a list of keychains by regex parsing the output.
    List<String> keychains =
        Stream.of(Shell.sh("security", "list-keychains", "-d", "user").getLines())
            .map(
                l ->
                    l.replaceAll("^.*?([^/]+.keychain).*$", "$1")
                        .replace(getKeychainName(), "") // filter the custom keychain
                        .strip())
            .filter(l -> !l.isEmpty())
            .collect(Collectors.toList());

    // Add the custom keychain to the list.
    if (addInCustomKeychain) keychains.add(getKeychainName());

    // Weirdly construct the command to circumvent Java’s lack of array unpacking.
    List<String> cmd = new ArrayList<>();
    cmd.addAll(Arrays.asList("security", "list-keychains", "-d", "user", "-s"));
    cmd.addAll(keychains);
    Shell.sh(cmd.toArray(new String[0]));
  }

  /**
   * Unlock the keychain. This is probably called way more often than needed, but better be safe.
   */
  private void unlockKeychain() {
    Shell.sh("security", "unlock-keychain", "-p", getKeychainPassword(), getKeychainName());
  }

  /**
   * Import a certificate from a URL. But only if it does not yet exist.
   *
   * @param url The URL to download from.
   * @param label A name label to compare against. Only download it, if it is not already present.
   */
  private void importCA(String url, String label) {
    // Don't bother if certificate is installed already
    if (Shell.test("security", "find-certificate", "-c", label)) return;

    // Define our temporary file and clean it up if it exists.
    File out = new File(getOutdir(), "tmp.cer");
    if (out.exists() && !out.delete()) {
      throw new IllegalStateException("Could not delete temporary certificate: " + out);
    }

    String cer = out.getAbsolutePath();
    DownloadUtils.downloadHttpToFile(url, out.toPath(), "certificate");
    // Import it into our keychain
    Shell.sh("security", "import", cer, "-t", "cert", "-k", getKeychainName(), "-A");
    // Import it into the login keychain
    Shell.sh("security", "import", cer, "-t", "cert", "-k", "login.keychain", "-A");

    // clean up the certificate. No need to have it laying about.
    if (out.exists() && !out.delete()) {
      throw new IllegalStateException("Could not delete temporary certificate: " + out);
    }
  }

  // ===============================================================================================
  // Main Processing Steps – Split Up into Functions
  // ===============================================================================================

  /** Delete the old keychain if it is around and create a new one that matches. */
  private void setupKeychains() {
    headline("Setting up Keychain");
    if (getKeychainName().equals("login.keychain")) {
      logger.info("Using login keychain, no setup will occurr");
      return;
    }

    // Delete remnant keychain if it exists
    if (Shell.sh("security", "list-keychains").out.contains(getKeychainName())) {
      logger.info("Existing keychain found, deleting first.");
      unlockKeychain();
      Shell.sh("security", "delete-keychain", getKeychainName());
    }

    // Create new keychain
    Shell.sh("security", "create-keychain", "-p", getKeychainPassword(), getKeychainName());

    // Add it to the search list
    updateKeychains(true);

    // https://developer.apple.com/forums/thread/712005
    Shell.sh("security", "unlock-keychain", "-p", getKeychainPassword(), getKeychainName());
    Shell.sh("security", "set-keychain-settings", "-lut", "1000000", getKeychainName());
    Shell.sh("security", "unlock-keychain", "-p", getKeychainPassword(), getKeychainName());
  }

  /** Import root certificates required for signing: https://stackoverflow.com/questions/69464483 */
  private void importRootCertificates() {
    headline("Importing root certificates");
    importCA(
        "https://developer.apple.com/certificationauthority/AppleWWDRCA.cer",
        "Apple Worldwide Developer Relations Certification Authority");
    importCA(
        "https://www.apple.com/certificateauthority/AppleWWDRCAG3.cer",
        "Apple Worldwide Developer Relations Certification Authority");
    importCA(
        "https://www.apple.com/certificateauthority/DevAuthCA.cer",
        "Developer Authentication Certification Authority");
    importCA("https://www.apple.com/appleca/AppleIncRootCertificate.cer", "Apple Root CA");
  }

  /** Import the signing certificate */
  private void importSigningCertificate() {
    headline("Importing Certificate");
    if (getCertificate() == null) {
      logger.info("No certificate specified, skipping");
      return;
    }
    unlockKeychain();
    Shell.sh(
        "security",
        "import",
        getCertificate(),
        "-k",
        getKeychainName(),
        "-P",
        getCertificatePassword(),
        "-A", // Mark it for access by all apps.
        "-T",
        "/usr/bin/productsign", // Mark it for access by productsign.
        "-T",
        "/usr/bin/codesign", // Mark it for access by codesign.
        "-T",
        "/usr/bin/security"); // Mark it for access by security.
  }

  /**
   * Make the signing certificate available to the relevant processes. Otherwise it is shielded from
   * arbitrary processes.
   */
  private void makeCertificateAvailable() {
    headline("Making Certificate Available");
    unlockKeychain();
    Shell.sh(
        "security",
        "set-key-partition-list",
        "-S",
        "apple-tool:,apple:,codesign:",
        "-k",
        getKeychainPassword(),
        getKeychainName());

    // Check that this worked
    unlockKeychain();
    String testresult = Shell.sh("security", "find-identity", "-p", "codesigning").out;
    if (!testresult.contains(getAppleSignID())) {
      throw new GradleException("Code signing identity cannot be found.");
    }
  }

  /** Copy the app from the unsigned output directory to the one where it will be signed. */
  private void copyAppOver() {
    headline("Copy unsigned app to signing directory");
    File inputApp = getUnsignedMacApp();
    if (!inputApp.exists()) {
      throw new InvalidUserDataException("Could not find input app bundle: " + inputApp);
    }
    if (!inputApp.getName().endsWith(".app")) {
      throw new InvalidUserDataException(
          "Input app bundle must have the .app suffix: " + inputApp);
    }
    try {
      FileUtils.copyDir(inputApp, getSignedAndNotarizedMacApp());
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }

  /** Codesign a specific file. */
  private void codesign(File file, boolean verify) {
    String signPath = file.getAbsolutePath();
    headline("Code-Signing " + getOutdir().toPath().relativize(file.toPath()));
    unlockKeychain();
    Shell.sh(
        "codesign",
        "--keychain",
        getKeychainName(),
        "--force",
        "--verbose",
        "--options",
        "runtime",
        "--sign",
        getAppleSignID(),
        signPath);

    // Evaluate the result. These can throw errors if something went wrong – and abort the process.
    if (verify) {
      Shell.sh("codesign", "--verify", "--strict", "--deep", "--verbose", signPath);
      Shell.sh("spctl", "-a", "-t", "exec", "-vv", signPath);
    }
  }

  /** Codesign all relevant files inside the app bundle and the app bundle itself. */
  private void codeSignAll() {
    File appDir = getSignedAndNotarizedMacApp();
    List<String> extensions =
        Arrays.asList(
            ".app",
            ".dylib",
            ".so",
            ".framework",
            ".node",
            ".xpc",
            ".bundle",
            ".kext",
            ".appex",
            ".xcframework");
    try (Stream<Path> stream =
        Files.find(
            appDir.toPath(),
            16,
            (path, attrs) ->
                extensions.stream().anyMatch(extension -> path.toString().endsWith(extension))
                    && path != appDir.toPath())) {
      stream.forEach(p -> codesign(p.toFile(), false));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    codesign(appDir, true);
  }

  /** Create a simple DiskImage that can be notarized. */
  private void createDmg() {
    headline("Create DMG");
    String appPath = getSignedAndNotarizedMacApp().getAbsolutePath();
    String dmgPath = getNotarizedDMG().getAbsolutePath();
    String volumeName = getAppName() + ' ' + getProjectVersion();

    Shell.sh(
        "hdiutil",
        "create",
        "-volname",
        volumeName,
        "-srcfolder",
        appPath,
        "-ov",
        "-format",
        "UDZO",
        dmgPath);
  }

  /** Codesign the DiskImage. */
  private void codesignDmg() {
    headline("Code-Signing DMG");
    String dmgPath = getNotarizedDMG().getAbsolutePath();
    Shell.sh(
        "codesign",
        "--force",
        "--verbose",
        "--options",
        "runtime",
        "--sign",
        getAppleSignID(),
        dmgPath);
    // Confirm that this worked. Throw an error otherwise.
    Shell.sh("codesign", "--verify", "--strict", "--deep", "--verbose", dmgPath);
  }

  /** Notarize the DiskImage. */
  private void notarizeDmg() {
    headline("Notarizing DMG");
    String dmgPath = getNotarizedDMG().getAbsolutePath();
    Shell.sh(
        "xcrun",
        "notarytool",
        "submit",
        dmgPath,
        "--wait",
        "--apple-id",
        getAppleIDUser(),
        "--password",
        getAppleIDPassword(),
        "--team-id",
        getAppleIDTeamID());

    // validate. Throw an error if the validation did not work.
    Shell.sh("spctl", "-a", "-t", "install", "-vv", dmgPath);
  }

  /** Staple the ticket from the DiskImage onto the app. */
  private void staple() {
    headline("Stapling Notarization Ticket");
    String dmgPath = getNotarizedDMG().getAbsolutePath();
    String appPath = getSignedAndNotarizedMacApp().getAbsolutePath();

    Shell.sh("xcrun", "stapler", "staple", dmgPath);
    Shell.sh("xcrun", "stapler", "staple", appPath);
    // Validate that the stapling worked. Throw an error if it did not.
    Shell.sh("xcrun", "stapler", "validate", appPath);
  }

  /**
   * Cleanup: Delete the certificate and the keychain to not leave any sensitive data accessible. In
   * case of a crash during the process, the cleanup should still be performed.
   */
  private void cleanup() {
    headline("Cleanup");
    // Delete the certificate from the keychain. The keychain potentially is the login keychain. The
    // certificate must not remain there!
    Shell.failOk("security", "delete-certificate", "-c", getAppleSignID());

    // Delete the keychain if it is not the login keychain.
    if (!getKeychainName().equals("login.keychain")) {
      unlockKeychain();
      Shell.failOk("security", "delete-keychain", getKeychainName());
      // Remove the custom keychain from the search list.
      updateKeychains(false);
    }

    // Delete the certificate
    if (deleteCertificateAfter && getCertificate() != null) Shell.failOk("rm", getCertificate());
  }

  /** The execution of the task. */
  @TaskAction
  public void taskAction() {
    // Abort if this is not macOS
    String os = System.getProperty("os.name").toLowerCase();
    if (!(os.contains("mac") || os.contains("darwin"))) {
      logger.error("Forced to skip signing, OS is not macOS!");
      return;
    }

    // Create the outdir first. For base64 certificate input we write a temp .p12 into this folder.
    if (getOutdir().exists()) FileUtils.deleteRecursively(getOutdir());
    if (!getOutdir().mkdirs()) {
      throw new IllegalStateException("Could not create output directory: " + getOutdir());
    }

    // Make sure, that all variables are set. Set them now from runtime properties or environment.
    if (getCertificate() == null) {
      String resolvedCertificate = getCertificateFilePath();
      if (resolvedCertificate != null) setCertificate(resolvedCertificate);
    }
    if (getCertificate() != null && getCertificatePassword() == null)
      throw new InvalidUserDataException("Required property 'certificatePassword' not set.");
    if (getAppleSignID() == null)
      throw new InvalidUserDataException("Required property 'appleSignID' not set.");
    if (getAppleIDUser() == null)
      throw new InvalidUserDataException("Required property 'appleIDUser' not set.");
    if (getAppleIDPassword() == null)
      throw new InvalidUserDataException("Required property 'appleIDPassword' not set.");
    if (getAppleIDTeamID() == null)
      throw new InvalidUserDataException("Required property 'appleIDTeamID' not set.");


    // For better documentation of the individual steps, read the descriptions of the methods.
    try {
      setupKeychains();
      importRootCertificates();
      importSigningCertificate();
      makeCertificateAvailable();
      copyAppOver();
      codeSignAll();
      createDmg();
      codesignDmg();
      notarizeDmg();
      staple();
    } finally {
      // Perform cleanup at the end, even in case of an error.
      cleanup();
    }
  }
}
