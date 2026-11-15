package fr.free.nrw.commons.description

import fr.free.nrw.commons.upload.UploadMediaDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class DescriptionWikiTextTest {
    @Test
    fun replacesSingleLineDescriptionAndKeepsFollowingParameters() {
        val wikiText =
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=Old caption text}}\n" +
                "|source={{own}}\n" +
                "|author=[[User:Example|Example]]\n" +
                "}}\n"

        val updated =
            replaceDescriptionParameter(
                wikiText,
                listOf(UploadMediaDetail("en", "New caption text")),
            )

        assertEquals(
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=New caption text}}\n" +
                "|source={{own}}\n" +
                "|author=[[User:Example|Example]]\n" +
                "}}\n",
            updated,
        )
    }

    @Test
    fun replacesEveryNearbyDescriptionLineAndKeepsWikidataTemplate() {
        val wikiText =
            "== {{int:filedesc}} ==\n" +
                "{{Information\n" +
                "|description={{en|1=Old nearby text}}\n" +
                "{{fr|1=Ancien texte}}\n" +
                "{{ on Wikidata|Q42}}|source={{own}}\n" +
                "|author=[[User:Example|Example]]\n" +
                "}}\n"

        val updated =
            replaceDescriptionParameter(
                wikiText,
                listOf(
                    UploadMediaDetail("en", "New nearby text"),
                    UploadMediaDetail("fr", "Nouveau texte"),
                ),
            )

        assertEquals(
            "== {{int:filedesc}} ==\n" +
                "{{Information\n" +
                "|description={{en|1=New nearby text}}{{fr|1=Nouveau texte}}\n" +
                "{{ on Wikidata|Q42}}\n" +
                "|source={{own}}\n" +
                "|author=[[User:Example|Example]]\n" +
                "}}\n",
            updated,
        )
    }

    @Test
    fun keepsWikidataTemplateNestedInsideTheDescriptionText() {
        val description = "Chapel ceiling<br/>\n{{On Wikidata|Q17549757}}"
        val wikiText =
            "{{Information\n" +
                "|description={{en|1=$description}}\n" +
                "|date=2017-09-17\n" +
                "}}\n"

        val updated =
            replaceDescriptionParameter(
                wikiText,
                listOf(UploadMediaDetail("en", description)),
            )

        assertEquals(wikiText, updated)
    }
}
