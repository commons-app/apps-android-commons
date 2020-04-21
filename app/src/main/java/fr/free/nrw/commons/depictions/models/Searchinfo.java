package fr.free.nrw.commons.depictions.models;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for object obtained while parsing query object
 */

public class Searchinfo {

    @SerializedName("totalhits")
    @Expose
    private Integer totalhits;

    /**
     * No args constructor for use in serialization
     *
     */
    public Searchinfo() {
    }

    /**
     *
     * @param totalhits
     */
    public Searchinfo(Integer totalhits) {
        super();
        this.totalhits = totalhits;
    }

    /**
     * returns "totalhint" integer in SearchInfo object
     */
    public Integer getTotalhits() {
        return totalhits;
    }

    public void setTotalhits(Integer totalhits) {
        this.totalhits = totalhits;
    }

}
