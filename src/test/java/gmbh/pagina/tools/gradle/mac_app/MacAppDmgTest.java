package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
