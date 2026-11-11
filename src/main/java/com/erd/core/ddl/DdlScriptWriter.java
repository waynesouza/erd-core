package com.erd.core.ddl;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Writes a diagram as a MySQL script: a CREATE TABLE per table, then an ALTER TABLE per foreign
 * key, so the tables can be created in any order. Names are quoted, so a column called {@code order}
 * or {@code key} does not break the script, and default values are written as MySQL expects them
 * for the column's type.
 */
public final class DdlScriptWriter {

    private static final int MAX_IDENTIFIER_LENGTH = 64;

    private static final Set<String> NUMERIC_TYPES =
            Set.of("INTEGER", "INT", "BIGINT", "SMALLINT", "TINYINT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE");
    private static final Set<String> DATE_TIME_TYPES = Set.of("DATETIME", "TIMESTAMP");
    private static final Pattern NUMBER = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
    private static final Pattern BOOLEAN_LITERAL = Pattern.compile("TRUE|FALSE|1|0", Pattern.CASE_INSENSITIVE);
    // b'101' and x'1F' are literals of their own; they must not be quoted again.
    private static final Pattern PREFIXED_LITERAL = Pattern.compile("[bBxX]'[0-9A-Fa-f]*'");
    // Accepted as they are in the DEFAULT of a DATETIME or TIMESTAMP column.
    private static final Pattern CURRENT_TIMESTAMP = Pattern.compile(
            "(?:CURRENT_TIMESTAMP|NOW|LOCALTIME|LOCALTIMESTAMP)(?:\\(\\d*\\))?", Pattern.CASE_INSENSITIVE);
    // Any other function, which MySQL 8 only accepts as an expression in parentheses.
    private static final Pattern FUNCTION = Pattern.compile(
            "(?:CURRENT_DATE|CURRENT_TIME|[A-Za-z_]\\w*\\(.*\\))", Pattern.CASE_INSENSITIVE);

    private DdlScriptWriter() {
    }

    public static String write(List<NodeDataDTO> nodes, List<LinkDataDTO> links) {
        if (nodes == null || nodes.isEmpty()) {
            return "-- No tables found in diagram";
        }

        List<String> statements = new ArrayList<>();
        for (NodeDataDTO node : nodes) {
            statements.add(createTable(node));
        }

        // Two links can stand for the same key; each key is written once, with a name of its own.
        Map<String, String> foreignKeys = new LinkedHashMap<>();
        Set<String> constraintNames = new HashSet<>();
        for (LinkDataDTO link : Objects.requireNonNullElse(links, List.<LinkDataDTO>of())) {
            ForeignKeyResolution resolution = resolve(link, nodes);
            String identity = resolution.identity();
            if (!foreignKeys.containsKey(identity)) {
                foreignKeys.put(identity, resolution.statement(constraintNames));
            }
        }
        statements.addAll(foreignKeys.values());
        return String.join("\n\n", statements);
    }

    // ------------------------------------------------------------------
    // CREATE TABLE
    // ------------------------------------------------------------------

    private static String createTable(NodeDataDTO node) {
        List<ItemDTO> items = itemsOf(node);
        if (items.isEmpty()) {
            return String.format("-- Table %s was not exported: it has no columns.", node.getKey());
        }

        List<String> lines = new ArrayList<>();
        List<String> primaryKey = new ArrayList<>();
        for (ItemDTO item : items) {
            lines.add("    " + column(item));
            if (Boolean.TRUE.equals(item.getPk())) {
                primaryKey.add(quoted(item.getName()));
            }
        }
        if (!primaryKey.isEmpty()) {
            lines.add("    PRIMARY KEY (" + String.join(", ", primaryKey) + ")");
        }
        return "CREATE TABLE " + quoted(node.getKey()) + " (\n" + String.join(",\n", lines) + "\n);";
    }

    private static String column(ItemDTO item) {
        StringBuilder column = new StringBuilder(quoted(item.getName())).append(' ').append(sqlType(item.getType()));
        if (Boolean.TRUE.equals(item.getNotNull())) {
            column.append(" NOT NULL");
        }
        if (Boolean.TRUE.equals(item.getAutoIncrement())) {
            column.append(" AUTO_INCREMENT");
        }
        String defaultValue = sqlDefault(item.getType(), item.getDefaultValue());
        if (defaultValue != null) {
            column.append(" DEFAULT ").append(defaultValue);
        }
        if (Boolean.TRUE.equals(item.getUnique())) {
            column.append(" UNIQUE");
        }
        return column.toString();
    }

    static String sqlType(String type) {
        return switch (Objects.requireNonNullElse(type, "").toUpperCase(Locale.ROOT)) {
            case "INTEGER", "INT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "TINYINT" -> "TINYINT";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "FLOAT" -> "FLOAT";
            case "DOUBLE" -> "DOUBLE";
            case "CHAR" -> "CHAR(1)";
            case "TEXT" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "DATETIME", "TIMESTAMP" -> "DATETIME";
            case "BOOLEAN", "BOOL" -> "BOOLEAN";
            default -> "VARCHAR(255)";
        };
    }

