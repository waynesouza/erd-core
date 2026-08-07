package com.erd.core.service;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.LocationDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DdlService {

    private static final Logger logger = LoggerFactory.getLogger(DdlService.class);

    private final DiagramService diagramService;
    private final ObjectMapper objectMapper;

    public DdlService(DiagramService diagramService, ObjectMapper objectMapper) {
        this.diagramService = diagramService;
        this.objectMapper = objectMapper;
    }

    public ExportDdlResponseDTO exportDdl(String projectId) {
        logger.info("Exporting DDL for projectId: {}", projectId);
        
        DiagramDataResponseDTO diagramData = diagramService.getDiagramByProjectId(projectId);
        String ddlContent = generateDdlFromDiagram(diagramData);
        
        return new ExportDdlResponseDTO(ddlContent, projectId);
    }

    public void importDdl(ImportDdlRequestDTO requestDto) {
        logger.info("Importing DDL for projectId: {}", requestDto.getProjectId());
        
        List<NodeDataDTO> nodeDataList = parseDdlToNodeData(requestDto.getDdlContent());
        List<LinkDataDTO> linkDataList = parseDdlToLinkData(requestDto.getDdlContent(), nodeDataList);
        
        // Convert to JSON strings as expected by the Diagram entity
        try {
            String nodeDataJson = objectMapper.writeValueAsString(nodeDataList);
            String linkDataJson = objectMapper.writeValueAsString(linkDataList);
            
            // Save or update diagram
            diagramService.saveOrUpdateDiagram(requestDto.getProjectId(), nodeDataJson, linkDataJson);
            
            logger.info("DDL import completed successfully for projectId: {}", requestDto.getProjectId());
            
        } catch (Exception e) {
            logger.error("Error processing DDL import", e);
            throw new RuntimeException("Failed to import DDL", e);
        }
    }

    private String generateDdlFromDiagram(DiagramDataResponseDTO diagramData) {
        StringBuilder ddl = new StringBuilder();
        
        try {
            // Use the already populated arrays instead of parsing JSON strings
            List<NodeDataDTO> nodeDataList = diagramData.getNodeDataArray();
            List<LinkDataDTO> linkDataList = diagramData.getLinkDataArray();
            
            // Validate that we have data
            if (nodeDataList == null || nodeDataList.isEmpty()) {
                logger.warn("No node data found in diagram");
                return "-- No tables found in diagram";
            }
            
            // Generate CREATE TABLE statements
            for (NodeDataDTO nodeData : nodeDataList) {
                ddl.append(generateCreateTableStatement(nodeData));
                ddl.append("\n\n");
            }
            
            // Generate ALTER TABLE statements for foreign keys
            if (linkDataList != null && !linkDataList.isEmpty()) {
                for (LinkDataDTO linkData : linkDataList) {
                    ddl.append(generateForeignKeyStatement(linkData, nodeDataList));
                    ddl.append("\n\n");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating DDL", e);
            throw new RuntimeException("Failed to generate DDL", e);
        }
        
        return ddl.toString().trim();
    }
    
    private String generateCreateTableStatement(NodeDataDTO nodeData) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(nodeData.getKey()).append(" (\n");
        
        List<String> primaryKeys = new ArrayList<>();
        
        for (int i = 0; i < nodeData.getItems().size(); i++) {
            ItemDTO item = nodeData.getItems().get(i);
            sql.append("    ").append(item.getName()).append(" ").append(mapTypeToMySql(item.getType()));
            
            if (Boolean.TRUE.equals(item.getNotNull())) {
                sql.append(" NOT NULL");
            }
            
            if (Boolean.TRUE.equals(item.getAutoIncrement())) {
                sql.append(" AUTO_INCREMENT");
            }
            
            if (item.getDefaultValue() != null && !item.getDefaultValue().isEmpty()) {
                sql.append(" DEFAULT ").append(item.getDefaultValue());
            }
            
            if (Boolean.TRUE.equals(item.getUnique())) {
                sql.append(" UNIQUE");
            }
            
            if (Boolean.TRUE.equals(item.getPk())) {
                primaryKeys.add(item.getName());
            }
            
            if (i < nodeData.getItems().size() - 1 || !primaryKeys.isEmpty()) {
                sql.append(",");
            }
            sql.append("\n");
        }
        
        if (!primaryKeys.isEmpty()) {
            sql.append("    PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")\n");
        }
        
        sql.append(");");
        return sql.toString();
    }
    
    private String generateForeignKeyStatement(LinkDataDTO linkData, List<NodeDataDTO> nodeDataList) {
        NodeDataDTO fromTable = nodeDataList.stream()
            .filter(node -> node.getKey().equals(linkData.getFrom()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Table not found: " + linkData.getFrom()));

        // Prefer the FK column stored during import; fall back to heuristic for manually-created links
        String fkColumn;
        if (linkData.getFromColumn() != null && !linkData.getFromColumn().isEmpty()) {
            fkColumn = linkData.getFromColumn();
        } else {
            String targetSingular = linkData.getTo().toLowerCase().replaceAll("s$", "");
            fkColumn = fromTable.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getFk()) &&
                        item.getName().toLowerCase().contains(targetSingular))
                .map(ItemDTO::getName)
                .findFirst()
                .orElse(linkData.getTo() + "_id");
        }

        NodeDataDTO toTable = nodeDataList.stream()
            .filter(node -> node.getKey().equals(linkData.getTo()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Table not found: " + linkData.getTo()));

        String pkColumn = toTable.getItems().stream()
            .filter(item -> Boolean.TRUE.equals(item.getPk()))
            .map(ItemDTO::getName)
            .findFirst()
            .orElse("id");

        return String.format("ALTER TABLE %s ADD CONSTRAINT fk_%s_%s FOREIGN KEY (%s) REFERENCES %s(%s);",
            linkData.getFrom(), linkData.getFrom(), linkData.getTo(), fkColumn, linkData.getTo(), pkColumn);
    }
    
    private String mapTypeToMySql(String type) {
        return switch (type.toUpperCase()) {
            case "INTEGER", "INT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "TINYINT" -> "TINYINT";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "FLOAT" -> "FLOAT";
            case "DOUBLE" -> "DOUBLE";
            case "VARCHAR" -> "VARCHAR(255)";
            case "CHAR" -> "CHAR(1)";
            case "TEXT" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "DATETIME", "TIMESTAMP" -> "DATETIME";
            case "BOOLEAN", "BOOL" -> "BOOLEAN";
            default -> "VARCHAR(255)";
        };
    }
    
    private List<NodeDataDTO> parseDdlToNodeData(String ddlContent) {
        List<NodeDataDTO> nodeDataList = new ArrayList<>();

        Pattern tablePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher tableMatcher = tablePattern.matcher(ddlContent);
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

    private List<LinkDataDTO> parseDdlToLinkData(String ddlContent, List<NodeDataDTO> nodeDataList) {
        List<LinkDataDTO> linkDataList = new ArrayList<>();
        Map<String, String> tableMap = new HashMap<>();

        for (NodeDataDTO nodeData : nodeDataList) {
            tableMap.put(nodeData.getKey(), nodeData.getKey());
        }

        Pattern tablePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Pattern fkPattern = Pattern.compile(
            "FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+(\\w+)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher tableMatcher = tablePattern.matcher(ddlContent);
        while (tableMatcher.find()) {
            // tableMap was built from this very pattern over this very content, so every
            // table matched here is guaranteed to be present in it.
            String sourceTable = tableMatcher.group(1).trim();
            String tableBody   = tableMatcher.group(2);

            Matcher fkMatcher = fkPattern.matcher(tableBody);
            while (fkMatcher.find()) {
                String fkColumnName  = fkMatcher.group(1).trim();
                String referencedTable = fkMatcher.group(2).trim();
                if (!tableMap.containsKey(referencedTable)) continue;

                LinkDataDTO linkData = new LinkDataDTO();
                linkData.setFrom(sourceTable);
                linkData.setTo(referencedTable);
                linkData.setText("N:1");
                linkData.setToText(1);
                linkData.setFromColumn(fkColumnName);
                linkDataList.add(linkData);
            }
        }

        return linkDataList;
    }
}
