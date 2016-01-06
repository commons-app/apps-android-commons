package fr.free.nrw.commons;

import android.app.Application;

import fr.free.nrw.commons.caching.CacheController;


public class CacheApplication extends Application
{
    public CacheController data = new CacheController();
}