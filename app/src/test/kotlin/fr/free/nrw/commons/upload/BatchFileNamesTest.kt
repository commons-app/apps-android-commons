package fr.free.nrw.commons.upload

import org.junit.Assert.assertEquals
import org.junit.Test

class BatchFileNamesTest {
    @Test
    fun `files with the same name are numbered in upload order`() {
        val fileNames = listOf("Cross.jpg", "Cross.jpg", "Cross.jpg")

        assertEquals(
            listOf("Cross (48213-1).jpg", "Cross (48213-2).jpg", "Cross (48213-3).jpg"),
            numberFilesWithSameName(fileNames, 48213),
        )
    }

    @Test
    fun `files with different names are kept as they are`() {
        val fileNames = listOf("Cross.jpg", "Tree.jpg")

        assertEquals(fileNames, numberFilesWithSameName(fileNames, 48213))
    }

    @Test
    fun `only the files that share a name are numbered`() {
        val fileNames = listOf("Cross.jpg", "Cross.jpg", "Tree.jpg", "Cross.jpg")

        assertEquals(
            listOf("Cross (48213-1).jpg", "Cross (48213-2).jpg", "Tree.jpg", "Cross (48213-3).jpg"),
            numberFilesWithSameName(fileNames, 48213),
        )
    }

    @Test
    fun `the position has as many digits as the number of files`() {
        val fileNames = List(12) { "Cross.jpg" }

        val numbered = numberFilesWithSameName(fileNames, 48213)

        assertEquals("Cross (48213-01).jpg", numbered.first())
        assertEquals("Cross (48213-12).jpg", numbered.last())
    }

    @Test
    fun `the number goes before the last dot`() {
        val fileNames = listOf("St. Mary.jpg", "St. Mary.jpg")

        assertEquals(
            listOf("St. Mary (48213-1).jpg", "St. Mary (48213-2).jpg"),
            numberFilesWithSameName(fileNames, 48213),
        )
    }
}
