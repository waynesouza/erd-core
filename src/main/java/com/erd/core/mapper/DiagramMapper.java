package com.erd.core.mapper;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.model.mongo.Diagram;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class DiagramMapper {

    private static final Logger logger = LoggerFactory.getLogger(DiagramMapper.class);

    private final ObjectMapper objectMapper;

    public DiagramMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public Diagram toEntity(DiagramDataRequestDTO diagramDataRequestDto) {
        String nodeData = convertToSting(diagramDataRequestDto.getNodeDataArray());
        String linkData = convertToSting(diagramDataRequestDto.getLinkDataArray());
        String projectId = diagramDataRequestDto.getProjectId().toString();
        return new Diagram(nodeData, linkData, projectId);
    }

    public DiagramDataResponseDTO toResponseDto(Diagram diagram) {
        List<NodeDataDTO> nodeDataArray = Objects.requireNonNullElse(convertJsonToList(diagram.getNodeData(), new TypeReference<>() {}), new ArrayList<>());
        List<LinkDataDTO> linkDataArray = Objects.requireNonNullElse(convertJsonToList(diagram.getLinkData(), new TypeReference<>() {}), new ArrayList<>());
        return new DiagramDataResponseDTO(nodeDataArray, linkDataArray);
    }

    public String convertToSting(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Error while converting object to JSON", e);
            return null;
        }
    }

    private <T> List<T> convertJsonToList(String json, TypeReference<List<T>> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error while converting JSON to List", e);
            return null;
        }
    }

}
