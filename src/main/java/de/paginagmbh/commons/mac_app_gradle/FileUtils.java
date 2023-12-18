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

  /** Set the project object so that ant can be used. */
  public static void setProject(Project project) {
    FileUtils.project = project;
  }

  /**
   * Copy an input file to a destination directory, keeping the name.
   *
   * @param input The input file to copy.
   * @param output The directory it is copied into.
   */
  public static void copyToDir(File input, File output) {
    project
        .getAnt()
        .invokeMethod(
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
    project
        .getAnt()
        .invokeMethod(
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
