package fr.free.nrw.commons.media;

import fr.free.nrw.commons.Media;

public interface MediaDetailProvider {

    Media getMediaAtPosition(int i);

    int getTotalMediaCount();

    Integer getContributionStateAt(int position);

    // Reload media detail fragment once media is nominated
    void refreshNominatedMedia(int index);
}
