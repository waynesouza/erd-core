package com.erd.core.ddl;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DdlScriptWriterTest {

    private static ItemDTO item(String name, String type) {
        ItemDTO item = new ItemDTO();
        item.setName(name);
        item.setType(type);
        return item;
    }

    private static NodeDataDTO node(String key, ItemDTO... items) {
        NodeDataDTO node = new NodeDataDTO();
        node.setKey(key);
        node.setItems(List.of(items));
        return node;
    }

    // ------------------------------------------------------------------
    // Default values
    // ------------------------------------------------------------------

    @Test
    void testSqlDefault_writesNothingForAnEmptyValue() {
        assertNull(DdlScriptWriter.sqlDefault("VARCHAR", null));
        assertNull(DdlScriptWriter.sqlDefault("VARCHAR", "  "));
    }

    @Test
    void testSqlDefault_quotesTextAndEscapesItsQuotes() {
        assertEquals("'em aberto'", DdlScriptWriter.sqlDefault("VARCHAR", "em aberto"));
        assertEquals("'it''s'", DdlScriptWriter.sqlDefault("TEXT", "it's"));
        assertEquals("'42'", DdlScriptWriter.sqlDefault("VARCHAR", "42"), "A number in a text column is text");
        assertEquals("'TRUE'", DdlScriptWriter.sqlDefault("CHAR", "TRUE"));
        assertEquals("''''", DdlScriptWriter.sqlDefault("CHAR", "'"));
        assertEquals("'(open'", DdlScriptWriter.sqlDefault("VARCHAR", "(open"));
        assertEquals("'''open'", DdlScriptWriter.sqlDefault("VARCHAR", "'open"));
        assertEquals("'x'", DdlScriptWriter.sqlDefault(null, "x"));
    }

    @Test
    void testSqlDefault_keepsValuesThatAlreadyHaveTheirSyntax() {
        assertEquals("null", DdlScriptWriter.sqlDefault("VARCHAR", "null"));
        assertEquals("'ACTIVE'", DdlScriptWriter.sqlDefault("VARCHAR", "'ACTIVE'"), "Values saved with their quotes keep them");
        assertEquals("(UUID())", DdlScriptWriter.sqlDefault("VARCHAR", "(UUID())"));
        assertEquals("b'1'", DdlScriptWriter.sqlDefault("BOOLEAN", "b'1'"));
    }

    @Test
    void testSqlDefault_writesNumbersAndBooleansAsTheyAre() {
        assertEquals("-1.5e3", DdlScriptWriter.sqlDefault("DECIMAL", "-1.5e3"));
        assertEquals("0", DdlScriptWriter.sqlDefault("INTEGER", "0"));
        assertEquals("'abc'", DdlScriptWriter.sqlDefault("INTEGER", "abc"));
        assertEquals("TRUE", DdlScriptWriter.sqlDefault("BOOLEAN", "TRUE"));
        assertEquals("0", DdlScriptWriter.sqlDefault("BOOL", "0"));
        assertEquals("'yes'", DdlScriptWriter.sqlDefault("BOOLEAN", "yes"));
    }

    @Test
    void testSqlDefault_wrapsFunctionsThatMySqlOnlyAcceptsAsExpressions() {
        assertEquals("CURRENT_TIMESTAMP", DdlScriptWriter.sqlDefault("TIMESTAMP", "CURRENT_TIMESTAMP"));
        assertEquals("now(3)", DdlScriptWriter.sqlDefault("DATETIME", "now(3)"));
        assertEquals("(CURRENT_TIMESTAMP)", DdlScriptWriter.sqlDefault("DATE", "CURRENT_TIMESTAMP"));
        assertEquals("(CURRENT_DATE)", DdlScriptWriter.sqlDefault("DATE", "CURRENT_DATE"));
        assertEquals("(curdate())", DdlScriptWriter.sqlDefault("DATE", "curdate()"));
    }

    // ------------------------------------------------------------------
    // Types, tables and names
    // ------------------------------------------------------------------

    @Test
    void testSqlType_fallsBackToVarcharForAMissingType() {
        assertEquals("VARCHAR(255)", DdlScriptWriter.sqlType(null));
        assertEquals("VARCHAR(255)", DdlScriptWriter.sqlType("UUID"));
    }

    @Test
    void testWrite_quotesNamesSoReservedWordsDoNotBreakTheScript() {
        ItemDTO order = item("order", "INTEGER");
        order.setPk(true);

        String ddl = DdlScriptWriter.write(List.of(node("odd`table", order, item("key", "VARCHAR"))), List.of());

        assertEquals("""
                CREATE TABLE `odd``table` (
                    `order` INT,
                    `key` VARCHAR(255),
                    PRIMARY KEY (`order`)
                );""", ddl);
    }

    @Test
    void testWrite_toleratesMissingNames() {
        String ddl = DdlScriptWriter.write(List.of(node(null, item(null, "INTEGER"))), null);

        assertEquals("CREATE TABLE `` (\n    `` INT\n);", ddl);
    }

    @Test
    void testConstraintName_isUniqueAndFitsMySqlsLimit() {
        Set<String> used = new HashSet<>();
        String longTable = "a".repeat(70);

        assertEquals("fk_orders_user_id", DdlScriptWriter.constraintName("orders", "user_id", used));
        assertEquals("fk_orders_USER_ID_2", DdlScriptWriter.constraintName("orders", "USER_ID", used),
                "Names differing only in case collide in MySQL");
        assertEquals("fk_orders_user_id_3", DdlScriptWriter.constraintName("orders", "user_id", used));
        String truncated = DdlScriptWriter.constraintName(longTable, "id", used);
        assertEquals(64, truncated.length());
        String second = DdlScriptWriter.constraintName(longTable, "id", used);
        assertEquals(64, second.length());
        assertTrue(second.endsWith("_2"));
    }

    @Test
    void testWrite_namesTwoKeysToTheSameTableApart() {
        // Naming both after the referenced table gave them the same name, which MySQL rejects.
        ItemDTO home = item("home_team_id", "INTEGER");
        home.setFk(true);
        ItemDTO away = item("away_team_id", "INTEGER");
        away.setFk(true);
        ItemDTO teamId = item("id", "INTEGER");
        teamId.setPk(true);
        LinkDataDTO toHome = new LinkDataDTO();
        toHome.setFrom("matches");
        toHome.setTo("teams");
        toHome.setFromColumn("home_team_id");
        LinkDataDTO toAway = new LinkDataDTO();
        toAway.setFrom("matches");
        toAway.setTo("teams");
        toAway.setFromColumn("away_team_id");

        String ddl = DdlScriptWriter.write(List.of(node("teams", teamId), node("matches", home, away)), List.of(toHome, toAway));

        assertTrue(ddl.contains("ADD CONSTRAINT `fk_matches_home_team_id` FOREIGN KEY (`home_team_id`) REFERENCES `teams` (`id`);"));
        assertTrue(ddl.contains("ADD CONSTRAINT `fk_matches_away_team_id` FOREIGN KEY (`away_team_id`) REFERENCES `teams` (`id`);"));
    }

    @Test
    void testWrite_writesTheSameSkippedLinkOnce() {
        LinkDataDTO ghost = new LinkDataDTO();
        ghost.setFrom("ghost");
        ghost.setTo("users");

        String ddl = DdlScriptWriter.write(List.of(node("users", item("id", "INTEGER"))), List.of(ghost, ghost));

        String comment = "-- Relationship ghost -> users was not exported: one of its tables is not in the diagram.";
        assertEquals(ddl.indexOf(comment), ddl.lastIndexOf(comment));
    }

}
