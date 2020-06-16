package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.Media
import org.wikipedia.dataclient.mwapi.MwQueryPage
import javax.inject.Inject

class MediaConverter @Inject constructor() {
    fun convert(mwQueryPage: MwQueryPage): Media = Media.from(mwQueryPage)
}
