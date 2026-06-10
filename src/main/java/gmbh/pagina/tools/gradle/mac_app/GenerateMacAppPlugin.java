package gmbh.pagina.tools.gradle.mac_app;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

/** The plugin for generating and signing a mac app. */
public class GenerateMacAppPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply("application");

    JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
    JavaPluginExtension javaPluginExtension =
        project.getExtensions().getByType(JavaPluginExtension.class);
    Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
    TaskProvider<Jar> jar = project.getTasks().named("jar", Jar.class);

    // Task to download the JavaApplicationStub.
    TaskProvider<JASDownloader> jas =
        project.getTasks().register("javaApplicationStub", JASDownloader.class);

    // Task to create an unsigned mac app
    TaskProvider<AppBundler> appBundler = project.getTasks().register("macApp", AppBundler.class);
    // Task to sign the unsigned mac app
    TaskProvider<SignAndNotarize> signAndNotarizeMacApp =
        project.getTasks().register("signedAndNotarizedMacApp", SignAndNotarize.class);

    // Download the JavaApplicationStub and make jars before creating the mac app.
    appBundler.configure(
        task -> {
          task.getMainClassNameProperty().convention(javaApplication.getMainClass());
          task.getProjectNameProperty().convention(project.provider(project::getName));
          task.getProjectVersionProperty().convention(project.provider(() -> String.valueOf(project.getVersion())));
          task.getTargetJavaVersionProperty()
              .convention(
                  project.provider(
                      () ->
                          Integer.parseInt(
                              javaPluginExtension.getTargetCompatibility().getMajorVersion())));
          task.getJavaApplicationStubFiles().from(jas);
          task.getMainJarFiles().from(jar.flatMap(Jar::getArchiveFile));
          task.getRuntimeClasspath().from(runtimeClasspath);
          task.dependsOn(jas);
          task.dependsOn(jar);
        });

    // Only sign the mac app after it exists.
    signAndNotarizeMacApp.configure(
        task -> {
          task.getAppNameProperty().convention(appBundler.flatMap(AppBundler::getAppNameProperty));
          task.getUnsignedMacAppDirectoryProperty()
              .convention(appBundler.flatMap(AppBundler::getOutdirProperty));
          task.getMacAppIconProperty().convention(appBundler.flatMap(AppBundler::getIconProperty));
          task.getProjectVersionProperty()
              .convention(project.provider(() -> String.valueOf(project.getVersion())));
          task.dependsOn(appBundler);
        });
  }
}
