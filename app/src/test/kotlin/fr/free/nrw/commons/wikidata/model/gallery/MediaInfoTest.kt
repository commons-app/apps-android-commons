package fr.free.nrw.commons.wikidata.model.gallery

import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import org.junit.Assert.assertEquals
import org.junit.Test

private const val VIDEO_INFO_RESPONSE = """
{
  "pageid": 9000029,
  "title": "File:Big Buck Bunny medium.ogv",
  "videoinfo": [
    {
      "derivatives": [
        {
          "src": "https://upload.wikimedia.org/wikipedia/commons/4/41/Big_Buck_Bunny_medium.ogv",
          "type": "video/ogg; codecs=\"theora, vorbis\"",
          "width": 896,
          "height": 504,
          "bandwidth": 1317509
        },
        {
          "src": "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/Big_Buck_Bunny_medium.ogv/Big_Buck_Bunny_medium.ogv.240p.vp9.webm",
          "type": "video/webm; codecs=\"vp9, opus\"",
          "transcodekey": "240p.vp9.webm",
          "width": 426,
          "height": 240,
          "bandwidth": 333320
        }
      ],
      "timedtext": [
        {
          "src": "https://commons.wikimedia.org/w/api.php?action=timedtext&title=File%3ABig_Buck_Bunny_medium.ogv&lang=ar&trackformat=vtt&origin=%2A",
          "kind": "subtitles",
          "type": "text/vtt",
          "srclang": "ar",
          "dir": "rtl",
          "label": "العربية \u202a(ar)\u202c"
        },
        {
          "src": "https://commons.wikimedia.org/w/api.php?action=timedtext&title=File%3ABig_Buck_Bunny_medium.ogv&lang=en&trackformat=vtt&origin=%2A",
          "kind": "subtitles",
          "type": "text/vtt",
          "srclang": "en",
          "dir": "ltr",
          "label": "English \u202a(en)\u202c"
        }
      ]
    }
  ]
}
"""

private const val UNTRANSCODED_RESPONSE = """
{
  "pageid": 9000029,
  "title": "File:Big Buck Bunny medium.ogv",
  "videoinfo": [
    {
      "derivatives": [],
      "timedtext": []
    }
  ]
}
"""

class MediaInfoTest {
    private fun mediaInfo(json: String): MediaInfo =
        GsonUtil.defaultGson.fromJson(json, MwQueryPage::class.java).mediaInfo()!!

    @Test
    fun derivativesShouldBeReadFromVideoInfo() {
        val derivatives = mediaInfo(VIDEO_INFO_RESPONSE).derivatives()
        assertEquals(2, derivatives.size)

        val transcode = derivatives.single { it.transcodeKey() == "240p.vp9.webm" }
        assertEquals("video/webm; codecs=\"vp9, opus\"", transcode.type())
        assertEquals(333320, transcode.bandwidth())
        assertEquals(240, transcode.height())
    }

    @Test
    fun timedTextTracksShouldBeReadFromVideoInfo() {
        val tracks = mediaInfo(VIDEO_INFO_RESPONSE).timedTextTracks()
        assertEquals(2, tracks.size)

        val english = tracks.single { it.language() == "en" }
        assertEquals("English \u202a(en)\u202c", english.label())
    }

    @Test
    fun untranscodedFileShouldHaveNoDerivativesOrTracks() {
        val mediaInfo = mediaInfo(UNTRANSCODED_RESPONSE)
        assertEquals(emptyList<MediaDerivative>(), mediaInfo.derivatives())
        assertEquals(emptyList<TimedTextTrack>(), mediaInfo.timedTextTracks())
    }
}
