package fr.free.nrw.commons.media.zoomControllers.zoomable;

import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;


public class MultiZoomableControllerListener implements ZoomableController.Listener {

  private final List<ZoomableController.Listener> mListeners = new ArrayList<>();

  @Override
  public synchronized void onTransformBegin(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformBegin(transform);
    }
  }

  @Override
  public synchronized void onTransformChanged(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformChanged(transform);
    }
  }

  @Override
  public synchronized void onTransformEnd(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformEnd(transform);
    }
  }

  public synchronized void addListener(ZoomableController.Listener listener) {
    mListeners.add(listener);
  }

  public synchronized void removeListener(ZoomableController.Listener listener) {
    mListeners.remove(listener);
  }
}
