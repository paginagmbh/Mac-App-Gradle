package de.paginagmbh.commons.mac_app_gradle;

import static java.util.Map.entry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import org.gradle.api.GradleScriptException;
import org.gradle.api.Project;

/**
 * Tools for file interactions.
 *
 * <p>For ease of use some methods are implemented via ant. You need to set up the project object
 * since it provides the ant interface. Do this by calling {@link setProject(Project project)}. All
 * of the methods in this class are static methods. No need to set up any {@link FileUtils} object.
 */
public class FileUtils {
  private static Project project;
  private static org.gradle.api.AntBuilder ant;

  /** Set the project object so that ant can be used. */
  public static void setProject(Project project) {
    FileUtils.project = project;
    FileUtils.ant = project.getAnt();
  }

  /**
   * Copy an input file to a destination directory, keeping the name.
   *
   * @param input The input file to copy.
   * @param output The directory it is copied into.
   */
  public static void copyToDir(File input, File output) {
    ant.invokeMethod(
        "copy",
        Map.ofEntries(
            entry("file", input.getAbsolutePath()),
            entry("todir", output.getAbsolutePath()),
            entry("force", "true"),
            entry("overwrite", "true")));
  }

  /**
   * Copy an input file to a target, possibly renaming it along the way.
   *
   * @param input The input file to copy.
   * @param output The file it will be after copying.
   */
  public static void copyToFile(File input, File output) {
    ant.invokeMethod(
        "copy",
        Map.ofEntries(
            entry("file", input.getAbsolutePath()),
            entry("tofile", output.getAbsolutePath()),
            entry("force", "true"),
            entry("overwrite", "true")));
  }

  /**
   * Copy a directory to a target directory.
   *
   * @param input The input directory to copy.
   * @param output The directory it will be after copying.
   * @throws IOException If file writing goes wrong.
   */
  public static void copyDir(File input, File output) throws IOException {
    Path sourcePath = input.toPath();
    Path destinationPath = output.toPath();

    Files.walkFileTree(
        sourcePath,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path relativePath = sourcePath.relativize(dir);
            Path targetPath = destinationPath.resolve(relativePath);
            Files.createDirectories(targetPath);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path relativePath = sourcePath.relativize(file);
            Path targetPath = destinationPath.resolve(relativePath);
            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Unzip an input file to a target.
   *
   * @param zipFile The input file to copy.
   * @param unzippedFile The file once unzipped.
   */
  public static void unzip(File zipFile, File unzippedFile) {
    ant.invokeMethod(
        "unzip",
        Map.ofEntries(
            entry("src", zipFile.getAbsolutePath()),
            entry("dest", unzippedFile.getAbsolutePath())));
  }

  /**
   * Zips a file or directory to a target.
   *
   * @param unzippedFile The input file to zip.
   * @param zipFile The file once zipped.
   */
  public static void zip(File unzippedFile, File zipFile) {
    ant.invokeMethod(
        "zip",
        Map.ofEntries(
            // Base directory is parent directory since you do want to contain the actually intended
            // directory in the archive, not have the app be the root directory itself.
            // This then requires the slightly ugly hack with the includes, where I
            // 1)  Have to replace any space with a '?' since ant will otherwise decide that it is a
            //     separator between paremeters. (I have tried everything, trust me). So I match for
            //     any character here – and might possibly also include other erroneous files. (Even
            //     if the probability of that is low)
            // 2)  I have to append a "/ ** / *" to each directory to also include sub-files.
            entry("basedir", unzippedFile.getParentFile().getAbsolutePath()),
            entry(
                "includes",
                unzippedFile.getName().replace(' ', '?')
                    + (unzippedFile.isDirectory()
                        ? File.separator + "**" + File.separator + "*"
                        : "")),
            entry("destfile", zipFile.getAbsolutePath())));
  }

  /**
   * Creates a tar.gz archive.
   *
   * @param infile The input file to archive.
   * @param outfile The file once archived.
   */
  public static void tarGz(File infile, File outfile) {
    Shell.sh(
        "tar",
        "-czv",
        // Output file
        "-f",
        outfile.getAbsolutePath(),
        // Go to that directory so that our relative paths work
        "-C",
        infile.getParentFile().getAbsolutePath(),
        // File to compress
        infile.getName());
    /* ANT DOES NOT PRESERVE FILE PERMISSIONS – WHICH WAS THE WHOLE REASON WE WERE DOING THIS THING
     * IN THE FIRST PLACE
    ant.invokeMethod(
        "tar",
        Map.ofEntries(
            // Base directory is parent directory since you do want to contain the actually intended
            // directory in the archive, not have the app be the root directory itself.
            // This then requires the slightly ugly hack with the includes, where I
            // 1)  Have to replace any space with a '?' since ant will otherwise decide that it is a
            //     separator between paremeters. (I have tried everything, trust me). So I match for
            //     any character here – and might possibly also include other erroneous files. (Even
            //     if the probability of that is low)
            // 2)  I have to append a "/ ** / *" to each directory to also include sub-files.
            entry("basedir", infile.getParentFile().getAbsolutePath()),
            entry(
                "includes",
                infile.getName().replace(' ', '?')
                    + (infile.isDirectory() ? File.separator + "**" + File.separator + "*" : "")),
            entry("destfile", outfile.getAbsolutePath()),
            entry("compression", "gzip")));
     */
  }

  /** Write a string to a text file that does not yet need to exist. */
  public static void writeToFile(File file, String content) throws IOException {
    file.createNewFile();
    FileWriter writer = new FileWriter(file);
    writer.write(content);
    writer.close();
  }

  /** Mark a file as executable. Only works on UNIX-like systems! */
  public static void setExecutable(File file) {
    // try one
    file.setExecutable(true, false);
    // try two
    Set<PosixFilePermission> permissions;
    try {
      permissions = Files.getPosixFilePermissions(file.toPath());
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(file.toPath(), permissions);
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }
}
