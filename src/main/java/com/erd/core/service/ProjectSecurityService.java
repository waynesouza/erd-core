package com.erd.core.service;

import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.User;
import com.erd.core.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectSecurityService.class);

    private final TeamRepository teamRepository;
    private final UserService userService;

    public ProjectSecurityService(TeamRepository teamRepository, UserService userService) {
        this.teamRepository = teamRepository;
        this.userService = userService;
    }

    public boolean isUserOwnerOrMember(String projectId, String userEmail) {
        try {
            logger.info("Checking if user {} can access project {}", userEmail, projectId);
            UUID projectUuid = UUID.fromString(projectId);
            User currentUser = userService.findByEmail(userEmail);
            boolean hasAccess = teamRepository.existsByUserIdAndProjectId(currentUser.getId(), projectUuid);
            if (hasAccess) {
                logger.info("User {} has access to project {}", userEmail, projectId);
            } else {
                logger.warn("User {} has no access to project {}", userEmail, projectId);
            }
            return hasAccess;
        } catch (Exception e) {
            logger.error("Error checking project access for user {} and project {}", userEmail, projectId, e);
            return false;
        }
    }

    public boolean canUserAccessProject(String projectId) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
                return false;
            }
            return isUserOwnerOrMember(projectId, user.getEmail());
        } catch (Exception e) {
            logger.error("Error getting current user email", e);
            return false;
        }
    }

    public boolean isProjectOwner(String projectId, String userEmail) {
        try {
            UUID projectUuid = UUID.fromString(projectId);
            User currentUser = userService.findByEmail(userEmail);
            return teamRepository.existsByUserIdAndProjectIdAndRole(currentUser.getId(), projectUuid, RoleProjectEnum.OWNER);
        } catch (Exception e) {
            logger.error("Error checking project ownership", e);
            return false;
        }
    }

    public boolean canUserEditProject(String projectId, String userEmail) {
        try {
            UUID projectUuid = UUID.fromString(projectId);
            User currentUser = userService.findByEmail(userEmail);
            return teamRepository.existsByUserIdAndProjectIdAndRoleIn(currentUser.getId(), projectUuid,
                    java.util.List.of(RoleProjectEnum.OWNER, RoleProjectEnum.EDITOR));
        } catch (Exception e) {
            logger.error("Error checking project edit permission", e);
            return false;
        }
    }

}
