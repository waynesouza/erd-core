package com.erd.core.service;

import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.model.Project;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.erd.core.enumeration.RoleProjectEnum.OWNER;

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
        User user = userService.findByEmail(teamMemberRequestDto.getUserEmail());
        teamRepository.save(new Team(user, project, teamMemberRequestDto.getRoleProjectEnum()));
    }

    // TODO: Add method to remove team member from project

}
