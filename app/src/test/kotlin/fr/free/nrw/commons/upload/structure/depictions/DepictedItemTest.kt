package fr.free.nrw.commons.upload.structure.depictions

import com.nhaarman.mockitokotlin2.mock
import depictedItem
import entity
import entityId
import fr.free.nrw.commons.wikidata.WikidataProperties
import org.junit.Test
import place
import snak
import statement
import valueString
import wikiBaseEntityValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not

class DepictedItemTest {

    @Test
    fun `name and description get user language label`() {
        val depictedItem =
            DepictedItem(entity(mapOf("en" to "label"), mapOf("en" to "description")))
        assertThat(depictedItem.name, equalTo( "label"))
        assertThat(depictedItem.description, equalTo( "description"))
    }

    @Test
    fun `name and descriptions get first language label if user language not present`() {
        val depictedItem = DepictedItem(entity(mapOf("" to "label"), mapOf("" to "description")))
        assertThat(depictedItem.name, equalTo( "label"))
        assertThat(depictedItem.description, equalTo( "description"))
    }

    @Test
    fun `name and descriptions get empty if nothing present`() {
        val depictedItem = DepictedItem(entity())
        assertThat(depictedItem.name, equalTo( ""))
        assertThat(depictedItem.description, equalTo( ""))
    }

    @Test
    fun `image is empty with null statements`() {
        assertThat(DepictedItem(entity(statements = null)).imageUrl, equalTo( null))
    }

    @Test
    fun `image is empty with no image statement`() {
        assertThat(DepictedItem(entity()).imageUrl, equalTo( null))
    }

    @Test
    fun `image is empty with dataValue not of ValueString type`() {
        assertThat(
            DepictedItem(
                entity(
                    statements = mapOf(
                        WikidataProperties.IMAGE.propertyName to listOf(statement(snak(dataValue = mock())))
                    )
                )
            ).imageUrl,
            equalTo(null)
        )
    }

    @Test
    fun `image is not empty with dataValue of ValueString type`() {
        assertThat(
            DepictedItem(
                entity(
                    statements = mapOf(
                        WikidataProperties.IMAGE.propertyName to listOf(
                            statement(snak(dataValue = valueString("prefix: example_")))
                        )
                    )
                )
            ).imageUrl,
            equalTo("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/_example_/70px-_example_"))
    }

    @Test
    fun `instancesOf maps EntityIds to ids`() {
        assertThat(
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
            equalTo(listOf("1", "2")))
    }

    @Test
    fun `instancesOf is empty with no values`() {
        assertThat(DepictedItem(entity()).instanceOfs, equalTo( emptyList<String>()))
    }

    @Test
    fun `commonsCategory maps ValueString to strings`() {
        assertThat(
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
            equalTo(listOf("1", "2")))
    }

    @Test
    fun `commonsCategory is empty with no values`() {
        assertThat(DepictedItem(entity()).commonsCategories, equalTo( emptyList<String>()))
    }

    @Test
    fun `isSelected is false at creation`() {
        assertThat(DepictedItem(entity()).isSelected, equalTo( false))
    }

    @Test
    fun `id is entityId`() {
        assertThat(DepictedItem(entity(id = "1")).id, equalTo( "1"))
    }

    @Test
    fun `place constructor uses place name and longDescription`() {
        val depictedItem = DepictedItem(entity(), place(name = "1", longDescription = "2"))
        assertThat(depictedItem.name, equalTo( "1"))
        assertThat(depictedItem.description, equalTo( "2"))
    }


    @Test
    fun `same object is Equal`() {
        val depictedItem = depictedItem()
        assertThat(depictedItem == depictedItem, equalTo( true))
    }

    @Test
    fun `different type is not Equal`() {
        assertThat(depictedItem().equals(Unit), equalTo( false))
    }

    @Test
    fun `if names are equal is Equal`() {
        assertThat(
            depictedItem(name="a", id = "0") == depictedItem(name="a", id = "1"),
            equalTo(true))
    }

    @Test
    fun `if names are not equal is not Equal`() {
        assertThat(
            depictedItem(name="a") == depictedItem(name="b"),
            equalTo(false))
    }

    @Test
    fun `hashCode returns same values for objects with same name`() {
        assertThat(depictedItem(name="a").hashCode(), equalTo( depictedItem(name="a").hashCode()))
    }
    
    @Test
    fun `hashCode returns different values for objects with different name`() {
        assertThat(depictedItem(name="a").hashCode(), not(equalTo(depictedItem(name="b").hashCode())))
    }
}
