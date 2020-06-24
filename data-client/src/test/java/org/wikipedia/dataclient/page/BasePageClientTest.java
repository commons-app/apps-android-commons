package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.dataclient.Service;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;
import okhttp3.CacheControl;
import retrofit2.Response;

import static org.wikipedia.dataclient.Service.PREFERRED_THUMB_SIZE;

public abstract class BasePageClientTest extends MockRetrofitTest {
    @Test public void testLeadCacheControl() {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), CacheControl.FORCE_NETWORK, null, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("no-cache"));
    }

    @Test public void testLeadHttpRefererUrl() {
        String refererUrl = "https://en.wikipedia.org/wiki/United_States";
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, refererUrl, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Referer").contains(refererUrl));
    }

    @Test public void testLeadCacheOptionCache() {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(Service.OFFLINE_SAVE_HEADER) == null);
    }

    @Test public void testLeadCacheOptionSave() {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, Service.OFFLINE_SAVE_HEADER_SAVE, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertValue(result -> result.raw().request().header(Service.OFFLINE_SAVE_HEADER).contains(Service.OFFLINE_SAVE_HEADER_SAVE));
    }

    @Test public void testLeadTitle() {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, null, "Title", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> {
                    System.out.println(result.raw().request().url());
                    System.out.println(result.raw().request().url().toString());
                    return result.raw().request().url().toString().contains("Title");
                });
    }

    @Test public void testSectionsCacheControl() {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), CacheControl.FORCE_NETWORK, null, "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("no-cache"));
    }

    @Test public void testSectionsCacheOptionCache() {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, null, "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(Service.OFFLINE_SAVE_HEADER) == null);
    }

    @Test public void testSectionsCacheOptionSave() {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, Service.OFFLINE_SAVE_HEADER_SAVE,  "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(Service.OFFLINE_SAVE_HEADER).contains(Service.OFFLINE_SAVE_HEADER_SAVE));
    }

    @Test public void testSectionsTitle() {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, null, "Title").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().url().toString().contains("Title"));
    }

    @NonNull protected abstract PageClient subject();

    protected String preferredThumbSizeString() {
        return Integer.toString(PREFERRED_THUMB_SIZE) + "px";
    }
}
