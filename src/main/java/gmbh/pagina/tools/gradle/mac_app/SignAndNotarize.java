package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** The task that signs and notarizes the mac app. Only works on macOS. */
public class SignAndNotarize extends DefaultTask {

  /** The description of this gradle task. */
  public String getDescription() {
    return "Sign and notarize the mac .app bundle.";
  }

  /** The Gradle task group description this task belongs to. */
  public String getGroup() {
    return "Make Mac App";
  }

  /**
   * The name of the keychain used to hold the variable (customizable, auto). If not overwritten, it
   * generates a default one with a unique name that is deleted after use. However, one can also set
   * it to login.keychain – which will not be deleted after use.
   */
  public String keychainName = "TemporaryPaginaSigningKeychain.keychain";

  /** The password for the keychain. Has to be the user password if the login keychain is used. */
  public String keychainPassword = "TotallySecretPassword";

  /**
   * The path to the signing certificate (customizable, auto). Defaults to one read from environment
   * variables. Either a path in <em>$APPLE_SIGNING_P12</em> or the base64 encoded contents of a p12
   * file in <em>$APPLE_SIGNING_P12_BASE64</em>. If these variables don’t exist and this property is
   * not set, an error will be thrown.
   *
   * <p>If the variable is null, it will be assigned at task-runtime since the outdir can be changed
   * by the user before then..
   */
  public String certificate = null;

  /**
   * The password to the certificate (customizable, auto). Defaults to one read from the environment
   * variables. Either directly from <em>$APPLE_SIGNING_PASSWORD</em> or using a (not) “secure” base
   * 64 encoded variable <em>$APPLE_SIGNING_PASSWORD_BASE64</em>.
   */
  public String certificatePassword = getCertificatePassword();

  /**
   * The AppleID used for code signing (customizable, auto). Defaults to one found in an environment
   * variable named <em>$APPLE_SIGN_ID</em>. This has a format lie <em>Developer ID Application: The
   * Company (ASDF213FDSA)</em>.
   */
  public String appleSignID = System.getenv("APPLE_SIGN_ID");

  /**
   * The AppleID used for code signing (customizable, auto). Defaults to one found in an environment
   * variable named <em>$APPLE_SIGN_ID</em>. This is usually an e-mail address.
   */
  public String appleIDUser = System.getenv("APPLE_ID_USER");

  /**
   * The password to the AppleID used for code signing (customizable, auto). By default it is pulled
   * from the environment variable named <em>$APPLE_ID_PASSWORD</em>.
   */
  public String appleIDPassword = System.getenv("APPLE_ID_PASSWORD");

  /**
   * The TeamID of the AppleID used for signing and notarizing the app (customizable, auto). Default
   * is read from the environment variable <em>$APPLE_ID_TEAM_ID</em>. The parenthesized part of the
   * {@link appleSignID} should be the same.
   */
  public String appleIDTeamID = System.getenv("APPLE_ID_TEAM_ID");

  /** The icon used for the disk image (customizable, auto). Defaults to the app icon. */
  public String dmgIcon = null;

  /** The directory that is used for outputting the file (customizable, auto). */
  public File outdir =
      getProject().getLayout().getBuildDirectory().file("signedMacApp").get().getAsFile();

  /** The signed mac app bundle. */
  @OutputDirectory
  public File getSignedAndNotarizedMacApp() {
    return new File(outdir, getAppName() + ".app");
  }

  /** The notarized installation disk image. */
  @OutputFile
  public File getNotarizedDMG() {
    return new File(outdir, getAppName() + ".dmg");
  }

