package fr.free.nrw.commons.utils;

import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import fr.free.nrw.commons.utils.model.NetworkConnectionType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class NetworkUtilsTest {

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
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.getType())
                .thenReturn(ConnectivityManager.TYPE_WIFI);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);

        when(mockApplication.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mock(TelephonyManager.class));
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.WIFI);
    }

    @Test
    public void testCellular2GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.getType())
                .thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);

        TelephonyManager mockTelephonyManager = mock(TelephonyManager.class);
        when(mockTelephonyManager.getNetworkType())
                .thenReturn(NETWORK_TYPE_EDGE);

        when(mockApplication.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mockTelephonyManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.TWO_G);
    }

    @Test
    public void testCellular3GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.getType())
                .thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);

        TelephonyManager mockTelephonyManager = mock(TelephonyManager.class);
        when(mockTelephonyManager.getNetworkType())
                .thenReturn(NETWORK_TYPE_HSPA);

        when(mockApplication.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mockTelephonyManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.THREE_G);
    }

    @Test
    public void testCellular4GNetwork() {
        Context mockContext = mock(Context.class);
        Application mockApplication = mock(Application.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);
        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.getType())
                .thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(mockConnectivityManager.getActiveNetworkInfo())
                .thenReturn(mockNetworkInfo);
        when(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);

        TelephonyManager mockTelephonyManager = mock(TelephonyManager.class);
        when(mockTelephonyManager.getNetworkType())
                .thenReturn(NETWORK_TYPE_LTE);

        when(mockApplication.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mockTelephonyManager);
        when(mockContext.getApplicationContext()).thenReturn(mockApplication);

        NetworkConnectionType networkType = NetworkUtils.getNetworkType(mockContext);

        assertEquals(networkType, NetworkConnectionType.FOUR_G);
    }
}
