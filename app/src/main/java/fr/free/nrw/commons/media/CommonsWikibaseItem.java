package fr.free.nrw.commons.media;

import com.google.gson.annotations.SerializedName;

import fr.free.nrw.commons.media.media.Caption;
import java.util.Map;


/**
 * Represents the Wikibase item associated with a Wikimedia Commons file.
 * For instance the Wikibase item M63996 represents the Commons file "Paul Cézanne - The Pigeon Tower at Bellevue - 1936.19 - Cleveland Museum of Art.jpg"
 */
public class CommonsWikibaseItem {

    @SerializedName("type")
    private String type;
    @SerializedName("id")
    private String id;
    @SerializedName("labels")
    private Map<String, Caption> labels;
    @SerializedName("statements")
    private Object statements = null;

    /**
     * No args constructor for use in serialization
     */
    public CommonsWikibaseItem() {
    }

    /**
     * @param id
     * @param statements
     * @param labels
     * @param type
     */
    public CommonsWikibaseItem(String type, String id, Map<String, Caption> labels, Object statements) {
        super();
        this.type = type;
        this.id = id;
        this.labels = labels;
        this.statements = statements;
    }

    /**
     * Ex: "mediainfo
     */
    @SerializedName("type")
    public String getType() {
        return type;
    }

    /**
     * @return Wikibase Id
     */
    @SerializedName("id")
    public String getId() {
        return id;
    }

    /**
     * @return value of captions
     */
    @SerializedName("labels")
    public Map<String, Caption> getLabels() {
        return labels;
    }

    /**
     * Contains the Depicts item
     */
    @SerializedName("statements")
    public Object getStatements() {
        return statements;
    }


}
