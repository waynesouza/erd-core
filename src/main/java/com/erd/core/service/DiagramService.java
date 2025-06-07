package com.erd.core.service;

import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
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

    public void saveOrUpdateDiagram(String projectId, String nodeDataJson, String linkDataJson) {
        logger.info("Saving or updating diagram for projectId: {}", projectId);
        
        Diagram diagram = diagramRepository.findByProjectId(projectId)
                .orElse(new Diagram());
        
        diagram.setProjectId(projectId);
        diagram.setNodeData(nodeDataJson);
        diagram.setLinkData(linkDataJson);
        
        diagramRepository.save(diagram);
        logger.info("Diagram saved/updated successfully for projectId: {}", projectId);
    }

    public void deleteDiagramByProjectId(String projectId) {
        logger.info("Deleting diagram data for projectId: {}", projectId);
        try {
            diagramRepository.deleteByProjectId(projectId);
            logger.info("Diagram data successfully deleted for projectId: {}", projectId);
        } catch (Exception e) {
            logger.error("Error deleting diagram data for projectId: {}. Error: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to delete diagram data for project: " + projectId, e);
        }
    }

}
