# Mac App Gradle Plugin

A Gradle plugin that creates and signs Mac apps.
Must be run on a Mac.
An unsigned Mac app can also be built on Linux/WSL.

This task can be run locally, but it is primarily intended for CI use.
For this it will install some requirements as follows:

* It requires [electron-installer-dmg](https://github.com/electron-userland/electron-installer-dmg) for creating a DMG file which can be notarized.
* If it does not exist it will install it using npm.
* If npm is not installed, it will install it using brew.
* If brew is not installed, an error will be thrown.
* It downloads the configured *javaApplicationStub* from GitHub using curl.
  If curl is not available, an error is thrown.


## Usage

### Minimal Example

In build.gradle:

```groovy
plugins {
    id 'java'
    id 'application'

    id 'gmbh.pagina.tools.gradle.mac_app' version '2.0.0-SNAPSHOT'
}

version = "1.0.0"

application {
    mainClass = 'com.company.package.Class'
}

macApp {
    appName = "Example App"
    developmentRegion = "en"
    copyright = "© 2023–${java.time.Year.now().value} pagina GmbH, Tübingen, Germany"
    icon = "${projectDir}/src/build/icon.icns"
}
```

```sh
./gradlew macApp # unsigned
# or
./gradlew signedAndNotarizedMacApp
```


## Task Documentation

This plugin consists of task steps that are executed in order.


### javaApplicationStub

```groovy
javaApplicationStub {
    // No manual settings required. These are the defaults:
    source = nativeJavaApplicationStubv1
    // equivalent explicit values:
    // url = "https://github.com/paginagmbh/NativeJavaApplicationStub/releases/download/v1.0/NativeJavaApplicationStub"
    // unzip = false
    // executableName = "NativeJavaApplicationStub"
    // outdir = "${buildDir}/javaApplicationStub"
}
```


#### Arguments

**source** (Optional, default: `nativeJavaApplicationStubv1`)

Built-in source presets:

* `universalJavaApplicationStubShell`
* `universalJavaApplicationStubProcompiled`
* `nativeJavaApplicationStubv1`

Repositories:

* Universal Java Application Stub: <https://github.com/tofi86/universalJavaApplicationStub>
* Native Java Application Stub: <https://github.com/paginagmbh/NativeJavaApplicationStub>

You can assign them either in `macApp` or in `javaApplicationStub`.

```groovy
macApp {
    javaApplicationStub = universalJavaApplicationStubShell
}

// or
javaApplicationStub {
    source = universalJavaApplicationStubProcompiled
}
```

**url** (Optional, default: resolved from `source`)

The URL from which the program is downloaded.

**unzip** (Optional, default: resolved from `source`)

If `true`, the plugin expects an archive download and extracts it.
If `false`, the plugin expects a direct executable download.

**executableName** (Optional, default: resolved from `source`)

The file name expected after download/unpack and copied into the app bundle.

**outdir** (Optional, default: `"${buildDir}/javaApplicationStub"`)


#### API

The output file is described by the task property *targetFile*.
This is usually either *outdir/<executableName>* or
*outdir/<downloadFileNameWithoutExtension>/<executableName>*.
The first is used for direct downloads, the second for zipped sources.


### macApp

```groovy
macApp {
    appName = "My App"
    developmentRegion = "en"
    copyright = "© 2023–${java.time.Year.now().value} pagina GmbH, Tuebingen, Germany"
    icon = "${projectDir}/src/build/icon.icns"
}
```

Creates an executable but unsigned macOS app.
It uses the *javaApplicationStub* task.
None of the settings is required, but it is recommended to at least use the values from the code example above.
You can then simply run

```bash
./gradlew macApp
```

Creates the unsigned *.app* bundle.
The archive is produced by the separate `macAppArchive` task, which `macApp` finalizes automatically.


#### Arguments

**appName** (Optional, default: `${project.name}`)

Name of the *.app* file without the extension.
On macOS, this should also be the display name.


**outdir** (Optional, default: `"${buildDir}/unsignedMacApp"`)


**pkgInfoSignature** (Optional, default: calculated automatically)

Package signature for *PkgInfo*.
[Docs](https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html).

Example: `"APPLpaco"`


**developmentRegion** (Optional, default: `null`)

Default language region of the program.
For pagina, this should always be `"de"`.

**Examples:** `"de"`, `"en"`, ...


**bundleIdentifier** (Optional, default: calculated automatically from the main class)

App ID in reverse-URL format.
Since Java uses a similar notation for classes, a plausible default can be inferred.
If the main class is for example *de.paginagmbh.parsx.console.Console*, then the bundle identifier
`"de.paginagmbh.parsx.console"` is calculated (the same string without the class).

Example: `"de.paginagmbh.parsx.console"`


**copyright** (Optional, default: `null`)

Copyright string.

Example: `"© 2023–${java.time.Year.now().value} pagina GmbH, Tuebingen, Germany"`


**icon** (Optional, default: `null`)

Path to an *.icns* file.

Example: `"${projectDir}/src/build/icon.icns"`


**additionalResources** (Optional, default: `[]`)

List of additional files and folders to copy into the app's *Resources* directory.
This is useful for providing extra files such as configuration files, libraries, or other resources needed by the app.
Starting with macOS 26 (Tahoe), this especially applies to `Assets.car` files for icons and other resources.

Example: `["${projectDir}/src/build/Assets.car"]`


**viewableDocumentTypes** (Optional, default: `null`)

UTIs for document types that can be opened in this app.

Example: `["public.plain-text", "public.log"]`


**javaProperties** (Optional, default: derived from `application.applicationDefaultJvmArgs`)

Java `-D...` system properties written into the JavaX `Properties` dictionary in `Info.plist`.
By default, all `-D...` entries from `application.applicationDefaultJvmArgs` are used.

Example: `["-Dfile.encoding=UTF-8", "-Dmy.flag=true"]`


**vmOptions** (Optional, default: derived from `application.applicationDefaultJvmArgs`)

Additional Java VM options written into JavaX `VMOptions` in `Info.plist`.
By default, all non-`-D...` entries from `application.applicationDefaultJvmArgs` are used.

Example: `["-Xmx1g", "-XX:+UseG1GC"]`


**startOnMainThread** (Optional, default: `null`)

If set to `true`, writes JavaX `StartOnMainThread` to enable launch on the macOS main thread
(mapped by the stub to `-XstartOnFirstThread`).

Example: `true`


**mainArguments** (Optional, default: `null`)

Arguments written into JavaX `Arguments` and passed to the application's main class at startup.

Example: `["--profile", "prod"]`


**splashFile** (Optional, default: `null`)

Path to an image file written to JavaX `SplashFile`.
If set, the file is copied automatically into the app `Contents/Resources` directory,
and the plist stores the bundled file name.
If a sibling file with the same name plus `@2x` before the extension exists,
it is copied as well (for example `splash.png` -> `splash@2x.png`).

Example: `"${projectDir}/src/build/splash.png"`


#### JavaX Example (JVM args + startup flags)

```groovy
application {
    mainClass = 'com.company.package.Class'

    // These defaults are forwarded into JavaX:
    // -D... -> JavaX/Properties
    // others -> JavaX/VMOptions
    applicationDefaultJvmArgs = [
        '-Dfile.encoding=UTF-8',
        '-Dmy.feature=true',
        '-Xmx1g'
    ]
}

macApp {
    appName = 'Example App'

    // Optional JavaX additions
    startOnMainThread = true
    mainArguments = ['--profile', 'prod']
    splashFile = "${projectDir}/src/build/splash.png"
}
```


#### API

The app bundle is described by the task property *macApp* and lives at *outdir/appName.app*.
For compatibility, the task still exposes *macAppTarGz* as the conventional archive path
*outdir/appName.tar.gz*, but archive generation is handled by `macAppArchive`.


### macAppArchive

```bash
./gradlew macAppArchive
```

Creates a *.tar.gz* archive from the unsigned *.app* bundle so it can be transferred as a single file while preserving
internal file permissions.

This task depends on `macApp`, so running it directly will first generate the app bundle if needed.


### signedAndNotarizedMacApp

```bash
./gradlew signedAndNotarizedMacApp
```

Signs and notarizes the macOS app.
Builds on top of the *macApp* task.
The archive is produced by the separate `signedAndNotarizedMacAppArchive` task,
which `signedAndNotarizedMacApp` finalizes automatically.
All required arguments can also be set via environment variables to make CI integration easier.


#### Arguments

**keychainName** (Optional, default: `"TemporaryPaginaSigningKeychain.keychain"`)

Name of the temporary keychain used for signing.
It is usually deleted after use.
Set this variable to `"login.keychain"` to use the system keychain.
That keychain is not deleted and may already contain the certificate.


**keychainPassword** (Optional, default: `"TotallySecretPassword"`)

Password for the keychain.
For the login keychain, this should be the computer password.


**certificate** (Optional, default: env *$APPLE_SIGNING_P12* or env *$APPLE_SIGNING_P12_BASE64*)

Path to the certificate used for signing and notarization.
If the keychain is not the login keychain, the certificate is removed from the keychain afterwards.

If not provided as a Groovy argument, it is read from the environment variable *$APPLE_SIGNING_P12* by default.
Alternatively, you can provide the base64-encoded certificate content in *$APPLE_SIGNING_P12_BASE64*.
It will then be written as a file into the build directory.

This argument is optional. If omitted, no certificate is imported.


**certificatePassword** (Optional, default: env *$APPLE_SIGNING_PASSWORD* or env *$APPLE_SIGNING_PASSWORD_BASE64*)

Password for importing the P12 certificate.
If not provided as a Groovy argument, it can be read either as plain text from
*$APPLE_SIGNING_PASSWORD* or base64-encoded from *$APPLE_SIGNING_PASSWORD_BASE64*.


**appleSignID** (REQUIRED, default: env *$APPLE_SIGN_ID*)

Signing ID used for code signing.
If not provided as a Groovy argument, it is read from the *$APPLE_SIGN_ID* environment variable.

Example: *"Developer ID Application: The Company (ASDF213FDSA)"*


**appleIDUser** (REQUIRED, default: env *$APPLE_ID_USER*)

Apple ID used for signing.
Usually in email address format.
If not provided as a Groovy argument, it is read from the *$APPLE_ID_USER* environment variable.


**appleIDPassword** (REQUIRED, default: env *$APPLE_ID_PASSWORD*)

Password for the Apple ID.
If not provided as a Groovy argument, it is read from the *$APPLE_ID_PASSWORD* environment variable.


**appleIDTeamID** (REQUIRED, default: env *$APPLE_ID_TEAM_ID*)

Team ID for the Apple ID.
This should be the part in parentheses from *appleSignID*.
If not provided as a Groovy argument, it is read from the *$APPLE_ID_TEAM_ID* environment variable.

Example: *"ASDF213FDSA"*


**dmgIcon** (Optional, default: the same file as the macOS app icon)

Icon used for the *.dmg* file.


**outdir** (Optional, default: `"${buildDir}/signedMacApp"`)


#### API

The signed app and dmg are described by the task properties *signedAndNotarizedMacApp* and *notarizedDMG*.
These are usually *outdir/appName.app* and *outdir/appName.dmg*.
For compatibility, the task still exposes *signedAndNotarizedMacAppTarGz* as the conventional archive path
*outdir/appName.tar.gz*, but archive generation is handled by `signedAndNotarizedMacAppArchive`.


### signedAndNotarizedMacAppArchive

```bash
./gradlew signedAndNotarizedMacAppArchive
```

Creates a *.tar.gz* archive from the signed and notarized *.app* bundle.
This task depends on `signedAndNotarizedMacApp`, so running it directly will perform signing/notarization first.


## errSecInternalComponent

This is an error that sometimes occurs.
So far, this has only happened after repeated testing over SSH on a Mac.
This error can only be fixed by restarting.


## .tgz and .tar.gz

The generated archives are *.tgz* files, not *.tar.gz*.
These extensions are equivalent.
However, Windows and Artifactory have issues with multi-part extensions.
For example, Artifactory simply removes the *.tar* component.
To keep naming unambiguous, *.tgz* is used, even though some variable and function names in code still refer to "TarGz".


## Reusing Variables - displayname

You may want to use a name other than the project name (the folder name containing the source code).
To do this, store the name in its own variable.
In Groovy, you can do it like this:

```groovy

ext {
    displayname = "My App"
}

macApp {
    appName = "${displayname}"
    ...
}
```


## Read Project Name and Version Number in Java

In Gradle:

```groovy
version = "1.0.0"

ext {
    displayname = "My App"
}

// Create a properties file with version and program name
task createProperties(dependsOn: processResources) {
    doLast {
        new File("${buildDir}/resources/main/meta.properties").withWriter { w ->
            Properties p = new Properties()
            p['version'] = project.version.toString()
            p['name'] = displayname
            p.store w, null
        }
    }
}
```

In Java:

```Java
import java.util.ResourceBundle;

// ===============================================================================================
// Property Interface
// ===============================================================================================

/** The metadata properties file generated in gradle. */
private static final ResourceBundle meta = ResourceBundle.getBundle("meta");

/** The app’s build version. */
public static final String version = meta.getString("version");

/** The app’s display name. */
public static final String name = meta.getString("name");
```


## Changelog

### 2.0.0

* Added dedicated archive tasks: `macAppArchive` and `signedAndNotarizedMacAppArchive`.
  The main tasks `macApp` and `signedAndNotarizedMacApp` now finalize the archive tasks, so the archive is generated automatically after the app bundle.
* `javaApplicationStub` task is now cacheable to improve repeated build performance.
* Use of [NativeJavaApplicationStub](https://github.com/paginagmbh/NativeJavaApplicationStub) as default source.
* Added JavaX-related `macApp` properties: `javaProperties`, `vmOptions`, `startOnMainThread`, `mainArguments`, and `splashFile`. `vmOptions`/`javaProperties` now default from `application.applicationDefaultJvmArgs`.


### 1.x.x

* Initial version.


## (internal only) Publish New Versions

A merge into *main* automatically publishes the next version to Artifactory.
A push to *development* instead publishes a *-SNAPSHOT* version.
For this the pagina artifactory access needs to be setup locally.
This repository contains a git submodule to configure this.
It is not needed when not deploying snapshots.
You can also publish to Artifactory locally:

```sh
./gradlew publish
```

For local testing, use:

```sh
./gradlew publishToMavenLocal
```
