package fr.free.nrw.commons.login;

import android.net.Uri;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.MalformedJsonException;
import fr.free.nrw.commons.MockWebServerTest;
import fr.free.nrw.commons.auth.login.LoginInterface;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.json.NamespaceTypeAdapter;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.json.UriTypeAdapter;
import org.wikipedia.json.WikiSiteTypeAdapter;
import org.wikipedia.page.Namespace;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserExtendedInfoClientTest extends MockWebServerTest {

    private LoginInterface apiService;

    @Override
    @Before
    public void setUp() throws Throwable {
        super.setUp();

        apiService = new Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(getGson()))
            .baseUrl(server().getUrl())
            .build()
            .create(LoginInterface.class);
    }

    @Test
    public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_extended_info.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        apiService.getUserInfo("USER").subscribe(observer);

        observer
            .assertComplete()
            .assertNoErrors()
            .assertValue(
                result -> result.query().userInfo().id() == 24531888
                && result.query().getUserResponse("USER").name().equals("USER")
            );
    }

    @Test
    public void testRequestResponse404() {
        enqueue404();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        apiService.getUserInfo("USER").subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test
    public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        apiService.getUserInfo("USER").subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    private Gson getGson() {
        return new GsonBuilder()
            .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapter(WikiSite.class, new WikiSiteTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new PostProcessingTypeAdapter())
            .create();
    }
}
