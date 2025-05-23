package com.erd.core.service;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.model.Project;
import com.erd.core.repository.ProjectRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.erd.core.mapper.ProjectMapper.toEntity;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final TeamService teamService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public ProjectService(ProjectRepository projectRepository, TeamService teamService, UserService userService, ModelMapper modelMapper) {
        this.projectRepository = projectRepository;
        this.teamService = teamService;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    public ProjectResponseDTO create(ProjectCreateRequestDTO projectCreateRequestDto) {
        logger.info("Saving project data");
        Project createdProject = projectRepository.save(toEntity(projectCreateRequestDto));
        teamService.create(projectCreateRequestDto.getUserEmail(), createdProject);
        return modelMapper.map(createdProject, ProjectResponseDTO.class);
    }

    public List<ProjectResponseDTO> getProjectsByUserEmail(String email) {
        logger.info("Getting projects by userEmail: {}", email);
        List<Project> projects = projectRepository.findByUserEmail(email);
        return modelMapper.map(projects, new TypeToken<List<ProjectResponseDTO>>() {}.getType());
    }

    public ProjectDetailsResponseDTO getProjectDetailsById(UUID id) {
        logger.info("Checking if project exist");
        if (!isProjectExist(id)) {
            throw new RuntimeException("Project not found for id: " + id);
        }

        logger.info("Getting project details by id: {}", id);
        ProjectDetailsResponseDTO responseDto = projectRepository.findProjectDetailsById(id);
        responseDto.setUsersDto(teamService.findByProjectId(id));

        return responseDto;
    }

    public ProjectResponseDTO update(ProjectUpdateRequestDTO projectUpdateRequestDto) {
        logger.info("Checking if project exist");
        if (!isProjectExist(projectUpdateRequestDto.getId())) {
            throw new RuntimeException("Project not found for id: " + projectUpdateRequestDto.getId());
        }

        var userId = userService.getUserIdByLoggedUserEmail();
        if (teamService.isUserOwner(userId, projectUpdateRequestDto.getId())) {
            throw new RuntimeException("Only the OWNER can update the project");
        }

        logger.info("Updating project data");
        Project updatedProject = projectRepository.save(modelMapper.map(projectUpdateRequestDto, Project.class));
        return modelMapper.map(updatedProject, ProjectResponseDTO.class);
    }

    public void addTeamMember(TeamMemberRequestDTO teamMemberRequestDto) {
        UUID projectId = teamMemberRequestDto.getProjectId();
        logger.info("Finding project by id: {}", projectId);

        var userId = userService.getUserIdByLoggedUserEmail();
        if (teamService.isUserOwner(userId, projectId)) {
            throw new RuntimeException("Only the OWNER can add team members to the project");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found for id: " + projectId));
        teamService.addTeamMember(teamMemberRequestDto, project);
    }

    public void updateTeamMember(UpdateTeamMemberRequestDTO requestDTO) {
        var userId = userService.getUserIdByLoggedUserEmail();
        if (teamService.isUserOwner(userId, requestDTO.getProjectId())) {
            throw new RuntimeException("Only the OWNER can update team members");
        }

        teamService.updateTeamMember(requestDTO);
    }

    public void removeTeamMember(UUID memberId, UUID projectId) {
        var userId = userService.getUserIdByLoggedUserEmail();
        if (teamService.isUserOwner(userId, projectId)) {
            throw new RuntimeException("Only the OWNER can remove team members");
        }

        teamService.removeTeamMember(memberId, projectId);
    }

    public void deleteById(UUID id) {
        logger.info("Deleting project by id: {}", id);

        var userId = userService.getUserIdByLoggedUserEmail();
        if (teamService.isUserOwner(userId, id)) {
            throw new RuntimeException("Only the OWNER can delete the project");
        }

        projectRepository.deleteById(id);
        // TODO: delete diagram data
    }

    private Boolean isProjectExist(UUID id) {
        return projectRepository.existsById(id);
    }

}
