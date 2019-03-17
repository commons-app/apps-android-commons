package fr.free.nrw.commons.nearby.model;

public class NearbyResponse {
    private final NearbyResults results;

    public NearbyResponse(NearbyResults results) {
        this.results = results;
    }

    public NearbyResults getResults() {
        return results;
    }
}
