package com.erd.core.integration;

import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.enumeration.RoleEnum;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.ProjectRepository;
import com.erd.core.repository.TeamRepository;
import com.erd.core.repository.UserRepository;
import com.erd.core.repository.mongo.DiagramRepository;
import com.erd.core.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the real-time channel end to end: a real STOMP client connects over SockJS to the embedded
 * server, the JWT travels in the handshake cookie, and the project roles come from the JPA
 * repositories (H2) through the real {@code ProjectSecurityService}.
 * <p>
 * The unit tests replace the permission service and the broker, which is why they missed two
 * defects this class covers: the permission check failing outside an HTTP request (lazy
 * {@code Project.teams} without a session) and every update being broadcast to
 * {@code /topic/diagram/null}. Only the MongoDB repository is replaced, because CI has no MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealTimeChannelIntegrationTest {

    private static final long SILENCE_MS = 1500;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DiagramRepository diagramRepository;

    private final List<StompSession> sessions = new ArrayList<>();
    private WebSocketStompClient stompClient;
    private Project project;
    private User owner;
    private User editor;
    private User viewer;
    private User outsider;

    @BeforeEach
    void setUp() {
        String run = UUID.randomUUID().toString();
        owner = userRepository.save(new User("Ada", "Owner", "owner-" + run + "@erd.com", "x", RoleEnum.USER));
        editor = userRepository.save(new User("Grace", "Editor", "editor-" + run + "@erd.com", "x", RoleEnum.USER));
        viewer = userRepository.save(new User("Alan", "Viewer", "viewer-" + run + "@erd.com", "x", RoleEnum.USER));
        outsider = userRepository.save(new User("Eve", "Outsider", "outsider-" + run + "@erd.com", "x", RoleEnum.USER));

        project = projectRepository.save(new Project("Sales ERD", "integration"));
        teamRepository.save(new Team(owner, project, RoleProjectEnum.OWNER));
        teamRepository.save(new Team(editor, project, RoleProjectEnum.EDITOR));
        teamRepository.save(new Team(viewer, project, RoleProjectEnum.VIEWER));

        when(diagramRepository.findByProjectId(anyString())).thenReturn(Optional.empty());
        when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        sessions.forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        stompClient.stop();
    }

    private StompSession connect(User user) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (user != null) {
            String cookie = jwtService.generateTokenCookie(user).toString().split(";")[0];
            headers.add("Cookie", cookie);
        }
        StompSession session = stompClient
                .connectAsync("http://localhost:" + port + "/api/ws", headers, new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private BlockingQueue<DiagramDataResponseDTO> subscribeToDiagram(StompSession session) throws Exception {
        BlockingQueue<DiagramDataResponseDTO> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/diagram/" + project.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return DiagramDataResponseDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((DiagramDataResponseDTO) payload);
            }
        });
        // A SUBSCRIBE is fire-and-forget; give the server time to register it before publishing.
        Thread.sleep(300);
        return received;
    }

    private Map<String, Object> diagramWithTable(String tableName) {
        return Map.of(
                "projectId", project.getId().toString(),
                "nodeDataArray", List.of(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "key", tableName,
                        "items", List.of(),
                        "location", Map.of("x", "10", "y", "20"))),
                "linkDataArray", List.of());
    }

    @Test
    void testAnEditorsUpdateReachesTheOtherMembersOfTheProject() throws Exception {
        // Given
        BlockingQueue<DiagramDataResponseDTO> ownerInbox = subscribeToDiagram(connect(owner));
        StompSession editorSession = connect(editor);

        // When
        editorSession.send("/app/send", diagramWithTable("customers"));

        // Then
        DiagramDataResponseDTO update = ownerInbox.poll(5, TimeUnit.SECONDS);
        assertNotNull(update, "The owner should receive the editor's update");
        assertEquals(project.getId().toString(), update.getProjectId());
        assertEquals("customers", update.getNodeDataArray().getFirst().getKey());
    }

    @Test
    void testAViewersUpdateIsNeitherSavedNorBroadcast() throws Exception {
        // Given
        BlockingQueue<DiagramDataResponseDTO> ownerInbox = subscribeToDiagram(connect(owner));
        StompSession viewerSession = connect(viewer);

        // When
        viewerSession.send("/app/send", diagramWithTable("written_by_viewer"));

        // Then
        assertNull(ownerInbox.poll(SILENCE_MS, TimeUnit.MILLISECONDS));
        verify(diagramRepository, never()).save(any(Diagram.class));
    }

    @Test
    void testConcurrentPatchesToDifferentTablesAreBothKept() throws Exception {
        // Given - the repository keeps the stored document, as MongoDB would
        AtomicReference<Diagram> stored = new AtomicReference<>();
        when(diagramRepository.findByProjectId(project.getId().toString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        BlockingQueue<DiagramDataResponseDTO> ownerInbox = subscribeToDiagram(connect(owner));
        StompSession firstEditor = connect(editor);
        StompSession secondEditor = connect(owner);

        // When - both send at once, each knowing only its own table
        firstEditor.send("/app/patch", patchWithTable("customers"));
        secondEditor.send("/app/patch", patchWithTable("invoices"));

        // Then - the last broadcast carries both tables
        DiagramDataResponseDTO last = null;
        for (int i = 0; i < 2; i++) {
            DiagramDataResponseDTO update = ownerInbox.poll(5, TimeUnit.SECONDS);
            assertNotNull(update, "Each patch should be broadcast");
            last = update;
        }
        assertEquals(List.of("customers", "invoices"),
                last.getNodeDataArray().stream().map(node -> node.getKey()).sorted().toList());
    }

    private Map<String, Object> patchWithTable(String tableName) {
        return Map.of(
                "projectId", project.getId().toString(),
                "upsertNodes", List.of(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "key", tableName,
                        "items", List.of(),
                        "location", Map.of("x", "10", "y", "20"))));
    }

    @Test
    void testASessionWithoutATokenIsRefused() {
        // When & Then
        assertThrows(ExecutionException.class, () -> connect(null));
    }

    @Test
    void testAUserOutsideTheProjectDoesNotReceiveItsUpdates() throws Exception {
        // Given
        BlockingQueue<DiagramDataResponseDTO> outsiderInbox = subscribeToDiagram(connect(outsider));
        StompSession editorSession = connect(editor);

        // When
        editorSession.send("/app/send", diagramWithTable("secret_table"));

        // Then
        assertNull(outsiderInbox.poll(SILENCE_MS, TimeUnit.MILLISECONDS));
    }

}
