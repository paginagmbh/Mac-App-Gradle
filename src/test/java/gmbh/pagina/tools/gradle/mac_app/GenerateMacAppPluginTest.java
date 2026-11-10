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
    Task macAppDmg = project.getTasks().findByName("macAppDmg");
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
    assertInstanceOf(MacAppDmg.class, macAppDmg);
    assertInstanceOf(SignAndNotarize.class, signedAndNotarizedMacApp);
    assertInstanceOf(SignedAndNotarizedMacAppArchive.class, signedAndNotarizedMacAppArchive);
  }

  @Test
  void apply_wiresUnsignedArchivesToAppWithoutSigning() throws Exception {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply(GenerateMacAppPlugin.class);
    AppBundler app = (AppBundler) project.getTasks().getByName("macApp");
    app.getAppNameProperty().set("Unsigned Demo");
    app.getOutdirProperty().fileValue(Files.createTempDirectory("unsigned-output-").toFile());
    app.getProjectVersionProperty().set("2.0");
    MacAppArchive archive = (MacAppArchive) project.getTasks().getByName("macAppArchive");
    MacAppDmg dmg = (MacAppDmg) project.getTasks().getByName("macAppDmg");

    assertEquals("Unsigned Demo", dmg.getAppName().get());
    assertEquals("2.0", dmg.getProjectVersion().get());
    assertEquals(new File(app.getOutdirProperty().get().getAsFile(), "Unsigned Demo.dmg"),
        dmg.getMacAppDmg());
    assertEquals(archive.getMacApp(), dmg.getMacApp());
    assertTrue(app.getFinalizedBy().getDependencies(app).contains(archive));
    assertTrue(app.getFinalizedBy().getDependencies(app).contains(dmg));
    assertTrue(dmg.getTaskDependencies().getDependencies(dmg).contains(app));
    assertFalse(dmg.getTaskDependencies().getDependencies(dmg).contains(
        project.getTasks().getByName("signedAndNotarizedMacApp")));
  }

  @Test
  void apply_signedDmgInheritsUnsignedLayoutUnlessConfigured() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply(GenerateMacAppPlugin.class);
    SignAndNotarize signed =
        (SignAndNotarize) project.getTasks().getByName("signedAndNotarizedMacApp");
    AppBundler unsigned = (AppBundler) project.getTasks().getByName("macApp");
    MacAppDmg unsignedDmg = (MacAppDmg) project.getTasks().getByName("macAppDmg");
    unsigned.dmg(options -> {
      options.setAppIconPosition(java.util.List.of(180, 150));
      options.setIconSize(96);
    });
    assertEquals(java.util.List.of(180, 150), signed.getDmg().getAppIconPosition());
    assertEquals(96, signed.getDmg().getIconSize());
    assertEquals(96, unsignedDmg.getDmg().getIconSize());
    unsigned.getDmg().setIconSize(80);
    assertEquals(80, signed.getDmg().getIconSize());

    signed.dmg(options -> options.setIconSize(64));
    assertEquals(64, signed.getDmg().getIconSize());
    assertEquals(java.util.List.of(160, 140), signed.getDmg().getAppIconPosition());
    assertEquals(80, unsigned.getDmg().getIconSize());
    assertFalse(signed.getTaskDependencies().getDependencies(signed).contains(unsignedDmg));
  }

  @Test
  void apply_inheritedBackgroundRetainsGeneratorDependency() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply(GenerateMacAppPlugin.class);
    AppBundler app = (AppBundler) project.getTasks().getByName("macApp");
    MacAppDmg unsigned = (MacAppDmg) project.getTasks().getByName("macAppDmg");
    SignAndNotarize signed =
        (SignAndNotarize) project.getTasks().getByName("signedAndNotarizedMacApp");
    TaskProvider<Task> generator = project.getTasks().register("background",
        task -> task.getOutputs().file(project.file("background.png")));
    app.getDmg().getBackgroundPicture().set(project.getLayout().file(
        generator.map(task -> task.getOutputs().getFiles().getSingleFile())));
    assertTrue(unsigned.getTaskDependencies().getDependencies(unsigned).contains(generator.get()));
    assertTrue(signed.getTaskDependencies().getDependencies(signed).contains(generator.get()));
    assertFalse(signed.getTaskDependencies().getDependencies(signed).contains(unsigned));
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
