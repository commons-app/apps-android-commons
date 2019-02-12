package fr.free.nrw.commons.nearby.model;

import java.util.List;

public class NearbyResults {
    private final List<NearbyResultItem> bindings;

    public NearbyResults(List<NearbyResultItem> bindings) {
        this.bindings = bindings;
    }

    public List<NearbyResultItem> getBindings() {
        return bindings;
    }
}
