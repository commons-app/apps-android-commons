package fr.free.nrw.commons.description

import fr.free.nrw.commons.upload.UploadMediaDetail

/**
 * Replaces the Information template's description value.
 *
 * Nearby uploads put every language on its own line and then append
 * {{ on Wikidata|Qid}} immediately before |source. Replacing only the first
 * line leaves the previous description in place, so the edit looks unsaved.
 */
fun replaceDescriptionParameter(
    wikiText: String,
    details: List<UploadMediaDetail>,
): String {
    var descriptionIndex = wikiText.indexOf("description=")
    if (descriptionIndex == -1) {
        descriptionIndex = wikiText.indexOf("Description=")
    }
    if (descriptionIndex == -1) {
        return wikiText
    }

    val valueStart = descriptionIndex + "description=".length
    val valueEnd = endOfDescriptionValue(wikiText, valueStart)
    val wikidataTemplates = topLevelWikidataTemplates(wikiText.substring(valueStart, valueEnd))

    val updated = StringBuilder(wikiText.substring(0, valueStart))
    for (detail in details) {
        val text = detail.descriptionText
        val language = detail.languageCode
        if (!text.isNullOrEmpty() && !language.isNullOrEmpty()) {
            updated.append("{{").append(language).append("|1=").append(text).append("}}")
        }
    }
    if (wikidataTemplates.isNotEmpty()) {
        updated.append('\n')
        wikidataTemplates.forEach { updated.append(it) }
    }
    val rest = wikiText.substring(valueEnd)
    if (rest.startsWith("|") || rest.startsWith("}}")) {
        updated.append('\n')
    }
    updated.append(rest)
    return updated.toString().replace(", $".toRegex(), "")
}

/**
 * Index of the next Information-template parameter, or of the template's closing braces.
 * Pipes inside nested templates and links are not parameter boundaries.
 */
private fun endOfDescriptionValue(wikiText: String, start: Int): Int {
    var depth = 0
    var i = start
    while (i < wikiText.length) {
        if (wikiText.startsWith("{{", i)) {
            depth++
            i += 2
            continue
        }
        if (wikiText.startsWith("}}", i)) {
            if (depth == 0) {
                return i
            }
            depth--
            i += 2
            continue
        }
        if (wikiText[i] == '|' && depth == 0 && isParameterPipe(wikiText, i)) {
            return i
        }
        i++
    }
    return wikiText.length
}

private fun isParameterNameChar(char: Char): Boolean =
    char.isLetterOrDigit() || char == '_' || char == '-'

private fun isParameterPipe(wikiText: String, pipe: Int): Boolean {
    var i = pipe + 1
    while (i < wikiText.length && wikiText[i].isWhitespace()) {
        i++
    }
    val nameStart = i
    while (i < wikiText.length && isParameterNameChar(wikiText[i])) {
        i++
    }
    if (i == nameStart) {
        return false
    }
    while (i < wikiText.length && wikiText[i].isWhitespace()) {
        i++
    }
    return i < wikiText.length && wikiText[i] == '='
}

/**
 * Wikidata pointer templates the uploader inserted beside the language descriptions.
 * Templates nested inside a language description belong to that description text.
 */
private fun topLevelWikidataTemplates(value: String): List<String> {
    val templates = mutableListOf<String>()
    var depth = 0
    var i = 0
    while (i < value.length - 1) {
        if (value.startsWith("{{", i)) {
            if (depth == 0) {
                val close = value.indexOf("}}", i)
                if (close != -1) {
                    val template = value.substring(i, close + 2).trim()
                    if (WIKIDATA_TEMPLATE.matches(template)) {
                        templates.add(template)
                    }
                }
            }
            depth++
            i += 2
            continue
        }
        if (value.startsWith("}}", i)) {
            if (depth > 0) {
                depth--
            }
            i += 2
            continue
        }
        i++
    }
    return templates
}

private val WIKIDATA_TEMPLATE = Regex("""\{\{\s*[Oo]n Wikidata\|[^}]+\}\}""")
