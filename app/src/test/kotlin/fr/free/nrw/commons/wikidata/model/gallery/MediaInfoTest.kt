package fr.free.nrw.commons.wikidata.model.gallery

import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val WEBM_RESPONSE_FILE = "raw/videoinfo_elephants_dream_webm.json"
private const val AV1_WEBM_RESPONSE_FILE = "raw/videoinfo_where_it_happens_webm.json"
private const val OGV_RESPONSE_FILE = "raw/videoinfo_big_buck_bunny_ogv.json"
private const val OGG_AUDIO_RESPONSE_FILE = "raw/videoinfo_tromboon_sample_ogg.json"
private const val MP3_RESPONSE_FILE = "raw/videoinfo_anchors_aweigh_mp3.json"

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

    private fun mediaInfoFromResource(fileName: String): MediaInfo =
        mediaInfo(
            javaClass.classLoader!!
                .getResourceAsStream(fileName)!!
                .bufferedReader()
                .readText(),
        )

    @Test
    fun webmOriginalShouldUseTheStreamCodecs() {
        val original = mediaInfoFromResource(WEBM_RESPONSE_FILE).originalSource()!!

        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/0/02/Elephants_Dream%28HQ%29.webm",
            original.src(),
        )
        assertEquals("video/webm; codecs=\"vp8, vorbis\"", original.type())
        assertEquals(640, original.width())
        assertEquals(360, original.height())
        assertEquals(929989, original.bandwidth())
        assertNull(original.transcodeKey())
        assertEquals(MediaType.VIDEO, original.mediaType())
    }

    @Test
    fun av1WebmOriginalShouldMapTheMatroskaCodecIds() {
        val original = mediaInfoFromResource(AV1_WEBM_RESPONSE_FILE).originalSource()!!

        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/3/3e/Where_it_Happens_%281017746%29.webm",
            original.src(),
        )
        assertEquals("video/webm; codecs=\"av01, opus\"", original.type())
        assertEquals(1920, original.width())
        assertEquals(1080, original.height())
        assertEquals(2173901, original.bandwidth())
        assertEquals(MediaType.VIDEO, original.mediaType())
    }

    @Test
    fun ogvOriginalShouldBeVideoOggWithTheStreamCodecs() {
        val original = mediaInfoFromResource(OGV_RESPONSE_FILE).originalSource()!!

        assertEquals("https://upload.wikimedia.org/wikipedia/commons/4/41/Big_Buck_Bunny_medium.ogv", original.src())
        assertEquals("video/ogg; codecs=\"theora, vorbis\"", original.type())
        assertEquals(896, original.width())
        assertEquals(504, original.height())
        assertEquals(1317513, original.bandwidth())
        assertEquals(MediaType.VIDEO, original.mediaType())
    }

    @Test
    fun oggAudioOriginalShouldBeAudioOggWithTheStreamCodecs() {
        val original = mediaInfoFromResource(OGG_AUDIO_RESPONSE_FILE).originalSource()!!

        assertEquals("https://upload.wikimedia.org/wikipedia/commons/a/a9/Tromboon-sample.ogg", original.src())
        assertEquals("audio/ogg; codecs=\"vorbis\"", original.type())
        assertEquals(0, original.width())
        assertEquals(0, original.height())
        assertEquals(106695, original.bandwidth())
        assertEquals(MediaType.AUDIO, original.mediaType())
    }

    @Test
    fun mp3OriginalShouldUseTheStreamCodec() {
        val original = mediaInfoFromResource(MP3_RESPONSE_FILE).originalSource()!!

        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/b/b3/%22Anchors_Aweigh%22%2C_sung_by_the_U.S._Naval_Academy%27s_Midshipmen_Glee_Club.mp3",
            original.src(),
        )
        assertEquals("audio/mpeg; codecs=\"mp3\"", original.type())
        assertEquals(0, original.width())
        assertEquals(0, original.height())
        assertEquals(214325, original.bandwidth())
        assertEquals(MediaType.AUDIO, original.mediaType())
    }

    @Test
    fun playbackSourcesShouldAppendTheOriginalToTheTranscodes() {
        val sources = mediaInfoFromResource(WEBM_RESPONSE_FILE).playbackSources()

        // The original file has no transcode key, which is why it's null.
        assertEquals(listOf("240p.vp9.webm", "360p.mpeg4.mov", null), sources.map { it.transcodeKey() })
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/0/02/Elephants_Dream%28HQ%29.webm",
            sources.last().src(),
        )
    }

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
