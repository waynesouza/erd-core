# Graph Report - erd-core  (2026-08-30)

## Corpus Check
- Corpus is ~31,045 words - fits in a single context window. You may not need a graph.

## Summary
- 1128 nodes · 3789 edges · 44 communities (23 shown, 21 thin omitted)
- Extraction: 76% EXTRACTED · 24% INFERRED · 0% AMBIGUOUS · INFERRED: 922 edges (avg confidence: 0.81)
- Token cost: 11,949 input · 20,188 output

## Community Hubs (Navigation)
- Diagram Payloads and Mongo Storage
- JWT and WebSocket Authentication
- Project, Team and Token Entities
- Deployment, CI and Coverage Docs
- REST Controller Endpoints
- Authorization Rules and Enum Tests
- Link Data and Foreign Keys
- Entity Lock API and Notifications
- Project Creation and Mapping
- Project Detail Queries
- Collaboration Controller Tests
- User Identity and Roles
- Team and Project Repositories
- Column Items and DDL Types
- DDL Import Parsing Tests
- Test Harness Setup
- DDL Service Core
- Security Filter Chain and 401
- Refresh Token Error Handling
- Team Membership Model
- Project Service Ownership Tests
- STOMP Broker Configuration
- Login Request and Response
- Method Security Enforcement
- Disconnect Lock Release
- Node Layout and Geometry
- Team Service Role Rules
- DDL Coverage Edge Cases
- Authentication Beans and CORS
- WebSocket Handshake Cookie
- UserDetails Contract
- Add Team Member Flow
- User Entity Accessors
- Lock Registry Service Tests
- User Lookup and Signup
- ModelMapper and MVC CORS
- Signup Request Handling
- Project Update Request
- Team Member Update Request
- Project Controller Tests
- User Controller Tests
- Maven Wrapper Script
- Security Context Teardown
- Maven Project Descriptor

## God Nodes (most connected - your core abstractions)
1. `User` - 73 edges
2. `LinkDataDTO` - 40 edges
3. `NodeDataDTO` - 39 edges
4. `Project` - 39 edges
5. `UserProjectDetailsResponseDTO` - 38 edges
6. `DiagramDataResponseDTO` - 37 edges
7. `RoleProjectEnum` - 36 edges
8. `JwtService` - 36 edges
9. `ItemDTO` - 34 edges
10. `ProjectServiceTest` - 34 edges

## Surprising Connections (you probably didn't know these)
- `Cookie-Based Auth Contract for the Client` --cites--> `MvcConfig`  [INFERRED]
  docs/FRONTEND_COVERAGE_PLAYBOOK.md → src/main/java/com/erd/core/config/MvcConfig.java
- `doAnswer for Undeclared Checked Exceptions` --references--> `CollaborationController`  [EXTRACTED]
  docs/PROJECT_ANALYSIS.md → src/main/java/com/erd/core/controller/CollaborationController.java
- `Invalid Base64 Test JWT Secret` --references--> `JwtService`  [INFERRED]
  docs/PROJECT_ANALYSIS.md → src/main/java/com/erd/core/service/JwtService.java
- `mockStatic for Static Entry Points` --references--> `ProjectSecurityService`  [EXTRACTED]
  docs/PROJECT_ANALYSIS.md → src/main/java/com/erd/core/service/ProjectSecurityService.java
- `ERD Core Backend` --references--> `CoreApplication`  [INFERRED]
  README.md → src/main/java/com/erd/core/CoreApplication.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Real-Time Diagram Editing Flow** — src_main_java_com_erd_core_config_jwthandshakeinterceptor_jwthandshakeinterceptor, src_main_java_com_erd_core_config_websocketauthinterceptor_websocketauthinterceptor, src_main_java_com_erd_core_controller_websocketcontroller_websocketcontroller, src_main_java_com_erd_core_service_websocketservice_websocketservice, src_main_java_com_erd_core_mapper_diagrammapper_diagrammapper, src_main_java_com_erd_core_config_websocketconfig_websocketconfig, docs_project_analysis_realtime_diagram_editing [EXTRACTED 1.00]
- **Coverage Enforcement Chain** — docs_project_analysis_no_coverage_exclusions, docs_project_analysis_jacoco_bundle_gate, _github_workflows_tests_coverage_gate, _github_workflows_tests_jacoco_artifact, docs_frontend_coverage_playbook_istanbul_karma_coverage [EXTRACTED 1.00]
- **Cookie JWT Authentication Chain** — src_main_resources_application_jwt_properties, src_main_java_com_erd_core_service_jwtservice_jwtservice, src_main_java_com_erd_core_filter_jwtauthenticationfilter_jwtauthenticationfilter, src_main_java_com_erd_core_filter_authenticationentrypointjwt_authenticationentrypointjwt, src_main_java_com_erd_core_config_securityconfig_securityconfig, docs_frontend_coverage_playbook_cookie_auth_contract [EXTRACTED 1.00]

