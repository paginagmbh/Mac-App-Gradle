package gmbh.pagina.tools.gradle.mac_app;

/**
 * A data structure to store information about the document type for use in an Info.plist.
 *
 * <p>// https://stackoverflow.com/a/30980056/3646485
 */
public class DocumentType {

  public final String name;
  public final String role;
  public final String handlerRank;
  public final String[] contentTypes;

  public DocumentType(String name, String role, String handlerRank, String[] contentTypes) {
    this.name = name;
    this.role = role;
    this.handlerRank = handlerRank;
    this.contentTypes = contentTypes;
  }
}
