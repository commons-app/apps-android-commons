package fr.free.nrw.commons.widget;

import android.content.Context;

public interface ViewHolder<T> {

  void bindModel(Context context, T model);
}
