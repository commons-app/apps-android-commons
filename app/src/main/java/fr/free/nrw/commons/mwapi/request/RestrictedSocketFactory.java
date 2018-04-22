package fr.free.nrw.commons.mwapi.request;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Locale;

import javax.net.SocketFactory;

/**
 * <b>Restricted Socket Factory</b>
 * <p>
 * OkHttp buffers to the network interface but the network interface's default
 * buffer size is sometimes set very high e.g. 512Kb which makes tracking
 * upload progress impossible as the upload content is sitting in the network
 * interface buffer waiting to be transmitted.
 * Re: https://github.com/square/okhttp/issues/1078
 * <p>
 * So here, we create socket factory that forces all sockets to have a restricted
 * send buffer size. So that further down the chain in OkHttps' RequestBody we can
 * track the actual progress to the nearest [sendBufferSize] unit.
 * <p>
 * Example usage with OkHttpClient 2.x:
 * <pre>
 *     okHttpClient.setSocketFactory(new RestrictedSocketFactory(16 * 1024));
 * </pre>
 * <p>
 * Example usage with OkHttpClient 3.x:
 * <pre>
 *     okHttpClientBuilder.socketFactory(new RestrictedSocketFactory(16 * 1024))
 * </pre>
 * <p>
 * Created by Simon Lightfoot <simon@devangels.london> on 04/04/2016.
 * See: https://gist.github.com/slightfoot/00a26683ea68856ceb50e26c7d8a47d0
 */
public class RestrictedSocketFactory extends SocketFactory {

    private int mSendBufferSize;

    public RestrictedSocketFactory(int sendBufferSize) {
        mSendBufferSize = sendBufferSize;
        try {
            Socket socket = new Socket();
            String message = String.format(Locale.getDefault(),
                    "Changing SO_SNDBUF on new sockets from %d to %d.",
                    socket.getSendBufferSize(), sendBufferSize);
            Log.w("RestrictedSocketFactory", message);
        } catch (SocketException ignored) {
        }
    }

    @Override
    public Socket createSocket() throws IOException {
        return updateSendBufferSize(new Socket());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return updateSendBufferSize(new Socket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return updateSendBufferSize(new Socket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return updateSendBufferSize(new Socket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return updateSendBufferSize(new Socket(address, port, localAddress, localPort));
    }

    private Socket updateSendBufferSize(Socket socket) throws IOException {
        socket.setSendBufferSize(mSendBufferSize);
        return socket;
    }
}