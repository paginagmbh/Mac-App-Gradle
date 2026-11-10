package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import garden.ephemeral.macfiles.dsstore.DSStore;
import garden.ephemeral.macfiles.dsstore.DSStoreProperties;
import garden.ephemeral.macfiles.dsstore.DSStoreRecordKey;
import garden.ephemeral.macfiles.dsstore.types.BrowserWindowSettings;
import garden.ephemeral.macfiles.dsstore.types.IntPoint;
import garden.ephemeral.macfiles.dsstore.types.IconViewOptions;
import garden.ephemeral.macfiles.dsstore.util.FileMode;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class SignAndNotarizeTest {

  @TempDir Path tempDir;

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

  @Test
  void dmg_supportsGroovyConfigurationBlock() {
    SignAndNotarize task = newDmgTask();
    assertFalse(task.getDmg().isEnabled());
    Binding binding = new Binding();
    binding.setVariable("task", task);
    binding.setVariable("background", tempDir.resolve("background.png").toFile());
    new GroovyShell(binding).evaluate(
        "task.configure { dmg {\n"
            + "backgroundPicture = background\n"
            + "appIconPosition = [160, 140]\n"
            + "applicationsIconPosition = [480, 140]\n"
            + "windowSize = [640, 400]\n"
            + "iconSize = 72\n"
            + "} }");
    assertTrue(task.getDmg().isEnabled());
    assertEquals(Arrays.asList(160, 140), task.getDmg().getAppIconPosition());
    assertEquals(Arrays.asList(480, 140), task.getDmg().getApplicationsIconPosition());
    assertEquals(Arrays.asList(640, 400), task.getDmg().getWindowSize());
    assertEquals(72, task.getDmg().getIconSize());
    assertEquals(tempDir.resolve("background.png").toFile(),
        task.getDmg().getBackgroundPicture().get().getAsFile());
  }

  @Test
  void dmg_retainsBackgroundGeneratorDependency() {
    SignAndNotarize task = newDmgTask();
    TaskProvider<Task> generator = task.getProject().getTasks().register("background",
        generated -> generated.getOutputs().file(tempDir.resolve("background.png").toFile()));
    task.getDmg().getBackgroundPicture().set(task.getProject().getLayout().file(
        generator.map(generated -> generated.getOutputs().getFiles().getSingleFile())));
    assertTrue(task.getTaskDependencies().getDependencies(task).contains(generator.get()));
  }

  @Test
  void dmg_discoversOptional2xBackground() throws Exception {
    DmgOptions options = newDmgTask().getDmg();
    assertTrue(options.getBackgroundPicture2x().isEmpty());
    for (String name : Arrays.asList("background.png", "background.image.jpg", "background")) {
      options.setBackgroundPicture(tempDir.resolve(name).toFile());
      assertTrue(options.getBackgroundPicture2x().isEmpty());
      int extension = name.lastIndexOf('.');
      String retinaName = extension > 0
          ? name.substring(0, extension) + "@2x" + name.substring(extension)
          : name + "@2x";
      Path retina = Files.writeString(tempDir.resolve(retinaName), "image");
      assertEquals(java.util.List.of(retina.toFile()), options.getBackgroundPicture2x());
      Files.delete(retina);
      assertTrue(options.getBackgroundPicture2x().isEmpty());
    }
  }

  @Test
  void dmg_rejectsInvalidCoordinatesAndSizes() {
    DmgOptions options = newDmgTask().getDmg();
    assertThrows(InvalidUserDataException.class,
        () -> options.setAppIconPosition(Arrays.asList(1)));
    assertThrows(InvalidUserDataException.class,
        () -> options.setApplicationsIconPosition(Arrays.asList(-1, 140)));
    assertThrows(InvalidUserDataException.class,
        () -> options.setWindowSize(Arrays.asList(640, 0)));
    assertThrows(InvalidUserDataException.class, () -> options.setIconSize(0));
  }

  @Test
  void dmg_escapesAppleScriptStrings() {
    String script = DmgLayout.script(newDmgTask().getDmg(), Path.of("/tmp/a\"b\\c"),
        "An \"App\"", Path.of("/tmp/image.png"));
    assertTrue(script.contains("\"/tmp/a\\\"b\\\\c\""));
    assertTrue(script.contains("\"An \\\"App\\\".app\""));
  }

  @Test
  void createDmg_rejectsMissingBackground() {
    SignAndNotarize task = newDmgTask();
    task.getDmg().setBackgroundPicture(tempDir.resolve("missing.png").toFile());
    assertThrows(InvalidUserDataException.class, task::createDmg);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createDmg_includesBackgroundAndUnmodifiedApp() throws Exception {
    assertDmgContents(true, true, true);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createDmg_backgroundWithout2xRemainsSupported() throws Exception {
    assertDmgContents(true, true, false);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createDmg_withoutLayoutDoesNotRequireFinder() throws Exception {
    assertDmgContents(false, false);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createDmg_positionsIconsWithoutBackground() throws Exception {
    assertDmgContents(false, true);
  }

  private SignAndNotarize newDmgTask() {
    Project project = ProjectBuilder.builder().build();
    SignAndNotarize task = project.getTasks().create("sign", SignAndNotarize.class);
    task.getAppNameProperty().set("Example App");
    task.setProjectVersion("Layout " + java.util.UUID.randomUUID().toString().substring(0, 8));
    task.setOutdir(tempDir.resolve("signed").toFile());
    return task;
  }

  private void assertDmgContents(boolean withBackground, boolean withLayout) throws Exception {
    assertDmgContents(withBackground, withLayout, false);
  }

  private void assertDmgContents(
      boolean withBackground, boolean withLayout, boolean withRetina) throws Exception {
    SignAndNotarize task = newDmgTask();
    Path app = task.getSignedAndNotarizedMacApp().toPath();
    Path macOs = Files.createDirectories(app.resolve("Contents/MacOS"));
    Path executable = Files.writeString(macOs.resolve("launcher"), "#!/bin/sh\nexit 0\n");
    assertTrue(executable.toFile().setExecutable(true, false));
    Files.createSymbolicLink(macOs.resolve("launcher-link"), Path.of("launcher"));

    Path background = tempDir.resolve("background.png");
    BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(new Color(235, 240, 250));
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
      graphics.setColor(Color.DARK_GRAY);
      graphics.drawString("Drag Example App to Applications", 180, 200);
    } finally {
      graphics.dispose();
    }
    assertTrue(ImageIO.write(image, "PNG", background.toFile()));
    BufferedImage retina = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
    retina.setRGB(20, 20, Color.RED.getRGB());
    if (withRetina) {
      assertTrue(ImageIO.write(retina, "PNG", tempDir.resolve("background@2x.png").toFile()));
    }
    if (withLayout) {
      task.dmg(options -> {
        options.setAppIconPosition(Arrays.asList(160, 140));
        options.setApplicationsIconPosition(Arrays.asList(480, 140));
        options.setWindowSize(Arrays.asList(640, 400));
        options.setIconSize(72);
        if (withBackground) options.setBackgroundPicture(background.toFile());
      });
    }

    task.createDmg();
    assertTrue(task.getNotarizedDMG().isFile());
    if (withBackground) {
      Path previous = Files.createDirectory(tempDir.resolve("previous"));
      Shell.sh("hdiutil", "attach", "-readonly", "-nobrowse", "-noautoopen",
          "-mountpoint", previous.toString(), task.getNotarizedDMG().getAbsolutePath());
      try {
        task.createDmg();
        assertFalse(Files.exists(previous.resolve("Example App.app")),
            "The previous DMG must be ejected before rebuilding");
      } finally {
        if (Files.exists(previous.resolve("Example App.app"))) {
          Shell.sh("hdiutil", "detach", "-force", previous.toString());
        }
      }
    }
    Path mount = Files.createDirectory(tempDir.resolve("mounted"));
    Shell.sh(
        "hdiutil",
        "attach",
        "-readonly",
        "-nobrowse",
        "-noautoopen",
        "-mountpoint",
        mount.toString(),
        task.getNotarizedDMG().getAbsolutePath());
    try {
      Path mountedMacOs = mount.resolve("Example App.app/Contents/MacOS");
      assertEquals(Files.readString(executable), Files.readString(mountedMacOs.resolve("launcher")));
      assertTrue(Files.isExecutable(mountedMacOs.resolve("launcher")));
      assertTrue(Files.isSymbolicLink(mountedMacOs.resolve("launcher-link")));
      if (withLayout) {
        Path metadata = tempDir.resolve("finder.DS_Store");
        Files.copy(mount.resolve(".DS_Store"), metadata);
        assertFinderLayout(metadata, withBackground);
        assertTrue(Files.isSymbolicLink(mount.resolve("Applications")));
        assertEquals(Path.of("/Applications"), Files.readSymbolicLink(mount.resolve("Applications")));
        if (withBackground) {
          BufferedImage mountedBackground =
              ImageIO.read(mount.resolve(".background/background.tiff").toFile());
          assertEquals(640, mountedBackground.getWidth());
          assertEquals(400, mountedBackground.getHeight());
          assertEquals(image.getRGB(10, 10), mountedBackground.getRGB(10, 10));
          Path mountedRetina = mount.resolve(".background/background@2x.tiff");
          assertEquals(withRetina, Files.exists(mountedRetina));
          if (withRetina) {
            BufferedImage retinaBackground = ImageIO.read(mountedRetina.toFile());
            assertEquals(1280, retinaBackground.getWidth());
            assertEquals(800, retinaBackground.getHeight());
            assertEquals(retina.getRGB(20, 20), retinaBackground.getRGB(20, 20));
          }
        }
      } else {
        assertFalse(Files.exists(mount.resolve(".DS_Store")));
        assertFalse(Files.exists(mount.resolve(".background")));
      }
    } finally {
      Shell.sh("hdiutil", "detach", "-force", mount.toString());
    }

    assertTrue(Files.isSymbolicLink(macOs.resolve("launcher-link")));
    assertFalse(Files.exists(app.resolve(".DS_Store")));
    try (Stream<Path> temporaryFiles = Files.list(task.getTemporaryDir().toPath())) {
      assertEquals(0, temporaryFiles.count());
    }

    String artifactDirectory = System.getProperty("macApp.testDmgDirectory");
    if (withBackground && withLayout && artifactDirectory != null) {
      Path destination = Files.createDirectories(Path.of(artifactDirectory));
      Files.copy(
          task.getNotarizedDMG().toPath(),
          destination.resolve("Example App.dmg"),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void assertFinderLayout(Path dsStore, boolean withBackground) throws Exception {
    try (DSStore store = DSStore.Companion.open(dsStore, FileMode.READ_ONLY)) {
      assertEquals(1, store.get(".", DSStoreProperties.Companion.getUnknownDirectoryTag()));
      assertEquals(
          new IntPoint(160, 140),
          store.get("Example App.app", DSStoreProperties.Companion.getIconLocation()));
      assertEquals(
          new IntPoint(480, 140),
          store.get("Applications", DSStoreProperties.Companion.getIconLocation()));
      IconViewOptions options =
          (IconViewOptions) store.get(".", DSStoreProperties.Companion.getIconViewOptionsPList());
      assertEquals(withBackground ? 2 : 0, options.getBackgroundType());
      assertEquals("none", options.getArrangeBy());
      assertEquals(72.0, options.getIconSize());
      BrowserWindowSettings window =
          (BrowserWindowSettings) store.get(".", DSStoreProperties.Companion.getBrowserWindowSettings());
      assertTrue(window.getWindowBounds().endsWith("{640, 400}}"));
      assertEquals(false, window.getShowToolbar());
      assertEquals(false, window.getShowStatusBar());
      if (withBackground) {
        // The reader cannot decode modern native bookmarks, so inspect the raw record.
        assertTrue(store.find(new DSStoreRecordKey(
            ".", DSStoreProperties.Companion.getBackgroundBookmark())) != null);
      }
    }
  }
}
