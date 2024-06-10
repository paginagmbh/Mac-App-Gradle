# Mac App Gradle Plugin

Ein Gradle-Plugin, das Mac Apps erzeugt und signiert.
Muss auf einem Mac ausgeführt werden[^unix].

[^unix]: Eine unsignierte Mac App kann auch auf Linux/WSL erzeugt werden.


[[_TOC_]]


## Verwendung


### Minimalbeispiel

```{code-block} groovy
:caption: In build.gradle

plugins {
    id 'java'
    id 'application'

    id 'de.paginagmbh.commons.mac-app-gradle' version '1.2.8-SNAPSHOT'
}

version = "1.0.0"

application {
    mainClass = 'de.paginagmbh.app.Class'
}

macApp {
    appName = "Example App"
    developmentRegion = "de"
    copyright = "© 2023–${java.time.Year.now().value} pagina GmbH, Tübingen, Germany"
    icon = "${projectDir}/src/build/icon.icns"
}
```

```{code-block} sh
:caption: Erzeugen

./gradlew macApp # unsigned
# oder
./gradlew signedAndNotarizedMacApp
```


## Taskdokumentation

Dieses Plugin besteht aus drei sub-Tasks, die in Reihenfolge abgehandelt werden.


### universalJavaApplicationStub

```groovy
universalJavaApplicationStub {
    // Keine manuellen Angaben nötig. Das sind die Standardwerte:
	compiled = false
	downloadURL = "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub"
	outdir = "${buildDir}/universalJavaApplicationStub"
}
```


#### Argumente

**compiled** (Optional, Default: `false`)
:   Wenn *compiled == true*, dann wird die mit *csh* kompilierte Version des universalJavaApplicationStub heruntergeladen.
    Sonst wird die reine Shell-Version verwendet.
    Die kompilierte Version ist mit der Intel Toolchain kompiliert und somit nur auf Intel-Macs und auf AppleSilicon-Macs mit Rosetta ausführbar.
    Dies ist unschön für die Anwendenden, die Shell-Version ist somit flexibler.

    Wenn *compiled == true*, dann erwartet das Plugin einen zip-Download, den es entpackt, bei *false* einen direkten Download der ausführbaren Datei.
    Bei einem eigenen Downloadlink sollte diese Eigenschaft entsprechend gesetzt werden.
    

