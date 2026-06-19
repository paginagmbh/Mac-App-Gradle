package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.gradle.api.Project;
import org.gradle.api.Task;
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
}
