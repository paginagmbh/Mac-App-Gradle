package gmbh.pagina.tools.gradle.mac_app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/** Extracts a version-control-friendly Finder layout from an existing DMG. */
@DisableCachingByDefault(because = "Extraction mounts a disk image using macOS tools")
public abstract class ExtractDmgLayout extends DefaultTask {
  /** Creates the extraction task with its default prefix and destination. */
  public ExtractDmgLayout() {
    setGroup("Make Mac App");
    setDescription("Extract reusable Finder metadata and backgrounds from a DMG.");
    getPrefix().convention("TEMPLATE");
    getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("dmg-layout"));
  }

  /**
   * Returns the source DMG.
   *
   * @return source disk image property
   */
  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getSourceDmg();

  /**
   * Returns the prefix for the extracted DS_Store filename.
   *
   * @return prefix, defaulting to TEMPLATE
   */
  @Input
  public abstract Property<String> getPrefix();

  /**
   * Returns the directory receiving the template and optional background folder.
   *
   * @return output directory, owned and synchronized by this task
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  /** Extracts layout files without invoking Finder. */
  @TaskAction
  public void taskAction() {
    String name = DmgOptions.templateName(getPrefix().get());
    Path source = getSourceDmg().get().getAsFile().toPath().toAbsolutePath().normalize();
    Path output = getOutputDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();
    try {
      if (!Files.isRegularFile(source)) {
        throw new InvalidUserDataException("Layout DMG does not exist: " + source);
      }
      if (source.toRealPath().startsWith(output.toFile().getCanonicalFile().toPath())) {
        throw new InvalidUserDataException("Source DMG must be outside the extraction output directory.");
      }
      Path workspace = Files.createTempDirectory(getTemporaryDir().toPath(), "layout-");
      try {
        DmgLayout.extractLayout(source, workspace);
        Files.move(workspace.resolve(".DS_Store"), workspace.resolve(name));
        if (Files.exists(workspace.resolve(".background"))) {
          Files.move(workspace.resolve(".background"), workspace.resolve("background"));
        }
        getProject().sync(copy -> {
          copy.from(workspace.toFile());
          copy.into(output.toFile());
        });
      } finally {
        FileUtils.deleteRecursively(workspace.toFile());
      }
    } catch (IOException e) {
      throw new GradleException("Could not extract DMG layout", e);
    }
  }
}
