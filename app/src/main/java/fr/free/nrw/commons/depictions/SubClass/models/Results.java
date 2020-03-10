package fr.free.nrw.commons.depictions.SubClass.models;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for parsing SparqlQueryResponse
 */
public class Results {

    @SerializedName("bindings")
    @Expose
    private List<Binding> bindings = null;

    /**
     * No args constructor for use in serialization
     */
    public Results() {
    }

    /**
     * @param bindings
     */
    public Results(List<Binding> bindings) {
        super();
        this.bindings = bindings;
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public void setBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }

}
