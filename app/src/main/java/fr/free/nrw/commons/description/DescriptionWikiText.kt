package fr.free.nrw.commons.description

import fr.free.nrw.commons.upload.UploadMediaDetail

private val wikidataTemplate = Regex("""\{\{\s*[Oo]n Wikidata\|[^}]+\}\}""")

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
    val lowerCaseIndex = wikiText.indexOf("description=")
    val descriptionIndex =
        if (lowerCaseIndex == -1) wikiText.indexOf("Description=") else lowerCaseIndex
    if (descriptionIndex == -1) {
        return wikiText
    }

    val valueStart = descriptionIndex + "description=".length
    val valueEnd = endOfDescriptionValue(wikiText, valueStart)
    val updated = StringBuilder(wikiText.substring(0, valueStart))
    for (detail in details) {
        appendOneDescription(updated, detail)
    }
    appendSavedWikidataTemplates(updated, wikiText.substring(valueStart, valueEnd))
    val rest = wikiText.substring(valueEnd)
    if (rest.startsWith("|") || rest.startsWith("}}")) {
        updated.append('\n')
    }
    updated.append(rest)
    return updated.toString().replace(", $".toRegex(), "")
}

private fun appendOneDescription(
    updated: StringBuilder,
    detail: UploadMediaDetail,
) {
    val text = detail.descriptionText
    val language = detail.languageCode
    if (text.isNullOrEmpty() || language.isNullOrEmpty()) {
        return
    }
    updated
        .append("{{")
        .append(language)
        .append("|1=")
        .append(text)
        .append("}}")
}

private fun appendSavedWikidataTemplates(
    updated: StringBuilder,
    oldValue: String,
) {
    val templates = topLevelWikidataTemplates(oldValue)
    if (templates.isEmpty()) {
        return
    }
    updated.append('\n')
    templates.forEach { updated.append(it) }
}

/**
 * Index of the next Information-template parameter, or of the template's closing braces.
 * Pipes inside nested templates and links are not parameter boundaries.
 */
private fun endOfDescriptionValue(
    wikiText: String,
    start: Int,
): Int {
    var depth = 0
    var end = wikiText.length
    var index = start
    while (index < end) {
        val step = descriptionBoundary(wikiText, index, depth)
        depth = step.depth
        end = step.endIndex ?: end
        index += step.advance
    }
    return end
}

private fun descriptionBoundary(
    wikiText: String,
    index: Int,
    depth: Int,
): ScanStep =
    when {
        wikiText.startsWith("{{", index) -> ScanStep(depth + 1, advance = 2)
        wikiText.startsWith("}}", index) && depth == 0 ->
            ScanStep(depth, advance = 2, endIndex = index)
        wikiText.startsWith("}}", index) -> ScanStep(depth - 1, advance = 2)
        wikiText[index] == '|' && depth == 0 && isParameterPipe(wikiText, index) ->
            ScanStep(depth, advance = 1, endIndex = index)
        else -> ScanStep(depth, advance = 1)
    }

private fun isParameterNameChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_' || char == '-'

private fun isParameterPipe(
    wikiText: String,
    pipe: Int,
): Boolean {
    var index = pipe + 1
    while (index < wikiText.length && wikiText[index].isWhitespace()) {
        index++
    }
    val nameStart = index
    while (index < wikiText.length && isParameterNameChar(wikiText[index])) {
        index++
    }
    val hasName = index != nameStart
    while (index < wikiText.length && wikiText[index].isWhitespace()) {
        index++
    }
    val followedByEquals = index < wikiText.length && wikiText[index] == '='
    return hasName && followedByEquals
}

/**
 * Wikidata pointer templates the uploader inserted beside the language descriptions.
 * Templates nested inside a language description belong to that description text.
 */
private fun topLevelWikidataTemplates(value: String): List<String> {
    val templates = mutableListOf<String>()
    var depth = 0
    var index = 0
    while (index < value.length - 1) {
        val step = wikidataBoundary(value, index, depth, templates)
        depth = step.depth
        index += step.advance
    }
    return templates
}

private fun wikidataBoundary(
    value: String,
    index: Int,
    depth: Int,
    templates: MutableList<String>,
): ScanStep {
    val opening = value.startsWith("{{", index)
    val closing = value.startsWith("}}", index)
    if (opening && depth == 0) {
        addTopLevelWikidataTemplate(value, index, templates)
    }
    val nextDepth =
        when {
            opening -> depth + 1
            closing && depth > 0 -> depth - 1
            else -> depth
        }
    val advance = if (opening || closing) 2 else 1
    return ScanStep(nextDepth, advance)
}

private fun addTopLevelWikidataTemplate(
    value: String,
    index: Int,
    templates: MutableList<String>,
) {
    val close = value.indexOf("}}", index)
    if (close == -1) {
        return
    }
    val template = value.substring(index, close + 2).trim()
    if (wikidataTemplate.matches(template)) {
        templates.add(template)
    }
}

private data class ScanStep(
    val depth: Int,
    val advance: Int,
    val endIndex: Int? = null,
)
