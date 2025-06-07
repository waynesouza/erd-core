package com.erd.core.service;

import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.erd.core.enumeration.RoleProjectEnum.OWNER;
import static com.erd.core.enumeration.RoleProjectEnum.VIEWER;

@Service
public class TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final UserService userService;

    public TeamService(TeamRepository teamRepository, UserService userService) {
        this.teamRepository = teamRepository;
        this.userService = userService;
    }

    public void create(String email, Project project) {
        logger.info("Creating team for user: {} and project: {}", email, project.getName());
        User user = userService.findByEmail(email);
        teamRepository.save(new Team(user, project, OWNER));
    }

    public void addTeamMember(TeamMemberRequestDTO teamMemberRequestDto, Project project) {
        logger.info("Adding team member to project: {}", project.getName());

        if (OWNER.equals(teamMemberRequestDto.getRoleProjectEnum())) {
            if (hasOwner(project.getId())) {
                throw new RuntimeException("There is already an OWNER for this project");
            }
        }

        User user = userService.findByEmail(teamMemberRequestDto.getUserEmail());
        teamRepository.save(new Team(user, project, teamMemberRequestDto.getRoleProjectEnum()));
    }

    public void updateTeamMember(UpdateTeamMemberRequestDTO requestDTO) {
        logger.info("Updating team member: {}", requestDTO.getUserId());

        Team team = teamRepository.findByUserIdAndProjectId(requestDTO.getUserId(), requestDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (OWNER.equals(requestDTO.getRole())) {
            if (hasOwner(requestDTO.getProjectId())) {
                throw new RuntimeException("There is already an OWNER for this project");
            }
        }

        team.setRole(requestDTO.getRole());
        teamRepository.save(team);
    }

    public void removeTeamMember(UUID userId, UUID projectId) {
        logger.info("Removing team member: {}", userId);

        Team team = teamRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (OWNER.equals(team.getRole())) {
            throw new RuntimeException("It is not possible to remove the OWNER of the project");
        }

        teamRepository.delete(team);
    }

    public List<UserProjectDetailsResponseDTO> findByProjectId(UUID projectId) {
        logger.info("Getting team members by project: {}", projectId);
        return teamRepository.findByProjectId(projectId);
    }

    public List<UserProjectDetailsResponseDTO> findByProjectIdAndRole(UUID projectId, RoleProjectEnum role) {
        logger.info("Getting team members by project: {} and role: {}", projectId, role);
        return teamRepository.findByProjectIdAndRole(projectId, role);
    }

    public List<UserProjectDetailsResponseDTO> findByProjectIdAndNameContaining(UUID projectId, String name) {
        logger.info("Getting team members by project: {} and name containing: {}", projectId, name);
        return teamRepository.findByProjectIdAndNameContaining(projectId, name);
    }

    public boolean isUserOwner(UUID userId, UUID projectId) {
        return teamRepository.existsByUserIdAndProjectIdAndRole(userId, projectId, OWNER);
    }

    public boolean canUserEdit(UUID userId, UUID projectId) {
        Team team = teamRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new RuntimeException("User is not a member of the project"));
        return !VIEWER.equals(team.getRole());
    }

    private boolean hasOwner(UUID projectId) {
        return teamRepository.existsByProjectIdAndRole(projectId, OWNER);
    }

}
