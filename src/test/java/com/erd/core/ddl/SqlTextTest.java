package com.erd.core.ddl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlTextTest {

    // ------------------------------------------------------------------
    // statements
    // ------------------------------------------------------------------

    @Test
    void testStatements_splitsOnSemicolonsOutsideQuotes() {
        String script = "CREATE TABLE a (s VARCHAR(5) DEFAULT 'x;y'); CREATE TABLE `b;c` (id INT);"
                + " CREATE TABLE d (n VARCHAR(5) DEFAULT \"p;q\")";

        assertEquals(List.of(
                "CREATE TABLE a (s VARCHAR(5) DEFAULT 'x;y')",
                "CREATE TABLE `b;c` (id INT)",
                "CREATE TABLE d (n VARCHAR(5) DEFAULT \"p;q\")"), SqlText.statements(script));
    }

    @Test
    void testStatements_dropsLineAndBlockComments() {
        String script = """
                -- a comment; with a semicolon
                # another one; too
                /* a block; comment */ CREATE TABLE a (id INT); /*!40101 SET NAMES utf8 */;
                CREATE TABLE b (id INT) -- trailing comment""";

        assertEquals(List.of("CREATE TABLE a (id INT)", "CREATE TABLE b (id INT)"), SqlText.statements(script));
    }

    @Test
    void testStatements_readsTwoDashesWithoutASpaceAsOperators() {
        // MySQL only opens a comment at "--" followed by a space or a control character.
        assertEquals(List.of("SELECT 1--1"), SqlText.statements("SELECT 1--1"));
        assertEquals(List.of("SELECT 1"), SqlText.statements("SELECT 1 --"));
    }

    @Test
    void testStatements_toleratesUnterminatedQuotesAndComments() {
        assertEquals(List.of("CREATE TABLE a (s CHAR DEFAULT 'open"), SqlText.statements("CREATE TABLE a (s CHAR DEFAULT 'open"));
        assertEquals(List.of("CREATE TABLE a (id INT)"), SqlText.statements("CREATE TABLE a (id INT) /* never closed"));
    }

    @Test
    void testStatements_keepsEscapedAndDoubledQuotesInsideLiterals() {
        String script = "INSERT INTO t VALUES ('it\\'s; fine', 'a''b; c', `x``;y`); SELECT 2";

        assertEquals(List.of("INSERT INTO t VALUES ('it\\'s; fine', 'a''b; c', `x``;y`)", "SELECT 2"),
                SqlText.statements(script));
    }

    @Test
    void testStatements_doesNotTreatABackslashAsAnEscapeInsideBackticks() {
        assertEquals(List.of("SELECT `a\\`", "SELECT 'b'"), SqlText.statements("SELECT `a\\`; SELECT 'b'"));
    }

    @Test
    void testStatements_skipsEmptyStatements() {
        assertEquals(List.of("SELECT 1"), SqlText.statements(" ; ;SELECT 1;; "));
    }

    // ------------------------------------------------------------------
    // closingParenthesis and splitTopLevel
    // ------------------------------------------------------------------

    @Test
    void testClosingParenthesis_skipsNestedParenthesesAndQuotedOnes() {
        String text = "t (a DECIMAL(10,2), b CHAR DEFAULT ')') ENGINE=InnoDB";

        assertEquals(text.indexOf(" ENGINE") - 1, SqlText.closingParenthesis(text, 2));
    }

    @Test
    void testClosingParenthesis_returnsMinusOneWhenItIsNeverClosed() {
        assertEquals(-1, SqlText.closingParenthesis("t (a INT, b INT", 2));
    }

    @Test
    void testSplitTopLevel_splitsOnlyOnCommasOutsideParenthesesAndQuotes() {
        String body = "id INT, price DECIMAL(10, 2), note VARCHAR(9) DEFAULT 'a,b', `odd,name` INT,, ";

        assertEquals(List.of("id INT", "price DECIMAL(10, 2)", "note VARCHAR(9) DEFAULT 'a,b'", "`odd,name` INT"),
                SqlText.splitTopLevel(body));
    }

    // ------------------------------------------------------------------
    // unquote and tableName
    // ------------------------------------------------------------------

    @Test
    void testUnquote_removesBackticksAndDoubleQuotesAndUndoublesThem() {
        assertEquals("users", SqlText.unquote(" `users` "));
        assertEquals("odd`name", SqlText.unquote("`odd``name`"));
        assertEquals("users", SqlText.unquote("\"users\""));
        assertEquals("say \"hi\"", SqlText.unquote("\"say \"\"hi\"\"\""));
    }

    @Test
    void testUnquote_leavesOtherNamesAlone() {
        assertEquals("users", SqlText.unquote("users"));
        assertEquals("`", SqlText.unquote("`"));
        assertEquals("`users", SqlText.unquote("`users"));
        assertEquals("'users'", SqlText.unquote("'users'"));
    }

    @Test
    void testTableName_dropsTheDatabaseAndTheQuotes() {
        assertEquals("customers", SqlText.tableName("`shop`.`customers`"));
        assertEquals("customers", SqlText.tableName("shop . customers"));
        assertEquals("customers", SqlText.tableName("customers"));
    }

    @Test
    void testTableName_returnsTheTextWhenItIsNotAName() {
        assertEquals("", SqlText.tableName(""));
        assertEquals("(id)", SqlText.tableName("(id)"));
    }

}
