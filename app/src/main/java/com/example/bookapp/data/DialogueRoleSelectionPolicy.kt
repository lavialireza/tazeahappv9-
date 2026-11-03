package com.example.bookapp.data

/** Rules for constructing a multi-role rehearsal dialogue without changing source sections. */
object DialogueRoleSelectionPolicy {
    fun distinctRoleCount(roleIds: Iterable<Long>): Int = roleIds.filter { it > 0L }.distinct().size

    fun canAddRole(roleId: Long, selectedRoleIds: Iterable<Long>, maxRoles: Int = 4): Boolean {
        require(maxRoles in 2..4) { "maxRoles must be between 2 and 4" }
        if (roleId <= 0L) return false
        val selected = selectedRoleIds.filter { it > 0L }.toSet()
        return roleId in selected || selected.size < maxRoles
    }

    fun isValid(roleIds: Iterable<Long>, turnCount: Int): Boolean {
        val count = distinctRoleCount(roleIds)
        return turnCount >= 2 && count in 2..4
    }
}
