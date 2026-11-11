package com.erd.core.ddl;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.LocationDTO;
import com.erd.core.dto.NodeDataDTO;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the CREATE TABLE and ALTER TABLE statements of a MySQL script into the tables and
 * relationships of a diagram. Scripts written by hand and those MySQL itself produces (mysqldump,
 * SHOW CREATE TABLE, phpMyAdmin) are read alike: quoted and qualified names, table options,
 * comments and keys declared after the tables. Every other statement is skipped.
 * <p>
 * JSqlParser parses the column definitions and constraints. The parts of a table a diagram does
 * not represent (CHECK constraints and plain indexes) are removed first, because some of them make
 * JSqlParser reject the whole statement; and when a table still cannot be parsed, its elements are
 * parsed one at a time, so a single unreadable element costs that element only. Whatever is left
 * out is reported as a warning.
 */
public final class DdlScriptReader {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?" + SqlText.QUALIFIED_NAME + "\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_TABLE = Pattern.compile(
            "ALTER\\s+(?:ONLINE\\s+|IGNORE\\s+)*TABLE\\s+" + SqlText.QUALIFIED_NAME + "\\s+(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // CHECK constraints and spatial indexes are not represented in a diagram, and JSqlParser
    // rejects a whole table that has one, so they are removed before it parses the table.
    private static final Pattern IGNORED_ELEMENT = Pattern.compile(
            "(?:CONSTRAINT\\s+(?:" + SqlText.IDENTIFIER + "\\s+)?CHECK|CHECK|SPATIAL)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IGNORED_CLAUSE = Pattern.compile(
            "ADD\\s+(?:CONSTRAINT\\s+(?:" + SqlText.IDENTIFIER + "\\s+)?CHECK|CHECK|SPATIAL)\\b",
            Pattern.CASE_INSENSITIVE);
    // Forms MySQL accepts and JSqlParser does not, rewritten to equivalent ones it does.
    private static final Pattern UNIQUE_INDEX = Pattern.compile(
            "^((?:ADD\\s+)?(?:CONSTRAINT\\s+(?:" + SqlText.IDENTIFIER + "\\s+)?)?UNIQUE)\\s+INDEX\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_FOREIGN_KEY = Pattern.compile(
            "^((?:ADD\\s+)?(?:CONSTRAINT\\s+(?:" + SqlText.IDENTIFIER + "\\s+)?)?FOREIGN\\s+KEY)\\s+" + SqlText.IDENTIFIER + "\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TINYINT_ONE = Pattern.compile("TINYINT\\s*\\(\\s*1\\s*\\).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIER_TOKEN = Pattern.compile("[A-Za-z_]\\w*");

    private static final String PLACEHOLDER_TABLE = "erd_import_table";
    private static final String PLACEHOLDER_COLUMN = "erd_import_placeholder";
    private static final int ABBREVIATION_LENGTH = 60;
    private static final int TABLE_SPACING = 300;

    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    private DdlScriptReader() {
    }

    public static ImportedSchema read(String script) {
        return new DdlScriptReader().readScript(script);
    }

    private ImportedSchema readScript(String script) {
        for (String statement : SqlText.statements(script)) {
            Matcher createTable = CREATE_TABLE.matcher(statement);
            Matcher alterTable = ALTER_TABLE.matcher(statement);
            if (createTable.lookingAt()) {
                readCreateTable(statement, SqlText.unquote(createTable.group(1)), createTable.end());
            } else if (alterTable.lookingAt()) {
                readAlterTable(SqlText.unquote(alterTable.group(1)), alterTable.group(2));
            }
        }

        tables.values().forEach(this::applyPrimaryKey);
        List<LinkDataDTO> links = resolveForeignKeys();
        return new ImportedSchema(nodes(), links, List.copyOf(warnings));
    }

    // ------------------------------------------------------------------
    // CREATE TABLE
    // ------------------------------------------------------------------

    private void readCreateTable(String statement, String tableName, int bodyStart) {
        if (bodyStart == statement.length() || statement.charAt(bodyStart) != '(') {
            warn("Table %s was skipped: it has no column definitions, as in CREATE TABLE ... LIKE or AS SELECT.",
                    tableName);
            return;
        }
        int bodyEnd = SqlText.closingParenthesis(statement, bodyStart);
        if (bodyEnd < 0) {
            warn("Table %s was skipped: its definition is not closed.", tableName);
            return;
        }
        if (tables.containsKey(key(tableName))) {
            warn("Table %s is created more than once; only its first definition was read.", tableName);
            return;
        }

        List<String> elements = SqlText.splitTopLevel(statement.substring(bodyStart + 1, bodyEnd)).stream()
                .filter(element -> !IGNORED_ELEMENT.matcher(element).lookingAt())
                .map(DdlScriptReader::normalized)
                .toList();
        Table table = new Table(tableName);
        for (CreateTable part : parseElements(tableName, elements)) {
            for (ColumnDefinition column : Objects.requireNonNullElse(part.getColumnDefinitions(), List.<ColumnDefinition>of())) {
                if (!PLACEHOLDER_COLUMN.equals(column.getColumnName())) {
                    addColumn(table, column);
                }
            }
            for (Index index : Objects.requireNonNullElse(part.getIndexes(), List.<Index>of())) {
                addConstraint(table, index);
            }
        }

        if (table.columns.isEmpty()) {
            warn("Table %s was skipped: none of its columns could be read.", tableName);
            return;
        }
        tables.put(key(tableName), table);
    }

    // The whole table at once; when JSqlParser rejects it, one element at a time, so only the
    // elements it cannot read are lost. Each one gets a placeholder column, because a table made of
    // a single constraint is not valid SQL.
    private List<CreateTable> parseElements(String tableName, List<String> elements) {
        try {
            return List.of(parseCreateTable(String.join(", ", elements)));
        } catch (JSQLParserException e) {
            List<CreateTable> parts = new ArrayList<>();
            for (String element : elements) {
                try {
                    parts.add(parseCreateTable(PLACEHOLDER_COLUMN + " INT, " + element));
                } catch (JSQLParserException elementError) {
                    warn("Table %s: \"%s\" could not be read and was skipped.", tableName, abbreviated(element));
                }
            }
            return parts;
        }
    }

    private static CreateTable parseCreateTable(String body) throws JSQLParserException {
        return (CreateTable) CCJSqlParserUtil.parse("CREATE TABLE " + PLACEHOLDER_TABLE + " (" + body + ")");
    }

    private void addColumn(Table table, ColumnDefinition definition) {
        String name = SqlText.unquote(definition.getColumnName());
        ItemDTO item = new ItemDTO();
        item.setName(name);
        item.setType(diagramType(table.name, name, definition.getColDataType()));
        item.setPk(false);
        item.setFk(false);
        item.setUnique(false);
        item.setNotNull(false);
        item.setAutoIncrement(false);
        item.setDefaultValue("");

        List<String> specs = Objects.requireNonNullElse(definition.getColumnSpecs(), List.of());
        for (int i = 0; i < specs.size(); i++) {
            String next = i + 1 < specs.size() ? specs.get(i + 1) : "";
            switch (specs.get(i).toUpperCase(Locale.ROOT)) {
                case "NOT" -> {
                    if (next.equalsIgnoreCase("NULL")) {
                        item.setNotNull(true);
                        i++;
                    }
                }
                case "AUTO_INCREMENT" -> item.setAutoIncrement(true);
                // In a column definition, KEY on its own also means PRIMARY KEY.
                case "PRIMARY", "KEY" -> {
                    table.primaryKey.add(name);
                    i += next.equalsIgnoreCase("KEY") ? 1 : 0;
                }
                case "UNIQUE" -> {
                    item.setUnique(true);
                    i += next.equalsIgnoreCase("KEY") ? 1 : 0;
                }
                case "DEFAULT" -> i += readDefault(item, specs, i + 1);
                case "REFERENCES" -> {
                    // Followed by the table and its column list, which is the primary key in a diagram.
                    table.foreignKeys.add(new ForeignKey(List.of(name), SqlText.tableName(next)));
                    i += 2;
                }
                case "COMMENT", "COLLATE" -> i++;
                default -> {
                    // NULL, UNSIGNED, ON UPDATE, CHARACTER SET, GENERATED ... AS: not represented.
                }
            }
        }
        table.columns.put(key(name), item);
    }

    // Reads the default value that starts at specs[start] and returns how many tokens it took.
    private static int readDefault(ItemDTO item, List<String> specs, int start) {
        if (start >= specs.size()) {
            return 0;
        }
        String value = specs.get(start);
        int taken = 1;
        // CURRENT_TIMESTAMP(3) arrives as two tokens.
        if (IDENTIFIER_TOKEN.matcher(value).matches() && start + 1 < specs.size() && specs.get(start + 1).startsWith("(")) {
            value += specs.get(start + 1);
            taken = 2;
        }
        item.setDefaultValue(defaultValue(value));
        return taken;
    }

    /**
     * The default value as the editor shows it: a string literal without its quotes, an expression
     * without its parentheses, and none for NULL. The export adds the quotes back.
     */
    static String defaultValue(String token) {
        if (token.equalsIgnoreCase("NULL")) {
            return "";
        }
        int last = token.length() - 1;
        if (token.length() >= 2 && (token.charAt(0) == '\'' || token.charAt(0) == '"') && token.charAt(last) == token.charAt(0)) {
            String quote = String.valueOf(token.charAt(0));
            return token.substring(1, last).replace(quote + quote, quote).replace("\\" + quote, quote);
        }
        if (token.startsWith("(") && token.endsWith(")")) {
            return token.substring(1, last).trim();
        }
        return token;
    }

    // ------------------------------------------------------------------
    // Data types
    // ------------------------------------------------------------------

    private String diagramType(String tableName, String columnName, ColDataType dataType) {
        // JSqlParser keeps the length in the type: "varchar (150)", "double precision".
        String declared = dataType.getDataType().trim().split("[\\s(]", 2)[0].toUpperCase(Locale.ROOT);
        return switch (declared) {
            case "INT", "INTEGER", "MEDIUMINT", "SMALLINT" -> "INTEGER";
            // TINYINT(1) is how MySQL stores BOOLEAN.
            case "TINYINT" -> TINYINT_ONE.matcher(dataType.getDataType().trim()).matches() ? "BOOLEAN" : "INTEGER";
            case "BIGINT" -> "BIGINT";
            case "DECIMAL", "NUMERIC", "DEC", "FIXED" -> "DECIMAL";
            case "FLOAT", "DOUBLE", "REAL" -> {
                warn("Column %s.%s: type %s was imported as DECIMAL, the closest type the editor offers.",
                        tableName, columnName, declared);
                yield "DECIMAL";
            }
            case "VARCHAR", "NVARCHAR" -> "VARCHAR";
            case "CHAR", "NCHAR" -> "CHAR";
            case "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "DATETIME", "TIMESTAMP" -> "TIMESTAMP";
            case "BOOLEAN", "BOOL" -> "BOOLEAN";
            default -> {
                warn("Column %s.%s: type %s is not supported and was imported as VARCHAR.",
                        tableName, columnName, declared);
                yield "VARCHAR";
            }
        };
    }

    // ------------------------------------------------------------------
    // Constraints
    // ------------------------------------------------------------------

    private void addConstraint(Table table, Index index) {
        List<String> columns = columnNames(index.getColumnsNames());
        if (index instanceof ForeignKeyIndex foreignKey) {
            table.foreignKeys.add(new ForeignKey(columns, SqlText.tableName(foreignKey.getTable().getName())));
            return;
        }
        String type = Objects.requireNonNullElse(index.getType(), "").toUpperCase(Locale.ROOT);
        if (type.startsWith("PRIMARY")) {
            table.primaryKey.addAll(columns);
        } else if (type.startsWith("UNIQUE")) {
            addUnique(table, columns);
        }
        // KEY, INDEX and FULLTEXT are indexes, which the diagram does not represent.
    }

    private void addUnique(Table table, List<String> columns) {
        if (columns.size() != 1) {
            warn("Table %s: the unique constraint on (%s) spans several columns and is not represented.",
                    table.name, String.join(", ", columns));
            return;
        }
        ItemDTO column = table.columns.get(key(columns.get(0)));
        if (column == null) {
            warn("Table %s: the unique constraint refers to column %s, which the table does not have.",
                    table.name, columns.get(0));
            return;
        }
        column.setUnique(true);
    }

    private void applyPrimaryKey(Table table) {
        List<String> found = new ArrayList<>();
        for (String name : table.primaryKey) {
            ItemDTO column = table.columns.get(key(name));
            if (column == null) {
                warn("Table %s: the primary key refers to column %s, which the table does not have.", table.name, name);
            } else if (!column.getPk()) {
                column.setPk(true);
                found.add(column.getName());
            }
        }
        if (found.isEmpty()) {
            warn("Table %s has no primary key; the editor needs one to edit the table.", table.name);
        } else if (found.size() > 1) {
            warn("Table %s has a composite primary key (%s); the editor supports a primary key of one column.",
                    table.name, String.join(", ", found));
        }
    }

    // ------------------------------------------------------------------
    // ALTER TABLE
    // ------------------------------------------------------------------

    private void readAlterTable(String tableName, String clauses) {
        Table table = tables.get(key(tableName));
        if (table == null) {
            warn("ALTER TABLE %s was ignored: the script does not create that table.", tableName);
            return;
        }
        for (String clause : SqlText.splitTopLevel(clauses)) {
            if (IGNORED_CLAUSE.matcher(clause).lookingAt()) {
                continue;
            }
            try {
                Alter alter = (Alter) CCJSqlParserUtil.parse("ALTER TABLE " + PLACEHOLDER_TABLE + " " + normalized(clause));
                for (AlterExpression expression : alter.getAlterExpressions()) {
                    applyAlteration(table, expression, clause);
                }
            } catch (JSQLParserException e) {
                warn("ALTER TABLE %s: \"%s\" could not be read and was ignored.", tableName, abbreviated(clause));
            }
        }
    }

    private void applyAlteration(Table table, AlterExpression expression, String clause) {
        switch (expression.getOperation()) {
            case ADD -> applyAddition(table, expression);
            case MODIFY -> columnsOf(expression).forEach(column -> modifyColumn(table, column));
            default -> warn("ALTER TABLE %s: \"%s\" is not supported and was ignored.", table.name, abbreviated(clause));
        }
    }

    private void applyAddition(Table table, AlterExpression expression) {
        if (expression.getFkColumns() != null) {
            table.foreignKeys.add(new ForeignKey(columnNames(expression.getFkColumns()),
                    SqlText.tableName(expression.getFkSourceTable())));
        } else if (expression.getPkColumns() != null) {
            table.primaryKey.addAll(columnNames(expression.getPkColumns()));
        } else if (expression.getUkColumns() != null) {
            addUnique(table, columnNames(expression.getUkColumns()));
        } else if (expression.getIndex() != null) {
            addConstraint(table, expression.getIndex());
        } else {
            columnsOf(expression).forEach(column -> addColumn(table, column));
        }
    }

    private static List<? extends ColumnDefinition> columnsOf(AlterExpression expression) {
        return Objects.requireNonNullElse(expression.getColDataTypeList(), List.of());
    }

    // MODIFY replaces the definition of a column. Keys are indexes, not part of the definition,
    // so a column that was unique stays unique; the primary key is applied at the end.
    private void modifyColumn(Table table, ColumnDefinition definition) {
        ItemDTO previous = table.columns.get(key(SqlText.unquote(definition.getColumnName())));
        if (previous == null) {
            warn("ALTER TABLE %s: column %s cannot be modified because the table does not have it.",
                    table.name, SqlText.unquote(definition.getColumnName()));
            return;
        }
        addColumn(table, definition);
        ItemDTO modified = table.columns.get(key(previous.getName()));
        modified.setUnique(modified.getUnique() || previous.getUnique());
    }

    // ------------------------------------------------------------------
    // Relationships and layout
    // ------------------------------------------------------------------

    private List<LinkDataDTO> resolveForeignKeys() {
        Map<String, LinkDataDTO> links = new LinkedHashMap<>();
        for (Table source : tables.values()) {
            for (ForeignKey foreignKey : source.foreignKeys) {
                Table target = tables.get(key(foreignKey.referencedTable()));
                if (foreignKey.columns().size() != 1) {
                    warn("Table %s: the foreign key on (%s) spans several columns and is not represented.",
                            source.name, String.join(", ", foreignKey.columns()));
                    continue;
                }
                if (target == null) {
                    warn("Table %s: the foreign key on %s references table %s, which the script does not create.",
                            source.name, foreignKey.columns().get(0), foreignKey.referencedTable());
                    continue;
                }
                ItemDTO column = source.columns.get(key(foreignKey.columns().get(0)));
                if (column == null) {
                    warn("Table %s: the foreign key refers to column %s, which the table does not have.",
                            source.name, foreignKey.columns().get(0));
                    continue;
                }

                column.setFk(true);
                // A key declared both inside the table and in an ALTER TABLE is a single relationship.
                links.computeIfAbsent(key(source.name) + "." + key(column.getName()) + "->" + key(target.name),
                        ignored -> link(source, column, target));
            }
        }
        return new ArrayList<>(links.values());
    }

    // Drawn like the editor draws it: from the table holding the key to the one it references,
    // one-to-one when the key column is unique.
    private static LinkDataDTO link(Table source, ItemDTO column, Table target) {
        boolean oneToOne = column.getUnique() || (column.getPk() && source.primaryKey.size() == 1);
        LinkDataDTO link = new LinkDataDTO();
        link.setId(UUID.randomUUID().toString());
        link.setFrom(source.name);
        link.setTo(target.name);
        link.setText(oneToOne ? "1:1" : "1:N");
        link.setToText(1);
        link.setFromColumn(column.getName());
        return link;
    }

    private List<NodeDataDTO> nodes() {
        List<NodeDataDTO> nodes = new ArrayList<>();
        for (Table table : tables.values()) {
            LocationDTO location = new LocationDTO();
            location.setX(String.valueOf(nodes.size() * TABLE_SPACING));
            location.setY("50");

            NodeDataDTO node = new NodeDataDTO();
            node.setId(UUID.randomUUID());
            node.setKey(table.name);
            node.setItems(new ArrayList<>(table.columns.values()));
            node.setLocation(location);
            nodes.add(node);
        }
        return nodes;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String normalized(String element) {
        String unique = UNIQUE_INDEX.matcher(element).replaceFirst("$1 KEY");
        return NAMED_FOREIGN_KEY.matcher(unique).replaceFirst("$1 (");
    }

    // ["`a`", "b"] -> [a, b]
    private static List<String> columnNames(List<String> names) {
        return Objects.requireNonNullElse(names, List.<String>of()).stream().map(SqlText::unquote).toList();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String abbreviated(String text) {
        String singleLine = text.replaceAll("\\s+", " ");
        return singleLine.length() <= ABBREVIATION_LENGTH ? singleLine : singleLine.substring(0, ABBREVIATION_LENGTH) + "...";
    }

    private void warn(String format, Object... arguments) {
        warnings.add(String.format(format, arguments));
    }

    private static final class Table {
        private final String name;
        private final Map<String, ItemDTO> columns = new LinkedHashMap<>();
        private final List<String> primaryKey = new ArrayList<>();
        private final List<ForeignKey> foreignKeys = new ArrayList<>();

        private Table(String name) {
            this.name = name;
        }
    }

    private record ForeignKey(List<String> columns, String referencedTable) {
    }

}
