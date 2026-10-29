package com.erd.core.service;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.request.DiagramPatchRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class WebSocketService {

    private static final String TOPIC_PREFIX = "/topic/diagram/";
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    private final DiagramRepository diagramRepository;
    private final DiagramMapper diagramMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // Patches to one project are applied one at a time: two concurrent read-modify-write cycles on
    // the same document would otherwise both start from the same state and the last save would win.
    // Kept in memory, like the entity locks, so it only holds while there is a single instance.
    private final Map<String, Object> projectMonitors = new ConcurrentHashMap<>();

    public WebSocketService(DiagramRepository diagramRepository, DiagramMapper diagramMapper, SimpMessagingTemplate messagingTemplate) {
        this.diagramRepository = diagramRepository;
        this.diagramMapper = diagramMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public void save(DiagramDataRequestDTO diagramDataRequestDto) {
        try {
            logger.info("Saving or updating diagram data");
            Optional<Diagram> existingDiagram = diagramRepository.findByProjectId(diagramDataRequestDto.getProjectId().toString());
            Diagram diagram;

            if (existingDiagram.isPresent()) {
                diagram = existingDiagram.get();
                diagram.setNodeData(diagramMapper.convertToSting(diagramDataRequestDto.getNodeDataArray()));
                diagram.setLinkData(diagramMapper.convertToSting(diagramDataRequestDto.getLinkDataArray()));
                logger.info("Updating existing diagram data");
            } else {
                diagram = diagramMapper.toEntity(diagramDataRequestDto);
                logger.info("Creating new diagram data");
            }

            diagram = diagramRepository.save(diagram);

            DiagramDataResponseDTO dataResponseDto = diagramMapper.toResponseDto(diagram);
            sendToTopic(dataResponseDto);
        } catch (Exception e) {
            logger.error("Error saving or updating diagram data", e);
        }
    }

    public void applyPatch(DiagramPatchRequestDTO patch) {
        String projectId = patch.getProjectId().toString();

        try {
            synchronized (projectMonitors.computeIfAbsent(projectId, id -> new Object())) {
                Diagram diagram = diagramRepository.findByProjectId(projectId)
                        .orElseGet(() -> new Diagram("[]", "[]", projectId));
                DiagramDataResponseDTO current = diagramMapper.toResponseDto(diagram);

                List<NodeDataDTO> nodes = merge(current.getNodeDataArray(), patch.getUpsertNodes(),
                        patch.getRemovedNodeIds(), node -> node.getId() == null ? null : node.getId().toString());
                List<LinkDataDTO> links = merge(withIds(current.getLinkDataArray()), patch.getUpsertLinks(),
                        patch.getRemovedLinkIds(), LinkDataDTO::getId);

                diagram.setNodeData(diagramMapper.convertToSting(nodes));
                diagram.setLinkData(diagramMapper.convertToSting(links));
                // Broadcast before releasing the monitor so the states go out in the order they were
                // produced; otherwise a client could apply an older state after a newer one. Every
                // client, the sender included, rebases its own pending changes on top of it.
                sendToTopic(diagramMapper.toResponseDto(diagramRepository.save(diagram)));
            }
        } catch (Exception e) {
            logger.error("Error applying diagram patch to project {}", projectId, e);
        }
    }

    // Replaces the elements whose id appears in the upserts, appends the new ones and drops the
    // removed ids. Elements without an id cannot be addressed by a patch and are kept as they are.
    static <T> List<T> merge(List<T> current, List<T> upserts, List<String> removedIds, Function<T, String> idOf) {
        Map<String, T> byId = new LinkedHashMap<>();
        List<T> withoutId = new ArrayList<>();

        for (T element : current) {
            String id = idOf.apply(element);
            if (id == null) {
                withoutId.add(element);
            } else {
                byId.put(id, element);
            }
        }
        for (T element : Objects.requireNonNullElse(upserts, List.<T>of())) {
            String id = idOf.apply(element);
            if (id != null) {
                byId.put(id, element);
            }
        }
        Objects.requireNonNullElse(removedIds, List.<String>of()).forEach(byId::remove);

        List<T> result = new ArrayList<>(withoutId);
        result.addAll(byId.values());
        return result;
    }

    // Links saved before LinkDataDTO carried an id get one the first time their diagram is patched,
    // so the clients can address them from then on.
    private static List<LinkDataDTO> withIds(List<LinkDataDTO> links) {
        links.stream()
                .filter(link -> link.getId() == null)
                .forEach(link -> link.setId(UUID.randomUUID().toString()));
        return links;
    }

    private void sendToTopic(DiagramDataResponseDTO savedDto) {
        String topic = TOPIC_PREFIX + savedDto.getProjectId();
        logger.info("Sending message to topic: {}", topic);
        try {
            messagingTemplate.convertAndSend(topic, savedDto);
            logger.info("Message sent to project-scoped topic: {}", topic);
        } catch (MessagingException e) {
            logger.error("Error sending message", e);
        }
    }

}
