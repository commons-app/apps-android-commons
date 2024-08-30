package fr.free.nrw.commons.upload.structure.depictions

import com.nhaarman.mockitokotlin2.mock
import depictedItem
import entity
import entityId
import fr.free.nrw.commons.wikidata.WikidataProperties
import org.junit.Assert
import org.junit.Ignore
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
                    statements = mapOf(
                        WikidataProperties.IMAGE.propertyName to listOf(statement(snak(dataValue = mock())))
                    )
                )
            ).imageUrl,
            null
        )
    }

    @Test
    fun `image is not empty with dataValue of ValueString type`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements = mapOf(
                        WikidataProperties.IMAGE.propertyName to listOf(
                            statement(snak(dataValue = valueString("prefix: example_")))
                        )
                    )
                )
            ).imageUrl,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/_example_/70px-_example_")
    }

    @Test
    fun `instancesOf maps EntityIds to ids`() {
        Assert.assertEquals(
            DepictedItem(
                entity(
                    statements = mapOf(
                        WikidataProperties.INSTANCE_OF.propertyName to listOf(
                            statement(snak(dataValue = valueString("prefix: example_"))),
                            statement(snak(dataValue = entityId(wikiBaseEntityValue(id = "1")))),
                            statement(snak(dataValue = entityId(wikiBaseEntityValue(id = "2"))))
                        )
                    )
                )
            ).instanceOfs,
            listOf("1", "2"))
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
                    statements = mapOf(
                        WikidataProperties.COMMONS_CATEGORY.propertyName to listOf(
                            statement(snak(dataValue = valueString("1"))),
                            statement(snak(dataValue = valueString("2")))
                        )
                    )
                )
            ).commonsCategories.map { it.name },
            listOf("1", "2"))
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
            depictedItem(name="a", id = "0") == depictedItem(name="a", id = "1"),
            true)
    }

    @Test
    fun `if names are not equal is not Equal`() {
        Assert.assertEquals(
            depictedItem(name="a") == depictedItem(name="b"),
            false)
    }

    @Test
    fun `hashCode returns same values for objects with same name`() {
        Assert.assertEquals(depictedItem(name="a").hashCode(), depictedItem(name="a").hashCode())
    }
    
    @Test
    fun `hashCode returns different values for objects with different name`() {
        Assert.assertNotEquals(depictedItem(name="a").hashCode(), depictedItem(name="b").hashCode())
    }
}
