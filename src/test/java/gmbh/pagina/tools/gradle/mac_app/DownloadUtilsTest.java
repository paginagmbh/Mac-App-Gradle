package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class DownloadUtilsTest {

  @Test
  void downloadHttpToFile_rejectsBlankUrl() throws Exception {
    Path destination = Files.createTempDirectory("download-utils-").resolve("out.bin");

    GradleException exception =
        assertThrows(
            GradleException.class,
            () -> DownloadUtils.downloadHttpToFile("   ", destination, "fixture"));

    assertTrue(exception.getMessage().contains("URL is empty"));
    assertFalse(Files.exists(destination));
  }

  @Test
  void downloadHttpToFile_rejectsUnsupportedScheme() throws Exception {
    Path destination = Files.createTempDirectory("download-utils-").resolve("out.bin");

    GradleException exception =
        assertThrows(
            GradleException.class,
            () -> DownloadUtils.downloadHttpToFile("file:///tmp/a.bin", destination, "fixture"));

    assertTrue(exception.getMessage().contains("unsupported URL scheme"));
    assertFalse(Files.exists(destination));
  }
}
