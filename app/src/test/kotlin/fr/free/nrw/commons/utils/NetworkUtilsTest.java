package fr.free.nrw.commons.utils;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import fr.free.nrw.commons.utils.model.NetworkConnectionType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testInternetConnectionEstablished() {
        Context mockContext = getContext(true);
        boolean internetConnectionEstablished = NetworkUtils.isInternetConnectionEstablished(mockContext);
        assertTrue(internetConnectionEstablished);
    }

    @NotNull
    public static Context getContext(boolean connectionEstablished) {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnectedOrConnecting())
            .thenReturn(connectionEstablished);
        when(mockConnectivityManager.getActiveNetworkInfo())
            .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);
        return mockContext;
    }

    @Test
    public void testInternetConnectionNotEstablished() {
        Context mockContext = getContext(false);
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

    @Test
    public void testWifiNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        Network mockNetwork = mock(Network.class);
        NetworkCapabilities mockCapabilities = mock(NetworkCapabilities.class);

        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockCapabilities);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.WIFI);
    }

    @Test
    public void testCellular2GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        Network mockNetwork = mock(Network.class);
        NetworkCapabilities mockCapabilities = mock(NetworkCapabilities.class);

        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockCapabilities);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.UNKNOWN);
    }

    @Test
    public void testCellular3GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        Network mockNetwork = mock(Network.class);
        NetworkCapabilities mockCapabilities = mock(NetworkCapabilities.class);

        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockCapabilities);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.UNKNOWN);
    }

    @Test
    public void testCellular4GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        Network mockNetwork = mock(Network.class);
        NetworkCapabilities mockCapabilities = mock(NetworkCapabilities.class);

        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockCapabilities);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.UNKNOWN);
    }
}