## Communities (44 total, 21 thin omitted)

### Community 0 - "Diagram Payloads and Mongo Storage"
Cohesion: 0.05
Nodes (21): com.fasterxml.jackson.core.type.TypeReference, org.springframework.data.mongodb.core.mapping.Document, org.springframework.data.mongodb.repository.MongoRepository, org.springframework.messaging.handler.annotation.MessageMapping, WebSocketController, CreateDiagramRequestDTO, DiagramDataRequestDTO, ExportDdlRequestDTO (+13 more)

### Community 1 - "JWT and WebSocket Authentication"
Cohesion: 0.05
Nodes (28): One Crafted Token per Exception Type, Invalid Base64 Test JWT Secret, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, javax.crypto.SecretKey, MockHttpServletRequest, org.springframework.http.ResponseCookie, org.springframework.messaging.Message (+20 more)

### Community 2 - "Project, Team and Token Entities"
Cohesion: 0.07
Nodes (24): Authentication and Token Refresh Flow, Bare RuntimeException Error Signalling, Project and Team Management, Request Authorization Flow, jakarta.persistence.Entity, jakarta.persistence.Table, org.modelmapper.ModelMapper, org.slf4j.Logger (+16 more)

### Community 3 - "Deployment, CI and Coverage Docs"
Cohesion: 0.05
Nodes (45): Docker Publish Workflow, GitHub Container Registry Target, Run Tests Workflow, CI Coverage Enforcement, JaCoCo Report Artifact Upload, erd-client Service (compose), erd-core Service (compose), Local MongoDB Container (+37 more)

### Community 4 - "REST Controller Endpoints"
Cohesion: 0.10
Nodes (15): org.springframework.http.ResponseEntity, org.springframework.security.access.prepost.PreAuthorize, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.PutMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController (+7 more)

### Community 5 - "Authorization Rules and Enum Tests"
Cohesion: 0.12
Nodes (4): org.junit.jupiter.api.Test, RoleEnumerationTest, RefreshTokenExceptionTest, ProjectSecurityServiceTest

### Community 7 - "Entity Lock API and Notifications"
Cohesion: 0.11
Nodes (7): org.springframework.messaging.simp.SimpMessagingTemplate, CollaborationController, GetMapping, PostMapping, RequestMapping, RestController, EntityLockDTO

### Community 8 - "Project Creation and Mapping"
Cohesion: 0.11
Nodes (4): ProjectCreateRequestDTO, ProjectMapper, Project, ProjectMapperTest

### Community 11 - "User Identity and Roles"
Cohesion: 0.13
Nodes (7): org.springframework.data.jpa.repository.JpaRepository, org.springframework.security.crypto.password.PasswordEncoder, UserResponseDTO, RoleEnum, ADMIN, USER, UserRepository

### Community 12 - "Team and Project Repositories"
Cohesion: 0.13
Nodes (4): org.springframework.data.jpa.repository.Query, org.springframework.stereotype.Repository, UserProjectDetailsResponseDTO, TeamRepository

### Community 15 - "Test Harness Setup"
Cohesion: 0.17
Nodes (11): org.junit.jupiter.api.BeforeEach, org.junit.jupiter.api.extension.ExtendWith, org.mockito.junit.jupiter.MockitoExtension, org.springframework.mock.web.MockHttpServletRequest, org.springframework.mock.web.MockHttpServletResponse, org.springframework.test.web.servlet.MockMvc, AuthenticationControllerTest, ObjectMapper (+3 more)

### Community 16 - "DDL Service Core"
Cohesion: 0.15
Nodes (5): com.fasterxml.jackson.databind.ObjectMapper, Two Unreachable Branches Removed, ExportDdlResponseDTO, DdlService, DdlControllerTest

### Community 17 - "Security Filter Chain and 401"
Cohesion: 0.14
Nodes (14): Cookie-Based Auth Contract for the Client, jakarta.servlet.http.HttpServletResponse, MockHttpServletResponse, org.springframework.security.authentication.AuthenticationProvider, org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.core.AuthenticationException, org.springframework.security.web.AuthenticationEntryPoint (+6 more)

