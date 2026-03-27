# Mac App Gradle Plugin

A Gradle plugin that creates and signs Mac apps.
Must be run on a Mac.
An unsigned Mac app can also be built on Linux/WSL.

This task can be run locally but it is primarily intended for CI use.
For this it will install some requirements as follows:

* It requires [electron-installer-dmg](https://github.com/electron-userland/electron-installer-dmg) for creating a DMG file which can be notarized.
* If it does not exist it will install it using npm.
* If npm is not installed, it will install it using brew.
* If brew is not installed, an error will be thrown.
* It downloads the *universalJavaApplicationStub* from GitHub using curl.
    If curl is not available, an error is thrown.


## Usage

### Minimal Example

In build.gradle:

```groovy
plugins {
    id 'java'
    id 'application'

    id 'gmbh.pagina.tools.gradle.mac_app' version '1.3.0'
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

This plugin consists of three sub-tasks that are executed in order.


### universalJavaApplicationStub

```groovy
universalJavaApplicationStub {
    // No manual settings required. These are the defaults:
    compiled = false
    downloadURL = "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub"
    outdir = "${buildDir}/universalJavaApplicationStub"
}
```


#### Arguments

**compiled** (Optional, default: `false`)

If *compiled == true*, the version of `universalJavaApplicationStub` compiled with *csh* is downloaded.
Otherwise, the plain shell version is used.
The compiled version is built with the Intel toolchain and therefore only runs on Intel Macs and Apple Silicon Macs with
Rosetta.
This is inconvenient for end users, so the shell version is more flexible.

If *compiled == true*, the plugin expects a zip download that it then extracts; if *false*, it expects a direct download of the executable file.
If you use a custom download link, this property should be set accordingly.


**downloadURL** (Optional, default: see below)

The URL from which the program is downloaded.

Default for *compiled == true*: "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub"`

Default for *compiled == false*: `"https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip"`


**outdir** (Optional, default: `"${buildDir}/universalJavaApplicationStub"`)


#### API

The output file is described by the task property *targetFile*.
This is usually either *outdir/universalJavaApplicationStub* or
*outdir/universalJavaApplicationStub/universalJavaApplicationStub*.
Here, the first is the *targetFile* for the shell script, and the second is the *targetFile* for the compiled download.


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
It uses the *universalJavaApplicationStub* task.
None of the settings is required, but it is recommended to at least use the values from the code example above.
You can then simply run

```bash
./gradlew macApp
```

At the end, a *.tar.gz* file for the app is also provided so it can be transferred as a single file while preserving
internal file permissions.


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


#### API

The output files are described by the task properties *macApp* and *macAppTarGz*.
These are *outdir/appName.app* and *outdir/appName.tar.gz*.


### signedAndNotarizedMacApp

```bash
./gradlew signedAndNotarizedMacApp
```

Signs and notarizes the macOS app.
Builds on top of the *macApp* task.
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

The output files are described by the task properties *signedAndNotarizedMacApp*, *signedAndNotarizedMacAppTarGz*, and
*notarizedDMG*.
This is usually *outdir/appName.app*, *outdir/appName.tar.gz*, and *outdir/appName.dmg*.


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


## (internal only) Publish New Versions

A merge into *main* automatically publishes the next version to Artifactory.
A push to *development* instead publishes a *-SNAPSHOT* version.
For this the pagian artifactory access needs to be setup locally.
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
