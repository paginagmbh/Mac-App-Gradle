package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class GenerateMacAppPluginTest {

  @Test
  void apply_registersCoreTasks() {
    Project project = ProjectBuilder.builder().build();

    project.getPluginManager().apply(GenerateMacAppPlugin.class);

    Task javaApplicationStub = project.getTasks().findByName("javaApplicationStub");
    Task macApp = project.getTasks().findByName("macApp");
    Task macAppArchive = project.getTasks().findByName("macAppArchive");
    Task signedAndNotarizedMacApp = project.getTasks().findByName("signedAndNotarizedMacApp");
    Task signedAndNotarizedMacAppArchive =
        project.getTasks().findByName("signedAndNotarizedMacAppArchive");

    assertNotNull(javaApplicationStub);
    assertNotNull(macApp);
    assertNotNull(macAppArchive);
    assertNotNull(signedAndNotarizedMacApp);
    assertNotNull(signedAndNotarizedMacAppArchive);

    assertInstanceOf(JASDownloader.class, javaApplicationStub);
    assertInstanceOf(AppBundler.class, macApp);
    assertInstanceOf(MacAppArchive.class, macAppArchive);
    assertInstanceOf(SignAndNotarize.class, signedAndNotarizedMacApp);
    assertInstanceOf(SignedAndNotarizedMacAppArchive.class, signedAndNotarizedMacAppArchive);
  }

  @Test
  void apply_supportsExistingAppWithoutMainClass() throws Exception {
    Project project = ProjectBuilder.builder().build();

    project.getPluginManager().apply(GenerateMacAppPlugin.class);

    File existingAppBundleParent = Files.createTempDirectory("existing-app-").toFile();
    File existingAppBundle = new File(existingAppBundleParent, "ExistingDemo.app");
    assertTrue(existingAppBundle.mkdirs());

    TaskProvider<SignAndNotarize> signTaskProvider =
        project.getTasks().named("signedAndNotarizedMacApp", SignAndNotarize.class);
    signTaskProvider.configure(task -> task.setExistingMacAppBundle(existingAppBundle));

    SignAndNotarize signTask = signTaskProvider.get();
    Task macApp = project.getTasks().findByName("macApp");

    assertEquals("ExistingDemo", signTask.getAppName());
    assertFalse(signTask.getTaskDependencies().getDependencies(signTask).contains(macApp));
  }
}
