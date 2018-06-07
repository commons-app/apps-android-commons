package fr.free.nrw.commons.mwapi.model;

public class ApiResponse {
    public Query query;

    public ApiResponse() {
    }

    public boolean hasPages() {
        return query != null && query.pages != null;
    }
}
