package fr.free.nrw.commons;

import android.content.Context;

public interface BasePresenter {
    void onAttachView(MvpView view);

    void onDetachView();
}
