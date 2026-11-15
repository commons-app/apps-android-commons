package fr.free.nrw.commons.upload.structure.depictions

import depictedItem
import entity
import entityId
import fr.free.nrw.commons.wikidata.WikidataProperties
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import place
import snak
import statement
import valueString
import wikiBaseEntityValue

class DepictedItemTest {
    @Test
    fun `name and description get user language label`() {
        val depictedItem =
            DepictedItem(entity(mapOf("en" to "label"), mapOf("en" to "description")))
        Assert.assertEquals(depictedItem.name, "label")
        Assert.assertEquals(depictedItem.description, "description")
    }

    @Test
    fun `name and descriptions get first language label if user language not present`() {
        val depictedItem = DepictedItem(entity(mapOf("" to "label"), mapOf("" to "description")))
        Assert.assertEquals(depictedItem.name, "label")
        Assert.assertEquals(depictedItem.description, "description")
    }

    @Test
    fun `name and descriptions get empty if nothing present`() {
        val depictedItem = DepictedItem(entity())
        Assert.assertEquals(depictedItem.name, "")
        Assert.assertEquals(depictedItem.description, "")
    }

    @Test
    fun `image is empty with null statements`() {
        Assert.assertEquals(DepictedItem(entity(statements = null)).imageUrl, null)
    }

    @Test
    fun `image is empty with no image statement`() {
        Assert.assertEquals(DepictedItem(entity()).imageUrl, null)
    }

    @Test
    fun `image is empty with dataValue not of ValueString type`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to listOf(statement(snak(dataValue = mockk()))),
                        ),
                ),
            ).imageUrl,
            null,
        )
    }

    @Test
    fun `image is not empty with dataValue of ValueString type`() {
        val imageUrl =
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("prefix: example_"))),
                                ),
                        ),
                ),
            ).imageUrl
        Assert.assertNotNull(imageUrl)
        Assert.assertTrue(imageUrl!!.contains("_example_"))
        Assert.assertTrue(imageUrl.startsWith("https://commons.wikimedia.org/wiki/Special:FilePath/"))
    }

    @Test
    fun `image URL encodes reserved filename characters`() {
        val imageUrl =
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("File:Portrait #1?.jpg"))),
                                ),
                        ),
                ),
            ).imageUrl
        Assert.assertNotNull(imageUrl)
        // Wikimedia converts spaces to underscores, special chars get URL encoded
        Assert.assertTrue(imageUrl!!.contains("Portrait_%231%3F.jpg"))
        Assert.assertTrue(imageUrl.contains("width=120"))
    }

    @Test
    fun `instancesOf maps EntityIds to ids`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.INSTANCE_OF.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("prefix: example_"))),
                                    statement(snak(dataValue = entityId(wikiBaseEntityValue(id = "1")))),
                                    statement(snak(dataValue = entityId(wikiBaseEntityValue(id = "2")))),
                                ),
                        ),
                ),
            ).instanceOfs,
            listOf("1", "2"),
        )
    }

    @Test
    fun `instancesOf is empty with no values`() {
        Assert.assertEquals(DepictedItem(entity()).instanceOfs, emptyList<String>())
    }

    @Test
    fun `commonsCategory maps ValueString to strings`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.COMMONS_CATEGORY.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("1"))),
                                    statement(snak(dataValue = valueString("2"))),
                                ),
                        ),
                ),
            ).commonsCategories.map { it.name },
            listOf("1", "2"),
        )
    }

    @Test
    fun `commonsCategory is empty with no values`() {
        Assert.assertEquals(DepictedItem(entity()).commonsCategories, emptyList<String>())
    }

    @Test
    fun `isSelected is false at creation`() {
        Assert.assertEquals(DepictedItem(entity()).isSelected, false)
    }

    @Test
    fun `id is entityId`() {
        Assert.assertEquals(DepictedItem(entity(id = "1")).id, "1")
    }

    @Test
    fun `place constructor uses place name and longDescription`() {
        val depictedItem = DepictedItem(entity(), place(name = "1", longDescription = "2"))
        Assert.assertEquals(depictedItem.name, "1")
        Assert.assertEquals(depictedItem.description, "2")
    }

    @Test
    fun `same object is Equal`() {
        val depictedItem = depictedItem()
        Assert.assertEquals(depictedItem == depictedItem, true)
    }

    @Test
    fun `different type is not Equal`() {
        Assert.assertEquals(depictedItem().equals(Unit), false)
    }

    @Test
    fun `if names are equal is Equal`() {
        Assert.assertEquals(
            depictedItem(name = "a", id = "0") == depictedItem(name = "a", id = "1"),
            true,
        )
    }

    @Test
    fun `if names are not equal is not Equal`() {
        Assert.assertEquals(
            depictedItem(name = "a") == depictedItem(name = "b"),
            false,
        )
    }

    @Test
    fun `hashCode returns same values for objects with same name`() {
        Assert.assertEquals(depictedItem(name = "a").hashCode(), depictedItem(name = "a").hashCode())
    }

    @Test
    fun `hashCode returns different values for objects with different name`() {
        Assert.assertNotEquals(depictedItem(name = "a").hashCode(), depictedItem(name = "b").hashCode())
    }

    @Test
    fun `thumbnailUrl rebuilds URL from old 70px imageUrl`() {
        val item =
            DepictedItem(
                name = "test",
                description = "desc",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Example.jpg/70px-Example.jpg",
                instanceOfs = emptyList(),
                commonsCategories = emptyList(),
                isSelected = false,
                id = "Q1",
            )
        Assert.assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Example.jpg?width=120",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `thumbnailUrl handles filenames with hyphens correctly`() {
        val item =
            DepictedItem(
                name = "test",
                description = "desc",
                imageUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/rabbit-image-test.jpg?width=70",
                instanceOfs = emptyList(),
                commonsCategories = emptyList(),
                isSelected = false,
                id = "Q1",
            )
        Assert.assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/rabbit-image-test.jpg?width=120",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `thumbnailUrl adds png extension for SVG files`() {
        val item =
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("File:Pythagorean.svg"))),
                                ),
                        ),
                ),
            )
        Assert.assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Pythagorean.svg?width=120.png",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `thumbnailUrl does not add png extension for non-SVG files`() {
        val item =
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("File:Example.jpg"))),
                                ),
                        ),
                ),
            )
        Assert.assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Example.jpg?width=120",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `thumbnailUrl handles special characters in filename`() {
        val item =
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("File:Portrait #1?.jpg"))),
                                ),
                        ),
                ),
            )
        Assert.assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Portrait_%231%3F.jpg?width=120",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `thumbnailUrl is null when imageUrl is null`() {
        val item =
            DepictedItem(
                name = "test",
                description = "desc",
                imageUrl = null,
                instanceOfs = emptyList(),
                commonsCategories = emptyList(),
                isSelected = false,
                id = "Q1",
            )
        Assert.assertNull(item.thumbnailUrl)
    }

    @Test
    fun `primaryImage is derived correctly from imageUrl`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("prefix: example_image name"))),
                                ),
                        ),
                ),
            ).primaryImage,
            "_example_image_name",
        )
    }

    @Test
    fun `primaryImage keeps hyphens in filename`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements =
                        mapOf(
                            WikidataProperties.IMAGE.propertyName to
                                listOf(
                                    statement(snak(dataValue = valueString("File:rabbit-image.jpg"))),
                                ),
                        ),
                ),
            ).primaryImage,
            "rabbit-image.jpg",
        )
    }
}
