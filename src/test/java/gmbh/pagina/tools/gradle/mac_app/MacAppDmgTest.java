package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import garden.ephemeral.macfiles.dsstore.DSStore;
import garden.ephemeral.macfiles.dsstore.DSStoreProperties;
import garden.ephemeral.macfiles.dsstore.types.IntPoint;
import garden.ephemeral.macfiles.dsstore.util.FileMode;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.gradle.api.InvalidUserDataException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class MacAppDmgTest {
  @TempDir Path tempDir;

  private MacAppDmg newTask() {
    MacAppDmg task = ProjectBuilder.builder().build().getTasks().create("macAppDmg", MacAppDmg.class);
    task.getAppName().set("Unsigned Demo");
    task.getProjectVersion().set("1.0");
    task.getSourceDirectoryProperty().fileValue(tempDir.resolve("source").toFile());
    task.getOutdirProperty().fileValue(tempDir.resolve("output").toFile());
    return task;
  }

  @Test
  void rejectsMissingBundle() {
    assertThrows(InvalidUserDataException.class, () -> newTask().taskAction());
  }

  @Test
  void layoutDmg_supportsGroovyAndGeneratorDependency() {
    MacAppDmg task = newTask();
    AppBundler app = task.getProject().getTasks().create("macApp", AppBundler.class);
    task.useDmg(app::getDmg);
    Binding binding = new Binding();
    binding.setVariable("task", app);
    binding.setVariable("source", tempDir.resolve("layout.dmg").toFile());
    new GroovyShell(binding).evaluate("task.configure { dmg { layoutDmg = source } }");
    assertTrue(task.getDmg().isEnabled());
    assertEquals(tempDir.resolve("layout.dmg").toFile(),
        task.getDmg().getLayoutDmg().get().getAsFile());
    org.gradle.api.tasks.TaskProvider<org.gradle.api.Task> generator =
        task.getProject().getTasks().register("layout",
            generated -> generated.getOutputs().file(tempDir.resolve("layout.dmg").toFile()));
    app.getDmg().getLayoutDmg().set(task.getProject().getLayout().file(
        generator.map(generated -> generated.getOutputs().getFiles().getSingleFile())));
    assertTrue(task.getTaskDependencies().getDependencies(task).contains(generator.get()));
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void reusesLayoutWithoutFinder() throws Exception {
    assertReusedLayout(true);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void reusesLayoutWithoutBackground() throws Exception {
    assertReusedLayout(false);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void extractsAndReusesLayoutWithCustomPrefix() throws Exception {
    assertReusedLayout(true, true);
  }

  private void assertReusedLayout(boolean withBackground) throws Exception {
    assertReusedLayout(withBackground, false);
  }

  private void assertReusedLayout(boolean withBackground, boolean extracted) throws Exception {
    MacAppDmg task = newTask();
    Path app = Files.createDirectories(task.getMacApp().toPath());
    Files.writeString(app.resolve("current.txt"), "new app");
    Path contents = Files.createDirectory(tempDir.resolve("layout-contents"));
    Path metadata = contents.resolve(".DS_Store");
    try (DSStore store = DSStore.Companion.open(metadata, FileMode.READ_WRITE)) {
      store.set("Unsigned Demo.app", DSStoreProperties.Companion.getIconLocation(),
          new IntPoint(170, 150));
    }
    if (withBackground) {
      Path background = Files.createDirectory(contents.resolve(".background"));
      Files.write(background.resolve("background.tiff"), new byte[] {1, 2, 3});
      Files.write(background.resolve("background@2x.tiff"), new byte[] {4, 5, 6});
    }
    Files.writeString(contents.resolve("not-copied.txt"), "old content");
    Files.createDirectories(contents.resolve("Old.app"));
    Path source = tempDir.resolve("layout.dmg");
    Shell.sh("hdiutil", "create", "-srcfolder", contents.toString(), "-format", "UDZO",
        source.toString());
    byte[] sourceBytes = Files.readAllBytes(source);
    if (extracted) {
      ExtractDmgLayout extract = task.getProject().getTasks()
          .create("extractDmgLayout", ExtractDmgLayout.class);
      assertEquals("TEMPLATE", extract.getPrefix().get());
      extract.getSourceDmg().set(source.toFile());
      extract.getOutputDirectory().set(tempDir.resolve("templates").toFile());
      extract.getPrefix().set("INSTALLER");
      extract.taskAction();
      Path templates = extract.getOutputDirectory().get().getAsFile().toPath();
      assertArrayEquals(Files.readAllBytes(metadata),
          Files.readAllBytes(templates.resolve("INSTALLER.DS_Store")));
      assertTrue(Files.isRegularFile(templates.resolve("background/background@2x.tiff")));
      assertFalse(Files.exists(templates.resolve(".DS_Store")));
      assertFalse(Files.exists(templates.resolve(".background")));
      Files.writeString(templates.resolve("stale-file"), "stale");
      extract.taskAction();
      assertFalse(Files.exists(templates.resolve("stale-file")));
      assertArrayEquals(sourceBytes, Files.readAllBytes(source));
      Files.delete(source);
      task.getDmg().setLayoutDirectory(templates.toFile());
      task.getDmg().setLayoutPrefix("INSTALLER");
    } else {
      task.getDmg().setLayoutDmg(source.toFile());
    }
    // Source metadata wins; a generation attempt would fail on this missing image.
    task.getDmg().setBackgroundPicture(tempDir.resolve("missing.png").toFile());
    task.taskAction();

    Path mount = Files.createDirectory(tempDir.resolve("reused"));
    Shell.sh("hdiutil", "attach", "-readonly", "-nobrowse", "-noautoopen",
        "-mountpoint", mount.toString(), task.getMacAppDmg().getAbsolutePath());
    try {
      assertArrayEquals(Files.readAllBytes(metadata), Files.readAllBytes(mount.resolve(".DS_Store")));
      assertEquals("new app", Files.readString(mount.resolve("Unsigned Demo.app/current.txt")));
      assertFalse(Files.exists(mount.resolve("Old.app")));
      assertFalse(Files.exists(mount.resolve("not-copied.txt")));
      assertTrue(Files.isSymbolicLink(mount.resolve("Applications")));
      assertEquals(withBackground, Files.exists(mount.resolve(".background")));
      if (withBackground) {
        for (String name : java.util.List.of("background.tiff", "background@2x.tiff")) {
          assertArrayEquals(Files.readAllBytes(contents.resolve(".background").resolve(name)),
              Files.readAllBytes(mount.resolve(".background").resolve(name)));
        }
      }
    } finally {
      Shell.sh("hdiutil", "detach", "-force", mount.toString());
    }
    if (!extracted) assertArrayEquals(sourceBytes, Files.readAllBytes(source));
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void rejectsSourceWithoutMetadataAndDetachesIt() throws Exception {
    MacAppDmg task = newTask();
    Files.createDirectories(task.getMacApp().toPath());
    Path contents = Files.createDirectory(tempDir.resolve("empty-layout"));
    Files.writeString(contents.resolve("readme"), "No layout here");
    Path image = tempDir.resolve("layout.dmg");
    Shell.sh("hdiutil", "create", "-srcfolder", contents.toString(), "-format", "UDZO",
        image.toString());
    task.getDmg().setLayoutDmg(image.toFile());
    InvalidUserDataException error = assertThrows(InvalidUserDataException.class, task::taskAction);
    assertTrue(error.getMessage().contains("no root .DS_Store"));
    assertFalse(Shell.sh("hdiutil", "info").out.contains(image.toString()));
  }

  @Test
  void rejectsMissingAndOutputLayoutDmg() throws Exception {
    MacAppDmg task = newTask();
    Files.createDirectories(task.getMacApp().toPath());
    task.getDmg().setLayoutDmg(tempDir.resolve("missing.dmg").toFile());
    assertThrows(InvalidUserDataException.class, task::taskAction);
    Files.createDirectories(task.getMacAppDmg().toPath().getParent());
    Files.writeString(task.getMacAppDmg().toPath(), "output");
    task.getDmg().setLayoutDmg(task.getMacAppDmg());
    assertThrows(InvalidUserDataException.class, task::taskAction);
    assertEquals("output", Files.readString(task.getMacAppDmg().toPath()));
  }

  @Test
  void validatesExtractedLayoutAndPrefix() throws Exception {
    MacAppDmg task = newTask();
    Files.createDirectories(task.getMacApp().toPath());
    task.getDmg().setLayoutDirectory(tempDir.toFile());
    assertThrows(InvalidUserDataException.class, task::taskAction);
    assertThrows(InvalidUserDataException.class, () -> task.getDmg().setLayoutPrefix("../escape"));
    assertThrows(InvalidUserDataException.class, () -> task.getDmg().setLayoutPrefix(""));
    task.getDmg().setLayoutDmg(tempDir.resolve("layout.dmg").toFile());
    assertThrows(InvalidUserDataException.class, task::taskAction);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createsUnsignedDmg() throws Exception {
    assertDmg(false);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  void createsUnsignedDmgWithLayout() throws Exception {
    assertDmg(true);
  }

  private void assertDmg(boolean withLayout) throws Exception {
    MacAppDmg task = newTask();
    Path app = task.getMacApp().toPath();
    Path macOs = Files.createDirectories(app.resolve("Contents/MacOS"));
    Path launcher = Files.writeString(macOs.resolve("launcher"), "#!/bin/sh\nexit 0\n");
    assertTrue(launcher.toFile().setExecutable(true, false));
    Files.createSymbolicLink(macOs.resolve("launcher-link"), Path.of("launcher"));
    if (withLayout) {
      Path background = tempDir.resolve("background.png");
      assertTrue(ImageIO.write(new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB),
          "PNG", background.toFile()));
      Binding binding = new Binding();
      AppBundler appTask = task.getProject().getTasks().create("macApp", AppBundler.class);
      task.useDmg(appTask::getDmg);
      binding.setVariable("task", appTask);
      binding.setVariable("background", background.toFile());
      new GroovyShell(binding).evaluate(
          "task.configure { dmg { backgroundPicture = background; appIconPosition = [170, 150] } }");
    }

    task.taskAction();

    Path mount = Files.createDirectory(tempDir.resolve("mounted"));
    Shell.sh("hdiutil", "attach", "-readonly", "-nobrowse", "-noautoopen",
        "-mountpoint", mount.toString(), task.getMacAppDmg().getAbsolutePath());
    try {
      Path mountedApp = mount.resolve("Unsigned Demo.app");
      assertEquals(Files.readString(launcher),
          Files.readString(mountedApp.resolve("Contents/MacOS/launcher")));
      assertTrue(Files.isExecutable(mountedApp.resolve("Contents/MacOS/launcher")));
      assertTrue(Files.isSymbolicLink(mountedApp.resolve("Contents/MacOS/launcher-link")));
      assertFalse(Files.exists(mountedApp.resolve("Contents/_CodeSignature")));
      if (withLayout) {
        assertTrue(Files.exists(mount.resolve(".background/background.tiff")));
        assertTrue(Files.isSymbolicLink(mount.resolve("Applications")));
        Path metadata = Files.copy(mount.resolve(".DS_Store"), tempDir.resolve("layout.DS_Store"));
        try (DSStore store = DSStore.Companion.open(metadata, FileMode.READ_ONLY)) {
          assertEquals(new IntPoint(170, 150),
              store.get("Unsigned Demo.app", DSStoreProperties.Companion.getIconLocation()));
        }
      } else {
        assertFalse(Files.exists(mount.resolve(".DS_Store")));
      }
    } finally {
      Shell.sh("hdiutil", "detach", "-force", mount.toString());
    }
    assertFalse(Files.exists(app.resolve(".DS_Store")));
  }
}
