package com.erd.core.service;

import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.model.User;
import com.erd.core.repository.ProjectRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ProjectSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectSecurityService.class);

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public ProjectSecurityService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public boolean isUserOwnerOrMember(String projectId, String userEmail) {
        try {
            logger.info("Checking if user {} can access project {}", userEmail, projectId);
            
            UUID projectUuid = UUID.fromString(projectId);
            Project project = projectRepository.findById(projectUuid).orElse(null);
            
            if (Objects.isNull(project)) {
                logger.warn("Project {} not found", projectId);
                return false;
            }

            User currentUser = userService.findByEmail(userEmail);
            
            boolean hasAccessToProject = project.getTeams().stream()
                    .anyMatch(team -> team.getUser().getId().equals(currentUser.getId()));
            
            if (hasAccessToProject) {
                logger.info("User {} has access to project {}", userEmail, projectId);
                return true;
            }

            logger.warn("User {} has no access to project {}", userEmail, projectId);
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking project access for user {} and project {}", userEmail, projectId, e);
            return false;
        }
    }

    public boolean canUserAccessProject(String projectId) {
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return false;
        }
        return isUserOwnerOrMember(projectId, userEmail);
    }

    private String getCurrentUserEmail() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                return user.getEmail();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting current user email", e);
            return null;
        }
    }

    public boolean isProjectOwner(String projectId, String userEmail) {
        try {
            UUID projectUuid = UUID.fromString(projectId);
            Project project = projectRepository.findById(projectUuid).orElse(null);
            
            if (project == null) {
                return false;
            }

            User currentUser = userService.findByEmail(userEmail);
            
            return project.getTeams().stream()
                    .anyMatch(team -> team.getUser().getId().equals(currentUser.getId()) 
                            && team.getRole() == RoleProjectEnum.OWNER);
            
        } catch (Exception e) {
            logger.error("Error checking project ownership", e);
            return false;
        }
    }

    public boolean canUserEditProject(String projectId, String userEmail) {
        try {
            UUID projectUuid = UUID.fromString(projectId);
            Project project = projectRepository.findById(projectUuid).orElse(null);
            
            if (project == null) {
                return false;
            }

            User currentUser = userService.findByEmail(userEmail);
            
            return project.getTeams().stream()
                    .anyMatch(team -> team.getUser().getId().equals(currentUser.getId()) 
                            && (team.getRole() == RoleProjectEnum.OWNER || team.getRole() == RoleProjectEnum.EDITOR));
            
        } catch (Exception e) {
            logger.error("Error checking project edit permission", e);
            return false;
        }
    }

}
