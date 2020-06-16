package fr.free.nrw.commons;

import androidx.annotation.NonNull;

/**
 * Base presenter, enforcing contracts to atach and detach view
 */
public interface BasePresenter<T> {

  /**
   * Until a view is attached, it is open to listen events from the presenter
   */
  void onAttachView(@NonNull T view);

  /**
   * Detaching a view makes sure that the view no more receives events from the presenter
   */
  void onDetachView();
}
