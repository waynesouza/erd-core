package com.erd.core.mapper;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.model.Diagram;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagramMapper {

    private static final Logger logger = LoggerFactory.getLogger(DiagramMapper.class);

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public DiagramMapper(ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }
    public Diagram toEntity(DiagramDataRequestDTO diagramDataRequestDto) {
        String nodeData = convertToSting(diagramDataRequestDto.getNodeDataArray());
        String linkData = convertToSting(diagramDataRequestDto.getLinkDataArray());
        return new Diagram(nodeData, linkData, null);
    }

    public DiagramDataResponseDTO toResponseDto(Diagram diagram) {
        List<NodeDataDTO> nodeDataArray = convertJsonToList(diagram.getNodeData(), new TypeReference<>() {});
        List<LinkDataDTO> linkDataArray = convertJsonToList(diagram.getLinkData(), new TypeReference<>() {});
        return new DiagramDataResponseDTO(nodeDataArray, linkDataArray);
    }

    private String convertToSting(Object object) {
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
