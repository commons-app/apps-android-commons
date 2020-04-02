package fr.free.nrw.commons.depictions.SubClass.models;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for parsing SparqlQueryResponse
 */
public class SubclassDescription {

  @SerializedName("type")
  @Expose
  private String type;
  @SerializedName("value")
  @Expose
  private String value;
  @SerializedName("xml:lang")
  @Expose
  private String xmlLang;

  /**
   * No args constructor for use in serialization
   *
   */
  public SubclassDescription() {
  }

  /**
   *
   * @param value
   * @param xmlLang
   * @param type
   */
  public SubclassDescription(String type, String value, String xmlLang) {
    super();
    this.type = type;
    this.value = value;
    this.xmlLang = xmlLang;
  }

  public String getType() {
    return type;
  }

  /**
   * returns type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * gets value of the depiction
   */
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  /**
   * get language in which the depiction was requested
   */
  public String getXmlLang() {
    return xmlLang;
  }
}