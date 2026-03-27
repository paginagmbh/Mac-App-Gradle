# About Using JPackage

The following is the correct implementation for bundling with JPackage.
Abandoned because this *always* bundles the JRE, which I don't want.
However, this could prove useful in the future.
Additionally, it requires a fixed folder structure next to the exe/app, which is frustrating.

```groovy
// build.gradle
plugins { id 'org.beryx.jlink' version '3.0.1' }

jlink {
    options = ['--strip-debug', '--compress', '1', '--no-header-files', '--no-man-pages']
    moduleName = 'de.paginagmbh.parsx.console'
    jpackage {
        outputDir = 'jpackage'
        imageName = project.name
        skipInstaller = true
    }
}
```

```java
// src/main/java/module-info.java
module de.paginagmbh.parsx.console {
    requires java.desktop;
    requires java.prefs;
    requires com.formdev.flatlaf;
}
```
