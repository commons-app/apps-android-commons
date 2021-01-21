package fr.free.nrw.commons.di;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;

import androidx.fragment.app.Fragment;

import dagger.android.HasAndroidInjector;
import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasBroadcastReceiverInjector;
import dagger.android.HasContentProviderInjector;
import dagger.android.HasFragmentInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;

/**
 * Provides injectors for all sorts of components
 * Ex: Activities, Fragments, Services, ContentProviders
 */
public class ApplicationlessInjection
        implements
        HasAndroidInjector,
        HasActivityInjector,
        HasFragmentInjector,
        HasSupportFragmentInjector,
        HasServiceInjector,
        HasBroadcastReceiverInjector,
        HasContentProviderInjector {

    private static ApplicationlessInjection instance = null;

    @Inject DispatchingAndroidInjector<Object> androidInjector;
    @Inject DispatchingAndroidInjector<Activity> activityInjector;
    @Inject DispatchingAndroidInjector<BroadcastReceiver> broadcastReceiverInjector;
    @Inject DispatchingAndroidInjector<android.app.Fragment> fragmentInjector;
    @Inject DispatchingAndroidInjector<Fragment> supportFragmentInjector;
    @Inject DispatchingAndroidInjector<Service> serviceInjector;
    @Inject DispatchingAndroidInjector<ContentProvider> contentProviderInjector;

    private CommonsApplicationComponent commonsApplicationComponent;

    public ApplicationlessInjection(Context applicationContext) {
        commonsApplicationComponent = DaggerCommonsApplicationComponent.builder()
                .appModule(new CommonsApplicationModule(applicationContext)).build();
        commonsApplicationComponent.inject(this);
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return androidInjector;
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return activityInjector;
    }

    @Override
    public DispatchingAndroidInjector<android.app.Fragment> fragmentInjector() {
        return fragmentInjector;
    }

    @Override
    public DispatchingAndroidInjector<Fragment> supportFragmentInjector() {
        return supportFragmentInjector;
    }

    @Override
    public DispatchingAndroidInjector<BroadcastReceiver> broadcastReceiverInjector() {
        return broadcastReceiverInjector;
    }

    @Override
    public DispatchingAndroidInjector<Service> serviceInjector() {
        return serviceInjector;
    }

    @Override
    public AndroidInjector<ContentProvider> contentProviderInjector() {
        return contentProviderInjector;
    }

    public CommonsApplicationComponent getCommonsApplicationComponent() {
        return commonsApplicationComponent;
    }

    public static ApplicationlessInjection getInstance(Context applicationContext) {
        if (instance == null) {
            synchronized (ApplicationlessInjection.class) {
                if (instance == null) {
                    instance = new ApplicationlessInjection(applicationContext);
                }
            }
        }

        return instance;
    }
}
