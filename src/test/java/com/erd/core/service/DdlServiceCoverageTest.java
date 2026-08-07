package com.erd.core.service;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Complements {@link DdlServiceTest}, which covers the feature's happy paths and its regression
 * cases. This class targets the remaining decision points: both type-mapping tables, the defensive
 * branches of the SQL generator, and the parser's skip rules. It uses a real Jackson
 * {@link ObjectMapper} so the produced JSON can be asserted directly.
 */
@ExtendWith(MockitoExtension.class)
class DdlServiceCoverageTest {

    private static final String PROJECT_ID = "project-1";

    @Mock
    private DiagramService diagramService;

    private DdlService ddlService;

    @BeforeEach
    void setUp() {
        ddlService = new DdlService(diagramService, new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ItemDTO item(String name, String type) {
        ItemDTO item = new ItemDTO();
        item.setName(name);
        item.setType(type);
        item.setPk(false);
        item.setFk(false);
        item.setNotNull(false);
        item.setAutoIncrement(false);
        item.setUnique(false);
        item.setDefaultValue("");
        return item;
    }

    private NodeDataDTO node(String key, List<ItemDTO> items) {
        NodeDataDTO nodeData = new NodeDataDTO();
        nodeData.setId(UUID.randomUUID());
        nodeData.setKey(key);
        nodeData.setItems(items);
        return nodeData;
    }

    private DiagramDataResponseDTO diagram(List<NodeDataDTO> nodes, List<LinkDataDTO> links) {
        DiagramDataResponseDTO diagramData = new DiagramDataResponseDTO();
        diagramData.setNodeDataArray(nodes);
        diagramData.setLinkDataArray(links);
        diagramData.setProjectId(PROJECT_ID);
        return diagramData;
    }

    private String exportOf(DiagramDataResponseDTO diagramData) {
        when(diagramService.getDiagramByProjectId(PROJECT_ID)).thenReturn(diagramData);
        ExportDdlResponseDTO response = ddlService.exportDdl(PROJECT_ID);
        assertEquals(PROJECT_ID, response.getProjectId());
        return response.getDdlContent();
    }

    private String importAndCaptureNodeJson(String ddl) {
        ImportDdlRequestDTO request = new ImportDdlRequestDTO();
        request.setProjectId(PROJECT_ID);
        request.setDdlContent(ddl);

        ddlService.importDdl(request);

        ArgumentCaptor<String> nodeJson = ArgumentCaptor.forClass(String.class);
        verify(diagramService).saveOrUpdateDiagram(eqProjectId(), nodeJson.capture(), anyString());
        return nodeJson.getValue();
    }

    private String eqProjectId() {
        return org.mockito.ArgumentMatchers.eq(PROJECT_ID);
    }

    // ------------------------------------------------------------------
    // Export - type mapping
    // ------------------------------------------------------------------

    @Test
    void testExportDdl_mapsEveryDiagramTypeToItsSqlEquivalent() {
        // Given - one column per branch of the diagram-type to SQL-type mapping
        List<ItemDTO> items = new ArrayList<>(List.of(
                item("c_integer", "INTEGER"),
                item("c_int", "INT"),
                item("c_bigint", "BIGINT"),
                item("c_smallint", "SMALLINT"),
                item("c_tinyint", "TINYINT"),
                item("c_decimal", "DECIMAL"),
                item("c_numeric", "NUMERIC"),
                item("c_float", "FLOAT"),
                item("c_double", "DOUBLE"),
                item("c_varchar", "VARCHAR"),
                item("c_char", "CHAR"),
                item("c_text", "TEXT"),
                item("c_date", "DATE"),
                item("c_time", "TIME"),
                item("c_datetime", "DATETIME"),
                item("c_timestamp", "TIMESTAMP"),
                item("c_boolean", "BOOLEAN"),
                item("c_bool", "BOOL"),
                item("c_unknown", "JSONB")));

        // When
        String ddl = exportOf(diagram(List.of(node("all_types", items)), List.of()));

        // Then
        assertTrue(ddl.contains("c_integer INT"));
        assertTrue(ddl.contains("c_int INT"));
        assertTrue(ddl.contains("c_bigint BIGINT"));
        assertTrue(ddl.contains("c_smallint SMALLINT"));
        assertTrue(ddl.contains("c_tinyint TINYINT"));
        assertTrue(ddl.contains("c_decimal DECIMAL"));
        assertTrue(ddl.contains("c_numeric DECIMAL"));
        assertTrue(ddl.contains("c_float FLOAT"));
        assertTrue(ddl.contains("c_double DOUBLE"));
        assertTrue(ddl.contains("c_varchar VARCHAR(255)"));
        assertTrue(ddl.contains("c_char CHAR(1)"));
        assertTrue(ddl.contains("c_text TEXT"));
        assertTrue(ddl.contains("c_date DATE"));
        assertTrue(ddl.contains("c_time TIME"));
        assertTrue(ddl.contains("c_datetime DATETIME"));
        assertTrue(ddl.contains("c_timestamp DATETIME"));
        assertTrue(ddl.contains("c_boolean BOOLEAN"));
        assertTrue(ddl.contains("c_bool BOOLEAN"));
        assertTrue(ddl.contains("c_unknown VARCHAR(255)"),
                "An unrecognised type must fall back to VARCHAR(255)");
    }

    // ------------------------------------------------------------------
    // Export - empty and defensive branches
    // ------------------------------------------------------------------

    @Test
    void testExportDdl_returnsAPlaceholderWhenTheNodeArrayIsNull() {
        // When & Then
        assertEquals("-- No tables found in diagram", exportOf(diagram(null, null)));
    }

    @Test
    void testExportDdl_returnsAPlaceholderWhenTheNodeArrayIsEmpty() {
        // When & Then
        assertEquals("-- No tables found in diagram", exportOf(diagram(List.of(), List.of())));
    }

    @Test
    void testExportDdl_omitsAlterStatementsWhenTheLinkArrayIsNull() {
        // Given
        NodeDataDTO users = node("users", List.of(item("name", "VARCHAR")));

        // When
        String ddl = exportOf(diagram(List.of(users), null));

        // Then
        assertTrue(ddl.contains("CREATE TABLE users"));
        assertFalse(ddl.contains("ALTER TABLE"));
    }

    @Test
    void testExportDdl_omitsAlterStatementsWhenTheLinkArrayIsEmpty() {
        // Given
        NodeDataDTO users = node("users", List.of(item("name", "VARCHAR")));

        // When
        String ddl = exportOf(diagram(List.of(users), List.of()));

        // Then
        assertFalse(ddl.contains("ALTER TABLE"));
    }

    @Test
    void testExportDdl_rendersColumnConstraints() {
        // Given
        ItemDTO id = item("id", "INTEGER");
        id.setPk(true);
        id.setNotNull(true);
        id.setAutoIncrement(true);

        ItemDTO email = item("email", "VARCHAR");
        email.setUnique(true);
        email.setDefaultValue("'unknown'");

        // When
        String ddl = exportOf(diagram(List.of(node("users", List.of(id, email))), List.of()));

        // Then
        assertTrue(ddl.contains("id INT NOT NULL AUTO_INCREMENT"));
        assertTrue(ddl.contains("email VARCHAR(255) DEFAULT 'unknown' UNIQUE"));
        assertTrue(ddl.contains("PRIMARY KEY (id)"));
    }

    @Test
    void testExportDdl_omitsThePrimaryKeyClauseAndTrailingCommaForASingleUnkeyedColumn() {
        // Given - the last column of a table without a primary key must not be followed by a comma
        String ddl = exportOf(diagram(List.of(node("logs", List.of(item("message", "TEXT")))), List.of()));

        // Then
        assertTrue(ddl.contains("message TEXT\n"));
        assertFalse(ddl.contains("message TEXT,"));
        assertFalse(ddl.contains("PRIMARY KEY"));
    }

    @Test
    void testExportDdl_wrapsGenerationFailures() {
        // Given - a malformed node (no column list) makes the generator fail
        NodeDataDTO broken = new NodeDataDTO();
        broken.setKey("broken");
        broken.setItems(null);

        when(diagramService.getDiagramByProjectId(PROJECT_ID)).thenReturn(diagram(List.of(broken), List.of()));

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> ddlService.exportDdl(PROJECT_ID));

        // Then
        assertEquals("Failed to generate DDL", exception.getMessage());
    }

    // ------------------------------------------------------------------
    // Export - foreign key resolution
    // ------------------------------------------------------------------

    @Test
    void testExportDdl_throwsWhenTheLinkSourceTableIsMissing() {
        // Given
        LinkDataDTO link = new LinkDataDTO();
        link.setFrom("ghost");
        link.setTo("users");

        when(diagramService.getDiagramByProjectId(PROJECT_ID))
                .thenReturn(diagram(List.of(node("users", List.of(item("id", "INTEGER")))), List.of(link)));

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> ddlService.exportDdl(PROJECT_ID));

        // Then
        assertEquals("Failed to generate DDL", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("Table not found: ghost"));
    }

