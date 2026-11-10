package gmbh.pagina.tools.gradle.mac_app;

import java.io.File;
import java.util.List;
import java.util.Collections;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;

/** Optional Finder layout for a disk image. Coordinates and sizes are in pixels. */
public class DmgOptions {
  private final RegularFileProperty backgroundPicture;
  private List<Integer> appIconPosition = List.of(160, 140);
  private List<Integer> applicationsIconPosition = List.of(480, 140);
  private List<Integer> windowSize = List.of(640, 400);
  private int iconSize = 72;
  private boolean configured;

  /**
   * Creates layout options using Gradle's file property support.
   *
   * @param objects Gradle object factory
   */
  @Inject
  public DmgOptions(ObjectFactory objects) {
    backgroundPicture = objects.fileProperty();
  }

  /**
   * The optional background image; PNG, JPEG and TIFF are supported by macOS.
   *
   * @return background image file property
   */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public RegularFileProperty getBackgroundPicture() {
    return backgroundPicture;
  }

  /**
   * Sets the background image.
   *
   * @param file background image file
   */
  public void setBackgroundPicture(File file) {
    backgroundPicture.set(file);
  }

  /**
   * Finds the optional sibling image with an {@code @2x} suffix before its extension.
   *
   * @return the existing high-resolution background, or an empty list
   */
  @InputFiles
  @PathSensitive(PathSensitivity.NONE)
  public List<File> getBackgroundPicture2x() {
    if (!backgroundPicture.isPresent()) return Collections.emptyList();
    File background = backgroundPicture.get().getAsFile();
    String name = background.getName();
    int extension = name.lastIndexOf('.');
    String retinaName = extension > 0
        ? name.substring(0, extension) + "@2x" + name.substring(extension)
        : name + "@2x";
    File retina = new File(background.getParentFile(), retinaName);
    return retina.isFile() ? List.of(retina) : Collections.emptyList();
  }

  /**
   * The app icon's [x, y] position, defaulting to [160, 140].
   *
   * @return app icon coordinates
   */
  @Input
  public List<Integer> getAppIconPosition() {
    return appIconPosition;
  }

  /**
   * Sets the app icon's [x, y] position.
   *
   * @param position two nonnegative coordinates
   */
  public void setAppIconPosition(List<Integer> position) {
    appIconPosition = pair("appIconPosition", position, 0);
    configured = true;
  }

  /**
   * The Applications shortcut's [x, y] position, defaulting to [480, 140].
   *
   * @return Applications shortcut coordinates
   */
  @Input
  public List<Integer> getApplicationsIconPosition() {
    return applicationsIconPosition;
  }

  /**
   * Sets the Applications shortcut's [x, y] position.
   *
   * @param position two nonnegative coordinates
   */
  public void setApplicationsIconPosition(List<Integer> position) {
    applicationsIconPosition = pair("applicationsIconPosition", position, 0);
    configured = true;
  }

  /**
   * The Finder window's [width, height], defaulting to [640, 400].
   *
   * @return window dimensions in pixels
   */
  @Input
  public List<Integer> getWindowSize() {
    return windowSize;
  }

  /**
   * Sets the Finder window's [width, height].
   *
   * @param size two positive window dimensions in pixels
   */
  public void setWindowSize(List<Integer> size) {
    windowSize = pair("windowSize", size, 1);
    configured = true;
  }

  /**
   * The icon size, defaulting to 72 pixels.
   *
   * @return icon size in pixels
   */
  @Input
  public int getIconSize() {
    return iconSize;
  }

  /**
   * Sets the Finder icon size (16 to 512 pixels).
   *
   * @param size icon size in pixels
   */
  public void setIconSize(int size) {
    if (size < 16 || size > 512) {
      throw new InvalidUserDataException("dmg.iconSize must be between 16 and 512.");
    }
    iconSize = size;
    configured = true;
  }

  /**
   * Whether layout generation has been requested.
   *
   * @return true when layout generation is enabled
   */
  @Input
  public boolean isEnabled() {
    return configured || backgroundPicture.isPresent();
  }

  void enable() {
    configured = true;
  }

  private static List<Integer> pair(String name, List<?> values, int minimum) {
    if (values == null || values.size() != 2
        || values.stream().anyMatch(value -> !(value instanceof Integer)
            || (Integer) value < minimum)) {
      throw new InvalidUserDataException(
          "dmg." + name + " must contain two integers >= " + minimum + ".");
    }
    return List.of((Integer) values.get(0), (Integer) values.get(1));
  }
}
