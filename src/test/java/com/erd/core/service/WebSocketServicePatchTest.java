package com.erd.core.service;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramPatchRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WebSocketService#applyPatch} is mostly JSON in and JSON out, so it runs here with the real
 * {@link DiagramMapper} and a repository double that keeps the stored document in memory - the
 * same read-modify-write cycle MongoDB sees.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketServicePatchTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Diagram> stored = new AtomicReference<>();
    private WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        webSocketService = new WebSocketService(diagramRepository, new DiagramMapper(objectMapper), messagingTemplate);
        lenient().when(diagramRepository.findByProjectId(PROJECT_ID.toString())).thenAnswer(invocation -> {
            Diagram current = stored.get();
            return Optional.ofNullable(current == null ? null
                    : new Diagram(current.getNodeData(), current.getLinkData(), current.getProjectId()));
        });
        lenient().when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> {
            Diagram saved = invocation.getArgument(0);
            stored.set(saved);
            return saved;
        });
    }

    private void store(List<NodeDataDTO> nodes, List<LinkDataDTO> links) throws Exception {
        stored.set(new Diagram(objectMapper.writeValueAsString(nodes), objectMapper.writeValueAsString(links),
                PROJECT_ID.toString()));
    }

    private DiagramDataResponseDTO storedState() {
        return new DiagramMapper(objectMapper).toResponseDto(stored.get());
    }

    private static NodeDataDTO node(UUID id, String key) {
        NodeDataDTO node = new NodeDataDTO();
        node.setId(id);
        node.setKey(key);
        node.setItems(List.of());
        return node;
    }

    private static LinkDataDTO link(String id, String from, String to) {
        LinkDataDTO link = new LinkDataDTO();
        link.setId(id);
        link.setFrom(from);
        link.setTo(to);
        return link;
    }

    private static DiagramPatchRequestDTO patch() {
        DiagramPatchRequestDTO patch = new DiagramPatchRequestDTO();
        patch.setProjectId(PROJECT_ID);
        return patch;
    }

    private static List<String> keys(DiagramDataResponseDTO state) {
        return state.getNodeDataArray().stream().map(NodeDataDTO::getKey).toList();
    }

    @Test
    void testApplyPatch_replacesAppendsAndRemovesNodesById() throws Exception {
        // Given
        UUID users = UUID.randomUUID();
        UUID orders = UUID.randomUUID();
        store(List.of(node(users, "users"), node(orders, "orders")), List.of());
        DiagramPatchRequestDTO patch = patch();
        patch.setUpsertNodes(List.of(node(users, "customers"), node(UUID.randomUUID(), "products")));
        patch.setRemovedNodeIds(List.of(orders.toString()));

        // When
        webSocketService.applyPatch(patch);

        // Then
        assertEquals(List.of("customers", "products"), keys(storedState()));
        ArgumentCaptor<DiagramDataResponseDTO> broadcast = ArgumentCaptor.forClass(DiagramDataResponseDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/diagram/" + PROJECT_ID), broadcast.capture());
        assertEquals(List.of("customers", "products"), keys(broadcast.getValue()));
    }

    @Test
    void testApplyPatch_keepsTheChangesOfAnotherClientToOtherElements() throws Exception {
        // Given - client A already renamed "users"; client B, unaware of it, edits "orders"
        UUID users = UUID.randomUUID();
        UUID orders = UUID.randomUUID();
        store(List.of(node(users, "users_renamed_by_a"), node(orders, "orders")), List.of());
        DiagramPatchRequestDTO fromB = patch();
        fromB.setUpsertNodes(List.of(node(orders, "orders_edited_by_b")));

        // When
        webSocketService.applyPatch(fromB);

        // Then - sending B's whole diagram would have reverted A's rename
        assertEquals(List.of("users_renamed_by_a", "orders_edited_by_b"), keys(storedState()));
    }

    @Test
    void testApplyPatch_createsTheDiagramOnTheFirstPatch() {
        // Given
        DiagramPatchRequestDTO patch = patch();
        patch.setUpsertNodes(List.of(node(UUID.randomUUID(), "users")));
        patch.setUpsertLinks(List.of(link("link-1", "users", "users")));

        // When
        webSocketService.applyPatch(patch);

        // Then
        assertEquals(PROJECT_ID.toString(), stored.get().getProjectId());
        assertEquals(List.of("users"), keys(storedState()));
        assertEquals("link-1", storedState().getLinkDataArray().getFirst().getId());
    }

    @Test
    void testApplyPatch_givesLegacyLinksAnIdAndLetsThemBeRemoved() throws Exception {
        // Given - a link saved before links carried an id
        store(List.of(), List.of(link(null, "orders", "users")));

        // When
        webSocketService.applyPatch(patch());

        // Then
        LinkDataDTO legacy = storedState().getLinkDataArray().getFirst();
        assertNotNull(legacy.getId());

        // When - a client removes it by the id it received
        DiagramPatchRequestDTO removal = patch();
        removal.setRemovedLinkIds(List.of(legacy.getId()));
        webSocketService.applyPatch(removal);

        // Then
        assertEquals(List.of(), storedState().getLinkDataArray());
    }

    @Test
    void testApplyPatch_keepsNodesWithoutAnIdAndIgnoresUpsertsWithoutOne() throws Exception {
        // Given
        store(List.of(node(null, "legacy_table")), List.of());
        DiagramPatchRequestDTO patch = patch();
        patch.setUpsertNodes(List.of(node(null, "unaddressable")));

        // When
        webSocketService.applyPatch(patch);

        // Then
        assertEquals(List.of("legacy_table"), keys(storedState()));
    }

    @Test
    void testApplyPatch_toleratesMissingLists() throws Exception {
        // Given - a client that only sends removals leaves the other lists out of the JSON
        UUID users = UUID.randomUUID();
        store(List.of(node(users, "users")), List.of(link("link-1", "users", "users")));
        DiagramPatchRequestDTO patch = patch();
        patch.setUpsertNodes(null);
        patch.setRemovedNodeIds(null);
        patch.setUpsertLinks(null);
        patch.setRemovedLinkIds(null);

        // When
        webSocketService.applyPatch(patch);

        // Then
        assertEquals(List.of("users"), keys(storedState()));
        assertEquals(1, storedState().getLinkDataArray().size());
    }

    @Test
    void testApplyPatch_doesNotBroadcastWhenPersistenceFails() {
        // Given
        when(diagramRepository.findByProjectId(anyString())).thenThrow(new IllegalStateException("mongo down"));

        // When - must not propagate to the broker
        webSocketService.applyPatch(patch());

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testApplyPatch_keepsEveryConcurrentPatchToTheSameProject() throws Exception {
        // Given - the repository pauses between read and write, widening the window in which an
        // unsynchronised read-modify-write would lose the other clients' tables
        store(List.of(), List.of());
        when(diagramRepository.findByProjectId(PROJECT_ID.toString())).thenAnswer(invocation -> {
            Diagram current = stored.get();
            Thread.sleep(5);
            return Optional.of(new Diagram(current.getNodeData(), current.getLinkData(), current.getProjectId()));
        });
        int clients = 8;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> sent = new ArrayList<>();
        try {
            for (int c = 0; c < clients; c++) {
                DiagramPatchRequestDTO patch = patch();
                patch.setUpsertNodes(List.of(node(UUID.randomUUID(), "table_" + c)));
                sent.add(pool.submit(() -> {
                    start.await();
                    webSocketService.applyPatch(patch);
                    return null;
                }));
            }

            // When
            start.countDown();
            for (Future<?> future : sent) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // Then
        assertEquals(clients, storedState().getNodeDataArray().size());
    }

}
