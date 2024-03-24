package com.erd.core.service;

import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.repository.mongo.DiagramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiagramService {

    private static final Logger logger = LoggerFactory.getLogger(DiagramService.class);

    private final DiagramRepository diagramRepository;
    private final DiagramMapper diagramMapper;

    public DiagramService(DiagramRepository diagramRepository, DiagramMapper diagramMapper) {
        this.diagramRepository = diagramRepository;
        this.diagramMapper = diagramMapper;
    }

    public void createDiagram(CreateDiagramRequestDTO requestDto) {
        logger.info("Creating diagram");
        diagramRepository.save(diagramMapper.toEntity(requestDto));
    }

    public DiagramDataResponseDTO getDiagramByProjectId(String projectId) {
        logger.info("Getting diagram by projectId: {}", projectId);
        DiagramDataResponseDTO dataResponseDto = diagramRepository.findByProjectId(projectId)
                .map(diagramMapper::toResponseDto)
                .orElseThrow(() -> new RuntimeException("Diagram not found for projectId: " + projectId));
        logger.info("Returning diagram data: {}", diagramMapper.convertToSting(dataResponseDto));
        return dataResponseDto;
    }

}
