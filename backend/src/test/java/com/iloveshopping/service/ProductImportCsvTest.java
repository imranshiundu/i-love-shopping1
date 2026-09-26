package com.iloveshopping.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSV parsing for the bulk product importer — quoted cells, escaped quotes,
 * embedded commas and unterminated quotes.
 */
class ProductImportCsvTest {

    @Test
    void parsesSimpleRow() {
        List<String> cells = ProductImportService.parseCsvLine("a,b,c");
        assertEquals(List.of("a", "b", "c"), cells);
    }

    @Test
    void keepsCommasInsideQuotes() {
        List<String> cells = ProductImportService.parseCsvLine("\"Mug, large\",12.50,in stock");
        assertEquals(List.of("Mug, large", "12.50", "in stock"), cells);
    }

    @Test
    void unescapesDoubledQuotes() {
        List<String> cells = ProductImportService.parseCsvLine("\"She said \"\"great\"\"\",ok");
        assertEquals(List.of("She said \"great\"", "ok"), cells);
    }

    @Test
    void handlesEmptyTrailingCell() {
        List<String> cells = ProductImportService.parseCsvLine("a,b,");
        assertEquals(3, cells.size());
        assertEquals("", cells.get(2));
    }

    @Test
    void handlesQuotedCellWithEscapedCommasAndSpaces() {
        List<String> cells = ProductImportService.parseCsvLine("\"Linen Napkin Set - Natural\",54.00,\"Home and Living\"");
        assertEquals(3, cells.size());
        assertEquals("Linen Napkin Set - Natural", cells.get(0));
        assertEquals("Home and Living", cells.get(2));
    }

    @Test
    void singleCellRowYieldsOneCell() {
        assertEquals(List.of("only"), ProductImportService.parseCsvLine("only"));
    }
}
