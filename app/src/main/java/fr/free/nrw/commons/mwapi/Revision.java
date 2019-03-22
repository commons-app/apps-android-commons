package fr.free.nrw.commons.mwapi;

import org.wikipedia.page.PageTitle;

import fr.free.nrw.commons.Utils;

public class Revision {
    public final String revisionId;
    public final String username;
    public final PageTitle pageTitle;

    public Revision(String revisionId, String username, String pageTitle) {
        this.revisionId = revisionId;
        this.username = username;
        this.pageTitle = Utils.getPageTitle(pageTitle);
    }
}
