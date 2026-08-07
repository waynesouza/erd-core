package com.erd.core.mapper;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.model.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMapperTest {

    @Test
    void testToEntity_copiesNameAndDescription() {
        // Given
        ProjectCreateRequestDTO requestDto =
                new ProjectCreateRequestDTO("Sales ERD", "Model for the sales domain", "owner@erd.com");

        // When
        Project project = ProjectMapper.toEntity(requestDto);

        // Then
        assertEquals("Sales ERD", project.getName());
        assertEquals("Model for the sales domain", project.getDescription());
    }

    @Test
    void testToEntity_initialisesCreatedAtAndAnEmptyTeamList() {
        // Given
        ProjectCreateRequestDTO requestDto = new ProjectCreateRequestDTO("ERD", "desc", "owner@erd.com");

        // When
        Project project = ProjectMapper.toEntity(requestDto);

        // Then
        assertNotNull(project.getCreatedAt(), "The entity constructor stamps the creation time");
        assertNotNull(project.getTeams());
        assertTrue(project.getTeams().isEmpty(), "A brand new project starts without team members");
    }

    @Test
    void testConstructor_isInvokable() {
        // ProjectMapper is a static utility class that still carries an implicit public constructor.
        assertNotNull(new ProjectMapper());
    }

}
