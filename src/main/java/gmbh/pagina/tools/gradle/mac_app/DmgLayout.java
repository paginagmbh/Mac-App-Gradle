package gmbh.pagina.tools.gradle.mac_app;

import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.InvalidUserDataException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.SAXException;

/** Writes Finder metadata on the actual volume, so background aliases survive compression. */
final class DmgLayout {
  private DmgLayout() {}

  static void packageApp(
      DmgOptions options, File source, File output, String appName, String version,
      File temporaryDirectory) {
    if (!source.isDirectory()) {
      throw new InvalidUserDataException("Could not find app bundle to package: " + source);
    }
    if (options.getLayoutDmg().isPresent() && options.getLayoutDirectory().isPresent()) {
      throw new InvalidUserDataException("Configure either layoutDmg or layoutDirectory, not both.");
    }
    if (options.getLayoutDirectory().isPresent()) {
      Path directory = options.getLayoutDirectory().get().getAsFile().toPath();
      if (!Files.isRegularFile(directory.resolve(
          DmgOptions.templateName(options.getLayoutPrefix().get())), LinkOption.NOFOLLOW_LINKS)) {
        throw new InvalidUserDataException("Layout directory has no "
            + DmgOptions.templateName(options.getLayoutPrefix().get()) + ": " + directory);
      }
    } else if (options.getLayoutDmg().isPresent()) {
      File layout = options.getLayoutDmg().get().getAsFile();
      if (!layout.isFile()) {
        throw new InvalidUserDataException("Layout DMG does not exist: " + layout);
      }
      try {
        if (layout.getCanonicalFile().equals(output.getCanonicalFile())) {
          throw new InvalidUserDataException("Layout DMG must not be the output DMG: " + layout);
        }
      } catch (IOException e) {
        throw new GradleException("Could not resolve layout DMG path", e);
      }
    } else if (options.getBackgroundPicture().isPresent()
        && !options.getBackgroundPicture().get().getAsFile().isFile()) {
      throw new InvalidUserDataException(
          "DMG background picture does not exist: "
              + options.getBackgroundPicture().get().getAsFile());
    }
    ejectPreviousImages(appName);
    try {
      Files.createDirectories(output.toPath().toAbsolutePath().getParent());
      String volumeName = appName + ' ' + version;
      if (!options.isEnabled()) {
        Shell.sh("hdiutil", "create", "-volname", volumeName, "-srcfolder",
            source.getAbsolutePath(), "-ov", "-format", "UDZO", output.getAbsolutePath());
        return;
      }
      Path workspace = Files.createTempDirectory(temporaryDirectory.toPath(), "dmg-");
      try {
        create(options, source.toPath(), output.toPath(), volumeName, appName, workspace);
      } finally {
        FileUtils.deleteRecursively(workspace.toFile());
      }
    } catch (IOException e) {
      throw new GradleScriptException("Could not create DMG", e);
    }
  }

