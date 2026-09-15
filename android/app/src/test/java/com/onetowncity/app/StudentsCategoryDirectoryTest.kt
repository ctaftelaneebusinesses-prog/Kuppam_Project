package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Students hub replaced one dense grid with grouped sections
 * (studentsCategoryGroups) — this guards the one way that redesign could
 * silently regress: a category quietly missing from every group (unreachable
 * from the redesigned directory) or listed twice.
 */
class StudentsCategoryDirectoryTest {

    @Test
    fun `every student category appears in exactly one group`() {
        val flattened = studentsCategoryGroups.flatMap { it.categories }

        assertEquals(studentsCategories.toSet(), flattened.toSet())
        assertEquals(flattened.size, flattened.toSet().size)
    }

    @Test
    fun `every group has a non-blank title and at least one category`() {
        studentsCategoryGroups.forEach { group ->
            assertTrue("Group title must not be blank", group.title.isNotBlank())
            assertTrue("Group '${group.title}' must contain at least one category", group.categories.isNotEmpty())
        }
    }
}
