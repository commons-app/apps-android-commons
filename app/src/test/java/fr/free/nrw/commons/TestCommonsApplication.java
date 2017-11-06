package fr.free.nrw.commons;

import com.squareup.leakcanary.RefWatcher;

// This class is automatically discovered by Robolectric
public class TestCommonsApplication extends CommonsApplication {
    @Override
    protected RefWatcher setupLeakCanary() {
        // No leakcanary in unit tests.
        return RefWatcher.DISABLED;
    }
}
