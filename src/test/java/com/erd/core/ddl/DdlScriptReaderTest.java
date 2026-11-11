package com.erd.core.ddl;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DdlScriptReaderTest {

    private static NodeDataDTO table(ImportedSchema schema, String name) {
        return schema.nodes().stream().filter(node -> node.getKey().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No table " + name + " in " + keys(schema)));
    }

    private static ItemDTO column(ImportedSchema schema, String tableName, String columnName) {
        return table(schema, tableName).getItems().stream().filter(item -> item.getName().equals(columnName)).findFirst()
                .orElseThrow(() -> new AssertionError("No column " + columnName + " in " + tableName));
    }

    private static List<String> keys(ImportedSchema schema) {
        return schema.nodes().stream().map(NodeDataDTO::getKey).toList();
    }

    private static List<String> columnNames(ImportedSchema schema, String tableName) {
        return table(schema, tableName).getItems().stream().map(ItemDTO::getName).toList();
    }

    private static List<String> links(ImportedSchema schema) {
        return schema.links().stream()
                .map(link -> link.getFrom() + "." + link.getFromColumn() + " -> " + link.getTo() + " " + link.getText())
                .toList();
    }

    // ------------------------------------------------------------------
    // Scripts produced by MySQL tools
    // ------------------------------------------------------------------

    @Test
    void testRead_aMysqldumpScript() {
        String script = """
                -- MySQL dump 10.13  Distrib 8.0.36
                /*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
                DROP TABLE IF EXISTS `customers`;
                /*!40101 SET @saved_cs_client     = @@character_set_client */;
                CREATE TABLE `customers` (
                  `id` int NOT NULL AUTO_INCREMENT,
                  `email` varchar(150) NOT NULL,
                  `status` varchar(20) DEFAULT 'em aberto',
                  `active` tinyint(1) NOT NULL DEFAULT '1',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_email` (`email`)
                ) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
                LOCK TABLES `customers` WRITE;
                INSERT INTO `customers` VALUES (1,'a@b.c','ok; done',1);
                UNLOCK TABLES;
                CREATE TABLE `orders` (
                  `id` int NOT NULL AUTO_INCREMENT,
                  -- who placed the order
                  `customer_id` int NOT NULL,
                  `total` decimal(10,2) unsigned DEFAULT '0.00',
                  `created_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_customer` (`customer_id`),
                  CONSTRAINT `orders_chk_1` CHECK ((`total` >= 0)),
                  CONSTRAINT `fk_orders_customers` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE
                ) ENGINE=InnoDB;
                """;

        ImportedSchema schema = DdlScriptReader.read(script);

        assertEquals(List.of("customers", "orders"), keys(schema));
        assertEquals(List.of("id", "email", "status", "active"), columnNames(schema, "customers"));
        assertEquals(List.of("id", "customer_id", "total", "created_at"), columnNames(schema, "orders"));
        ItemDTO id = column(schema, "customers", "id");
        assertTrue(id.getPk());
        assertTrue(id.getNotNull());
        assertTrue(id.getAutoIncrement());
        assertTrue(column(schema, "customers", "email").getUnique());
        assertEquals("em aberto", column(schema, "customers", "status").getDefaultValue());
        assertEquals("BOOLEAN", column(schema, "customers", "active").getType());
        assertEquals("1", column(schema, "customers", "active").getDefaultValue());
        assertEquals("0.00", column(schema, "orders", "total").getDefaultValue());
        assertEquals("TIMESTAMP", column(schema, "orders", "created_at").getType());
        assertEquals("CURRENT_TIMESTAMP", column(schema, "orders", "created_at").getDefaultValue());
        assertTrue(column(schema, "orders", "customer_id").getFk());
        assertEquals(List.of("orders.customer_id -> customers 1:N"), links(schema));
        assertEquals(List.of(), schema.warnings());
    }

    @Test
    void testRead_aPhpMyAdminScriptWithKeysDeclaredAfterTheTables() {
        String script = """
                CREATE TABLE `users` (
                  `id` int(11) NOT NULL,
                  `email` varchar(100) NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                CREATE TABLE `posts` (
                  `id` int(11) NOT NULL,
                  `author` int(11) NOT NULL,
                  `body` text
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                ALTER TABLE `users`
                  ADD PRIMARY KEY (`id`),
                  ADD UNIQUE KEY `email` (`email`);

                ALTER TABLE `posts`
                  ADD PRIMARY KEY (`id`),
                  ADD KEY `author` (`author`);

                ALTER TABLE `users`
                  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

                ALTER TABLE `posts`
                  ADD CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`author`) REFERENCES `users` (`id`);
                COMMIT;
                """;

        ImportedSchema schema = DdlScriptReader.read(script);

        assertTrue(column(schema, "users", "id").getPk());
        assertTrue(column(schema, "users", "id").getAutoIncrement(), "MODIFY must add AUTO_INCREMENT");
        assertTrue(column(schema, "users", "email").getUnique());
        assertTrue(column(schema, "posts", "id").getPk());
        assertEquals(List.of("posts.author -> users 1:N"), links(schema));
        assertEquals(List.of(), schema.warnings());
    }

    @Test
    void testRead_theScriptTheExportWrites() {
        // The export declares the keys with ALTER TABLE, so its output must import back whole.
        NodeDataDTO users = node("users", item("id", "INTEGER", true), item("email", "VARCHAR", false));
        NodeDataDTO orders = node("orders", item("id", "INTEGER", true), item("user_id", "INTEGER", false));
        orders.getItems().get(1).setFk(true);
        users.getItems().get(1).setUnique(true);
        users.getItems().get(1).setDefaultValue("it's unknown");
        LinkDataDTO link = new LinkDataDTO();
        link.setFrom("orders");
        link.setTo("users");
        link.setFromColumn("user_id");

        ImportedSchema schema = DdlScriptReader.read(DdlScriptWriter.write(List.of(users, orders), List.of(link)));

        assertEquals(List.of("users", "orders"), keys(schema));
        assertEquals("it's unknown", column(schema, "users", "email").getDefaultValue());
        assertTrue(column(schema, "users", "email").getUnique());
        assertEquals(List.of("orders.user_id -> users 1:N"), links(schema));
        assertEquals(List.of(), schema.warnings());
    }

    private static NodeDataDTO node(String key, ItemDTO... items) {
        NodeDataDTO node = new NodeDataDTO();
        node.setKey(key);
        node.setItems(List.of(items));
        return node;
    }

    private static ItemDTO item(String name, String type, boolean pk) {
        ItemDTO item = new ItemDTO();
        item.setName(name);
        item.setType(type);
        item.setPk(pk);
        item.setFk(false);
        item.setUnique(false);
        item.setNotNull(pk);
        item.setAutoIncrement(false);
        item.setDefaultValue("");
        return item;
    }

    // ------------------------------------------------------------------
    // Statements and tables
    // ------------------------------------------------------------------

    @Test
    void testRead_ignoresStatementsThatAreNotTableDefinitions() {
        ImportedSchema schema = DdlScriptReader.read("SET NAMES utf8; INSERT INTO a VALUES (1); DROP TABLE a;");

        assertEquals(List.of(), schema.nodes());
        assertEquals(List.of(), schema.warnings());
    }

    @Test
    void testRead_acceptsQualifiedNamesTemporaryTablesAndIfNotExists() {
        ImportedSchema schema = DdlScriptReader.read("""
                create temporary table if not exists `shop`.`carts` (id int primary key);
                CREATE TABLE "shop"."lines" (id INT PRIMARY KEY);
                """);

        assertEquals(List.of("carts", "lines"), keys(schema));
    }

    @Test
    void testRead_skipsTablesWithoutColumnDefinitions() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (id INT PRIMARY KEY);
                CREATE TABLE b LIKE a;
                CREATE TABLE c AS SELECT * FROM a;
                CREATE TABLE d
                """);

        assertEquals(List.of("a"), keys(schema));
        assertEquals(List.of(
                "Table b was skipped: it has no column definitions, as in CREATE TABLE ... LIKE or AS SELECT.",
                "Table c was skipped: it has no column definitions, as in CREATE TABLE ... LIKE or AS SELECT.",
                "Table d was skipped: it has no column definitions, as in CREATE TABLE ... LIKE or AS SELECT."),
                schema.warnings());
    }

    @Test
    void testRead_skipsATableWhoseDefinitionIsNotClosed() {
        ImportedSchema schema = DdlScriptReader.read("CREATE TABLE a (id INT, name VARCHAR(9)");

        assertEquals(List.of(), schema.nodes());
        assertEquals(List.of("Table a was skipped: its definition is not closed."), schema.warnings());
    }

    @Test
    void testRead_keepsTheFirstDefinitionOfATableCreatedTwice() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (id INT PRIMARY KEY);
                CREATE TABLE A (other INT PRIMARY KEY);
                """);

        assertEquals(List.of("id"), columnNames(schema, "a"));
        assertEquals(List.of("Table A is created more than once; only its first definition was read."), schema.warnings());
    }

    @Test
    void testRead_skipsOnlyTheElementsJSqlParserCannotRead() {
        // JSqlParser does not know invisible columns (MySQL 8.0.23); the rest of the table is kept.
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (
                  id INT PRIMARY KEY,
                  hidden_with_a_rather_long_name_for_the_warning INT NOT NULL DEFAULT 0 INVISIBLE,
                  name VARCHAR(9)
                );
                """);

        assertEquals(List.of("id", "name"), columnNames(schema, "a"));
        assertTrue(column(schema, "a", "id").getPk());
        assertEquals(1, schema.warnings().size());
        assertTrue(schema.warnings().get(0).startsWith("Table a: \"hidden_with_a_rather_long_name"), schema.warnings().get(0));
        assertTrue(schema.warnings().get(0).endsWith("...\" could not be read and was skipped."), schema.warnings().get(0));
    }

    @Test
    void testRead_skipsATableNoneOfWhoseColumnsCanBeRead() {
        ImportedSchema schema = DdlScriptReader.read("CREATE TABLE a (CHECK (1 > 0)); CREATE TABLE b (x NONSENSE TYPE HERE (,));");

        assertEquals(List.of(), schema.nodes());
        assertEquals("Table a was skipped: none of its columns could be read.", schema.warnings().get(0));
        assertEquals("Table b was skipped: none of its columns could be read.",
                schema.warnings().get(schema.warnings().size() - 1));
    }

    @Test
    void testRead_rewritesTheIndexFormsJSqlParserDoesNotAccept() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE orders (
                  id INT PRIMARY KEY,
                  code VARCHAR(9),
                  user_id INT,
                  UNIQUE INDEX ux_code (code),
                  SPATIAL INDEX sx (code),
                  FOREIGN KEY fk_user (user_id) REFERENCES users (id)
                );
                """);

        assertTrue(column(schema, "orders", "code").getUnique());
        assertEquals(List.of("orders.user_id -> users 1:N"), links(schema));
        assertEquals(List.of(), schema.warnings());
    }

    // ------------------------------------------------------------------
    // Columns
    // ------------------------------------------------------------------

    @Test
    void testRead_columnAttributes() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (
                  id INT KEY,
                  code INT UNIQUE KEY,
                  other INT UNIQUE NOT NULL,
                  label VARCHAR(9) COLLATE utf8mb4_bin COMMENT 'a label' NOT NULL,
                  maybe INT NULL,
                  closing INT NOT
                );
                """);

        assertTrue(column(schema, "a", "id").getPk(), "KEY alone means PRIMARY KEY in a column definition");
        assertTrue(column(schema, "a", "code").getUnique());
        assertFalse(column(schema, "a", "code").getPk(), "UNIQUE KEY is not a primary key");
        assertTrue(column(schema, "a", "other").getUnique());
        assertTrue(column(schema, "a", "other").getNotNull());
        assertTrue(column(schema, "a", "label").getNotNull());
        assertFalse(column(schema, "a", "maybe").getNotNull());
    }

    @Test
    void testRead_defaultValues() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (
                  id INT PRIMARY KEY,
                  n INT DEFAULT -1,
                  nothing INT DEFAULT NULL,
                  d DATE DEFAULT (curdate()),
                  ts TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
                  quoted VARCHAR(9) DEFAULT "say \"\"hi\"\"",
                  flag BIT(1) DEFAULT b'1',
                  enabled BOOLEAN DEFAULT TRUE,
                  last_one INT DEFAULT
                );
                """);

        assertEquals("-1", column(schema, "a", "n").getDefaultValue());
        assertEquals("", column(schema, "a", "nothing").getDefaultValue());
        assertEquals("curdate()", column(schema, "a", "d").getDefaultValue());
        assertEquals("CURRENT_TIMESTAMP(3)", column(schema, "a", "ts").getDefaultValue());
        assertEquals("say \"hi\"", column(schema, "a", "quoted").getDefaultValue());
        assertEquals("b'1'", column(schema, "a", "flag").getDefaultValue());
        assertEquals("TRUE", column(schema, "a", "enabled").getDefaultValue());
        assertEquals("", column(schema, "a", "last_one").getDefaultValue());
    }

    @Test
    void testDefaultValue_normalisesTheTokenTheWayTheEditorShowsIt() {
        assertEquals("", DdlScriptReader.defaultValue("null"));
        assertEquals("it's", DdlScriptReader.defaultValue("'it''s'"));
        assertEquals("it's", DdlScriptReader.defaultValue("'it\\'s'"));
        assertEquals("a", DdlScriptReader.defaultValue("\"a\""));
        assertEquals("'open", DdlScriptReader.defaultValue("'open"));
        assertEquals("'", DdlScriptReader.defaultValue("'"));
        assertEquals("now()", DdlScriptReader.defaultValue("( now() )"));
        assertEquals("(open", DdlScriptReader.defaultValue("(open"));
        assertEquals("close)", DdlScriptReader.defaultValue("close)"));
        assertEquals("42", DdlScriptReader.defaultValue("42"));
    }

    @Test
    void testRead_mapsMySqlTypesToTheEditorTypesAndWarnsAboutApproximations() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE t (
                  c_int INT PRIMARY KEY, c_integer INTEGER, c_medium MEDIUMINT, c_small SMALLINT,
                  c_tiny TINYINT, c_bool_tiny TINYINT(1), c_big BIGINT,
                  c_decimal DECIMAL(10,2), c_numeric NUMERIC, c_dec DEC(5), c_fixed FIXED(4,1),
                  c_float FLOAT, c_double DOUBLE PRECISION, c_real REAL,
                  c_varchar VARCHAR(9), c_nvarchar NVARCHAR(9), c_char CHAR(2), c_nchar NCHAR(2),
                  c_text TEXT, c_tinytext TINYTEXT, c_mediumtext MEDIUMTEXT, c_longtext LONGTEXT,
                  c_date DATE, c_time TIME, c_datetime DATETIME(6), c_timestamp TIMESTAMP,
                  c_boolean BOOLEAN, c_bool BOOL, c_json JSON
                );
                """);

        List<String> types = table(schema, "t").getItems().stream().map(ItemDTO::getType).toList();
        assertEquals(List.of(
                "INTEGER", "INTEGER", "INTEGER", "INTEGER",
                "INTEGER", "BOOLEAN", "BIGINT",
                "DECIMAL", "DECIMAL", "DECIMAL", "DECIMAL",
                "DECIMAL", "DECIMAL", "DECIMAL",
                "VARCHAR", "VARCHAR", "CHAR", "CHAR",
                "TEXT", "TEXT", "TEXT", "TEXT",
                "DATE", "TIME", "TIMESTAMP", "TIMESTAMP",
                "BOOLEAN", "BOOLEAN", "VARCHAR"), types);
        assertEquals(List.of(
                "Column t.c_float: type FLOAT was imported as DECIMAL, the closest type the editor offers.",
                "Column t.c_double: type DOUBLE was imported as DECIMAL, the closest type the editor offers.",
                "Column t.c_real: type REAL was imported as DECIMAL, the closest type the editor offers.",
                "Column t.c_json: type JSON is not supported and was imported as VARCHAR."), schema.warnings());
    }

    // ------------------------------------------------------------------
    // Keys
    // ------------------------------------------------------------------

    @Test
    void testRead_primaryKeyWarnings() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE no_key (name VARCHAR(9));
                CREATE TABLE composite (a INT, b INT, PRIMARY KEY (a, b));
                CREATE TABLE ghost_key (a INT, PRIMARY KEY (missing));
                CREATE TABLE declared_twice (id INT PRIMARY KEY, CONSTRAINT pk PRIMARY KEY (id));
                """);

        assertTrue(column(schema, "composite", "a").getPk());
        assertTrue(column(schema, "composite", "b").getPk());
        assertTrue(column(schema, "declared_twice", "id").getPk());
        assertEquals(List.of(
                "Table no_key has no primary key; the editor needs one to edit the table.",
                "Table composite has a composite primary key (a, b); the editor supports a primary key of one column.",
                "Table ghost_key: the primary key refers to column missing, which the table does not have.",
                "Table ghost_key has no primary key; the editor needs one to edit the table."), schema.warnings());
    }

    @Test
    void testRead_uniqueConstraints() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (
                  id INT PRIMARY KEY, x INT, y INT, z INT,
                  CONSTRAINT uq_x UNIQUE (x),
                  UNIQUE (x, y),
                  UNIQUE KEY (missing),
                  INDEX ix_z (z),
                  FULLTEXT KEY ft (z)
                );
                """);

        assertTrue(column(schema, "a", "x").getUnique());
        assertFalse(column(schema, "a", "z").getUnique(), "A plain index is not a unique constraint");
        assertEquals(List.of(
                "Table a: the unique constraint on (x, y) spans several columns and is not represented.",
                "Table a: the unique constraint refers to column missing, which the table does not have."),
                schema.warnings());
    }

    @Test
    void testRead_foreignKeysInEveryForm() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE inline_ref (id INT PRIMARY KEY, user_id INT REFERENCES `db`.`users` (id));
                CREATE TABLE table_level (id INT PRIMARY KEY, user_id INT, FOREIGN KEY (user_id) REFERENCES users (id));
                CREATE TABLE named (id INT PRIMARY KEY, owner INT, CONSTRAINT fk_owner FOREIGN KEY (owner) REFERENCES users (id));
                CREATE TABLE altered (id INT PRIMARY KEY, user_id INT);
                ALTER TABLE altered ADD FOREIGN KEY (user_id) REFERENCES users (id);
                """);

        assertEquals(List.of(
                "inline_ref.user_id -> users 1:N",
                "table_level.user_id -> users 1:N",
                "named.owner -> users 1:N",
                "altered.user_id -> users 1:N"), links(schema));
        schema.links().forEach(link -> assertNotNull(link.getId()));
        schema.links().forEach(link -> assertEquals(1, link.getToText()));
        assertTrue(column(schema, "named", "owner").getFk());
        assertEquals(List.of(), schema.warnings());
    }

    @Test
    void testRead_drawsAOneToOneRelationshipForAUniqueOrPrimaryKeyColumn() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE users (id INT PRIMARY KEY);
                CREATE TABLE profiles (user_id INT PRIMARY KEY, FOREIGN KEY (user_id) REFERENCES users (id));
                CREATE TABLE badges (id INT PRIMARY KEY, user_id INT UNIQUE, FOREIGN KEY (user_id) REFERENCES users (id));
                CREATE TABLE memberships (user_id INT, team INT, PRIMARY KEY (user_id, team),
                  FOREIGN KEY (user_id) REFERENCES users (id));
                """);

        assertEquals(List.of(
                "profiles.user_id -> users 1:1",
                "badges.user_id -> users 1:1",
                "memberships.user_id -> users 1:N"), links(schema));
    }

    @Test
    void testRead_foreignKeysThatCannotBeRepresented() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE users (id INT, region INT, PRIMARY KEY (id, region));
                CREATE TABLE orders (
                  id INT PRIMARY KEY,
                  user_id INT,
                  region INT,
                  warehouse_id INT,
                  FOREIGN KEY (user_id, region) REFERENCES users (id, region),
                  FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
                  FOREIGN KEY (missing) REFERENCES users (id),
                  FOREIGN KEY (user_id) REFERENCES users (id),
                  FOREIGN KEY (user_id) REFERENCES users (id)
                );
                """);

        assertEquals(List.of("orders.user_id -> users 1:N"), links(schema), "A key declared twice is one relationship");
        assertTrue(schema.warnings().contains(
                "Table orders: the foreign key on (user_id, region) spans several columns and is not represented."));
        assertTrue(schema.warnings().contains(
                "Table orders: the foreign key on warehouse_id references table warehouses, which the script does not create."));
        assertTrue(schema.warnings().contains(
                "Table orders: the foreign key refers to column missing, which the table does not have."));
    }

    // ------------------------------------------------------------------
    // ALTER TABLE
    // ------------------------------------------------------------------

    @Test
    void testRead_alterTableOperations() {
        ImportedSchema schema = DdlScriptReader.read("""
                CREATE TABLE a (id INT, email VARCHAR(9) UNIQUE, nick VARCHAR(9));
                ALTER TABLE a
                  ADD PRIMARY KEY (id),
                  ADD COLUMN age INT NOT NULL DEFAULT 0,
                  ADD CONSTRAINT uq_nick UNIQUE (nick),
                  ADD UNIQUE INDEX ux_age (age),
                  ADD INDEX (age),
                  ADD CONSTRAINT chk CHECK (age >= 0),
                  MODIFY email VARCHAR(20) NOT NULL,
                  MODIFY nick VARCHAR(20),
                  MODIFY age INT NOT NULL DEFAULT 0 UNIQUE,
                  MODIFY id INT NOT NULL AUTO_INCREMENT,
                  MODIFY missing INT,
                  DROP COLUMN gone,
                  ADD THIS IS NOT SQL;
                ALTER TABLE elsewhere ADD PRIMARY KEY (id);
                ALTER ONLINE TABLE a RENAME TO b;
                """);

        assertEquals(List.of("id", "email", "nick", "age"), columnNames(schema, "a"));
        assertTrue(column(schema, "a", "id").getPk());
        assertEquals("0", column(schema, "a", "age").getDefaultValue());
        assertTrue(column(schema, "a", "age").getUnique());
        assertTrue(column(schema, "a", "email").getUnique(), "MODIFY keeps the unique constraint");
        assertTrue(column(schema, "a", "email").getNotNull());
        assertTrue(column(schema, "a", "nick").getUnique());
        assertTrue(column(schema, "a", "id").getAutoIncrement());
        assertFalse(column(schema, "a", "id").getUnique());
        assertEquals(List.of(
                "ALTER TABLE a: column missing cannot be modified because the table does not have it.",
                "ALTER TABLE a: \"DROP COLUMN gone\" is not supported and was ignored.",
                "ALTER TABLE a: \"ADD THIS IS NOT SQL\" could not be read and was ignored.",
                "ALTER TABLE elsewhere was ignored: the script does not create that table.",
                "ALTER TABLE a: \"RENAME TO b\" is not supported and was ignored."), schema.warnings());
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @Test
    void testRead_placesTheTablesSideBySide() {
        ImportedSchema schema = DdlScriptReader.read("CREATE TABLE a (id INT PRIMARY KEY); CREATE TABLE b (id INT PRIMARY KEY);");

        assertEquals("0", table(schema, "a").getLocation().getX());
        assertEquals("300", table(schema, "b").getLocation().getX());
        assertEquals("50", table(schema, "b").getLocation().getY());
        assertNotNull(table(schema, "a").getId());
    }

}
