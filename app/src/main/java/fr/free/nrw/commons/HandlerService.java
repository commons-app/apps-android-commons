package fr.free.nrw.commons;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import fr.free.nrw.commons.di.CommonsDaggerService;

public abstract class HandlerService<T> extends CommonsDaggerService {
    private volatile Looper threadLooper;
    private volatile ServiceHandler threadHandler;
    private String serviceName;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //FIXME: Google Photos bug
            handle(msg.what, (T)msg.obj);
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onDestroy() {
        threadLooper.quit();
        super.onDestroy();
    }

    public class HandlerServiceLocalBinder extends Binder {
        public HandlerService getService() {
            return HandlerService.this;
        }
    }

    private final IBinder localBinder = new HandlerServiceLocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    protected HandlerService(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread(serviceName);
        thread.start();

        threadLooper = thread.getLooper();
        threadHandler = new ServiceHandler(threadLooper);
    }

    private void postMessage(int type, T t) {
        Message msg = threadHandler.obtainMessage(type);
        msg.obj = t;
        threadHandler.sendMessage(msg);
    }

    public void queue(int what, T t) {
        postMessage(what, t);
    }

    protected abstract void handle(int what, T t);
}
