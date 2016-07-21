package fr.free.nrw.commons;

import android.app.*;
import android.content.*;
import android.os.*;

public abstract class HandlerService<T> extends Service {
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
        super.onDestroy();
        threadLooper.quit();
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

    private void postMessage(int type, Object obj) {
        Message msg = threadHandler.obtainMessage(type);
        msg.obj = obj;
        threadHandler.sendMessage(msg);
    }

    public void queue(int what, T t) {
        postMessage(what, t);
    }

    protected abstract void handle(int what, T t);
}
