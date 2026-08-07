package com.erd.core.model;

import com.erd.core.enumeration.RoleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the behaviour {@link User} adds on top of plain field access as a Spring Security
 * {@code UserDetails} implementation. The accessors themselves are covered by
 * {@link com.erd.core.PojoContractTest}.
 */
class UserTest {

    @Test
    void testGetAuthorities_returnsTheConfiguredRole() {
        // Given
        User user = new User("Ada", "Lovelace", "ada@erd.com", "secret", RoleEnum.ADMIN);

        // When
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Then
        assertEquals(List.of("ADMIN"), authorities);
    }

    @Test
    void testGetAuthorities_defaultsToUserWhenRoleIsNull() {
        // Given
        User user = new User("Ada", "Lovelace", "ada@erd.com", "secret", null);

        // When
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Then
        assertEquals(List.of("USER"), authorities,
                "A user without an explicit role must fall back to the USER authority");
    }

    @Test
    void testGetUsername_returnsTheEmail() {
        // Given
        User user = new User("Ada", "Lovelace", "ada@erd.com", "secret", RoleEnum.USER);

        // When & Then
        assertEquals("ada@erd.com", user.getUsername());
        assertEquals("secret", user.getPassword());
    }

    @Test
    void testAccountFlags_areAlwaysEnabled() {
        // Given
        User user = new User();

        // When & Then
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void testAllArgsConstructor_populatesEveryField() {
        // Given
        User user = new User("Ada", "Lovelace", "ada@erd.com", "secret", RoleEnum.ADMIN);

        // When & Then
        assertEquals("Ada", user.getFirstName());
        assertEquals("Lovelace", user.getLastName());
        assertEquals("ada@erd.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(RoleEnum.ADMIN, user.getRole());
    }

}
