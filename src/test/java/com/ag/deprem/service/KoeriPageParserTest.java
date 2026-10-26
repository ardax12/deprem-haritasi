package com.ag.deprem.service;

import com.ag.deprem.model.dto.KoeriRawEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class KoeriPageParserTest {

    private KoeriPageParser parser;

    @BeforeEach
    void setUp() {
        parser = new KoeriPageParser();
    }

    @Test
    void testParseSampleFixture() throws Exception {
        byte[] bytes;
        try (InputStream is = getClass().getResourceAsStream("/fixtures/koeri/koeri_sample_lst0.html")) {
            assertNotNull(is, "Fixture file koeri_sample_lst0.html must exist");
            bytes = is.readAllBytes();
        }

        KoeriPageParser.ParseResult result = parser.parse(bytes, StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(10, result.getEvents().size(), "Fixture should contain 10 earthquake rows");

        // First row: 2026.10.14 14:39:40  37.1263   35.0497       11.2      -.-  2.1  -.-   BOZCALAR-CUKUROVA (ADANA)                       İlksel
        KoeriRawEventDto first = result.getEvents().get(0);
        assertEquals("2026.10.14", first.getDate());
        assertEquals("14:39:40", first.getTime());
        assertEquals("37.1263", first.getLatitude());
        assertEquals("35.0497", first.getLongitude());
        assertEquals("11.2", first.getDepth());
        assertNull(first.getMd(), "-.- should become null");
        assertEquals("2.1", first.getMl());
        assertNull(first.getMw(), "-.- should become null");
        assertEquals("BOZCALAR-CUKUROVA (ADANA)", first.getLocation());
        assertEquals("İlksel", first.getSolutionQuality());

        // Row with Mw: 2026.10.14 12:44:09  39.2448   29.0063        9.8      -.-  2.0  3.1   YEMISLI-SIMAV (KUTAHYA)                         Revize Edilmiş
        KoeriRawEventDto revize = result.getEvents().get(7);
        assertEquals("2026.10.14", revize.getDate());
        assertEquals("12:44:09", revize.getTime());
        assertEquals("9.8", revize.getDepth());
        assertEquals("2.0", revize.getMl());
        assertEquals("3.1", revize.getMw());
        assertEquals("YEMISLI-SIMAV (KUTAHYA)", revize.getLocation());
        assertEquals("Revize Edilmiş", revize.getSolutionQuality());

        // Row with MD: 2026.10.14 12:31:30  39.1803   28.9727        8.6      2.4  -.-  -.-   EGIRLER-SIMAV (KUTAHYA)                         İlksel
        KoeriRawEventDto mdRow = result.getEvents().get(8);
        assertEquals("2.4", mdRow.getMd());
        assertNull(mdRow.getMl());
        assertNull(mdRow.getMw());

        // Row with all null magnitudes: 2026.10.14 12:26:33  38.0775   37.2412       12.4      -.-  -.-  -.-   GUMUSDOVEN-ELBISTAN (KAHRAMANMARAS)             İlksel
        KoeriRawEventDto noMagRow = result.getEvents().get(9);
        assertNull(noMagRow.getMd());
        assertNull(noMagRow.getMl());
        assertNull(noMagRow.getMw());
    }

    @Test
    void testParseMalformedLinesGracefully() {
        String malformedHtml = """
                <pre>
                Tarih      Saat      Enlem(N)  Boylam(E) Derinlik(km)  MD   ML   Mw    Yer                                             Çözüm Niteliği
                ---------- --------  --------  -------   ----------    ------------    --------------                                  --------------
                NOT A VALID DATE LINE
                2026.10.14 14:39:40  37.1263   35.0497       11.2      -.-  2.1  -.-   VALID LOCATION (ADANA)                          İlksel
                RANDOM FOOTER TEXT
                </pre>
                """;

        KoeriPageParser.ParseResult result = parser.parse(malformedHtml);
        assertEquals(1, result.getEvents().size(), "Only valid line should be parsed");
        assertEquals("VALID LOCATION (ADANA)", result.getEvents().get(0).getLocation());
    }

    @Test
    void testParseWindows1254Charset() {
        String turkishLine = "2026.10.14 14:39:40  37.1263   35.0497       11.2      -.-  2.1  -.-   SÖĞÜTLÜ-ŞEHİTKAMİL (GAZİANTEP)                  İlksel\n";
        byte[] encoded = turkishLine.getBytes(Charset.forName("windows-1254"));

        KoeriPageParser.ParseResult result = parser.parse(encoded, Charset.forName("windows-1254"));
        assertEquals(1, result.getEvents().size());
        assertEquals("SÖĞÜTLÜ-ŞEHİTKAMİL (GAZİANTEP)", result.getEvents().get(0).getLocation());
        assertEquals("İlksel", result.getEvents().get(0).getSolutionQuality());
    }
}