**downloadURL** (Optional, Default: siehe unten)
:   Die URL, von der das Programm heruntergeladen wird.

    Default bei *compiled == true*: "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub"`

    Default bei *compiled == false*: `"https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip"`

**outdir** (Optional, Default: `"${buildDir}/universalJavaApplicationStub"`)


#### API-Doku

Die Output-Datei wird durch die Task-Eigenschaft *targetFile* beschrieben.
Dieser ist in der Regel entweder *outdir/universalJavaApplicationStub* oder *outdir/universalJavaApplicationStub/universalJavaApplicationStub*.
Hier ist ersteres das *targetFile* des Shell-Skripts und letzteres das des kompilierten Downloads.


### macApp

```groovy
macApp {
    appName = "Meine App"
    developmentRegion = "de"
    copyright = "© 2023–${java.time.Year.now().value} pagina GmbH, Tübingen, Germany"
    icon = "${projectDir}/src/build/icon.icns"
}
```

Erzeugt eine ausführbare aber unsignierte macOS-App.
Diese verwendet den  *universalJavaApplicationStub*-Task.
Keine der Angaben ist notwendig, aber es empfiehlt sich zumindest die aus dem Codebeispiel oben zu verwenden.
Es wird dann einfach ausgeführt mit

```bash
./gradlew macApp
```

Am Ende wird auch eine *.tar.gz*-Datei für die App bereitgestellt, sodass sie als einzelne Datei übertragen werden kann, aber Dateiberechtigungen intern beibehält. 


#### Argumente

**appName** (Optional, Default: `${project.name}`)
:   Der Name der *.app*-Datei ohne die Endung.
    Sollte auf macOS auch der Anzeigename sein.

**outdir** (Optional, Default: `"${buildDir}/unsignedMacApp"`)

**pkgInfoSignature** (Optional, Default: automatisch berechnet)
:   Paketsignatur für *PkgInfo*.
    [Doku](https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html).

    **Beispiel:** `"APPLpaco"`

**developmentRegion** (Optional, Default: `null`)
:   Die Standardsprachregion des Programms.
    Sollte für pagina immer `"de"` sein.

    **Beispiele:** `"de"`, `"en"`, …

**bundleIdentifier** (Optional, Default: automatisch aus Hauptklasse berechnet)
:   ID für die App im reverse-URL format.
    Da Java eine ähnliche Notation für Klassen verwendet kann eine plausible Standardannahme getroffen werden.
    Ist die Hauptklasse zum Beipsiel *de.paginagmbh.parsx.console.Console*, so wird der BundleIdentifier `"de.paginagmbh.parsx.console"` (das selbe ohne die Klasse) berechnet.

    **Beispiel:** `"de.paginagmbh.parsx.console"`

**copyright** (Optional, Default: `null`)
:   Copyright-String.

    **Beispiel:** `"© 2023–${java.time.Year.now().value} pagina GmbH, Tübingen, Germany"`

**icon** (Optional, Default: `null`)
:   Pfad zu einer *.icns*-Datei.

    **Beispiel:** `"${projectDir}/src/build/icon.icns"`

**viewableDocumentTypes** (Optional, Default: `null`)
:   UTIs verschiedener Dokumententypen, die in dieser app geöffnet werden können.

    **Beispiel:** `["public.plain-text", "public.log"]`


#### API-Doku

Die Output-Dateien werden durch die Task-Eigenschaften *macApp* und *macAppTarGz* beschrieben.
Dieser sind  *outdir/appName.app* und *outdir/appName.tar.gz*.


### signedAndNotarizedMacApp

```bash
./gradlew signedAndNotarizedMacApp
```

Signiert und notarisiert die macOS-App.
Baut auf den *macApp*-Task auf.
Alle Argumente, die angegeben werden müssen, können auch direkt über Umgebungsvariablen gesetzt werden, was CI-Integration erleichtern soll.


#### Argumente

**keychainName** (Optional, Default: `"TemporaryPaginaSigningKeychain.keychain"`)
:   Name der temporären Keychain, die zum signieren verwendet wird.
    Diese wird in der Regel nach Verwendung wieder gelöscht.
    Diese Variable sollte auf `"login.keychain"` gesetzt werden, um die Systemkeychain zu verwenden.
    Diese wird dann nicht gelöscht, kann das Zertifikat aber bereits enthalten.

**keychainPassword** (Optional, Default: `"TotallySecretPassword"`)
:   Das Passwort zu der Keychain.
    Für die Login-Keychain sollte das das Computerpasswort sein.

**certificate** (Optional, Default: env *&dollar;APPLE_SIGNING_P12* bzw. env *&dollar;APPLE_SIGNING_P12_BASE64*)
:   Der Pfad zum Zertifikat das für signieren und notarisieren verwendet werden soll.
    Wenn die Keychain nicht die Login-Keychain ist, dann wird es danach wieder aus der Keychain gelöscht.

    Wenn es nicht als Groovy-Argument angegeben wird, wird es standardmäßig aus der Umgebungsvariable *&dollar;APPLE_SIGNING_P12* gelesen.
    Alternativ kann der base64-kodierte Inhalt des Zertifikats in *&dollar;APPLE_SIGNING_P12_BASE64* abgelegt werden.
    Dies wird dann in den build-Ordner als Datei abgelegt.

    Dieses Argument ist optional, wenn es nicht angegeben wird, wird kein Zertifikat importiert.

**certificatePassword** (Optional, Default: env *&dollar;APPLE_SIGNING_PASSWORD* bzw. env *&dollar;APPLE_SIGNING_PASSWORD_BASE64*)
:   Das Passwort für das importieren des P12-Zertifikats.
    Wenn es nicht als Groovy-Argument angegeben wird, kann es entweder als plain-Text aus der *&dollar;APPLE_SIGNING_PASSWORD*-Variable gelesen werden oder base64-kodiert aus *&dollar;APPLE_SIGNING_PASSWORD_BASE64*.

**appleSignID** (REQUIRED, Default: env *&dollar;APPLE_SIGN_ID*)
:   Sign-ID die zum signieren verwendet wird.
    Wenn es nicht als Groovy-Argument angegeben wird, wird es aus der *&dollar;APPLE_SIGN_ID*-Umgebungsvariable gelesen.

    **Beispiel:** *"Developer ID Application: The Company (ASDF213FDSA)"*

**appleIDUser** (REQUIRED, Default: env *&dollar;APPLE_ID_USER*)
:   AppleID die zum signieren verwendet wird.
    In der Regel im Format einer E-Mail–Adresse.
    Wenn es nicht als Groovy-Argument angegeben wird, wird es aus der *&dollar;APPLE_ID_USER*-Umgebungsvariable gelesen.

**appleIDPassword** (REQUIRED, Default: env *&dollar;APPLE_ID_PASSWORD*)
:   Das Passwort zur AppleID.
    Wenn es nicht als Groovy-Argument angegeben wird, wird es aus der *&dollar;APPLE_ID_PASSWORD*-Umgebungsvariable gelesen.

**appleIDTeamID** (REQUIRED, Default: env *&dollar;APPLE_ID_TEAM_ID*)
:   Die TeamID zur AppleID.
    Sollte der Teil in Klammern in der *appleSignID* sein.
    Wenn es nicht als Groovy-Argument angegeben wird, wird es aus der *&dollar;APPLE_ID_TEAM_ID*-Umgebungsvariable gelesen.

    **Beispiel:** *"ASDF213FDSA"*

**dmgIcon** (Optional, Default: Die gleiche Datei wie das App-Icon der macOS-App)
:   Das Icon das für die *.dmg*-Datei verwendet werden soll.

**outdir** (Optional, Default: `"${buildDir}/signedMacApp"`)


#### API-Doku

Die Output-Dateien wird durch die Task-Eigenschaften *signedAndNotarizedMacApp*, *signedAndNotarizedMacAppTarGz* und *notarizedDMG* beschrieben.
Dieser ist in der Regel *outdir/appName.app*, *outdir/appName.tar.gz* und *outdir/appName.dmg*.


### shadowJar

Bei [shadowJar](https://github.com/johnrengelman/shadow) handelt es sich um das einzige externe Plugin, das hier benötigt wird.
Es vereint alle JARs in eine.
Es muss nicht weiter konfiguriert werden, es empfiehlt sich aber im *build.gradle* mit

```{code-block} groovy
:caption: In build.gradle

