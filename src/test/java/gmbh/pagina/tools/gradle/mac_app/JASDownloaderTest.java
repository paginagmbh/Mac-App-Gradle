package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class JASDownloaderTest {

  @Test
  void sourceRoundTrip_andTargetFileWhenNotUnzipping() throws Exception {
    Project project = ProjectBuilder.builder().build();
    JASDownloader task = project.getTasks().create("javaApplicationStub", JASDownloader.class);
    task.setOutdir(Files.createTempDirectory("jas-").toFile());

    JavaApplicationStubSource source =
        new JavaApplicationStubSource("https://example.org/stub", false, "stub.bin");
    task.setSource(source);

    assertEquals("https://example.org/stub", task.getSourceUrl().get());
    assertFalse(task.isUnzip());
    assertEquals("stub.bin", task.getExecutableName());
    assertEquals("stub.bin", task.getTargetFile().getName());
  }

  @Test
  void targetFileForZipSource_pointsToExtractedExecutable() throws Exception {
    Project project = ProjectBuilder.builder().build();
    JASDownloader task = project.getTasks().create("javaApplicationStub", JASDownloader.class);
    File outdir = Files.createTempDirectory("jas-").toFile();
    task.setOutdir(outdir);

    task.setSource(
        new JavaApplicationStubSource("https://example.org/archive.zip", true, "runner"));

    File target = task.getTargetFile();
    assertTrue(target.getAbsolutePath().contains("archive"));
    assertEquals("runner", target.getName());
    assertEquals(
        outdir.getAbsolutePath(), target.getParentFile().getParentFile().getAbsolutePath());
  }
}
