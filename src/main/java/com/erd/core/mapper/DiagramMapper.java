package com.erd.core.mapper;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.model.Diagram;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagramMapper {

    private final ObjectMapper objectMapper;

    public DiagramMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Diagram toEntity(DiagramDataRequestDTO diagramDataRequestDto) throws JsonProcessingException {
        String nodeData = objectMapper.writeValueAsString(diagramDataRequestDto.getNodeDataArray());
        String linkData = objectMapper.writeValueAsString(diagramDataRequestDto.getLinkDataArray());
        String projectId = diagramDataRequestDto.getProjectId().toString();
        return new Diagram(nodeData, linkData, projectId);
    }

    public DiagramDataResponseDTO toResponseDto(Diagram diagram) throws JsonProcessingException {
        List<NodeDataDTO> nodeDataArray = objectMapper.readValue(diagram.getNodeData(), new TypeReference<>() {});
        List<LinkDataDTO> linkDataArray = objectMapper.readValue(diagram.getLinkData(), new TypeReference<>() {});
        return new DiagramDataResponseDTO(nodeDataArray, linkDataArray);
    }

}
