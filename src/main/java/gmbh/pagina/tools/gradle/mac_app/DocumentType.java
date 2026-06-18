package gmbh.pagina.tools.gradle.mac_app;

/**
 * A data structure to store information about the document type for use in an Info.plist.
 *
 * <p>Reference: <a href="https://stackoverflow.com/a/30980056/3646485">Stack Overflow answer</a>
 */
public class DocumentType {

  /** Display name used in <code>CFBundleTypeName</code>. */
  public final String name;

  /** Declared role used in <code>CFBundleTypeRole</code> (for example Viewer or Editor). */
  public final String role;

  /** Launch Services handler rank used in <code>LSHandlerRank</code>. */
  public final String handlerRank;

  /** Uniform Type Identifiers listed under <code>LSItemContentTypes</code>. */
  public final String[] contentTypes;

  /**
   * Constructs a DocumentType with the provided properties.
   *
   * @param name the name of the document type
   * @param role the role of the document type
   * @param handlerRank the handler rank
   * @param contentTypes the array of content type UTIs
   */
  public DocumentType(String name, String role, String handlerRank, String[] contentTypes) {
    this.name = name;
    this.role = role;
    this.handlerRank = handlerRank;
    this.contentTypes = contentTypes;
  }
}
