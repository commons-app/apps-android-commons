package fr.free.nrw.commons.auth.csrf;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import com.google.gson.stream.MalformedJsonException;
import fr.free.nrw.commons.MockWebServerTest;
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient.Callback;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import retrofit2.Call;

public class CsrfTokenClientTest extends MockWebServerTest {
    private static final WikiSite TEST_WIKI = new WikiSite("test.wikipedia.org");
    @NonNull private final CsrfTokenClient subject = new CsrfTokenClient(TEST_WIKI, TEST_WIKI);

    @Test public void testRequestSuccess() throws Throwable {
        String expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\";
        enqueueFromFile("csrf_token.json");

        Callback cb = Mockito.mock(Callback.class);
        request(cb);

        server().takeRequest();
        assertCallbackSuccess(cb, expected);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        Callback cb = Mockito.mock(Callback.class);
        request(cb);

        server().takeRequest();
        assertCallbackFailure(cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        Callback cb = Mockito.mock(Callback.class);
        request(cb);

        server().takeRequest();
        assertCallbackFailure(cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();

        Callback cb = Mockito.mock(Callback.class);
        request(cb);

        server().takeRequest();
        assertCallbackFailure(cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Callback cb,
                                       @NonNull String expected) {
        verify(cb).success(ArgumentMatchers.eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(ArgumentMatchers.any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(ArgumentMatchers.any(String.class));
        verify(cb).failure(ArgumentMatchers.isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull Callback cb) {
        return subject.request(service(Service.class), cb);
    }
}
