package com.erd.core.service;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.model.Project;
import com.erd.core.repository.ProjectRepository;
import org.modelmapper.ModelMapper;
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
    private final DiagramService diagramService;
    private final ModelMapper modelMapper;

    public ProjectService(ProjectRepository projectRepository, TeamService teamService, UserService userService, DiagramService diagramService, ModelMapper modelMapper) {
        this.projectRepository = projectRepository;
        this.teamService = teamService;
        this.userService = userService;
        this.diagramService = diagramService;
        this.modelMapper = modelMapper;
    }

    public ProjectResponseDTO create(ProjectCreateRequestDTO projectCreateRequestDto) {
        logger.info("Saving project data");
        Project createdProject = projectRepository.save(toEntity(projectCreateRequestDto));
        
        // Create team with project owner
        teamService.create(projectCreateRequestDto.getUserEmail(), createdProject);
        
        // Create empty diagram
        logger.info("Creating empty diagram for project: {}", createdProject.getId());
        createEmptyDiagram(createdProject.getId().toString());
        
        return modelMapper.map(createdProject, ProjectResponseDTO.class);
    }
    
    private void createEmptyDiagram(String projectId) {
        try {
            diagramService.saveOrUpdateDiagram(projectId, "[]", "[]");
            logger.info("Empty diagram created successfully for project: {}", projectId);
        } catch (Exception e) {
            logger.error("Failed to create empty diagram for project: {}. Error: {}", projectId, e.getMessage());
            // Don't throw exception to avoid failing project creation if diagram creation fails
        }
    }

    /**
     * Retrieves all projects associated with a user's email address.
     * This method fetches the projects and their associated team members.
     *
     * @param email the email address of the user
     * @return a list of project details including team members
     * @throws IllegalArgumentException if the email is null or empty
     * @throws RuntimeException if no projects are found for the user
     */
    public List<ProjectDetailsResponseDTO> getProjectsByUserEmail(String email) {
        logger.info("Getting projects by userEmail: {}", email);

        if (email == null || email.trim().isEmpty()) {
            logger.error("Invalid email provided: {}", email);
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        List<Project> projects = projectRepository.findByUserEmail(email);

        if (projects.isEmpty()) {
            logger.warn("No projects found for user with email: {}", email);
            throw new RuntimeException("No projects found for user with email: " + email);
        }

        return projects.stream()
                .map(project -> {
                    ProjectDetailsResponseDTO responseDto = modelMapper.map(project, ProjectDetailsResponseDTO.class);
                    responseDto.setUsersDto(teamService.findByProjectId(project.getId()));
                    return responseDto;
                })
                .toList();
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
        if (!teamService.isUserOwner(userId, projectUpdateRequestDto.getId())) {
            throw new RuntimeException("Only the OWNER can update the project");
        }

        logger.info("Updating project data");
        Project updatedProject = projectRepository.save(modelMapper.map(projectUpdateRequestDto, Project.class));
        return modelMapper.map(updatedProject, ProjectResponseDTO.class);
    }

    public UserProjectDetailsResponseDTO addTeamMember(TeamMemberRequestDTO teamMemberRequestDto) {
        UUID projectId = teamMemberRequestDto.getProjectId();
        logger.info("Finding project by id: {}", projectId);

        var userId = userService.getUserIdByLoggedUserEmail();
        if (!teamService.isUserOwner(userId, projectId)) {
            throw new RuntimeException("Only the OWNER can add team members to the project");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found for id: " + projectId));
        
        return teamService.addTeamMember(teamMemberRequestDto, project);
    }

    public UserProjectDetailsResponseDTO updateTeamMember(UpdateTeamMemberRequestDTO requestDTO) {
        var userId = userService.getUserIdByLoggedUserEmail();
        if (!teamService.isUserOwner(userId, requestDTO.getProjectId())) {
            throw new RuntimeException("Only the OWNER can update team members");
        }

        return teamService.updateTeamMember(requestDTO);
    }

    public void removeTeamMember(UUID memberId, UUID projectId) {
        var userId = userService.getUserIdByLoggedUserEmail();
        if (!teamService.isUserOwner(userId, projectId)) {
            throw new RuntimeException("Only the OWNER can remove team members");
        }

        teamService.removeTeamMember(memberId, projectId);
    }

    public void deleteById(UUID id) {
        logger.info("Deleting project by id: {}", id);

        var userId = userService.getUserIdByLoggedUserEmail();
        if (!teamService.isUserOwner(userId, id)) {
            throw new RuntimeException("Only the OWNER can delete the project");
        }

        try {
            // Delete diagram data first (foreign key dependency)
            logger.info("Deleting diagram data for project: {}", id);
            diagramService.deleteDiagramByProjectId(id.toString());
            
            // Delete the project (this will cascade delete team members via JPA)
            logger.info("Deleting project entity: {}", id);
            projectRepository.deleteById(id);
            
            logger.info("Project and all associated data successfully deleted: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting project: {}. Error: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete project and associated data", e);
        }
    }

    private Boolean isProjectExist(UUID id) {
        return projectRepository.existsById(id);
    }

}
