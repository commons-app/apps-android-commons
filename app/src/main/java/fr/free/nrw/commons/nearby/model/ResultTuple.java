package fr.free.nrw.commons.nearby.model;

public class ResultTuple {
    private final String type;
    private final String value;

    public ResultTuple(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public ResultTuple() {
        this.type = "";
        this.value = "";
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
