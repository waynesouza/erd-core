package com.erd.core.service;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WebSocketService {

    private static final String TOPIC = "/topic/receive";
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    private final DiagramRepository diagramRepository;
    private final DiagramMapper diagramMapper;
    private final SimpMessagingTemplate messagingTemplate;

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

    private void sendToTopic(DiagramDataResponseDTO savedDto) {
        logger.info("Sending message to topic: {}", TOPIC);
        try {
            messagingTemplate.convertAndSend(TOPIC, savedDto);
            logger.info("Message re-sent with data: {}", diagramMapper.convertToSting(savedDto));
        } catch (MessagingException e) {
            logger.error("Error sending message", e);
        }
    }

}
