package gmbh.pagina.tools.gradle.mac_app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.gradle.api.GradleException;

/** Shared HTTP-only download helper used by plugin tasks. */
final class DownloadUtils {

  /** Shared HTTP client with conservative timeouts and redirect handling. */
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(15))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private DownloadUtils() {}

  /**
   * Download an HTTP(S) URL to a file using sane defaults.
   *
   * @param url The source URL (HTTP/HTTPS).
   * @param destination The file path to write.
   * @param description Human-readable description used in error messages.
   */
  static void downloadHttpToFile(String url, Path destination, String description) {
    if (url == null || url.isBlank()) {
      throw new GradleException("Could not download " + description + ": URL is empty");
    }

    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      throw new GradleException("Could not download " + description + ": invalid URL " + url, e);
    }

    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw new GradleException(
          "Could not download "
              + description
              + ": unsupported URL scheme '"
              + scheme
              + "' in "
              + url);
    }

    Path parent = destination.getParent();
    try {
      if (parent != null) Files.createDirectories(parent);
    } catch (IOException e) {
      throw new GradleException(
          "Could not download " + description + ": cannot create directory " + parent, e);
    }

    HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(60)).build();

    try {
      HttpResponse<Path> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(destination));
      int statusCode = response.statusCode();
      if (statusCode < 200 || statusCode >= 300) {
        Files.deleteIfExists(destination);
        throw new GradleException(
            "Could not download " + description + " from " + url + " (HTTP " + statusCode + ")");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GradleException("Download interrupted for " + description + " from " + url, e);
    } catch (IOException e) {
      try {
        Files.deleteIfExists(destination);
      } catch (IOException ignored) {
        // Best effort cleanup of partial download.
      }
      throw new GradleException("Could not download " + description + " from " + url, e);
    }
  }
}
