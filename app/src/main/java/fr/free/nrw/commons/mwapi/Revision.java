package fr.free.nrw.commons.mwapi;

import fr.free.nrw.commons.PageTitle;

public class Revision {
    public final String revisionId;
    public final String username;
    public final PageTitle pageTitle;

    public Revision(String revisionId, String username, String pageTitle) {
        this.revisionId = revisionId;
        this.username = username;
        this.pageTitle = new PageTitle(pageTitle);
    }
}
