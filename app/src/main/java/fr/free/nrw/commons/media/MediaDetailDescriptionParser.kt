package fr.free.nrw.commons.media

import java.util.regex.Matcher
import java.util.regex.Pattern

private val allLanguageCodes: Set<String> =
    HashSet(
        mutableListOf(
            "en",
            "es",
            "de",
            "ja",
            "fr",
            "ru",
            "pt",
            "it",
            "zh-hans",
            "zh-hant",
            "ar",
            "ko",
            "id",
            "pl",
            "nl",
            "fa",
            "hi",
            "th",
            "vi",
            "sv",
            "uk",
            "cs",
            "simple",
            "hu",
            "ro",
            "fi",
            "el",
            "he",
            "nb",
            "da",
            "sr",
            "hr",
            "ms",
            "bg",
            "ca",
            "tr",
            "sk",
            "sh",
            "bn",
            "tl",
            "mr",
            "ta",
            "kk",
            "lt",
            "az",
            "bs",
            "sl",
            "sq",
            "arz",
            "zh-yue",
            "ka",
            "te",
            "et",
            "lv",
            "ml",
            "hy",
            "uz",
            "kn",
            "af",
            "nn",
            "mk",
            "gl",
            "sw",
            "eu",
            "ur",
            "ky",
            "gu",
            "bh",
            "sco",
            "ast",
            "is",
            "mn",
            "be",
            "an",
            "km",
            "si",
            "ceb",
            "jv",
            "eo",
            "als",
            "ig",
            "su",
            "be-x-old",
            "la",
            "my",
            "cy",
            "ne",
            "bar",
            "azb",
            "mzn",
            "as",
            "am",
            "so",
            "pa",
            "map-bms",
            "scn",
            "tg",
            "ckb",
            "ga",
            "lb",
            "war",
            "zh-min-nan",
            "nds",
            "fy",
            "vec",
            "pnb",
            "zh-classical",
            "lmo",
            "tt",
            "io",
            "ia",
            "br",
            "hif",
            "mg",
            "wuu",
            "gan",
            "ang",
            "or",
            "oc",
            "yi",
            "ps",
            "tk",
            "ba",
            "sah",
            "fo",
            "nap",
            "vls",
            "sa",
            "ce",
            "qu",
            "ku",
            "min",
            "bcl",
            "ilo",
            "ht",
            "li",
            "wa",
            "vo",
            "nds-nl",
            "pam",
            "new",
            "mai",
            "sn",
            "pms",
            "eml",
            "yo",
            "ha",
            "gn",
            "frr",
            "gd",
            "hsb",
            "cv",
            "lo",
            "os",
            "se",
            "cdo",
            "sd",
            "ksh",
            "bat-smg",
            "bo",
            "nah",
            "xmf",
            "ace",
            "roa-tara",
            "hak",
            "bjn",
            "gv",
            "mt",
            "pfl",
            "szl",
            "bpy",
            "rue",
            "co",
            "diq",
            "sc",
            "rw",
            "vep",
            "lij",
            "kw",
            "fur",
            "pcd",
            "lad",
            "tpi",
            "ext",
            "csb",
            "rm",
            "kab",
            "gom",
            "udm",
            "mhr",
            "glk",
            "za",
            "pdc",
            "om",
            "iu",
            "nv",
            "mi",
            "nrm",
            "tcy",
            "frp",
            "myv",
            "kbp",
            "dsb",
            "zu",
            "ln",
            "mwl",
            "fiu-vro",
            "tum",
            "tet",
            "tn",
            "pnt",
            "stq",
            "nov",
            "ny",
            "xh",
            "crh",
            "lfn",
            "st",
            "pap",
            "ay",
            "zea",
            "bxr",
            "kl",
            "sm",
            "ak",
            "ve",
            "pag",
            "nso",
            "kaa",
            "lez",
            "gag",
            "kv",
            "bm",
            "to",
            "lbe",
            "krc",
            "jam",
            "ss",
            "roa-rup",
            "dv",
            "ie",
            "av",
            "cbk-zam",
            "chy",
            "inh",
            "ug",
            "ch",
            "arc",
            "pih",
            "mrj",
            "kg",
            "rmy",
            "dty",
            "na",
            "ts",
            "xal",
            "wo",
            "fj",
            "tyv",
            "olo",
            "ltg",
            "ff",
            "jbo",
            "haw",
            "ki",
            "chr",
            "sg",
            "atj",
            "sat",
            "ady",
            "ty",
            "lrc",
            "ti",
            "din",
            "gor",
            "lg",
            "rn",
            "bi",
            "cu",
            "kbd",
            "pi",
            "cr",
            "koi",
            "ik",
            "mdf",
            "bug",
            "ee",
            "shn",
            "tw",
            "dz",
            "srn",
            "ks",
            "test",
            "en-x-piglatin",
            "ab",
        ),
    )

/** Parses multilingual descriptions from file wikitext. */
internal object MediaDetailDescriptionParser {
    /**
     * Filters descriptions from current wikiText and arranges it in LinkedHashmap according to the
     * language code
     * @param s wikitext
     * @return LinkedHashMap<LanguageCode></LanguageCode>,Description>
     */
    fun parse(s: String): LinkedHashMap<String, String> {
        val pattern: Pattern = Pattern.compile("[dD]escription *=(.*?)\n *\\|", Pattern.DOTALL)
        val matcher: Matcher = pattern.matcher(s)
        var description: String? = null
        if (matcher.find()) {
            description = matcher.group()
        }
        if (description == null) {
            return LinkedHashMap()
        }

        val descriptionList: LinkedHashMap<String, String> = LinkedHashMap()

        var count = 0 // number of "{{"
        var bounds = DescriptionTemplateBounds(0, 0, 0)
        var i = 0
        while (i < description.length - 1) {
            if (description.startsWith("{{", i)) {
                if (count == 0) {
                    bounds = descriptionTemplateBounds(description, i)
                    i += bounds.contentStart - bounds.separator - 1
                }
                i++
                count++
            } else if (description.startsWith("}}", i)) {
                count--
                if (count == 0) {
                    appendDescription(descriptionList, description, bounds, i)
                }
                i++
            }
            i++
        }
        return descriptionList
    }

    /** Positions of a top-level language template and its description. */
    private data class DescriptionTemplateBounds(
        val start: Int,
        val separator: Int,
        val contentStart: Int,
    )

    /** Finds the description start after the language code and optional named parameter. */
    private fun descriptionTemplateBounds(
        description: String,
        start: Int,
    ): DescriptionTemplateBounds {
        val separator = description.indexOf("|", start)
        val contentStart = separator + 1
        return DescriptionTemplateBounds(
            start,
            separator,
            if (description.startsWith("1=", contentStart)) contentStart + 2 else contentStart,
        )
    }

    /** Adds a recognized language template to the description list. */
    private fun appendDescription(
        descriptions: MutableMap<String, String>,
        source: String,
        bounds: DescriptionTemplateBounds,
        end: Int,
    ) {
        // Nearby uploads append {{ on Wikidata|Qid}} before |source, so this match
        // includes {{own}}. That template's pipe belongs to the next field, and
        // slicing it throws before the edit screen can open.
        if (bounds.separator >= bounds.start + 2 && end >= bounds.contentStart) {
            val languageCode = source.substring(bounds.start + 2, bounds.separator)
            val languageDescription = source.substring(bounds.contentStart, end)
            if (allLanguageCodes.contains(languageCode)) {
                descriptions[languageCode] = languageDescription
            }
        }
    }
}
