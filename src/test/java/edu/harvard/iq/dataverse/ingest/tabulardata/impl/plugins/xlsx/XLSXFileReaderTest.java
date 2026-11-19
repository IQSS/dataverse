package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XLSXFileReaderTest {

    private static final String SHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

    @Test
    public void testReadFirstRowWithSpans() throws IOException {
        String header = "<row r=\"1\" spans=\"1:3\">"
                + "<c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\" t=\"s\"><v>2</v></c></row>";
        assertIdNameScoreTable(read(header));
    }

    /**
     * The spans attribute is optional, see issue 12856.
     */
    @Test
    public void testReadFirstRowWithoutSpans() throws IOException {
        String header = "<row r=\"1\">"
                + "<c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\" t=\"s\"><v>2</v></c></row>";
        assertIdNameScoreTable(read(header));
    }

    @Test
    public void testReadFirstRowWithoutSpansAndWithEmptyLastHeaderCell() throws IOException {
        String header = "<row r=\"1\">"
                + "<c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\"/></row>";
        TabularDataIngest ingest = read(header);
        DataTable dataTable = ingest.getDataTable();

        assertEquals(3L, dataTable.getVarQuantity());
        assertEquals("id", dataTable.getDataVariables().get(0).getName());
        assertEquals("name", dataTable.getDataVariables().get(1).getName());
        // a header cell without a value is named after its column
        assertEquals("C", dataTable.getDataVariables().get(2).getName());
    }

    private void assertIdNameScoreTable(TabularDataIngest ingest) throws IOException {
        DataTable dataTable = ingest.getDataTable();

        assertEquals(3L, dataTable.getVarQuantity());
        assertEquals(2L, dataTable.getCaseQuantity());
        assertEquals("id", dataTable.getDataVariables().get(0).getName());
        assertEquals("name", dataTable.getDataVariables().get(1).getName());
        assertEquals("score", dataTable.getDataVariables().get(2).getName());
        assertTrue(dataTable.getDataVariables().get(0).isTypeNumeric());
        assertFalse(dataTable.getDataVariables().get(1).isTypeNumeric());
        assertTrue(dataTable.getDataVariables().get(2).isTypeNumeric());

        List<String> lines = Files.readAllLines(ingest.getTabDelimitedFile().toPath(), StandardCharsets.UTF_8);
        assertEquals(List.of("1.0\t\"a\"\t2.5", "2.0\t\"b\"\t3.0"), lines);
    }

    private TabularDataIngest read(String headerRow) throws IOException {
        String sheet = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"" + SHEET_NS + "\"><sheetData>"
                + headerRow
                + "<row r=\"2\"><c r=\"A2\"><v>1</v></c><c r=\"B2\" t=\"s\"><v>3</v></c><c r=\"C2\"><v>2.5</v></c></row>"
                + "<row r=\"3\"><c r=\"A3\"><v>2</v></c><c r=\"B3\" t=\"s\"><v>4</v></c><c r=\"C3\"><v>3</v></c></row>"
                + "</sheetData></worksheet>";
        byte[] xlsx = createXlsx(sheet);
        XLSXFileReader reader = new XLSXFileReader(new XLSXFileReaderSpi());
        return reader.read(new BufferedInputStream(new ByteArrayInputStream(xlsx)), false, null);
    }

    /**
     * Builds the smallest package Apache POI accepts, with the given worksheet
     * as its first sheet. The shared strings are: id, name, score, a, b.
     */
    private byte[] createXlsx(String sheetXml) throws IOException {
        String xmlDecl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        String relsNs = "http://schemas.openxmlformats.org/package/2006/relationships";
        String docRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "[Content_Types].xml", xmlDecl
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"
                    + "</Types>");
            put(zip, "_rels/.rels", xmlDecl
                    + "<Relationships xmlns=\"" + relsNs + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + docRel + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/workbook.xml", xmlDecl
                    + "<workbook xmlns=\"" + SHEET_NS + "\" xmlns:r=\"" + docRel + "\">"
                    + "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", xmlDecl
                    + "<Relationships xmlns=\"" + relsNs + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + docRel + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" Type=\"" + docRel + "/sharedStrings\" Target=\"sharedStrings.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/sharedStrings.xml", xmlDecl
                    + "<sst xmlns=\"" + SHEET_NS + "\" count=\"5\" uniqueCount=\"5\">"
                    + "<si><t>id</t></si><si><t>name</t></si><si><t>score</t></si><si><t>a</t></si><si><t>b</t></si></sst>");
            put(zip, "xl/worksheets/sheet1.xml", sheetXml);
        }
        return bytes.toByteArray();
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
