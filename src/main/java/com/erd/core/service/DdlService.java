package com.erd.core.service;

import com.erd.core.ddl.DdlScriptWriter;
import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.LocationDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.exception.DdlImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DdlService {

    private static final Logger logger = LoggerFactory.getLogger(DdlService.class);

    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.*?)\\);",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile(
        "ALTER\\s+TABLE\\s+(\\w+)\\s+(.*?);",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile(
        "FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+(\\w+)\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );

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

    public void importDdl(ImportDdlRequestDTO requestDto) {
        String projectId = requestDto.getProjectId();
        logger.info("Importing DDL for projectId: {}", projectId);
        String ddlContent = Objects.requireNonNullElse(requestDto.getDdlContent(), "");
        
        List<NodeDataDTO> nodeDataList = parseDdlToNodeData(ddlContent);
        if (nodeDataList.isEmpty()) {
            // Saving the empty result would wipe the diagram while the user is told the import worked.
            throw new DdlImportException(
                    "No CREATE TABLE statement could be read from the script, so the diagram was not changed.");
        }
        List<LinkDataDTO> linkDataList = parseDdlToLinkData(ddlContent, nodeDataList);
        
        webSocketService.replaceDiagram(projectId, nodeDataList, linkDataList);
        // The tables these locks protected are gone. Releasing them, and saying so, lets the
        // collaborators who held them close their editors instead of saving tables that no longer exist.
        collaborationService.clearProjectLocks(projectId).forEach(collaborationNotifier::lockReleased);
            
        logger.info("DDL import completed successfully for projectId: {}", projectId);
    }

    private static List<ItemDTO> itemsOf(NodeDataDTO node) {
        return Objects.requireNonNullElse(node.getItems(), List.of());
    }
    
    private List<NodeDataDTO> parseDdlToNodeData(String ddlContent) {
        List<NodeDataDTO> nodeDataList = new ArrayList<>();

        Matcher tableMatcher = TABLE_PATTERN.matcher(ddlContent);
        int xOffset = 0;
        
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String tableDefinition = tableMatcher.group(2);
            
            NodeDataDTO nodeData = new NodeDataDTO();
            nodeData.setId(UUID.randomUUID());
            nodeData.setKey(tableName);
            nodeData.setItems(parseTableColumns(tableDefinition));
            
            // Set default location
            LocationDTO location = new LocationDTO();
            location.setX(String.valueOf(xOffset));
            location.setY("50");
            nodeData.setLocation(location);
            
            nodeDataList.add(nodeData);
            xOffset += 300; // Space tables horizontally
        }
        
        return nodeDataList;
    }
    
    private List<ItemDTO> parseTableColumns(String tableDefinition) {
        List<ItemDTO> items = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        
        // Extract PRIMARY KEY definition first
        Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher pkMatcher = pkPattern.matcher(tableDefinition);
        
        if (pkMatcher.find()) {
            String[] keys = pkMatcher.group(1).split(",");
            for (String key : keys) {
                primaryKeys.add(key.trim());
            }
        }
        
        // Parse column definitions (split respecting parentheses so DECIMAL(10,2) stays intact)
        List<String> lines = splitRespectingParentheses(tableDefinition);

        for (String line : lines) {
            line = line.trim();
            
            // Skip PRIMARY KEY and other constraints
            if (line.toUpperCase().startsWith("PRIMARY KEY") || 
                line.toUpperCase().startsWith("FOREIGN KEY") ||
                line.toUpperCase().startsWith("CONSTRAINT")) {
                continue;
            }
            
            ItemDTO item = parseColumnDefinition(line, primaryKeys);
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    private ItemDTO parseColumnDefinition(String columnDef, List<String> primaryKeys) {
        // Pattern to match: column_name TYPE [constraints]
        Pattern columnPattern = Pattern.compile(
            "^(\\w+)\\s+(\\w+)(?:\\([^)]*\\))?(.*)$",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = columnPattern.matcher(columnDef.trim());
        
        if (!matcher.find()) {
            return null;
        }
        
        String columnName = matcher.group(1);
        String columnType = matcher.group(2);
        // Group 3 is (.*)$ and always matches, possibly as an empty string.
        String constraints = matcher.group(3).toUpperCase();
        
        ItemDTO item = new ItemDTO();
        item.setName(columnName);
        item.setType(mapMySqlTypeToGeneric(columnType));
        boolean hasInlinePk = constraints.contains("PRIMARY KEY");
        item.setPk(primaryKeys.contains(columnName) || hasInlinePk);
        item.setFk(columnName.toLowerCase().endsWith("_id") && !item.getPk());
        item.setNotNull(constraints.contains("NOT NULL"));
        item.setAutoIncrement(constraints.contains("AUTO_INCREMENT"));
        item.setUnique(constraints.contains("UNIQUE"));
        item.setDefaultValue("");
        
        // Extract default value if present
        Pattern defaultPattern = Pattern.compile("DEFAULT\\s+([^\\s,]+)", Pattern.CASE_INSENSITIVE);
        Matcher defaultMatcher = defaultPattern.matcher(constraints);
        if (defaultMatcher.find()) {
            item.setDefaultValue(defaultMatcher.group(1));
        }
        
        return item;
    }
    
    private String mapMySqlTypeToGeneric(String mysqlType) {
        return switch (mysqlType.toUpperCase()) {
            case "INT", "INTEGER" -> "INTEGER";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "TINYINT" -> "TINYINT";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "FLOAT" -> "FLOAT";
            case "DOUBLE" -> "DOUBLE";
            case "VARCHAR" -> "VARCHAR";
            case "CHAR" -> "CHAR";
            case "TEXT" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "DATETIME", "TIMESTAMP" -> "DATETIME";
            case "BOOLEAN", "BOOL" -> "BOOLEAN";
            default -> "VARCHAR";
        };
    }
    
    private List<String> splitRespectingParentheses(String input) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(input.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(input.substring(start));
        return parts;
    }

    // Foreign keys come from the table bodies and, as the export writes them, from ALTER TABLE
    // statements after the tables. A key declared in both places becomes a single link.
    private List<LinkDataDTO> parseDdlToLinkData(String ddlContent, List<NodeDataDTO> nodeDataList) {
        Map<String, NodeDataDTO> tables = new HashMap<>();
        for (NodeDataDTO nodeData : nodeDataList) {
            tables.put(nodeData.getKey(), nodeData);
        }
        Map<String, LinkDataDTO> links = new LinkedHashMap<>();

        Matcher tableMatcher = TABLE_PATTERN.matcher(ddlContent);
        while (tableMatcher.find()) {
            addForeignKeys(tableMatcher.group(1).trim(), tableMatcher.group(2), tables, links);
        }
        Matcher alterMatcher = ALTER_TABLE_PATTERN.matcher(ddlContent);
        while (alterMatcher.find()) {
            addForeignKeys(alterMatcher.group(1).trim(), alterMatcher.group(2), tables, links);
        }

        return new ArrayList<>(links.values());
    }

    private void addForeignKeys(String sourceTable, String definition, Map<String, NodeDataDTO> tables,
                                Map<String, LinkDataDTO> links) {
        NodeDataDTO source = tables.get(sourceTable);
        if (source == null) {
            // An ALTER TABLE on a table the script does not create.
            return;
        }

        Matcher fkMatcher = FOREIGN_KEY_PATTERN.matcher(definition);
        while (fkMatcher.find()) {
            String fkColumnName = fkMatcher.group(1).trim();
            String referencedTable = fkMatcher.group(2).trim();
            if (!tables.containsKey(referencedTable)) continue;

            links.computeIfAbsent(sourceTable + "." + fkColumnName + "->" + referencedTable,
                    key -> newLink(sourceTable, fkColumnName, referencedTable));
            itemsOf(source).stream()
                    .filter(item -> fkColumnName.equals(item.getName()))
                    .forEach(item -> item.setFk(true));
        }
    }

    private static LinkDataDTO newLink(String sourceTable, String fkColumnName, String referencedTable) {
        LinkDataDTO linkData = new LinkDataDTO();
        // Without an id a client cannot address the link in a patch, so deleting it would not stick.
        linkData.setId(UUID.randomUUID().toString());
        linkData.setFrom(sourceTable);
        linkData.setTo(referencedTable);
        linkData.setText("N:1");
        linkData.setToText(1);
        linkData.setFromColumn(fkColumnName);
        return linkData;
    }
}
