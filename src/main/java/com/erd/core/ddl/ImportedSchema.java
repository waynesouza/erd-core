package com.erd.core.ddl;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;

import java.util.List;

/**
 * What a script turned into: the tables and relationships of the diagram, and a warning for each
 * part of the script that could not be represented, so the user knows what was left out.
 */
public record ImportedSchema(List<NodeDataDTO> nodes, List<LinkDataDTO> links, List<String> warnings) {
}
