package fr.free.nrw.commons.depictions.models;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for list of depicted images obtained by fetching using depiction entity
 */
public class DepictionResponse {

    @SerializedName("batchcomplete")
    @Expose
    private String batchcomplete;
    @SerializedName("continue")
    @Expose
    private Continue _continue;
    @SerializedName("query")
    @Expose
    private Query query;

    /**
     * No args constructor for use in serialization
     *
     */
    public DepictionResponse() {
    }

    /**
     *
     * @param query
     * @param batchcomplete
     * @param _continue
     */
    public DepictionResponse(String batchcomplete, Continue _continue, Query query) {
        super();
        this.batchcomplete = batchcomplete;
        this._continue = _continue;
        this.query = query;
    }

    /**
     * returns batchcomplete string from DepictionResponse object
     */
    public String getBatchcomplete() {
        return batchcomplete;
    }

    public void setBatchcomplete(String batchcomplete) {
        this.batchcomplete = batchcomplete;
    }

    /**
     * returns continue object from DepictionResponse object
     */
    public Continue getContinue() {
        return _continue;
    }

    public void setContinue(Continue _continue) {
        this._continue = _continue;
    }

    /**
     * returns query object from DepictionResponse object
     */
    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

}
