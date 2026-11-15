package fr.free.nrw.commons.media

import fr.free.nrw.commons.Media

interface MediaDetailProvider {
    fun getMediaAtPosition(i: Int): Media?

    fun getTotalMediaCount(): Int

    fun getContributionStateAt(position: Int): Int?

    // Reload media detail fragment once media is nominated
    fun refreshNominatedMedia(index: Int)
}
