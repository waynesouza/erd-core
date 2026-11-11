package com.erd.core.service;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.exception.DdlImportException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private CollaborationNotifier collaborationNotifier;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DdlService ddlService;

    @BeforeEach
    void setUp() {
        ddlService = new DdlService(diagramService, webSocketService, collaborationService, collaborationNotifier);
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

        try {
            return objectMapper.writeValueAsString(importedNodes());
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<NodeDataDTO> importedNodes() {
        ArgumentCaptor<List<NodeDataDTO>> nodes = ArgumentCaptor.forClass(List.class);
        verify(webSocketService).replaceDiagram(eq(PROJECT_ID), nodes.capture(), any());
        return nodes.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<LinkDataDTO> importedLinks() {
        ArgumentCaptor<List<LinkDataDTO>> links = ArgumentCaptor.forClass(List.class);
        verify(webSocketService).replaceDiagram(eq(PROJECT_ID), any(), links.capture());
        return links.getValue();
    }

    private void importScript(String ddl) {
        ImportDdlRequestDTO request = new ImportDdlRequestDTO();
        request.setProjectId(PROJECT_ID);
        request.setDdlContent(ddl);
        ddlService.importDdl(request);
    }

    private ItemDTO pk(String name) {
        ItemDTO item = item(name, "INTEGER");
        item.setPk(true);
        return item;
    }

    private ItemDTO fk(String name) {
        ItemDTO item = item(name, "INTEGER");
        item.setFk(true);
        return item;
    }

    private LinkDataDTO link(String from, String to, String fromColumn) {
        LinkDataDTO link = new LinkDataDTO();
        link.setFrom(from);
        link.setTo(to);
        link.setFromColumn(fromColumn);
        return link;
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
    void testExportDdl_commentsOutALinkWhoseSourceTableIsMissing() {
        // Given
        LinkDataDTO link = link("ghost", "users", null);

        // When
        String ddl = exportOf(diagram(List.of(node("users", List.of(pk("id")))), List.of(link)));

        // Then - one dangling link must not make the whole export fail
        assertTrue(ddl.contains("CREATE TABLE users"));
        assertTrue(ddl.contains("-- Relationship ghost -> users was not exported: one of its tables is not in the diagram."));
        assertFalse(ddl.contains("ALTER TABLE"));
    }

    @Test
    void testExportDdl_commentsOutALinkWhoseTargetTableIsMissing() {
        // Given
        LinkDataDTO link = link("orders", "ghosts", "ghost_id");

        // When
        String ddl = exportOf(diagram(List.of(node("orders", List.of(pk("id"), fk("ghost_id")))), List.of(link)));

        // Then
        assertTrue(ddl.contains("-- Relationship orders -> ghosts was not exported: one of its tables is not in the diagram."));
    }

    @Test
    void testExportDdl_commentsOutALinkWithoutAForeignKeyColumnInsteadOfInventingOne() {
        // Given - a blank fromColumn forces the heuristic, and no FK column matches either table
        LinkDataDTO link = link("orders", "users", "");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(pk("id"), fk("category_ref"))), node("users", List.of(pk("id")))),
                List.of(link)));

        // Then - users_id does not exist, so a statement using it would fail in MySQL
        assertTrue(ddl.contains("-- Relationship orders -> users was not exported: no foreign key column was found in either table."));
        assertFalse(ddl.contains("users_id"));
    }

    @Test
    void testExportDdl_commentsOutALinkToATableWithoutAPrimaryKey() {
        // Given
        LinkDataDTO link = link("orders", "users", "users_id");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(fk("users_id"))), node("users", List.of(item("name", "VARCHAR")))),
                List.of(link)));

        // Then
        assertTrue(ddl.contains("-- Relationship orders -> users was not exported: table users has no primary key."));
        assertFalse(ddl.contains("REFERENCES users(id)"));
    }

    @Test
    void testExportDdl_usesTheRecordedColumnEvenWhenItIsNotNamedAfterTheTarget() {
        // Given
        LinkDataDTO link = link("orders", "users", "buyer");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(pk("id"), item("buyer", "INTEGER"))), node("users", List.of(pk("id")))),
                List.of(link)));

        // Then
        assertTrue(ddl.contains("ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (buyer) REFERENCES users(id);"));
    }

    @Test
    void testExportDdl_fallsBackToTheNamingHeuristicWhenTheRecordedColumnWasRenamed() {
        // Given - the link still names the column the editor created, which was renamed afterwards
        LinkDataDTO link = link("orders", "users", "users_id");

        // When
        String ddl = exportOf(diagram(
                List.of(node("orders", List.of(pk("id"), fk("user_id"))), node("users", List.of(pk("id")))),
                List.of(link)));

        // Then
        assertTrue(ddl.contains("FOREIGN KEY (user_id) REFERENCES users(id);"));
    }

    @Test
    void testExportDdl_putsTheKeyOnTheTargetWhenOnlyTheTargetHoldsTheColumn() {
        // Given - the first link of a many-to-many relationship, as the editor used to draw it:
        // from the entity to the intermediary table, which holds the foreign key
        NodeDataDTO students = node("students", List.of(pk("id")));
        NodeDataDTO courses = node("courses", List.of(pk("id")));
        NodeDataDTO studentsCourses = node("students_courses", List.of(fk("students_id"), fk("courses_id")));

        // When
        String ddl = exportOf(diagram(List.of(students, courses, studentsCourses), List.of(
                link("students", "students_courses", null),
                link("students_courses", "courses", null))));

        // Then
        assertTrue(ddl.contains("ALTER TABLE students_courses ADD CONSTRAINT fk_students_courses_students "
                + "FOREIGN KEY (students_id) REFERENCES students(id);"));
        assertTrue(ddl.contains("ALTER TABLE students_courses ADD CONSTRAINT fk_students_courses_courses "
                + "FOREIGN KEY (courses_id) REFERENCES courses(id);"));
        assertFalse(ddl.contains("ALTER TABLE students "));
    }

    @Test
    void testExportDdl_writesAKeyReachedByTwoLinksOnlyOnce() {
        // Given - two links that resolve to the same foreign key
        NodeDataDTO orders = node("orders", List.of(pk("id"), fk("user_id")));
        NodeDataDTO users = node("users", List.of(pk("id")));

        // When
        String ddl = exportOf(diagram(List.of(orders, users), List.of(
                link("orders", "users", "user_id"),
                link("users", "orders", null))));

        // Then
        String statement = "ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id);";
        assertEquals(ddl.indexOf(statement), ddl.lastIndexOf(statement));
    }

    @Test
    void testExportDdl_skipsTablesWithoutAKeyAndColumnsWithoutAName() {
        // Given - malformed entries the generator must step over while looking for the key
        NodeDataDTO unnamedTable = node(null, List.of(pk("id")));
        ItemDTO unnamedColumn = fk(null);
        NodeDataDTO orders = node("orders", List.of(pk("id"), unnamedColumn, fk("user_id")));
        NodeDataDTO users = node("users", List.of(pk("id")));

        // When
        String ddl = exportOf(diagram(List.of(unnamedTable, orders, users), List.of(link("orders", "users", null))));

        // Then
        assertTrue(ddl.contains("FOREIGN KEY (user_id) REFERENCES users(id);"));
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

        // When
        importScript(ddl);

        // Then
        assertTrue(importedLinks().isEmpty(),
                "A relationship can only be drawn between two tables of the same diagram");
    }

    // ------------------------------------------------------------------
    // Import - ALTER TABLE foreign keys, ids and collaboration
    // ------------------------------------------------------------------

    @Test
    void testImportDdl_readsForeignKeysDeclaredWithAlterTable() {
        // Given - the layout the export writes: tables first, keys afterwards
        String ddl = """
                CREATE TABLE users (
                    id INT NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (id)
                );

                CREATE TABLE orders (
                    id INT NOT NULL AUTO_INCREMENT,
                    buyer INT NOT NULL,
                    PRIMARY KEY (id)
                );

                ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (buyer) REFERENCES users(id);
                """;

        // When
        importScript(ddl);

        // Then
        List<LinkDataDTO> links = importedLinks();
        assertEquals(1, links.size());
        assertEquals("orders", links.get(0).getFrom());
        assertEquals("users", links.get(0).getTo());
        assertEquals("buyer", links.get(0).getFromColumn());
        ItemDTO buyer = importedNodes().get(1).getItems().get(1);
        assertTrue(buyer.getFk(), "A column used by a foreign key is a foreign key whatever its name");
    }

    @Test
    void testImportDdl_readsAnAlterTableWithoutAConstraintName() {
        // Given
        String ddl = """
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE orders (id INT PRIMARY KEY, user_id INT);
                ALTER TABLE orders ADD FOREIGN KEY (user_id) REFERENCES users(id);
                """;

        // When
        importScript(ddl);

        // Then
        assertEquals(1, importedLinks().size());
    }

    @Test
    void testImportDdl_ignoresAnAlterTableOnATableTheScriptDoesNotCreate() {
        // Given
        String ddl = """
                CREATE TABLE users (id INT PRIMARY KEY);
                ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id);
                """;

        // When
        importScript(ddl);

        // Then
        assertTrue(importedLinks().isEmpty());
    }

    @Test
    void testImportDdl_turnsAKeyDeclaredTwiceIntoOneLink() {
        // Given - the same key inside the table and again in an ALTER TABLE
        String ddl = """
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE orders (
                    id INT PRIMARY KEY,
                    user_id INT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id);
                """;

        // When
        importScript(ddl);

        // Then
        assertEquals(1, importedLinks().size());
    }

    @Test
    void testImportDdl_givesEveryLinkAnId() {
        // Given
        String ddl = """
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, FOREIGN KEY (user_id) REFERENCES users(id));
                CREATE TABLE reviews (id INT PRIMARY KEY, user_id INT, FOREIGN KEY (user_id) REFERENCES users(id));
                """;

        // When
        importScript(ddl);

        // Then - a link without an id cannot be addressed by a patch, so deleting it would not stick
        List<String> ids = importedLinks().stream().map(LinkDataDTO::getId).toList();
        assertEquals(2, ids.size());
        ids.forEach(id -> assertDoesNotThrow(() -> UUID.fromString(id)));
        assertEquals(2, ids.stream().distinct().count());
    }

    @Test
    void testImportDdl_releasesTheProjectLocksAndAnnouncesEachOne() {
        // Given
        EntityLockDTO first = new EntityLockDTO("table-a", "ada@example.com", "ada@example.com", "Ada", null, PROJECT_ID);
        EntityLockDTO second = new EntityLockDTO("table-b", "bob@example.com", "bob@example.com", "Bob", null, PROJECT_ID);
        when(collaborationService.clearProjectLocks(PROJECT_ID)).thenReturn(List.of(first, second));

        // When
        importScript("CREATE TABLE users (id INT PRIMARY KEY);");

        // Then - the diagram is replaced before the locks of its old tables are released
        InOrder order = inOrder(webSocketService, collaborationService, collaborationNotifier);
        order.verify(webSocketService).replaceDiagram(eq(PROJECT_ID), any(), any());
        order.verify(collaborationService).clearProjectLocks(PROJECT_ID);
        order.verify(collaborationNotifier).lockReleased(first);
        order.verify(collaborationNotifier).lockReleased(second);
    }

    @Test
    void testImportDdl_rejectsAMissingScriptWithoutTouchingTheDiagram() {
        // Given
        ImportDdlRequestDTO request = new ImportDdlRequestDTO();
        request.setProjectId(PROJECT_ID);
        request.setDdlContent(null);

        // When & Then
        assertThrows(DdlImportException.class, () -> ddlService.importDdl(request));
        verify(webSocketService, never()).replaceDiagram(any(), any(), any());
        verifyNoInteractions(collaborationService, collaborationNotifier);
    }

}
