package gmbh.pagina.tools.gradle.mac_app;

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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.gradle.api.GradleScriptException;

/**
 * Tools for file interactions.
 *
 * <p>All methods in this class are static methods. No need to set up any {@link FileUtils} object.
 */
public class FileUtils {

  /** Utility class; not intended to be instantiated. */
  private FileUtils() {}

  /**
   * Copy an input file to a destination directory, keeping the name.
   *
   * @param input The input file to copy.
   * @param output The directory it is copied into.
   */
  public static void copyToDir(File input, File output) {
    try {
      Files.createDirectories(output.toPath());
      Files.copy(
          input.toPath(),
          output.toPath().resolve(input.getName()),
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }

  /**
   * Copy an input file to a target, possibly renaming it along the way.
   *
   * @param input The input file to copy.
   * @param output The file it will be after copying.
   */
  public static void copyToFile(File input, File output) {
    try {
      File parent = output.getParentFile();
      if (parent != null) Files.createDirectories(parent.toPath());
      Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
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
        new SimpleFileVisitor<>() {
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
    Path outputRoot = unzippedFile.toPath();
    try {
      Files.createDirectories(outputRoot);
      try (ZipInputStream zipInputStream =
          new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          Path target = outputRoot.resolve(entry.getName()).normalize();
          if (!target.startsWith(outputRoot)) {
            throw new GradleScriptException(
                "IOException",
                new IOException("Zip entry escapes target directory: " + entry.getName()));
          }
          if (entry.isDirectory()) {
            Files.createDirectories(target);
          } else {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
          }
          zipInputStream.closeEntry();
        }
      }
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }

  /**
   * Zips a file or directory to a target.
   *
   * @param unzippedFile The input file to zip.
   * @param zipFile The file once zipped.
   */
  public static void zip(File unzippedFile, File zipFile) {
    try {
      File parent = zipFile.getParentFile();
      if (parent != null) Files.createDirectories(parent.toPath());
      try (ZipOutputStream zipOutputStream =
          new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
        Path sourceRoot = unzippedFile.getParentFile().toPath();
        if (unzippedFile.isDirectory()) {
          try (java.util.stream.Stream<Path> stream = Files.walk(unzippedFile.toPath())) {
            stream
                .filter(path -> !Files.isDirectory(path))
                .forEach(
                    path -> {
                      ZipEntry entry =
                          new ZipEntry(
                              sourceRoot
                                  .relativize(path)
                                  .toString()
                                  .replace(File.separatorChar, '/'));
                      try {
                        zipOutputStream.putNextEntry(entry);
                        Files.copy(path, zipOutputStream);
                        zipOutputStream.closeEntry();
                      } catch (IOException e) {
                        throw new GradleScriptException("IOException", e);
                      }
                    });
          }
        } else {
          ZipEntry entry = new ZipEntry(unzippedFile.getName());
          zipOutputStream.putNextEntry(entry);
          Files.copy(unzippedFile.toPath(), zipOutputStream);
          zipOutputStream.closeEntry();
        }
      }
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
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

  /**
   * Write a string to a text file that does not yet need to exist.
   *
   * @param file Target file.
   * @param content Text content to write.
   * @throws IOException If the file cannot be created or written.
   */
  public static void writeToFile(File file, String content) throws IOException {
    if (!file.exists() && !file.createNewFile()) {
      throw new IOException("Could not create file: " + file.getAbsolutePath());
    }
    try (FileWriter writer = new FileWriter(file)) {
      writer.write(content);
    }
  }

  /**
   * Mark a file as executable. Only works on UNIX-like systems.
   *
   * @param file file to mark executable
   */
  public static void setExecutable(File file) {
    // try one
    boolean executable = file.setExecutable(true, false);
    // try two
    Set<PosixFilePermission> permissions;
    try {
      permissions = Files.getPosixFilePermissions(file.toPath());
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(file.toPath(), permissions);
      executable = true;
    } catch (IOException e) {
      if (!executable) throw new GradleScriptException("IOException", e);
    }
    if (!executable) {
      throw new GradleScriptException(
          "IOException", new IOException("Could not mark file executable: " + file));
    }
  }

  /**
   * Delete a file or directory recursively.
   *
   * @param file file or directory to delete
   */
  public static void deleteRecursively(File file) {
    if (!file.exists()) return;
    try {
      Files.walkFileTree(
          file.toPath(),
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(path);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              if (exc != null) throw exc;
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
  }
}
