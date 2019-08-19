package fr.free.nrw.commons.depictions.SubClass.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * SparqlQueryResponse obtained while fetching parent classes and sub classes for depicted items in explore
 */
public class SparqlQueryResponse {

    @SerializedName("head")
    @Expose
    private Head head;
    @SerializedName("results")
    @Expose
    private Results results;

    /**
     * No args constructor for use in serialization
     *
     */
    public SparqlQueryResponse() {
    }

    /**
     *
     * @param results
     * @param head
     */
    public SparqlQueryResponse(Head head, Results results) {
        super();
        this.head = head;
        this.results = results;
    }

    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

}
