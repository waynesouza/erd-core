package com.erd.core.enumeration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins down the two role vocabularies. {@code values()} and {@code valueOf(String)} are compiler
 * generated, so they are exercised explicitly rather than assumed to be filtered out of coverage.
 */
class RoleEnumerationTest {

    @Test
    void testRoleEnum_declaresTheGlobalAuthorities() {
        // When & Then
        assertArrayEquals(new RoleEnum[]{RoleEnum.ADMIN, RoleEnum.USER}, RoleEnum.values());
        assertEquals(RoleEnum.ADMIN, RoleEnum.valueOf("ADMIN"));
        assertEquals(RoleEnum.USER, RoleEnum.valueOf("USER"));
    }

    @Test
    void testRoleEnum_rejectsAnUnknownName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> RoleEnum.valueOf("ROOT"));
    }

    @Test
    void testRoleProjectEnum_declaresTheProjectRoles() {
        // When & Then
        assertArrayEquals(
                new RoleProjectEnum[]{RoleProjectEnum.OWNER, RoleProjectEnum.EDITOR, RoleProjectEnum.VIEWER},
                RoleProjectEnum.values());
        assertEquals(RoleProjectEnum.OWNER, RoleProjectEnum.valueOf("OWNER"));
        assertEquals(RoleProjectEnum.EDITOR, RoleProjectEnum.valueOf("EDITOR"));
        assertEquals(RoleProjectEnum.VIEWER, RoleProjectEnum.valueOf("VIEWER"));
    }

    @Test
    void testRoleProjectEnum_rejectsAnUnknownName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> RoleProjectEnum.valueOf("GUEST"));
    }

}
