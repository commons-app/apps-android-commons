package fr.free.nrw.commons.depictions.subClass.models;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for parsing SparqlQueryResponse
 */
public class Head {

    @SerializedName("vars")
    @Expose
    private List<String> vars = null;

    /**
     * No args constructor for use in serialization
     *
     */
    public Head() {
    }

    /**
     *
     * @param vars
     */
    public Head(List<String> vars) {
        super();
        this.vars = vars;
    }

    public List<String> getVars() {
        return vars;
    }

    public void setVars(List<String> vars) {
        this.vars = vars;
    }

}
