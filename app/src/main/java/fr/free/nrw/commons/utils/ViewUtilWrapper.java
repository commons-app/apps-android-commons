package fr.free.nrw.commons.utils;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ViewUtilWrapper {

    @Inject
    public ViewUtilWrapper() {

    }

    public void showShortToast(Context context, String text) {
        ViewUtil.showShortToast(context, text);
    }
}
