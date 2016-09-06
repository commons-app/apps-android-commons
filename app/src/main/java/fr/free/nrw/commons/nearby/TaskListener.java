package fr.free.nrw.commons.nearby;

import java.util.List;

// As per https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/
public interface TaskListener {
    void onTaskStarted();

    void onTaskFinished(List<Place> result);
}