package com.erd.core.mapper;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.model.Project;

public class ProjectMapper {

    public static Project toEntity(ProjectCreateRequestDTO projectCreateRequestDTO) {
        return new Project(projectCreateRequestDTO.getName(), projectCreateRequestDTO.getDescription());
    }

}
