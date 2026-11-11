package com.erd.core.ddl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical helpers for MySQL scripts. String literals, quoted identifiers and comments can contain
 * semicolons, commas and parentheses, so statement and element boundaries are only looked for
 * outside them.
 */
final class SqlText {

    /** An identifier as MySQL accepts it: quoted with backticks or double quotes, or bare. */
    static final String IDENTIFIER = "(?:`(?:[^`]|``)+`|\"(?:[^\"]|\"\")+\"|[\\w$]+)";

    /** A table name, optionally qualified by its database; the table is captured. */
    static final String QUALIFIED_NAME = "(?:" + IDENTIFIER + "\\s*\\.\\s*)?(" + IDENTIFIER + ")";

    private static final Pattern LAST_NAME = Pattern.compile(QUALIFIED_NAME + "\\s*$");

    private SqlText() {
    }

    /**
     * The statements of a script, without their comments. Conditional comments ({@code /*!...*\/})
     * are dropped too: in a dump they hold session settings and table options, which a diagram
     * does not need.
     */
    static List<String> statements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = script.length();
        int i = 0;
        while (i < length) {
            char c = script.charAt(i);
            if (isQuote(c)) {
                int end = endOfQuoted(script, i);
                current.append(script, i, end);
                i = end;
            } else if (c == '#' || isDashComment(script, i)) {
                int end = script.indexOf('\n', i);
                i = end < 0 ? length : end;
            } else if (script.startsWith("/*", i)) {
                int end = script.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
                current.append(' ');
            } else if (c == ';') {
                addStatement(statements, current);
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        addStatement(statements, current);
        return statements;
    }

    /** The index of the parenthesis that closes the one at {@code open}, or -1 when there is none. */
    static int closingParenthesis(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isQuote(c)) {
                i = endOfQuoted(text, i) - 1;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /** The parts of {@code text} separated by commas outside parentheses and quotes. */
    static List<String> splitTopLevel(String text) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isQuote(c)) {
                i = endOfQuoted(text, i) - 1;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                addPart(parts, text.substring(start, i));
                start = i + 1;
            }
        }
        addPart(parts, text.substring(start));
        return parts;
    }

    /** An identifier without its quotes; a doubled quote inside it stands for one. */
    static String unquote(String identifier) {
        String name = identifier.trim();
        if (name.length() >= 2) {
            char quote = name.charAt(0);
            if ((quote == '`' || quote == '"') && name.charAt(name.length() - 1) == quote) {
                String doubled = String.valueOf(quote) + quote;
                return name.substring(1, name.length() - 1).replace(doubled, String.valueOf(quote));
            }
        }
        return name;
    }

    /** The table part of a name that may be qualified by its database, without quotes. */
    static String tableName(String qualifiedName) {
        Matcher matcher = LAST_NAME.matcher(qualifiedName.trim());
        return unquote(matcher.find() ? matcher.group(1) : qualifiedName);
    }

    private static boolean isQuote(char c) {
        return c == '\'' || c == '"' || c == '`';
    }

    // MySQL only starts a comment at "--" when a space or a control character follows it.
    private static boolean isDashComment(String text, int i) {
        return text.startsWith("--", i) && (i + 2 == text.length() || text.charAt(i + 2) <= ' ');
    }

    // The index just after the quote that closes the one at `start`. Inside string literals a
    // backslash escapes the next character; in all three forms a doubled quote stands for one.
    private static int endOfQuoted(String text, int start) {
        char quote = text.charAt(start);
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && quote != '`') {
                i += 2;
            } else if (c != quote) {
                i++;
            } else if (i + 1 < text.length() && text.charAt(i + 1) == quote) {
                i += 2;
            } else {
                return i + 1;
            }
        }
        return text.length();
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    private static void addPart(List<String> parts, String part) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }

}
