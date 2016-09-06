package fr.free.nrw.commons.nearby;

import java.util.List;

public interface TaskListener {
    void onTaskStarted();

    void onTaskFinished(List<Place> result);
}