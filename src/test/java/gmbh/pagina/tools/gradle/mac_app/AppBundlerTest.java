package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class AppBundlerTest {

  @Test
  void conventions_computePkgSignatureAndBundleIdentifier() {
    Project project = ProjectBuilder.builder().withName("demo-project").build();
    AppBundler task = project.getTasks().create("macApp", AppBundler.class);

    task.setAppName("Demo Application");
    task.setMainClassName("com.example.app.Main");

    assertEquals(4, task.getPkgInfoSignature().length());
    assertEquals("com.example.app", task.getBundleIdentifier());
  }

  @Test
  void taskAction_createsBundleLayoutAndInfoPlist() throws Exception {
    Project project = ProjectBuilder.builder().withName("demo-project").build();
    AppBundler task = project.getTasks().create("macApp", AppBundler.class);

    Path temp = Files.createTempDirectory("app-bundler-");
    File outDir = temp.resolve("out").toFile();

    Path stub = temp.resolve("universalJavaApplicationStub");
    Files.writeString(stub, "#!/bin/sh\necho stub\n", StandardCharsets.UTF_8);

    Path mainJar = temp.resolve("main.jar");
    Files.writeString(mainJar, "jar", StandardCharsets.UTF_8);

    Path depJar = temp.resolve("dep.jar");
    Files.writeString(depJar, "dep", StandardCharsets.UTF_8);

    Path icon = temp.resolve("icon.icns");
    Files.writeString(icon, "icon", StandardCharsets.UTF_8);

    Path splash = temp.resolve("splash.png");
    Files.writeString(splash, "splash", StandardCharsets.UTF_8);
    Path splash2x = temp.resolve("splash@2x.png");
    Files.writeString(splash2x, "splash2x", StandardCharsets.UTF_8);

    Path extra = temp.resolve("extra.txt");
    Files.writeString(extra, "extra", StandardCharsets.UTF_8);

    task.setOutdir(outDir);
    task.setAppName("Demo");
    task.setMainClassName("com.example.Main");
    task.setProjectName("demo-project");
    task.setProjectVersion("1.2.3");
    task.setTargetJavaVersion(21);
    task.setIcon(icon.toString());
    task.setAdditionalResources(List.of(extra.toString()));
    task.setSplashFile(splash.toString());
    task.setVmOptions(List.of("-Xmx512m"));
    task.setJavaProperties(List.of("-Dfoo=bar"));
    task.setMainArguments(List.of("arg1"));

    task.getJavaApplicationStubFiles().from(stub.toFile());
    task.getMainJarFiles().from(mainJar.toFile());
    task.getRuntimeClasspath().from(depJar.toFile());

    task.taskAction();

    File app = task.getMacApp();
    assertTrue(app.isDirectory());

    File contents = new File(app, "Contents");
    File resources = new File(contents, "Resources");
    File javaDir = new File(resources, "Java");
    assertTrue(new File(contents, "PkgInfo").isFile());
    assertTrue(new File(contents, "MacOS/universalJavaApplicationStub").isFile());
    assertTrue(new File(resources, "icon.icns").isFile());
    assertTrue(new File(resources, "extra.txt").isFile());
    assertTrue(new File(resources, "splash.png").isFile());
    assertTrue(new File(resources, "splash@2x.png").isFile());
    assertTrue(new File(javaDir, "demo-project.jar").isFile());
    assertTrue(new File(javaDir, "dep.jar").isFile());

    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new File(contents, "Info.plist"));
    Element root = rootDict(doc);
    assertEquals("Demo", valueForKey(root, "CFBundleName").getTextContent());
    assertEquals("com.example", valueForKey(root, "CFBundleIdentifier").getTextContent());

    Element javaX = valueForKey(root, "JavaX");
    assertNotNull(javaX);
    assertEquals("com.example.Main", valueForKey(javaX, "MainClass").getTextContent());
  }

  private static Element rootDict(Document doc) {
    Node child = doc.getDocumentElement().getFirstChild();
    while (child != null && !(child instanceof Element)) {
      child = child.getNextSibling();
    }
    return (Element) child;
  }

  private static Element valueForKey(Element dict, String key) {
    NodeList children = dict.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Element
          && "key".equals(((Element) child).getTagName())
          && key.equals(child.getTextContent().trim())) {
        Node sibling = child.getNextSibling();
        while (sibling != null && !(sibling instanceof Element)) {
          sibling = sibling.getNextSibling();
        }
        return sibling instanceof Element ? (Element) sibling : null;
      }
    }
    return null;
  }
}
