package fr.free.nrw.commons.utils;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testInternetConnectionEstablished() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnectedOrConnecting())
                .thenReturn(true);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);
        boolean internetConnectionEstablished = NetworkUtils.isInternetConnectionEstablished(mockContext);
        assertTrue(internetConnectionEstablished);
    }

    @Test
    public void testInternetConnectionNotEstablished() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnectedOrConnecting())
                .thenReturn(false);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);
        boolean internetConnectionEstablished = NetworkUtils.isInternetConnectionEstablished(mockContext);
        assertFalse(internetConnectionEstablished);
    }

    @Test
    public void testInternetConnectionStatusForNullContext() {
        boolean internetConnectionEstablished = NetworkUtils.isInternetConnectionEstablished(null);
        assertFalse(internetConnectionEstablished);
    }

    @Test
    public void testInternetConnectionForNullConnectivityManager() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(null);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);
        boolean internetConnectionEstablished = NetworkUtils.isInternetConnectionEstablished(mockContext);
        assertFalse(internetConnectionEstablished);
    }
}