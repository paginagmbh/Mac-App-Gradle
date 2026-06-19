package gmbh.pagina.tools.gradle.mac_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShellTest {

  @Test
  void failOk_capturesOutputAndExitCode() {
    Shell shell = Shell.failOk("sh", "-c", "printf 'line1\\nline2\\n'");

    assertTrue(shell.isOk());
    assertEquals(0, shell.exitcode);
    assertEquals(2, shell.getLines().length);
    assertEquals("line1", shell.getLines()[0]);
    assertEquals("line2", shell.getLines()[1]);
  }

  @Test
  void test_returnsFalseForFailingCommand() {
    boolean ok = Shell.test("sh", "-c", "exit 7");
    assertFalse(ok);
  }
}
