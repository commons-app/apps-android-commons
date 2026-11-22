package fr.free.nrw.commons.upload

/**
 * When several files in one upload have the same name, adds a number to each in upload order,
 * e.g. "Cross (48213-1).jpg", "Cross (48213-2).jpg". Other names are kept as they are.
 */
fun numberFilesWithSameName(
    fileNames: List<String>,
    batchNumber: Int,
): List<String> {
    val countByName = fileNames.groupingBy { it }.eachCount()
    val positionByName = mutableMapOf<String, Int>()
    return fileNames.map { fileName ->
        val count = countByName.getValue(fileName)
        if (count == 1) {
            return@map fileName
        }
        val position = (positionByName[fileName] ?: 0) + 1
        positionByName[fileName] = position
        val number = "$batchNumber-" + position.toString().padStart(count.toString().length, '0')
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) {
            "$fileName ($number)"
        } else {
            "${fileName.substring(0, dot)} ($number)${fileName.substring(dot)}"
        }
    }
}