shadowJar {
    minimize()
}
```

Die Größe der Output-JAR zu (ca.) halbieren.

:::{warning}
Mit bestimmten Paketen kann es mit *minimize* zu Problemen kommen.
Insbesondere eine Kombination aus launch4j und FlatLaf hat mit minimize zu einer kaputten Exe geführt, die aber ohne minimize funktionierte.
:::


## errSecInternalComponent

Dies ist ein Fehler, der manchmal auftritt.
Bisher war das nur, nachdem wiederholt über SSH auf einem Mac getested wurde.
Dieser Fehler kann nur durch einen Neustart behoben werden.


## .tgz und .tar.gz

Die generierten Archive sind *.tgz*-Dateien, nicht *.tar.gz.*
Diese Endungen sind gleichbedeutend.
Jedoch haben Windows und Artifactory Probleme mit Mehr-Komponenten-Endungen.
Artifactory entfernt zum Beispiel die *.tar*-Komponente einfach.
Um eindeutig zu bleiben wird somit *.tgz* verwendet, auch wenn im Code manche Variablen und Funktionen noch von „TarGz“ reden.


## Variablenwiederverwendung – displayname

Womöglich soll ein anderer Name als der Projektname (Name des Ordners in dem der Quellcode liegt) verwendet werden.
Hierfür sollte der Name in einer eigenen Variable abgespeichert werden.
In Groovy macht man das so:

```groovy

ext {
    displayname = "Meine App"
}

macApp {
    appName = "${displayname}"
    ...
}
```


## Projektnamen und Versionsnummer in Java auslesen

```{code-block} groovy
:caption: In build.gradle
version = "1.0.0"

ext {
    displayname = "Meine App"
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

```{code-block} Java
:caption: Im Java-Code

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


## Neue Versionen veröffentlichen

Ein merge auf *main* veröffentlich automatisch die nächste Version auf Artifactory.
Ein push auf *development* veröffentlicht stattdessen eine *-SNAPSHOT*-Version.
Man kann jedoch auch lokal auf Artifactory veröffentlichen

```sh
./gradlew publish
```

Zum lokalen Testen verwendet man

```sh
./gradlew publishToMavenLocal
```
