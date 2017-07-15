package fr.free.nrw.commons.category;

import android.os.AsyncTask;

import java.util.ArrayList;

import fr.free.nrw.commons.libs.mediawiki_api.MWApi;

/**
 * Created by nesli on 04.07.2017.
 */

public abstract class UpdaterTask extends AsyncTask<Void, Void, ArrayList<String>> {
    protected String filter;
    protected CategorizationFragment catFragment;
    public MWApi.RequestBuilder requestBuilder;
}