    @Test
    void testExportDdl_throwsWhenTheLinkTargetTableIsMissing() {
        // Given
        ItemDTO fk = item("ghost_id", "INTEGER");
        fk.setFk(true);

        LinkDataDTO link = new LinkDataDTO();
        link.setFrom("orders");
        link.setTo("ghosts");
        link.setFromColumn("ghost_id");

        when(diagramService.getDiagramByProjectId(PROJECT_ID))
                .thenReturn(diagram(List.of(node("orders", List.of(fk))), List.of(link)));

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> ddlService.exportDdl(PROJECT_ID));

        // Then
        assertTrue(exception.getCause().getMessage().contains("Table not found: ghosts"));
    }

    @Test
    void testExportDdl_fallsBackToTheDefaultColumnNameWhenNoForeignKeyMatches() {
        // Given - a blank fromColumn forces the heuristic, and no FK column matches the target name
        ItemDTO unrelatedFk = item("category_ref", "INTEGER");
        unrelatedFk.setFk(true);

        ItemDTO usersId = item("id", "INTEGER");
        usersId.setPk(true);

        LinkDataDTO link = new LinkDataDTO();
        link.setFrom("orders");
        link.setTo("users");
        link.setFromColumn("");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(unrelatedFk)), node("users", List.of(usersId))),
                List.of(link)));

        // Then
        assertTrue(ddl.contains("FOREIGN KEY (users_id) REFERENCES users(id)"),
                "Without a stored or matching FK column the generator falls back to <table>_id");
    }

    @Test
    void testExportDdl_fallsBackToIdWhenTheTargetTableHasNoPrimaryKey() {
        // Given
        ItemDTO fk = item("users_id", "INTEGER");
        fk.setFk(true);

        LinkDataDTO link = new LinkDataDTO();
        link.setFrom("orders");
        link.setTo("users");
        link.setFromColumn("users_id");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(fk)), node("users", List.of(item("name", "VARCHAR")))),
                List.of(link)));

        // Then
        assertTrue(ddl.contains("REFERENCES users(id)"));
    }

    // ------------------------------------------------------------------
    // Import - type mapping
    // ------------------------------------------------------------------

    @Test
    void testImportDdl_mapsEverySqlTypeToItsDiagramEquivalent() {
        // Given - one column per branch of the SQL-type to diagram-type mapping
        String ddl = """
                CREATE TABLE all_types (
                    c_int INT,
                    c_integer INTEGER,
                    c_bigint BIGINT,
                    c_smallint SMALLINT,
                    c_tinyint TINYINT,
                    c_decimal DECIMAL(10,2),
                    c_numeric NUMERIC(8,3),
                    c_float FLOAT,
                    c_double DOUBLE,
                    c_varchar VARCHAR(255),
                    c_char CHAR(1),
                    c_text TEXT,
                    c_date DATE,
                    c_time TIME,
                    c_datetime DATETIME,
                    c_timestamp TIMESTAMP,
                    c_boolean BOOLEAN,
                    c_bool BOOL,
                    c_unknown JSONB
                );
                """;

        // When
        String nodeJson = importAndCaptureNodeJson(ddl);

        // Then
        assertTrue(nodeJson.contains("\"name\":\"c_int\",\"type\":\"INTEGER\""));
        assertTrue(nodeJson.contains("\"name\":\"c_integer\",\"type\":\"INTEGER\""));
        assertTrue(nodeJson.contains("\"name\":\"c_bigint\",\"type\":\"BIGINT\""));
        assertTrue(nodeJson.contains("\"name\":\"c_smallint\",\"type\":\"SMALLINT\""));
        assertTrue(nodeJson.contains("\"name\":\"c_tinyint\",\"type\":\"TINYINT\""));
        assertTrue(nodeJson.contains("\"name\":\"c_decimal\",\"type\":\"DECIMAL\""));
        assertTrue(nodeJson.contains("\"name\":\"c_numeric\",\"type\":\"DECIMAL\""));
        assertTrue(nodeJson.contains("\"name\":\"c_float\",\"type\":\"FLOAT\""));
        assertTrue(nodeJson.contains("\"name\":\"c_double\",\"type\":\"DOUBLE\""));
        assertTrue(nodeJson.contains("\"name\":\"c_varchar\",\"type\":\"VARCHAR\""));
        assertTrue(nodeJson.contains("\"name\":\"c_char\",\"type\":\"CHAR\""));
        assertTrue(nodeJson.contains("\"name\":\"c_text\",\"type\":\"TEXT\""));
        assertTrue(nodeJson.contains("\"name\":\"c_date\",\"type\":\"DATE\""));
        assertTrue(nodeJson.contains("\"name\":\"c_time\",\"type\":\"TIME\""));
        assertTrue(nodeJson.contains("\"name\":\"c_datetime\",\"type\":\"DATETIME\""));
        assertTrue(nodeJson.contains("\"name\":\"c_timestamp\",\"type\":\"DATETIME\""));
        assertTrue(nodeJson.contains("\"name\":\"c_boolean\",\"type\":\"BOOLEAN\""));
        assertTrue(nodeJson.contains("\"name\":\"c_bool\",\"type\":\"BOOLEAN\""));
        assertTrue(nodeJson.contains("\"name\":\"c_unknown\",\"type\":\"VARCHAR\""),
                "An unrecognised SQL type must fall back to VARCHAR");
    }

    // ------------------------------------------------------------------
    // Import - parser skip rules
    // ------------------------------------------------------------------

    @Test
    void testImportDdl_skipsNamedConstraintLines() {
        // Given
        String ddl = """
                CREATE TABLE orders (
                    id INT PRIMARY KEY,
                    total DECIMAL(10,2),
                    CONSTRAINT chk_total CHECK (total > 0)
                );
                """;

        // When
        String nodeJson = importAndCaptureNodeJson(ddl);

        // Then
        assertTrue(nodeJson.contains("\"name\":\"id\""));
        assertTrue(nodeJson.contains("\"name\":\"total\""));
        assertFalse(nodeJson.contains("chk_total"), "A CONSTRAINT line is not a column");
    }

    @Test
    void testImportDdl_ignoresLinesThatAreNotColumnDefinitions() {
        // Given - UNIQUE(...) has no "name TYPE" shape, so the column parser rejects it
        String ddl = """
                CREATE TABLE users (
                    id INT PRIMARY KEY,
                    email VARCHAR(255),
                    UNIQUE(email)
                );
                """;

        // When
        String nodeJson = importAndCaptureNodeJson(ddl);

        // Then
        assertTrue(nodeJson.contains("\"name\":\"id\""));
        assertTrue(nodeJson.contains("\"name\":\"email\""));
        assertFalse(nodeJson.contains("\"name\":\"UNIQUE\""));
    }

    @Test
    void testImportDdl_doesNotMarkAPrimaryKeyIdColumnAsForeign() {
        // Given - user_id is both a primary key and an _id column
        String ddl = """
                CREATE TABLE profiles (
                    user_id INT PRIMARY KEY,
                    bio TEXT
                );
                """;

        // When
        String nodeJson = importAndCaptureNodeJson(ddl);

        // Then
        assertTrue(nodeJson.contains("\"name\":\"user_id\",\"type\":\"INTEGER\",\"pk\":true,\"fk\":false"),
                "A primary key must never also be reported as a foreign key");
    }

    @Test
    void testImportDdl_ignoresForeignKeysPointingOutsideTheScript() {
        // Given - warehouses is not declared in this script
        String ddl = """
                CREATE TABLE stock (
                    id INT PRIMARY KEY,
                    warehouse_id INT,
                    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
                );
                """;
        ImportDdlRequestDTO request = new ImportDdlRequestDTO();
        request.setProjectId(PROJECT_ID);
        request.setDdlContent(ddl);

        // When
        ddlService.importDdl(request);

        // Then
        ArgumentCaptor<String> linkJson = ArgumentCaptor.forClass(String.class);
        verify(diagramService).saveOrUpdateDiagram(eqProjectId(), anyString(), linkJson.capture());
        assertEquals("[]", linkJson.getValue(),
                "A relationship can only be drawn between two tables of the same diagram");
    }

    // ------------------------------------------------------------------
    // Import - serialisation failure
    // ------------------------------------------------------------------

    @Test
    void testImportDdl_wrapsSerialisationFailures() throws Exception {
        // Given
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        DdlService serviceWithFailingMapper = new DdlService(diagramService, failingMapper);

        ImportDdlRequestDTO request = new ImportDdlRequestDTO();
        request.setProjectId(PROJECT_ID);
        request.setDdlContent("CREATE TABLE users (id INT PRIMARY KEY);");

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> serviceWithFailingMapper.importDdl(request));

        // Then
        assertEquals("Failed to import DDL", exception.getMessage());
        verify(diagramService, never()).saveOrUpdateDiagram(anyString(), anyString(), anyString());
    }

}
