package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Files;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class SignAndNotarizeTest {

  @Test
  void appName_isDerivedFromExistingBundleWhenNotExplicitlySet() throws Exception {
    Project project = ProjectBuilder.builder().build();
    SignAndNotarize task =
        project.getTasks().create("signedAndNotarizedMacApp", SignAndNotarize.class);

    File out = Files.createTempDirectory("signed-out-").toFile();
    File existingApp = new File(Files.createTempDirectory("existing-").toFile(), "Cool.app");
    if (!existingApp.mkdirs()) {
      throw new IllegalStateException("Could not create test app bundle");
    }

    task.setOutdir(out);
    task.setExistingMacAppBundle(existingApp);

    assertEquals("Cool", task.getAppName());
    assertEquals(new File(out, "Cool.app"), task.getSignedAndNotarizedMacApp());
    assertEquals(new File(out, "Cool.dmg"), task.getNotarizedDMG());
  }

  @Test
  void appName_throwsWhenNoInputBundleInformationIsConfigured() {
    Project project = ProjectBuilder.builder().build();
    SignAndNotarize task =
        project.getTasks().create("signedAndNotarizedMacApp", SignAndNotarize.class);

    assertThrows(InvalidUserDataException.class, task::getAppName);
  }
}
