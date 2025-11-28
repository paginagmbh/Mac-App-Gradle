package de.paginagmbh.commons.mac_app_gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;

/** The plugin for generating and signing a mac app. */
public class GenerateMacAppPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply("application");

    // Activate ant integration in the FileUtils helper class
    FileUtils.setProject(project);

    // Task to download the UniversalJavaApplicationStub.
    Task ujas = project.getTasks().create("universalJavaApplicationStub", UJASDownloader.class);
    // Task to create an unsigned mac app
    Task appBundler = project.getTasks().create("macApp", AppBundler.class);
    // Task to sign the unsigned mac app
    Task signAndNotarizeMacApp =
        project.getTasks().create("signedAndNotarizedMacApp", SignAndNotarize.class);

    // Download the UniversalJavaApplicationStub and make jars before creating the mac app.
    appBundler.dependsOn(ujas);
    appBundler.dependsOn("jar");

    // Only sign the mac app after it exists.
    signAndNotarizeMacApp.dependsOn(appBundler);
  }
}