### Community 18 - "Refresh Token Error Handling"
Cohesion: 0.15
Nodes (10): doAnswer for Undeclared Checked Exceptions, No Global Exception Handler, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.ResponseStatus, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.context.request.WebRequest, TokenControllerAdvice, ErrorMessageDTO (+2 more)

### Community 19 - "Team Membership Model"
Cohesion: 0.14
Nodes (3): RoleProjectEnum, EDITOR, Team

### Community 21 - "STOMP Broker Configuration"
Cohesion: 0.17
Nodes (10): Real-Time Diagram Editing, WebSocket Handshake Authentication, org.springframework.messaging.simp.config.ChannelRegistration, org.springframework.messaging.simp.config.MessageBrokerRegistry, org.springframework.scheduling.TaskScheduler, org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker, org.springframework.web.socket.config.annotation.StompEndpointRegistry, org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer (+2 more)

### Community 23 - "Method Security Enforcement"
Cohesion: 0.19
Nodes (8): Guards and Interceptor Parity, Hybrid Controller Testing, org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc, org.springframework.boot.test.context.SpringBootTest, org.springframework.security.test.context.support.WithAnonymousUser, org.springframework.security.test.context.support.WithMockUser, CoreApplicationTests, MethodSecurityRulesTest

### Community 24 - "Disconnect Lock Release"
Cohesion: 0.21
Nodes (8): java.security.Principal, org.springframework.context.event.EventListener, org.springframework.web.socket.messaging.SessionDisconnectEvent, WebSocketEventListener, DeleteMapping, CollaborationService, SessionDisconnectEvent, WebSocketEventListenerTest

### Community 28 - "Authentication Beans and CORS"
Cohesion: 0.24
Nodes (6): org.springframework.context.annotation.Bean, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration, org.springframework.web.cors.CorsConfigurationSource, ApplicationConfig, ConfigurationBeansTest

### Community 29 - "WebSocket Handshake Cookie"
Cohesion: 0.30
Nodes (7): org.springframework.http.server.ServerHttpRequest, org.springframework.http.server.ServerHttpResponse, org.springframework.web.socket.server.HandshakeInterceptor, org.springframework.web.socket.WebSocketHandler, Override, JwtHandshakeInterceptor, JwtHandshakeInterceptorTest

### Community 30 - "UserDetails Contract"
Cohesion: 0.21
Nodes (3): org.springframework.security.core.GrantedAuthority, Override, UserTest

### Community 35 - "ModelMapper and MVC CORS"
Cohesion: 0.21
Nodes (7): org.springframework.context.annotation.Configuration, org.springframework.web.servlet.config.annotation.CorsRegistry, org.springframework.web.servlet.config.annotation.WebMvcConfigurer, ModelMapper, MapperConfig, Override, MvcConfig

### Community 41 - "Maven Wrapper Script"
Cohesion: 0.70
Nodes (4): mvnw script, concat_lines(), find_maven_basedir(), log()

## Knowledge Gaps
- **8 isolated node(s):** `com.erd:core`, `ADMIN`, `USER`, `EDITOR`, `GitHub Container Registry Target` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 107 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `User` connect `User Entity Accessors` to `Diagram Payloads and Mongo Storage`, `JWT and WebSocket Authentication`, `Project, Team and Token Entities`, `User Lookup and Signup`, `Deployment, CI and Coverage Docs`, `Authorization Rules and Enum Tests`, `Signup Request Handling`, `Project Creation and Mapping`, `User Identity and Roles`, `Test Harness Setup`, `Security Filter Chain and 401`, `Team Membership Model`, `Team Service Role Rules`, `UserDetails Contract`, `Add Team Member Flow`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Why does `UserProjectDetailsResponseDTO` connect `Team and Project Repositories` to `Diagram Payloads and Mongo Storage`, `Project, Team and Token Entities`, `REST Controller Endpoints`, `Team Member Update Request`, `Project Controller Tests`, `Project Detail Queries`, `Team Membership Model`, `Project Service Ownership Tests`, `Add Team Member Flow`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Why does `Diagram` connect `Diagram Payloads and Mongo Storage` to `Deployment, CI and Coverage Docs`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **What connects `com.erd:core`, `ADMIN`, `USER` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Diagram Payloads and Mongo Storage` be split into smaller, more focused modules?**
  _Cohesion score 0.051831501831501835 - nodes in this community are weakly interconnected._
- **Should `JWT and WebSocket Authentication` be split into smaller, more focused modules?**
  _Cohesion score 0.05272727272727273 - nodes in this community are weakly interconnected._
- **Should `Project, Team and Token Entities` be split into smaller, more focused modules?**
  _Cohesion score 0.07364185110663984 - nodes in this community are weakly interconnected._