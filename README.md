# Mac App Gradle Plugin

Ein Gradle-Plugin, das Mac Apps erzeugt und signiert.
Muss auf einem Mac ausgeführt werden[^unix].

[^unix] Eine unsignierte Mac App kann auf auf Linux/WSL erzeugt werden.

## Verwendung

### Minimalbeispiel

```{code-block} groovy
:caption: In build.gradle

plugins {
    id 'java'
    id 'application'

    id 'de.paginagmbh.commons.mac-app-gradle' version '1.0.0-SNAPSHOT'
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

**compiled** (Optional)
:   Wenn *compiled == true*, dann wird die mit *csh* kompilierte Version des universalJavaApplicationStub heruntergeladen.
    Sonst wird die reine Shell-Version verwendet.
    Die kompilierte Version ist mit der Intel Toolchain kompiliert und somit nur auf Intel-Macs und auf AppleSilicon-Macs mit Rosetta ausführbar.
    Dies ist unschön für die Anwendenden, die Shell-Version ist somit flexibler.

    Wenn *compiled == true*, dann erwartet das Plugin einen zip-Download, den es entpackt, bei *false* einen direkten Download der ausführbaren Datei.
    Bei einem eigenen Downloadlink sollte diese Eigenschaft entsprechend gesetzt werden.

    Default: `false`

**downloadURL** (Optional)
:   Die URL, von der das Programm heruntergeladen wird.

    Default bei *compiled == true*: "https://raw.githubusercontent.com/tofi86/universalJavaApplicationStub/master/src/universalJavaApplicationStub"`

    Default bei *compiled == false*: `"https://github.com/tofi86/universalJavaApplicationStub/releases/download/v3.3.0/universalJavaApplicationStub-v3.3.0-binary-macos-10.15.zip"`

**outdir** (Optional)
:   Default: `"${buildDir}/universalJavaApplicationStub"`


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

#### Argumente

**appName**  (Optional)
:   Der Name der *.app*-Datei ohne die Endung.
    Sollte auf macOS auch der Anzeigename sein.

    Default: `${project.name}`

**outdir**  (Optional)
:   Default: `"${buildDir}/unsignedMacApp"`

**pkgInfoSignature**  (Optional)
:   Paketsignatur für *PkgInfo*.
    [Doku](https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html).

    Default: automatisch berechnet

    Beispiel: `"APPLpaco"`

**developmentRegion**  (Optional)
:   Die Standardsprachregion des Programms.
    Sollte für pagina immer `"de"` sein.

    Default: `null`

    Beispiele: "de"`, `"en"`, …

**bundleIdentifier**  (Optional)
:   ID für die App im reverse-URL format.
    Da Java eine ähnliche Notation für Klassen verwendet kann eine plausible Standardannahme getroffen werden.
    Ist die Hauptklasse zum Beipsiel *de.paginagmbh.parsx.console.Console*, so wird der BundleIdentifier `"de.paginagmbh.parsx.console"` (das selbe ohne die Klasse) berechnet.

    Default: automatisch aus Hauptklasse berechnet

    Beispiel: `"de.paginagmbh.parsx.console"`

**copyright**  (Optional)
:   Copyright-String.

    Default: `null`

    Beispiel: `"© 2023–${java.time.Year.now().value} pagina GmbH, Tübingen, Germany"`

**icon**  (Optional)
:   Pfad zu einer *.icns*-Datei.

    Beispiel: `"${projectDir}/src/build/icon.icns"`

**viewableDocumentTypes**  (Optional)
:   UTIs verschiedener Dokumententypen, die in dieser app geöffnet werden können.

    Default: `null`

    Beispiel: `["public.plain-text", "public.log"]`

#### API-Doku

Die Output-Datei wird durch die Task-Eigenschaft *macApp* beschrieben.
Dieser ist in der Regel *outdir/appName.app*.

### signedAndNotarizedMacApp

:::{caution}
TODO ------------------------------------------------
:::

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

## Neue Versionen veröffentlichen

Zurzeit hat das Plugin noch keine automatische Anpassung der Versionsnummern.
Dies muss also in *build.gradle* manuell vollzogen werden.

Zum hochladen auf Artifactory reicht

```sh
./gradlew publish
```

Zum lokalen Testen verwendet man

```sh
./gradlew publishToMavenLocal
```

## Todo

- [ ] Versionsnummern von selbst inkrementieren