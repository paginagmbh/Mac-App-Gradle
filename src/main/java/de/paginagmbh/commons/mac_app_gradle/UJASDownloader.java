package de.paginagmbh.commons.mac_app_gradle;

import static java.util.Map.entry;

import java.io.File;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Download the universalJavaApplicationStub. */
public class UJASDownloader extends DefaultTask {

  /** URL if the compiled version should be downloaded in a zip. */
  private final String compiledDownloadURL =
      "https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip";

      /** URL if the shell version should be downloaded directly. */
  private final String shellDownloadURL =
      "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub";

      /** The URL to download the file from, if it is overwritten by the user. */
  private String _downloadURL = null;

  /** 
   * Download the compiled version over the shell version. The compiled version is compiled with the
   * Intel toolchain, so it can be also run using Rosetta. This is not nice and might break.
   */
  public boolean compiled = false;

  /**
   * The download URL that can be set manually. Also set the {@link compiled} flag if it should be
   * unzipped.
   */
  @Input
  public String getDownloadURL() {
    if (_downloadURL != null) return _downloadURL;
    if (compiled) return compiledDownloadURL;
    return shellDownloadURL;
  }

  /** Set the download URL to use. Also set the {@link compiled} flag if it should be unzipped. */
  public void setDownloadURL(String downloadURL) {
    _downloadURL = downloadURL;
  }

  /** The output directory (customizable, auto). */
  public String outdir =
      getProject()
          .getLayout()
          .getBuildDirectory()
          .file("universalJavaApplicationStub")
          .get()
          .getAsFile()
          .getAbsolutePath();
  private final String unzippedName = getFileName();
  private final String ujasName = "universalJavaApplicationStub";

  /** The universalJavaApplicationStub file. */
  @OutputFile
  public File getTargetFile() {
    if (compiled) return new File(new File(outdir, unzippedName), ujasName);
    return new File(outdir, ujasName);
  }

  /** The unzipped archive file. Does not exist if shell version is used. */
  private File getUnzippedFile() {
    return new File(outdir, unzippedName);
  }

  /** The downloaded zip file. Does not exist if shell version is used. */
  private File getZipFile() {
    return new File(outdir, getDownloadName());
  }

  /** The filename of universalJavaApplicationStub. */
  private String getFileName() {
    String download = getDownloadName();
    return download.split("\\.").length == 1
        ? download
        : download.substring(0, download.lastIndexOf("."));
  }

  /** Get the name of the zip archive  */
  private String getDownloadName() {
    String[] components = getDownloadURL().split("/");
    return components[components.length - 1];
  }

  /** Download the zip or shell file. */
  private void download() {
    getProject()
        .getAnt()
        .invokeMethod(
            "get",
            Map.ofEntries(
                entry("src", getDownloadURL()),
                entry("dest", compiled ? outdir : getTargetFile())));
  }

  /** Unzip the zip file. */
  private void unzip() {
    getProject()
        .getAnt()
        .invokeMethod(
            "unzip",
            Map.ofEntries(
                entry("src", getZipFile().getAbsolutePath()),
                entry("dest", getUnzippedFile().getAbsolutePath())));
  }

  /** Perform the plugin action. */
  @TaskAction
  public void taskAction() {
    // Ensure the output directory exists
    new File(outdir).mkdirs();
    if (getTargetFile().exists()) {
      // If the file has already been downloaded, don’t download it again. Assume that it's fine.
      Logging.getLogger(getClass()).info("Target file already exists; skipping.");
    } else {
      // Download the file. Duh.
      download();
      // Unzip it, if the compiled archive is downloaded. Shell file is downloaded without zip.
      if (compiled) unzip();
    }
  }
}
