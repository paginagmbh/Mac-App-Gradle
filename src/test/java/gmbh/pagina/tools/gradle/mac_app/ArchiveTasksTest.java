package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class ArchiveTasksTest {

  @Test
  void macAppArchive_throwsWhenBundleIsMissing() throws Exception {
    Project project = ProjectBuilder.builder().build();
    MacAppArchive task = project.getTasks().create("macAppArchive", MacAppArchive.class);

    File sourceDir = Files.createTempDirectory("archive-src-").toFile();
    task.getSourceDirectoryProperty().fileValue(sourceDir);
    task.setAppName("MissingApp");
    task.setOutdir(Files.createTempDirectory("archive-out-").toFile());

    assertThrows(IllegalStateException.class, task::taskAction);
  }

  @Test
  void macAppArchive_createsTarGzWhenBundleExists() throws Exception {
    Project project = ProjectBuilder.builder().build();
    MacAppArchive task = project.getTasks().create("macAppArchive", MacAppArchive.class);

    File sourceDir = Files.createTempDirectory("archive-src-").toFile();
    File app = new File(sourceDir, "Demo.app");
    assertTrue(app.mkdirs());

    task.getSourceDirectoryProperty().fileValue(sourceDir);
    task.setAppName("Demo");
    task.setOutdir(Files.createTempDirectory("archive-out-").toFile());

    task.taskAction();

    assertTrue(task.getMacAppTarGz().isFile());
  }

  @Test
  void signedArchive_throwsWhenBundleIsMissing() throws Exception {
    Project project = ProjectBuilder.builder().build();
    SignedAndNotarizedMacAppArchive task =
        project.getTasks().create("signedArchive", SignedAndNotarizedMacAppArchive.class);

    File sourceDir = Files.createTempDirectory("archive-src-").toFile();
    task.getSourceDirectoryProperty().fileValue(sourceDir);
    task.setAppName("MissingApp");
    task.setOutdir(Files.createTempDirectory("archive-out-").toFile());

    assertThrows(IllegalStateException.class, task::taskAction);
  }

  @Test
  void signedArchive_createsTarGzWhenBundleExists() throws Exception {
    Project project = ProjectBuilder.builder().build();
    SignedAndNotarizedMacAppArchive task =
        project.getTasks().create("signedArchive", SignedAndNotarizedMacAppArchive.class);

    File sourceDir = Files.createTempDirectory("archive-src-").toFile();
    File app = new File(sourceDir, "Signed.app");
    assertTrue(app.mkdirs());

    task.getSourceDirectoryProperty().fileValue(sourceDir);
    task.setAppName("Signed");
    task.setOutdir(Files.createTempDirectory("archive-out-").toFile());

    task.taskAction();

    assertTrue(task.getSignedAndNotarizedMacAppTarGz().isFile());
  }
}
