package fr.free.nrw.commons.mwapi.model;

public class PageCategory {
    public String title;

    public PageCategory() {
    }

    public String withoutPrefix() {
        return title != null ? title.replace("Category:", "") : "";
    }
}
