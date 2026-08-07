package com.erd.core.model.mongo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unlike the JPA entities, {@link Diagram} assigns its own identifier in the constructor rather
 * than delegating to the persistence provider, so both constructors are asserted explicitly.
 */
class DiagramTest {

    @Test
    void testNoArgsConstructor_generatesAnIdentifier() {
        // When
        Diagram diagram = new Diagram();

        // Then
        assertNotNull(diagram.getId(), "The no-args constructor must assign a UUID");
        assertNull(diagram.getNodeData());
        assertNull(diagram.getLinkData());
        assertNull(diagram.getProjectId());
    }

    @Test
    void testAllArgsConstructor_generatesAnIdentifierAndStoresTheData() {
        // When
        Diagram diagram = new Diagram("[{\"key\":\"users\"}]", "[{\"from\":\"a\"}]", "project-1");

        // Then
        assertNotNull(diagram.getId());
        assertEquals("[{\"key\":\"users\"}]", diagram.getNodeData());
        assertEquals("[{\"from\":\"a\"}]", diagram.getLinkData());
        assertEquals("project-1", diagram.getProjectId());
    }

    @Test
    void testEachInstance_receivesADistinctIdentifier() {
        // When
        Diagram first = new Diagram();
        Diagram second = new Diagram();

        // Then
        assertNotEquals(first.getId(), second.getId());
    }

}
