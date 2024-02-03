package fr.free.nrw.commons.wikidata;

import android.net.Uri;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.free.nrw.commons.wikidata.json.RequiredFieldsCheckOnReadTypeAdapterFactory;
import fr.free.nrw.commons.wikidata.model.DataValue;
import fr.free.nrw.commons.wikidata.model.WikiSite;
import fr.free.nrw.commons.wikidata.json.NamespaceTypeAdapter;
import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter;
import fr.free.nrw.commons.wikidata.json.UriTypeAdapter;
import fr.free.nrw.commons.wikidata.json.WikiSiteTypeAdapter;
import fr.free.nrw.commons.wikidata.model.page.Namespace;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";

    private static final GsonBuilder DEFAULT_GSON_BUILDER = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeAdapterFactory(DataValue.getPolymorphicTypeAdapter())
            .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapter(WikiSite.class, new WikiSiteTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new RequiredFieldsCheckOnReadTypeAdapterFactory())
            .registerTypeAdapterFactory(new PostProcessingTypeAdapter());

    private static final Gson DEFAULT_GSON = DEFAULT_GSON_BUILDER.create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    private GsonUtil() { }
}
