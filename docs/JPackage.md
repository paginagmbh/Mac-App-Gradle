# Über die Verwendung von JPackage

Es folgt die korrekte implementierung für Bundling mit JPackage.
Abgebrochen, da dies *immer* die JRE bündelt, was ich nicht will.
Dies könnte sich jedoch in der Zukunft als nützlich erweisen.
Außerdem erfordert es eine fixe Ordnerstruktur neben der Exe/app, was deprimierend ist.

```{code-block} groovy
:caption: build.gradle

plugins { id 'org.beryx.jlink' version '3.0.1' }

jlink {
	options = ['--strip-debug', '--compress', '1', '--no-header-files', '--no-man-pages']
	moduleName =    'de.paginagmbh.parsx.console'
	jpackage {
		outputDir = 'jpackage'
		imageName = project.name
		skipInstaller = true
	}
}
```

```{code-block} java
:caption: src/main/java/module-info.java

module de.paginagmbh.parsx.console {
	requires java.desktop;
	requires java.prefs;
	requires com.formdev.flatlaf;
}
```
