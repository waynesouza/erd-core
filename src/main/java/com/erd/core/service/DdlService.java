package com.erd.core.service;

import com.erd.core.ddl.DdlScriptReader;
import com.erd.core.ddl.DdlScriptWriter;
import com.erd.core.ddl.ImportedSchema;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.dto.response.ImportDdlResponseDTO;
import com.erd.core.exception.DdlImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DdlService {

    private static final Logger logger = LoggerFactory.getLogger(DdlService.class);

    private final DiagramService diagramService;
    private final WebSocketService webSocketService;
    private final CollaborationService collaborationService;
    private final CollaborationNotifier collaborationNotifier;

    public DdlService(DiagramService diagramService, WebSocketService webSocketService,
                      CollaborationService collaborationService, CollaborationNotifier collaborationNotifier) {
        this.diagramService = diagramService;
        this.webSocketService = webSocketService;
        this.collaborationService = collaborationService;
        this.collaborationNotifier = collaborationNotifier;
    }

    public ExportDdlResponseDTO exportDdl(String projectId) {
        logger.info("Exporting DDL for projectId: {}", projectId);
        
        DiagramDataResponseDTO diagramData = diagramService.getDiagramByProjectId(projectId);
        String ddlContent = DdlScriptWriter.write(diagramData.getNodeDataArray(), diagramData.getLinkDataArray());
        
        return new ExportDdlResponseDTO(ddlContent, projectId);
    }

    public ImportDdlResponseDTO importDdl(ImportDdlRequestDTO requestDto) {
        String projectId = requestDto.getProjectId();
        logger.info("Importing DDL for projectId: {}", projectId);
        
        ImportedSchema schema = DdlScriptReader.read(Objects.requireNonNullElse(requestDto.getDdlContent(), ""));
        if (schema.nodes().isEmpty()) {
            // Saving the empty result would wipe the diagram while the user is told the import worked.
            // The first warning, when there is one, says why the tables could not be read.
            String reason = schema.warnings().isEmpty() ? "" : " " + schema.warnings().get(0);
            throw new DdlImportException(
                    "No CREATE TABLE statement could be read from the script, so the diagram was not changed." + reason);
        }
        
        webSocketService.replaceDiagram(projectId, schema.nodes(), schema.links());
        // The tables these locks protected are gone. Releasing them, and saying so, lets the
        // collaborators who held them close their editors instead of saving tables that no longer exist.
        collaborationService.clearProjectLocks(projectId).forEach(collaborationNotifier::lockReleased);
            
        logger.info("DDL import completed for projectId: {} with {} warning(s)", projectId, schema.warnings().size());
        return new ImportDdlResponseDTO(schema.nodes().size(), schema.links().size(), schema.warnings());
    }

}
