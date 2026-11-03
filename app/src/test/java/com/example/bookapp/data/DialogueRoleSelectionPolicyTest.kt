package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueRoleSelectionPolicyTest {
    @Test fun allowsTwoToFourDistinctRolesAndAtLeastTwoTurns() {
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 2), 2))
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 2, 3, 4), 8))
        assertFalse(DialogueRoleSelectionPolicy.isValid(listOf(1), 4))
        assertFalse(DialogueRoleSelectionPolicy.isValid(listOf(1, 2, 3, 4, 5), 8))
        assertFalse(DialogueRoleSelectionPolicy.isValid(listOf(1, 2), 1))
    }

    @Test fun preventsAddingFifthRoleButAllowsMoreSectionsFromSelectedRoles() {
        val selected = listOf(1L, 2L, 3L, 4L)
        assertTrue(DialogueRoleSelectionPolicy.canAddRole(2L, selected))
        assertFalse(DialogueRoleSelectionPolicy.canAddRole(5L, selected))
    }

    @Test fun ignoresInvalidAndDuplicateRoleIds() {
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 1, 2, 0, -1), 2))
    }
}
