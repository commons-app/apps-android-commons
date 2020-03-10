package fr.free.nrw.commons.depictions.SubClass.models;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for parsing SparqlQueryResponse
 */
public class Binding {

    @SerializedName("subclass")
    @Expose
    private Subclass subclass;
    @SerializedName("subclassLabel")
    @Expose
    private SubclassLabel subclassLabel;

    /**
     * No args constructor for use in serialization
     *
     */
    public Binding() {
    }

    /**
     *
     * @param subclassLabel
     * @param subclass
     */
    public Binding(Subclass subclass, SubclassLabel subclassLabel) {
        super();
        this.subclass = subclass;
        this.subclassLabel = subclassLabel;
    }

    public Subclass getSubclass() {
        return subclass;
    }

    public void setSubclass(Subclass subclass) {
        this.subclass = subclass;
    }

    public SubclassLabel getSubclassLabel() {
        return subclassLabel;
    }

    public void setSubclassLabel(SubclassLabel subclassLabel) {
        this.subclassLabel = subclassLabel;
    }

}
