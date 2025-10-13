package resources

import org.junit.Test
import org.junit.Assert.assertTrue
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsDocumentationTest {

    @Test
    fun `string resources and qq documentation are in sync`() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val sourceFile = File(projectRoot, "src/main/res/values/strings.xml")
        val docFile = File(projectRoot, "src/main/res/values-qq/strings.xml")

        assertTrue("Source strings.xml not found", sourceFile.exists())
        assertTrue("Documentation strings.xml (qq) not found", docFile.exists())

        val sourceKeys = parseStringKeys(sourceFile)
        val docKeys = parseStringKeys(docFile)

        val extraDocs = docKeys - sourceKeys

        assertTrue(
            "Documentation exists for ${extraDocs.size} non-existent string(s): " +
                    extraDocs.joinToString(", "),
            extraDocs.isEmpty()
        )

        val missingDocs = sourceKeys - docKeys

        assertTrue(
            "Missing documentation for ${missingDocs.size} string(s): " +
                    missingDocs.joinToString(", "),
            missingDocs.isEmpty()
        )
    }

    private fun parseStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)

        val keys = mutableSetOf<String>()

        listOf("string", "plurals").forEach { tagName ->
            val elements = doc.getElementsByTagName(tagName)
            for (i in 0 until elements.length) {
                val element = elements.item(i) as Element
                val name = element.getAttribute("name")
                if (name.isNotEmpty()) {
                    keys.add(name)
                }
            }
        }

        return keys
    }
}
