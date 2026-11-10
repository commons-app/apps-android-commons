package fr.free.nrw.commons.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** Exercises the descriptions passed from file wikitext to the editor. */
class MediaDetailDescriptionParserTest {
    /** Named and positional language parameters preserve nested markup and their original order. */
    @Test
    fun parsesDescriptionsInOrderWithNestedMarkup() {
        val wikitext =
            "|description={{fr|1=Un [[exemple]] avec {{small|du texte}}}}{{en|An example}}\n" +
                "|source={{own}}"

        assertEquals(
            listOf("fr" to "Un [[exemple]] avec {{small|du texte}}", "en" to "An example"),
            MediaDetailDescriptionParser.parse(wikitext).toList(),
        )
    }
}