  /** A tar.gz file containing the signed app. */
  @OutputFile
  public File getSignedAndNotarizedMacAppTarGz() {
    return new File(outdir, getAppName() + ".tgz");
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
      File outfile = new File(outdir, "certificate.p12");
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
  private String getCertificatePassword() {
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

  /** The task that generates the mac app. Will be used when the task is run to read variables. */
  private Task macAppTask = getProject().getTasks().findByName("macApp");

  /** Get the directory that the mac app is in. */
  private File getMacAppDirectory() {
    return new File((String) macAppTask.property("outdir"));
  }

  /** Get the name of the app without the extension. */
  private String getAppName() {
    return (String) macAppTask.property("appName");
  }

  /** Get the path to the unsigned app. */
  private File getUnsignedMacApp() {
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
                l -> {
                  return l.replaceAll("^.*?([^\\/]+.keychain).*$", "$1")
                      .replace(keychainName, "") // filter the custom keychain
                      .strip();
                })
            .filter(
                l -> {
                  return !l.isEmpty();
                })
            .collect(Collectors.toList());

    // Add the custom keychain to the list.
    if (addInCustomKeychain) keychains.add(keychainName);

    // Weirdly construct the command to circumvent Java’s lack of array unpacking.
    List<String> cmd = new ArrayList<String>();
    cmd.addAll(Arrays.asList("security", "list-keychains", "-d", "user", "-s"));
    cmd.addAll(keychains);
    Shell.sh(cmd.toArray(new String[0]));
  }

  /**
   * Unlock the keychain. This is probably called way more often than needed, but better be safe.
   */
  private void unlockKeychain() {
    Shell.sh("security", "unlock-keychain", "-p", keychainPassword, keychainName);
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
    File out = new File(outdir, "tmp.cer");
    if (out.exists()) out.delete();

    // Escape spaces
    String cer = out.getAbsolutePath().replace(" ", "\\ ");

    // Download the certificate
    Shell.sh("curl", "-s", "-o", cer, url);
    // Import it into our keychain
    Shell.sh("security", "import", cer, "-t", "cert", "-k", keychainName, "-A");
    // Import it into the login keychain
    Shell.sh("security", "import", cer, "-t", "cert", "-k", "login.keychain", "-A");

    // clean up the certificate. No need to have it laying about.
    if (out.exists()) out.delete();
  }

  /**
   * Ensure that brew is installed. If it is not, the user will be instructed to install it and this
   * method will throw an error.
   */
  private void ensureBrew() {
    if (!Shell.test("which", "brew")) {
      throw new InvalidUserDataException(
          "brew is not installed but required. Please visit https://brew.sh/");
    }
  }

  /** Ensure that NPM is installed. If it is not installed, it is installed with brew. */
  private void ensureNPM() {
    if (!Shell.test("which", "npm")) {
      ensureBrew();
      subHeadline("Installing NPM");
      Shell.sh("brew", "install", "node");
    }
  }

  /** Ensure that electron-installer-dmg is installed. If not, it will be installed with npm. */
  private void ensureElectronInstallerDMG() {
    if (!Shell.test("which", "electron-installer-dmg")) {
      ensureNPM();
      subHeadline("Installing electron-installer-dmg");
      Shell.sh("npm", "install", "-g", "electron-installer-dmg");
    }
  }

  // ===============================================================================================
  // Main Processing Steps – Split Up into Functions
  // ===============================================================================================

  /** Delete the old keychain if it is around and create a new one that matches. */
  private void setupKeychains() {
    headline("Setting up Keychain");
    if (keychainName == "login.keychain") {
      logger.info("Using login keychain, no setup will occurr");
      return;
    }

    // Delete remnant keychain if it exists
    if (Shell.sh("security", "list-keychains").out.contains(keychainName)) {
      logger.info("Existing keychain found, deleting first.");
      unlockKeychain();
      Shell.sh("security", "delete-keychain", keychainName);
    }

    // Create new keychain
    Shell.sh("security", "create-keychain", "-p", keychainPassword, keychainName);

    // Add it to the search list
    updateKeychains(true);

    // https://developer.apple.com/forums/thread/712005
    Shell.sh("security", "unlock-keychain", "-p", keychainPassword, keychainName);
    Shell.sh("security", "set-keychain-settings", "-lut", "1000000", keychainName);
    Shell.sh("security", "unlock-keychain", "-p", keychainPassword, keychainName);
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
    if (certificate == null) {
      logger.info("No certificate specified, skipping");
      return;
    }
    unlockKeychain();
    Shell.sh(
        "security",
        "import",
        certificate,
        "-k",
        keychainName,
        "-P",
        certificatePassword,
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
        keychainPassword,
        keychainName);

    // Check that this worked
    unlockKeychain();
    String testresult = Shell.sh("security", "find-identity", "-p", "codesigning").out;
    if (!testresult.contains(appleSignID)) {
      throw new GradleException("Code signing identity cannot be found.");
    }
  }

  /** Copy the app from the unsigned output directory to the one where it will be signed. */
  private void copyAppOver() {
    headline("Copy unsigned app to signing directory");
    try {
      FileUtils.copyDir(getUnsignedMacApp(), getSignedAndNotarizedMacApp());
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }

  /** Codesign a specific file. */
  private void codesign(File file, boolean verify) {
    String signPath = file.getAbsolutePath();
    headline("Code-Signing " + outdir.toPath().relativize(file.toPath()));
    unlockKeychain();
    Shell.sh(
        "codesign",
        "--keychain",
        keychainName,
        "--force",
        "--verbose",
        "--options",
        "runtime",
        "--sign",
        appleSignID,
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
    try {
      Files.find(
              appDir.toPath(),
              16,
              (path, attrs) ->
                  extensions.stream().anyMatch(extension -> path.toString().endsWith(extension))
                      && path != appDir.toPath())
          .forEach(
              p -> {
                codesign(p.toFile(), false);
              });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    codesign(appDir, true);
  }

  /** Create a DiskImage that can be notarized. */
  private void createDmg() {
    headline("Create DMG");
    // Install the required tools, if they are not already installed.
    ensureElectronInstallerDMG();
    String titleCandidate = getAppName() + ' ' + getProject().getVersion().toString();
    if (titleCandidate.length() > 27 && titleCandidate.contains("-SNAPSHOT"))
      titleCandidate = titleCandidate.replace("-SNAPSHOT", "β");
    if (titleCandidate.length() > 26) // Worried about unicode here so one character of margin
    titleCandidate = titleCandidate.substring(0, 26);
    // Different calls with or without icon.
    if (dmgIcon == null)
      Shell.sh(
          "electron-installer-dmg",
          "--title",
          titleCandidate,
          "--out",
          outdir.getAbsolutePath(),
          "--overwrite",
          getSignedAndNotarizedMacApp().getAbsolutePath(),
          getAppName());
    else
      Shell.sh(
          "electron-installer-dmg",
          "--title",
          titleCandidate,
          "--out",
          outdir.getAbsolutePath(),
          "--icon",
          dmgIcon,
          "--overwrite",
          getSignedAndNotarizedMacApp().getAbsolutePath(),
          getAppName());
    // // Option for a custom background picture:
    // # --background=src/build/splashscreen/DmgBackground.png \
    // // Create a DMG without the electron tool. Would have to do the icon and background myself
    // # hdiutil create -volname "${APP_NAME}" -srcfolder "${APP_PATH}" "${DMG_PATH}"
    //
  }

  /** Codesign the DiskImage. */
  private void codesignDmg() {
    headline("Code-Signing DMG");
    String dmgPath = getNotarizedDMG().getAbsolutePath();
    Shell.sh(
        "codesign", "--force", "--verbose", "--options", "runtime", "--sign", appleSignID, dmgPath);
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
        appleIDUser,
        "--password",
        appleIDPassword,
        "--team-id",
        appleIDTeamID);

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

  /** Archive the mac app. */
  private void archive() {
    FileUtils.tarGz(getSignedAndNotarizedMacApp(), getSignedAndNotarizedMacAppTarGz());
  }

  /**
   * Cleanup: Delete the certificate and the keychain to not leave any sensitive data accessible. In
   * case of a crash during the process, the cleanup should still be performed.
   */
  private void cleanup() {
    headline("Cleanup");
    // Delete the certificate from the keychain. The keychain potentially is the login keychain. The
    // certificate must not remain there!
    Shell.failOk("security", "delete-certificate", "-c", appleSignID);

    // Delete the keychain if it is not the login keychain.
    if (!keychainName.equals("login.keychain")) {
      unlockKeychain();
      Shell.failOk("security", "delete-keychain", keychainName);
      // Remove the custom keychain from the search list.
      updateKeychains(false);
    }

    // Delete the certificate
    if (deleteCertificateAfter) Shell.failOk("rm", certificate);
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

    // Make sure, that all variables are set. Set them now from runtime properties or environment.
    if (certificate == null) certificate = getCertificateFilePath();
    if (certificate == null)
      throw new InvalidUserDataException("Required property 'certificate' not set.");

    if (certificatePassword == null)
      throw new InvalidUserDataException("Required property 'certificatePassword' not set.");
    if (appleSignID == null)
      throw new InvalidUserDataException("Required property 'appleSignID' not set.");

    if (dmgIcon == null) dmgIcon = (String) macAppTask.property("icon");

    // Create the outdir. Delete it, if it is still around.
    if (outdir.exists()) outdir.delete();
    outdir.mkdirs();

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
      archive();
    } finally {
      // Perform cleanup at the end, even in case of an error.
      cleanup();
    }
  }
}