  static void ejectPreviousImages(String appName) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Node document = factory.newDocumentBuilder().parse(
          new InputSource(new StringReader(Shell.sh("hdiutil", "info", "-plist").out.strip())));
      javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList images = (NodeList) xpath.evaluate(
          "/plist/dict/key[.='images']/following-sibling::*[1]/dict",
          document, XPathConstants.NODESET);
      for (int i = 0; i < images.getLength(); i++) {
        Node image = images.item(i);
        String path = xpath.evaluate("key[.='image-path']/following-sibling::*[1]", image);
        if (!Path.of(path).getFileName().toString().equals(appName + ".dmg")) continue;
        NodeList mounts = (NodeList) xpath.evaluate(
            "key[.='system-entities']/following-sibling::*[1]/dict"
                + "/key[.='mount-point']/following-sibling::*[1]",
            image, XPathConstants.NODESET);
        for (int j = 0; j < mounts.getLength(); j++) {
          Path mount = Path.of(mounts.item(j).getTextContent());
          if (Files.isDirectory(mount.resolve(appName + ".app"))) {
            Shell.sh("hdiutil", "detach", mount.toString());
          }
        }
      }
    } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
      throw new GradleException("Could not inspect mounted DMGs before packaging " + appName, e);
    }
  }

  static void create(
      DmgOptions options, Path source, Path output, String volumeName, String appName,
      Path workspace) throws IOException {
    Path contents = Files.createDirectory(workspace.resolve("contents"));
    Shell.sh("ditto", source.toString(), contents.resolve(source.getFileName()).toString());
    if (options.getLayoutDmg().isPresent() || options.getLayoutDirectory().isPresent()) {
      if (options.getLayoutDirectory().isPresent()) {
        Path directory = options.getLayoutDirectory().get().getAsFile().toPath();
        copyLayout(directory, DmgOptions.templateName(options.getLayoutPrefix().get()),
            "background", contents);
      } else {
        extractLayout(options.getLayoutDmg().get().getAsFile().toPath(), contents);
      }
      Files.createSymbolicLink(contents.resolve("Applications"), Path.of("/Applications"));
      Shell.sh("hdiutil", "create", "-volname", volumeName, "-srcfolder", contents.toString(),
          "-fs", "HFS+", "-format", "UDZO", "-ov", output.toString());
      return;
    }
    if (options.getBackgroundPicture().isPresent()) {
      Path directory = Files.createDirectory(contents.resolve(".background"));
      Shell.sh("sips", "-s", "format", "tiff",
          options.getBackgroundPicture().get().getAsFile().getAbsolutePath(),
          "--out", directory.resolve("background.tiff").toString());
      for (java.io.File retina : options.getBackgroundPicture2x()) {
        Shell.sh("sips", "-s", "format", "tiff", retina.getAbsolutePath(),
            "--out", directory.resolve("background@2x.tiff").toString());
      }
    }
    Path image = workspace.resolve("writable.dmg");
    Shell.sh("hdiutil", "create", "-volname", volumeName, "-srcfolder", contents.toString(),
        "-fs", "HFS+", "-format", "UDRW", image.toString());

    // Keep mountpoints outside recursively cleaned workspaces, even if detaching fails.
    Path mount = Files.createTempDirectory("mac-app-dmg-volume-").toRealPath();
    boolean mounted = false;
    try {
      Shell.sh("hdiutil", "attach", "-nobrowse", "-noautoopen",
          "-mountpoint", mount.toString(), image.toString());
      mounted = true;
      try {
        Files.createSymbolicLink(mount.resolve("Applications"), Path.of("/Applications"));
        Path background = options.getBackgroundPicture().isPresent()
            ? mount.resolve(".background/background.tiff") : null;
        Path script = workspace.resolve("layout.applescript");
        Files.writeString(script, script(options, mount, appName, background));
        Shell.sh("osascript", script.toString());
        if (!Files.isRegularFile(mount.resolve(".DS_Store"))) {
          throw new GradleException("Finder did not write the DMG layout (.DS_Store). "
              + "DMG layout requires a graphical macOS session and Finder automation permission.");
        }
      } finally {
        Shell.sh("hdiutil", "detach", mount.toString());
        mounted = false;
      }
    } finally {
      // Never recursively delete a mountpoint.
      if (!mounted) Files.delete(mount);
    }
    Shell.sh("hdiutil", "convert", image.toString(), "-format", "UDZO",
        "-ov", "-o", output.toString());
  }

  static void extractLayout(Path image, Path contents) throws IOException {
    Path mount = Files.createTempDirectory("mac-app-dmg-layout-").toRealPath();
    boolean mounted = false;
    try {
      Shell.sh("hdiutil", "attach", "-readonly", "-nobrowse", "-noautoopen",
          "-mountpoint", mount.toString(), image.toAbsolutePath().toString());
      mounted = true;
      try {
        copyLayout(mount, ".DS_Store", ".background", contents);
      } finally {
        Shell.sh("hdiutil", "detach", mount.toString());
        mounted = false;
      }
    } finally {
      if (!mounted) Files.delete(mount);
    }
  }

  private static void copyLayout(
      Path source, String metadataName, String backgroundName, Path contents) throws IOException {
    Path metadata = source.resolve(metadataName);
    if (!Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)) {
      throw new InvalidUserDataException("Layout has no root " + metadataName + " file: " + source);
    }
    Files.copy(metadata, contents.resolve(".DS_Store"));
    Path background = source.resolve(backgroundName);
    if (Files.exists(background, LinkOption.NOFOLLOW_LINKS)) {
      if (!Files.isDirectory(background, LinkOption.NOFOLLOW_LINKS)) {
        throw new InvalidUserDataException("Layout background must be a directory: " + background);
      }
      Shell.sh("ditto", background.toString(), contents.resolve(".background").toString());
    }
  }

  static String script(DmgOptions options, Path mount, String appName, Path background) {
    List<Integer> size = options.getWindowSize();
    return "tell application \"Finder\"\n"
        + "  set theDisk to POSIX file " + quote(mount.toString()) + " as alias\n"
        + "  open theDisk\n"
        + "  set theWindow to container window of theDisk\n"
        + "  set current view of theWindow to icon view\n"
        + "  set toolbar visible of theWindow to false\n"
        + "  set statusbar visible of theWindow to false\n"
        + "  set bounds of theWindow to {400, 100, " + (400 + size.get(0)) + ", "
        + (100 + size.get(1)) + "}\n"
        + "  set viewOptions to icon view options of theWindow\n"
        + "  set arrangement of viewOptions to not arranged\n"
        + "  set icon size of viewOptions to " + options.getIconSize() + "\n"
        + (background == null ? ""
            : "  set background picture of viewOptions to POSIX file "
                + quote(background.toString()) + "\n")
        + "  set position of item " + quote(appName + ".app") + " of theWindow to "
        + coordinates(options.getAppIconPosition()) + "\n"
        + "  set position of item \"Applications\" of theWindow to "
        + coordinates(options.getApplicationsIconPosition()) + "\n"
        + "  update theDisk without registering applications\n"
        + "  delay 5\n"
        + "  close theWindow\n"
        + "end tell\n";
  }

  private static String coordinates(List<Integer> values) {
    return "{" + values.get(0) + ", " + values.get(1) + "}";
  }

  private static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\r", "\\r").replace("\n", "\\n") + "\"";
  }
}
