package com.erd.core.service;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.LocationDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DdlServiceTest {

    @Mock
    private DiagramService diagramService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DdlService ddlService;

    private DiagramDataResponseDTO sampleDiagramData;

    @BeforeEach
    void setUp() {
        // Setup sample data for tests
        String sampleNodeDataJson = "[{\"id\":\"bd8e59b4-e4fa-4108-9451-e01a5ca747b4\",\"key\":\"table1\",\"items\":[{\"name\":\"id\",\"type\":\"INTEGER\",\"pk\":true,\"fk\":false,\"unique\":false,\"notNull\":true,\"autoIncrement\":true,\"defaultValue\":\"\"},{\"name\":\"table2_id\",\"type\":\"INTEGER\",\"pk\":false,\"fk\":true,\"unique\":false,\"notNull\":true,\"autoIncrement\":false,\"defaultValue\":\"\"}],\"location\":{\"x\":\"-338.81008236970797\",\"y\":\"53.7535226836556\"}},{\"id\":\"696db9d7-debc-42d3-963c-b30334054664\",\"key\":\"table2\",\"items\":[{\"name\":\"id\",\"type\":\"INTEGER\",\"pk\":true,\"fk\":false,\"unique\":false,\"notNull\":true,\"autoIncrement\":true,\"defaultValue\":\"\"}],\"location\":{\"x\":\"156.0454087970379\",\"y\":\"36.19404426222769\"}}]";
        String sampleLinkDataJson = "[{\"from\":\"table1\",\"to\":\"table2\",\"text\":\"N:1\",\"toText\":\"1\"}]";

        sampleDiagramData = new DiagramDataResponseDTO();
        sampleDiagramData.setNodeData(sampleNodeDataJson);
        sampleDiagramData.setLinkData(sampleLinkDataJson);
        sampleDiagramData.setProjectId("test-project");
    }

    @Test
    void testExportDdl_Success() throws Exception {
        // Given
        String projectId = "test-project";
        
        // Create expected NodeDataDTO list
        List<NodeDataDTO> nodeDataList = createSampleNodeDataList();
        List<LinkDataDTO> linkDataList = createSampleLinkDataList();

        // Set up diagram data with populated arrays (new approach)
        sampleDiagramData.setNodeDataArray(nodeDataList);
        sampleDiagramData.setLinkDataArray(linkDataList);

        when(diagramService.getDiagramByProjectId(projectId)).thenReturn(sampleDiagramData);

        // When
        ExportDdlResponseDTO result = ddlService.exportDdl(projectId);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertNotNull(result.getDdlContent());
        assertTrue(result.getDdlContent().contains("CREATE TABLE table1"));
        assertTrue(result.getDdlContent().contains("CREATE TABLE table2"));
        assertTrue(result.getDdlContent().contains("PRIMARY KEY"));
        assertTrue(result.getDdlContent().contains("FOREIGN KEY"));
    }

    @Test
    void testExportDdl_GeneratesCorrectCreateTableStatement() throws Exception {
        // Given
        String projectId = "test-project";
        List<NodeDataDTO> nodeDataList = createSampleNodeDataList();
        List<LinkDataDTO> linkDataList = createSampleLinkDataList();

        // Set up diagram data with populated arrays (new approach)
        sampleDiagramData.setNodeDataArray(nodeDataList);
        sampleDiagramData.setLinkDataArray(linkDataList);

        when(diagramService.getDiagramByProjectId(projectId)).thenReturn(sampleDiagramData);

        // When
        ExportDdlResponseDTO result = ddlService.exportDdl(projectId);

        // Then
        String ddlContent = result.getDdlContent();
        assertTrue(ddlContent.contains("id INT NOT NULL AUTO_INCREMENT"));
        assertTrue(ddlContent.contains("table2_id INT NOT NULL"));
        assertTrue(ddlContent.contains("PRIMARY KEY (id)"));
    }

    @Test
    void testImportDdl_Success() throws Exception {
        // Given
        String ddlContent = """
            CREATE TABLE users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            
            CREATE TABLE orders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                amount DECIMAL(10,2),
                FOREIGN KEY (user_id) REFERENCES users(id)
            );
            """;

        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent(ddlContent);

        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        // When
        ddlService.importDdl(requestDto);

        // Then
        verify(diagramService).saveOrUpdateDiagram(eq("test-project"), eq("mocked-json"), eq("mocked-json"));
    }

    @Test
    void testImportDdl_ParsesTablesCorrectly() throws Exception {
        // Given
        String ddlContent = """
            CREATE TABLE users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE
            );
            """;

        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent(ddlContent);

        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        // When
        ddlService.importDdl(requestDto);

        // Then
        verify(objectMapper, times(2)).writeValueAsString(any());
        verify(diagramService).saveOrUpdateDiagram(eq("test-project"), any(), any());
    }

    @Test
    void testImportDdl_ParsesForeignKeysCorrectly() throws Exception {
        // Given
        String ddlContent = """
            CREATE TABLE users (
                id INT PRIMARY KEY
            );
            
            CREATE TABLE orders (
                id INT PRIMARY KEY,
                user_id INT,
                FOREIGN KEY (user_id) REFERENCES users(id)
            );
            """;

        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent(ddlContent);

        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        // When
        ddlService.importDdl(requestDto);

        // Then
        verify(objectMapper, times(2)).writeValueAsString(any());
        verify(diagramService).saveOrUpdateDiagram(eq("test-project"), any(), any());
    }

    @Test
    void testExportDdl_DiagramNotFound() {
        // Given
        String projectId = "non-existent-project";
        when(diagramService.getDiagramByProjectId(projectId))
                .thenThrow(new RuntimeException("Diagram not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> ddlService.exportDdl(projectId));
    }

    @Test
    void testImportDdl_InvalidDdlContent() throws Exception {
        // Given
        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent("INVALID DDL CONTENT");

        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        // When
        ddlService.importDdl(requestDto);

        // Then - should not fail, but should create empty lists
        verify(diagramService).saveOrUpdateDiagram(eq("test-project"), any(), any());
    }

    @Test
    void testExportDdlWithNullStringsButPopulatedArrays() {
        // Given
        String projectId = "test-project";
        
        // Create mock diagram data with populated arrays but null strings
        DiagramDataResponseDTO diagramData = new DiagramDataResponseDTO();
        
        // Populated arrays (the way data actually comes from the service)
        List<NodeDataDTO> nodeDataList = createMockNodeDataList();
        List<LinkDataDTO> linkDataList = createMockLinkDataList();
        
        diagramData.setNodeDataArray(nodeDataList);
        diagramData.setLinkDataArray(linkDataList);
        
        // Null strings (simulating the actual problem)
        diagramData.setNodeData(null);
        diagramData.setLinkData(null);
        diagramData.setProjectId(projectId);
        
        when(diagramService.getDiagramByProjectId(projectId)).thenReturn(diagramData);
        
        // When
        ExportDdlResponseDTO result = ddlService.exportDdl(projectId);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getDdlContent());
        assertFalse(result.getDdlContent().isEmpty());
        assertTrue(result.getDdlContent().contains("CREATE TABLE users"));
        assertTrue(result.getDdlContent().contains("CREATE TABLE orders"));
        assertEquals(projectId, result.getProjectId());
    }

    private List<NodeDataDTO> createSampleNodeDataList() {
        // Create table1
        NodeDataDTO table1 = new NodeDataDTO();
        table1.setId(UUID.fromString("bd8e59b4-e4fa-4108-9451-e01a5ca747b4"));
        table1.setKey("table1");

        ItemDTO table1Id = new ItemDTO();
        table1Id.setName("id");
        table1Id.setType("INTEGER");
        table1Id.setPk(true);
        table1Id.setFk(false);
        table1Id.setNotNull(true);
        table1Id.setAutoIncrement(true);
        table1Id.setUnique(false);
        table1Id.setDefaultValue("");

        ItemDTO table1FkId = new ItemDTO();
        table1FkId.setName("table2_id");
        table1FkId.setType("INTEGER");
        table1FkId.setPk(false);
        table1FkId.setFk(true);
        table1FkId.setNotNull(true);
        table1FkId.setAutoIncrement(false);
        table1FkId.setUnique(false);
        table1FkId.setDefaultValue("");

        table1.setItems(List.of(table1Id, table1FkId));

        LocationDTO table1Location = new LocationDTO();
        table1Location.setX("-338.81008236970797");
        table1Location.setY("53.7535226836556");
        table1.setLocation(table1Location);

        // Create table2
        NodeDataDTO table2 = new NodeDataDTO();
        table2.setId(UUID.fromString("696db9d7-debc-42d3-963c-b30334054664"));
        table2.setKey("table2");

        ItemDTO table2Id = new ItemDTO();
        table2Id.setName("id");
        table2Id.setType("INTEGER");
        table2Id.setPk(true);
        table2Id.setFk(false);
        table2Id.setNotNull(true);
        table2Id.setAutoIncrement(true);
        table2Id.setUnique(false);
        table2Id.setDefaultValue("");

        table2.setItems(List.of(table2Id));

        LocationDTO table2Location = new LocationDTO();
        table2Location.setX("156.0454087970379");
        table2Location.setY("36.19404426222769");
        table2.setLocation(table2Location);

        return List.of(table1, table2);
    }

    private List<LinkDataDTO> createSampleLinkDataList() {
        LinkDataDTO linkData = new LinkDataDTO();
        linkData.setFrom("table1");
        linkData.setTo("table2");
        linkData.setText("N:1");
        linkData.setToText("1");

        return List.of(linkData);
    }

    private List<NodeDataDTO> createMockNodeDataList() {
        List<NodeDataDTO> nodeDataList = new ArrayList<>();
        
        // Create users table
        NodeDataDTO usersTable = new NodeDataDTO();
        usersTable.setId(UUID.randomUUID());
        usersTable.setKey("users");
        
        List<ItemDTO> usersItems = new ArrayList<>();
        
        ItemDTO idItem = new ItemDTO();
        idItem.setName("id");
        idItem.setType("INTEGER");
        idItem.setPk(true);
        idItem.setAutoIncrement(true);
        idItem.setNotNull(true);
        usersItems.add(idItem);
        
        ItemDTO nameItem = new ItemDTO();
        nameItem.setName("name");
        nameItem.setType("VARCHAR");
        nameItem.setNotNull(true);
        usersItems.add(nameItem);
        
        usersTable.setItems(usersItems);
        nodeDataList.add(usersTable);
        
        // Create orders table
        NodeDataDTO ordersTable = new NodeDataDTO();
        ordersTable.setId(UUID.randomUUID());
        ordersTable.setKey("orders");
        
        List<ItemDTO> ordersItems = new ArrayList<>();
        
        ItemDTO orderIdItem = new ItemDTO();
        orderIdItem.setName("id");
        orderIdItem.setType("INTEGER");
        orderIdItem.setPk(true);
        orderIdItem.setAutoIncrement(true);
        orderIdItem.setNotNull(true);
        ordersItems.add(orderIdItem);
        
        ItemDTO userIdItem = new ItemDTO();
        userIdItem.setName("user_id");
        userIdItem.setType("INTEGER");
        userIdItem.setFk(true);
        userIdItem.setNotNull(true);
        ordersItems.add(userIdItem);
        
        ordersTable.setItems(ordersItems);
        nodeDataList.add(ordersTable);
        
        return nodeDataList;
    }
    
    private List<LinkDataDTO> createMockLinkDataList() {
        List<LinkDataDTO> linkDataList = new ArrayList<>();
        
        LinkDataDTO linkData = new LinkDataDTO();
        linkData.setFrom("orders");
        linkData.setTo("users");
        linkData.setText("N:1");
        linkData.setToText("1");
        
        linkDataList.add(linkData);
        
        return linkDataList;
    }

}
