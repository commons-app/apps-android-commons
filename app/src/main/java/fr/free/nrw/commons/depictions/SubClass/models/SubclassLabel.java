package fr.free.nrw.commons.depictions.SubClass.models;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SubclassLabel {

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
    public SubclassLabel() {
    }

    /**
     *
     * @param value
     * @param xmlLang
     * @param type
     */
    public SubclassLabel(String type, String value, String xmlLang) {
        super();
        this.type = type;
        this.value = value;
        this.xmlLang = xmlLang;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getXmlLang() {
        return xmlLang;
    }

    public void setXmlLang(String xmlLang) {
        this.xmlLang = xmlLang;
    }

}
