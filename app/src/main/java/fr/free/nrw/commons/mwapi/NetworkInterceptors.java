package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.ClientParamsStack;
import org.apache.http.params.HttpParamsNames;
import org.apache.http.protocol.HttpContext;

import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class NetworkInterceptors {

    /**
     * Interceptor to log the HTTP request
     */
    @NonNull
    public static HttpRequestInterceptor getHttpRequestInterceptor() {
        return (HttpRequest request, HttpContext httpContext) -> {
            Timber.v("<<<<<<<<<<<<<< START OF REQUEST LOGGING [%s] >>>>>>>>>>>>", request.getRequestLine().getUri());

            Timber.v("Request line:\n %s", request.getRequestLine().toString());
            logRequestParams(request);
            logRequestHeaders(request);
            Timber.v("Protocol version:\n %s", request.getProtocolVersion());

            Timber.v("<<<<<<<<<<<<<< END OF REQUEST LOGGING [%s] >>>>>>>>>>>>", request.getRequestLine().getUri());
        };
    }

    /**
     * Log all request params from a HTTPRequest
     * @param request
     */
    private static void logRequestParams(HttpRequest request) {
        Set<String> names = new HashSet<>();
        if (request.getParams() instanceof ClientParamsStack) {
            ClientParamsStack cps = (ClientParamsStack) request.getParams();
            if (cps.getApplicationParams() != null
                    && cps.getRequestParams() instanceof HttpParamsNames) {
                names.addAll(((HttpParamsNames) cps.getApplicationParams()).getNames());
            }
            if (cps.getClientParams() != null
                    && cps.getClientParams() instanceof HttpParamsNames) {
                names.addAll(((HttpParamsNames) cps.getClientParams()).getNames());
            }
            if (cps.getRequestParams() != null
                    && cps.getRequestParams() instanceof HttpParamsNames) {
                names.addAll(((HttpParamsNames) cps.getRequestParams()).getNames());
            }
            if (cps.getOverrideParams() != null
                    && cps.getRequestParams() instanceof HttpParamsNames) {
                names.addAll(((HttpParamsNames) cps.getOverrideParams()).getNames());
            }
        } else {
            HttpParamsNames params = (HttpParamsNames) request.getParams();
            names = params.getNames();
        }

        Timber.v("<<<<<<<<<<<<<< REQUEST PARAMS >>>>>>>>>>>>");
        for (String name : names) {
            Timber.v("Param >> %s: %s", name, request.getParams().getParameter(name));
        }
        Timber.v("<<<<<<<<<<<<<< REQUEST PARAMS >>>>>>>>>>>>");
    }

    /**
     * Log all headers from a HTTPRequest
     * @param request
     */
    private static void logRequestHeaders(HttpRequest request) {
        Header[] headerFields = request.getAllHeaders();

        Timber.v("<<<<<<<<<<<<<< HEADERS >>>>>>>>>>>>");
        for (int e = 0; e < request.getAllHeaders().length; e++) {
            Timber.v("Header >> %s: %s", headerFields[e].getName(), headerFields[e].getValue());
        }
        Timber.v("<<<<<<<<<<<<<< HEADERS >>>>>>>>>>>>");
    }
}
