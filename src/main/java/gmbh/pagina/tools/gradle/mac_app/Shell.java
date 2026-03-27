package gmbh.pagina.tools.gradle.mac_app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** An object to interact with the system shell. */
public class Shell {

  /** The shell output, stdout and stderr. */
  public final String out;

  /** The exit code of the shell call. */
  public final int exitcode;

  /** Create a shell object that is used as output. */
  private Shell(String out, int exitcode) {
    this.out = out;
    this.exitcode = exitcode;
  }

  /** Has the shell terminated correctly with a normal exit code. */
  public boolean isOk() {
    return exitcode == 0;
  }

  /** Return the output lines of the message */
  public String[] getLines() {
    return this.out.strip().split(System.lineSeparator());
  }

  // ===============================================================================================
  // Static Functions
  // ===============================================================================================

  /** The logger to use for printing to the console. */
  private static final Logger logger = Logging.getLogger(Shell.class);

  /** Test whether a shell command is successful. Long output is truncated. */
  protected static boolean test(String... command) {
    Shell shell = sh(true, true, false, command);
    // If the output is more than two lines truncate it. Otherwise print it in full.
    if (shell.getLines().length > 2) logger.info("(multiple lines truncated)");
    else logger.info(shell.out.strip());
    // Return whether the exit code is good.
    return shell.isOk();
  }

  /** Execute a shell command but don’t throw an error when a bad exit code is encountered. */
  protected static Shell failOk(String... command) {
    return sh(true, true, true, command);
  }

  /** Execute a shell command. */
  protected static Shell sh(String... command) {
    return sh(false, true, true, command);
  }

  /**
   * Execute a shell command.
   *
   * @param errorOk If {@link false}, an error will be thrown on non-zero exit codes. If it has been
   *     set to {@link true}, this will only result in printing the exit code.
   * @param printPrompt Print the prompt that has been passed in into the log at info level.
   * @param printOutput Print the shell output into the log at info level.
   * @param command The command to execute.
   * @return A {@link Shell} object with the shell output and exit code.
   */
  private static Shell sh(
      boolean errorOk, boolean printPrompt, boolean printOutput, String... command) {
    // Set up the process builder
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(command);

    // Print the prompt, neatly formatted
    if (printPrompt)
      logger.info("\033[32;1m$\033[0;1m " + String.join(" ", builder.command()) + "\033[0m");

    // Also capture output
    builder.redirectErrorStream(true);

    // Prepare variables for the main execution.
    String messageBuffer = "";
    int exitCode;
    try {
      // Start the process
      Process process = builder.start();

      // Capture the output of the process line by line.
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        if (printOutput) logger.info(line);
        // Accumulate the output into the messageBuffer variable
        messageBuffer += System.lineSeparator() + line;
      }
      // Wait for the process to finish
      exitCode = process.waitFor();
      if (exitCode == 0 && messageBuffer.isEmpty()) {
        // No output here
      } else if (errorOk) {
        // Only print an exit code if it was not zero
        if (exitCode != 0) logger.info("(Exit Code: " + exitCode + ")");
      } else if (exitCode == 0) {
        // // Don't print zero exit codes
        // logger.info("Exit Code: " + exitCode);
      } else {
        // Non-zero exit code encountered. Print it in red.
        logger.error("\033[31;1mExit Code: " + exitCode + "\033[0m");

        // This is a specific error encountered when using SSH that will bring the system into a non
        // usable state. I will help out the user with a specific error message here.
        String errSecMessage =
            (messageBuffer.contains("errSecInternalComponent"))
                ? "\n"
                    + "You are might be using SSH. "
                    + "In this case you need to configure the login keychain. "
                    + "In your case a reboot will probably be enough. "
                    + "Read the README of the mac app builder repo!"
                : "";
        // Throw an error here that stops program execution. If a command is not successful we don’t
        // want the pipline to continue as if everything is great. Markup the error message with the
        // errSecInternalComponent error message generated above and the exit code.
        throw new GradleException(
            "An error occured executing '"
                + String.join(" ", builder.command())
                + "':\n"
                + messageBuffer
                + errSecMessage
                + "\n(Exit Code "
                + exitCode
                + ")");
      }
    } catch (InterruptedException e) {
      throw new GradleScriptException("Interrupt occured", e);
    } catch (IOException e) {
      throw new GradleScriptException("IOException", e);
    }
    // Return the printed message and the exit code.
    return new Shell(messageBuffer, exitCode);
  }
}
