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

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithComma() {
        val s =
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=Antique cash register in a cafe, Darjeeling}}\n" +
                "|date=2017-05-17 17:07:26\n" +
                "|source={{own}}\n" +
                "|author=[[User:Subhrajyoti07|Subhrajyoti07]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "}}\n" +
                "{{Location|27.043186|88.267003}}\n" +
                "{{Assessments|featured=1}}\n"
        val map = linkedMapOf("en" to "Antique cash register in a cafe, Darjeeling")
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithNestedBrackets() {
        val s =
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n" +
                "{{On Wikidata|Q17549757}}}}\n" +
                "|date=2017-09-17 13:09:39\n" +
                "|source={{own}}\n" +
                "|author=[[User:Colin|Colin]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "|Other fields = {{Credit line |Author = © [[User:Colin]] | " +
                "Other = Wikimedia Commons |License = CC-BY-SA-4.0}}\n" +
                "}}\n" +
                "{{Location|51.519003|-0.138353}}\n" +
                "{{Assessments|featured=1}}"
        val map =
            linkedMapOf(
                "en" to "[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n{{On Wikidata|Q17549757}}",
            )
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithNearbyWikidataTemplate() {
        val s =
            "== {{int:filedesc}} ==\n" +
                "{{Information\n" +
                "|description={{en|1=A nearby place}}\n" +
                "{{ on Wikidata|Q42}}|source={{own}}\n" +
                "|author=[[User:Example|Example]]\n" +
                "|date=2020-01-01\n" +
                "}}\n" +
                "{{Location|1.2|3.4}}\n"
        val map = linkedMapOf("en" to "A nearby place")
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithInvalidLanguageCode() {
        val s =
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n" +
                "}}{{Listed building England|1223496}}\n" +
                "|date=2017-09-17 13:09:39\n" +
                "|source={{own}}\n" +
                "|author=[[User:Colin|Colin]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "|Other fields = {{Credit line |Author = © [[User:Colin]] | " +
                "Other = Wikimedia Commons |License = CC-BY-SA-4.0}}\n" +
                "}}\n" +
                "{{Location|51.519003|-0.138353}}\n" +
                "{{Assessments|featured=1}}\n"
        val map = linkedMapOf("en" to "[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n")
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithSpaces() {
        val s =
            "=={{int:filedesc}}==\n" +
                "{{Artwork\n" +
                " |artist = {{Creator:Filippo Peroni}} Restored by {{Creator:Adam Cuerden}}\n" +
                " |author = \n" +
                " |title = Ricchi giardini nel Palazzo di Monforte a Palermo\n" +
                " |description = {{en|''Ricchi giardini nel Palazzo di Monforte a Palermo''," +
                " set design for ''I Vespri siciliani'' act 5 (undated).}} {{it|''Ricchi" +
                " giardini nel Palazzo di Monforte a Palermo'', bozzetto per ''I Vespri" +
                " siciliani'' atto 5 (s.d.).}}\n" +
                " |date = {{between|1855|1878}} (Premiére of the opera and death of the artist, " +
                "respectively)\n" +
                " |medium = {{technique|watercolor|and=tempera|and2=|over=paper}}\n" +
                " |dimensions = {{Size|unit=mm|height=210|width=270}}\n" +
                " |institution = {{Institution:Archivio Storico Ricordi}}\n" +
                " |department = \n" +
                " |place of discovery = \n" +
                " |object history = \n" +
                " |exhibition history = \n" +
                " |credit line = \n" +
                " |inscriptions = \n" +
                " |notes = \n" +
                " |accession number = ICON000132\n" +
                " |place of creation = \n" +
                " |source = [https://www.archivioricordi.com/chi-siamo/glam-archivio-ricordi/#/" +
                " Archivio Storico Ricordi], [https://www.digitalarchivioricordi.com/it/" +
                "works/display/108/Vespri_Siciliani__I Collezione Digitale Ricordi]\n" +
                " |permission={{PermissionTicket|id=2022031410007974|user=Ruthven}} \n" +
                " |other_versions = \n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di" +
                " Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi" +
                " ICON000132 - Restoration.jpg]] - Restoration (JPEG)\n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di" +
                " Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi" +
                " ICON000132 - Restoration.png]] - Restoration (PNG)\n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di" +
                " Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi" +
                " ICON000132.jpg]] - Original (JPEG)\n" +
                " |references = \n" +
                " |wikidata = \n" +
                "}}"
        val map =
            linkedMapOf(
                "en" to
                    "''Ricchi giardini nel Palazzo di Monforte a Palermo'', " +
                    "set design for ''I Vespri siciliani'' act 5 (undated).",
                "it" to
                    "''Ricchi giardini nel Palazzo di Monforte a Palermo'', " +
                    "bozzetto per ''I Vespri siciliani'' atto 5 (s.d.).",
            )
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithLongSpaces() {
        val s =
            "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|Description   ={{en|1=The interior of Sacred Heart RC Church, Wimbledon, London.}}\n" +
                "|Source        ={{own}}\n" +
                "|Author        =[[User:Diliff|Diliff]]\n" +
                "|Date          =2015-02-17\n" +
                "|Permission    ={{Diliff/Licensing}}\n" +
                "|other_versions=\n" +
                "}}\n"
        val map = linkedMapOf("en" to "The interior of Sacred Heart RC Church, Wimbledon, London.")
        assertEquals(map, MediaDetailDescriptionParser.parse(s))
    }
}
