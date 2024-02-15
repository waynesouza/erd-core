package com.erd.core.service;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    private final DiagramRepository diagramRepository;
    private final DiagramMapper diagramMapper;

    public WebSocketService(DiagramRepository diagramRepository, DiagramMapper diagramMapper) {
        this.diagramRepository = diagramRepository;
        this.diagramMapper = diagramMapper;
    }

    public DiagramDataResponseDTO getDiagramByProjectId(String projectId) {
        logger.info("Getting diagram by projectId: {}", projectId);
        return diagramRepository.findByProjectId(projectId)
                .map(diagramMapper::toResponseDto)
                .orElseThrow(() -> new RuntimeException("Diagram not found for projectId: " + projectId));
    }

    public DiagramDataResponseDTO save(DiagramDataRequestDTO diagramDataRequestDto) {
        try {
            logger.info("Saving diagram data");
            Diagram diagram = diagramRepository.save(diagramMapper.toEntity(diagramDataRequestDto));
            return diagramMapper.toResponseDto(diagram);
        } catch (Exception e) {
            logger.error("Error saving diagram data", e);
            return null;
        }
    }
}