    /**
     * The DEFAULT clause value for a column of the given type, or null when there is none. The
     * editor stores text without quotes, so they are added here; values that already carry their
     * own quotes, numbers in numeric columns, booleans and functions are written as they are.
     */
    static String sqlDefault(String type, String value) {
        String trimmed = Objects.requireNonNullElse(value, "").trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String upperType = Objects.requireNonNullElse(type, "").toUpperCase(Locale.ROOT);
        if (trimmed.equalsIgnoreCase("NULL")
                || (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2)
                || (trimmed.startsWith("(") && trimmed.endsWith(")"))
                || PREFIXED_LITERAL.matcher(trimmed).matches()
                || (NUMERIC_TYPES.contains(upperType) && NUMBER.matcher(trimmed).matches())
                || (upperType.startsWith("BOOL") && BOOLEAN_LITERAL.matcher(trimmed).matches())) {
            return trimmed;
        }
        if (CURRENT_TIMESTAMP.matcher(trimmed).matches()) {
            return DATE_TIME_TYPES.contains(upperType) ? trimmed : "(" + trimmed + ")";
        }
        if (FUNCTION.matcher(trimmed).matches()) {
            return "(" + trimmed + ")";
        }
        return "'" + trimmed.replace("'", "''") + "'";
    }

    // ------------------------------------------------------------------
    // Foreign keys
    // ------------------------------------------------------------------

    /**
     * The foreign key a link stands for. It lives in the table that holds the column, which is
     * normally the link's source; links drawn before the editor recorded the column may point the
     * other way, as the first link of a many-to-many relationship did, so the target is searched
     * as well. A link that resolves to no column or to a table without a primary key becomes a
     * comment: an invented column would make the whole script fail in MySQL.
     */
    private static ForeignKeyResolution resolve(LinkDataDTO link, List<NodeDataDTO> nodes) {
        NodeDataDTO from = findTable(nodes, link.getFrom());
        NodeDataDTO to = findTable(nodes, link.getTo());
        if (from == null || to == null) {
            return skipped(link, "one of its tables is not in the diagram");
        }

        NodeDataDTO holder = from;
        NodeDataDTO referenced = to;
        String column = foreignKeyColumn(from, to, link.getFromColumn());
        if (column == null) {
            holder = to;
            referenced = from;
            column = foreignKeyColumn(to, from, null);
        }
        if (column == null) {
            return skipped(link, "no foreign key column was found in either table");
        }

        String primaryKey = itemsOf(referenced).stream()
                .filter(item -> Boolean.TRUE.equals(item.getPk()))
                .map(ItemDTO::getName)
                .findFirst()
                .orElse(null);
        if (primaryKey == null) {
            return skipped(link, "table " + referenced.getKey() + " has no primary key");
        }
        return new ForeignKeyResolution(holder.getKey(), column, referenced.getKey(), primaryKey, null);
    }

    // The column of `holder` that references `referenced`: the one recorded on the link while it is
    // still in the table, otherwise a foreign key column named after the referenced table.
    private static String foreignKeyColumn(NodeDataDTO holder, NodeDataDTO referenced, String recordedColumn) {
        List<ItemDTO> items = itemsOf(holder);
        if (recordedColumn != null && items.stream().anyMatch(item -> recordedColumn.equals(item.getName()))) {
            return recordedColumn;
        }

        String referencedSingular = referenced.getKey().toLowerCase(Locale.ROOT).replaceAll("s$", "");
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getFk()) && item.getName() != null
                        && item.getName().toLowerCase(Locale.ROOT).contains(referencedSingular))
                .map(ItemDTO::getName)
                .findFirst()
                .orElse(null);
    }

    private static ForeignKeyResolution skipped(LinkDataDTO link, String reason) {
        String comment = String.format("-- Relationship %s -> %s was not exported: %s.", link.getFrom(), link.getTo(), reason);
        return new ForeignKeyResolution(null, null, null, null, comment);
    }

    // Either a foreign key or, when the link cannot become one, the comment that says why.
    private static final class ForeignKeyResolution {
        private final String table;
        private final String column;
        private final String referencedTable;
        private final String referencedColumn;
        private final String comment;

        private ForeignKeyResolution(String table, String column, String referencedTable, String referencedColumn,
                                     String comment) {
            this.table = table;
            this.column = column;
            this.referencedTable = referencedTable;
            this.referencedColumn = referencedColumn;
            this.comment = comment;
        }

        private String identity() {
            return comment != null ? comment : table + "." + column + "->" + referencedTable;
        }

        private String statement(Set<String> usedNames) {
            if (comment != null) {
                return comment;
            }
            String name = constraintName(table, column, usedNames);
            return String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s);",
                    quoted(table), quoted(name), quoted(column), quoted(referencedTable), quoted(referencedColumn));
        }
    }

    // fk_<table>_<column>, cut to MySQL's 64 characters and made unique within the script, since
    // constraint names are unique per database.
    static String constraintName(String table, String column, Set<String> usedNames) {
        String base = truncated("fk_" + table + "_" + column, MAX_IDENTIFIER_LENGTH);
        String name = base;
        for (int suffix = 2; usedNames.contains(name.toLowerCase(Locale.ROOT)); suffix++) {
            String ending = "_" + suffix;
            name = truncated(base, MAX_IDENTIFIER_LENGTH - ending.length()) + ending;
        }
        usedNames.add(name.toLowerCase(Locale.ROOT));
        return name;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    static String quoted(String identifier) {
        return "`" + Objects.requireNonNullElse(identifier, "").replace("`", "``") + "`";
    }

    private static String truncated(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }

    private static NodeDataDTO findTable(List<NodeDataDTO> nodes, String key) {
        return nodes.stream()
                .filter(node -> node.getKey() != null && node.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    private static List<ItemDTO> itemsOf(NodeDataDTO node) {
        return Objects.requireNonNullElse(node.getItems(), List.of());
    }

}
