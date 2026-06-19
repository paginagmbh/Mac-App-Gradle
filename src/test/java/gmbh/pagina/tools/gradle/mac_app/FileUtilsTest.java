package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileUtilsTest {

  @Test
  void copyHelpers_zipAndUnzip_roundTripFileContents() throws Exception {
    Path tempDir = Files.createTempDirectory("file-utils-");
    Path inputDir = tempDir.resolve("input");
    Path nestedDir = inputDir.resolve("nested");
    Files.createDirectories(nestedDir);
    Path sourceFile = nestedDir.resolve("message.txt");
    Files.writeString(sourceFile, "hello", StandardCharsets.UTF_8);

    Path copiedDir = tempDir.resolve("copied");
    FileUtils.copyDir(inputDir.toFile(), copiedDir.toFile());
    assertEquals(
        "hello",
        Files.readString(
            copiedDir.resolve("nested").resolve("message.txt"), StandardCharsets.UTF_8));

    Path renamed = tempDir.resolve("single").resolve("renamed.txt");
    FileUtils.copyToFile(sourceFile.toFile(), renamed.toFile());
    assertEquals("hello", Files.readString(renamed, StandardCharsets.UTF_8));

    Path copiedFileDir = tempDir.resolve("single-copy");
    FileUtils.copyToDir(sourceFile.toFile(), copiedFileDir.toFile());
    assertEquals(
        "hello", Files.readString(copiedFileDir.resolve("message.txt"), StandardCharsets.UTF_8));

    Path zipFile = tempDir.resolve("archive.zip");
    FileUtils.zip(inputDir.toFile(), zipFile.toFile());

    Path unzipTarget = tempDir.resolve("unzipped");
    FileUtils.unzip(zipFile.toFile(), unzipTarget.toFile());
    assertEquals(
        "hello",
        Files.readString(
            unzipTarget.resolve("input").resolve("nested").resolve("message.txt"),
            StandardCharsets.UTF_8));
  }

  @Test
  void writeSetExecutableAndDeleteRecursively_workTogether() throws Exception {
    Path tempDir = Files.createTempDirectory("file-utils-");
    File target = tempDir.resolve("run.sh").toFile();

    FileUtils.writeToFile(target, "#!/bin/sh\necho ok\n");
    assertTrue(target.exists());

    FileUtils.setExecutable(target);
    assertTrue(target.canExecute());

    FileUtils.deleteRecursively(tempDir.toFile());
    assertFalse(Files.exists(tempDir));
  }
}
