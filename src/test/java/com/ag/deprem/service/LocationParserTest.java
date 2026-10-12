package com.ag.deprem.service;

import com.ag.deprem.model.dto.LocationInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationParserTest {

    private LocationParser parser;

    @BeforeEach
    void setUp() {
        parser = new LocationParser();
    }

    @Test
    void parse_mediterraneanOffshoreWithDistance_correctlyIdentified() {
        LocationInfoDto info = parser.parse(
                "Akdeniz - [54.20 km] Silifke (Mersin)",
                "Mersin",
                "Silifke",
                "Türkiye"
        );

        assertTrue(info.getIsOffshore());
        assertEquals("offshore", info.getEnvironment());
        assertEquals(54.20, info.getDistanceToCoastKm());
        assertEquals("Silifke (Mersin)", info.getNearestSettlement());
        assertEquals("Akdeniz (Türkiye kıyısına • Silifke açıkları • 54.2 km)", info.getDisplayLocation());
    }

    @Test
    void parse_aegeanOffshore_correctlyIdentified() {
        LocationInfoDto info = parser.parse(
                "Ege Denizi - [18.87 km] Ayvacık (Çanakkale)",
                "Çanakkale",
                "Ayvacık",
                "Türkiye"
        );

        assertTrue(info.getIsOffshore());
        assertEquals("offshore", info.getEnvironment());
        assertEquals(18.87, info.getDistanceToCoastKm());
        assertEquals("Ayvacık (Çanakkale)", info.getNearestSettlement());
        assertEquals("Ege Denizi (Türkiye kıyısına • Ayvacık açıkları • 18.87 km)", info.getDisplayLocation());
    }

    @Test
    void parse_openSeaWithoutDistance_correctlyIdentified() {
        LocationInfoDto info = parser.parse(
                "Ege Denizi",
                "Muğla",
                "Bodrum",
                "Türkiye"
        );

        assertTrue(info.getIsOffshore());
        assertEquals("offshore", info.getEnvironment());
        assertNull(info.getDistanceToCoastKm());
        assertEquals("Ege Denizi (Açık Deniz)", info.getDisplayLocation());
        assertEquals("Bodrum (Muğla)", info.getNearestSettlement());
    }

    @Test
    void parse_crossBorder_correctlyIdentified() {
        LocationInfoDto info = parser.parse(
                "Khoy, West Azarbaijan (İran) - [49.21 km] Özalp (Van)",
                "Van",
                "Özalp",
                "Türkiye"
        );

        assertFalse(info.getIsOffshore());
        assertEquals("cross_border", info.getEnvironment());
        assertEquals(49.21, info.getDistanceToCoastKm());
        assertEquals("Özalp (Van)", info.getNearestSettlement());
        assertEquals("Khoy, West Azarbaijan (İran) (Özalp sınırında • 49.21 km)", info.getDisplayLocation());
    }

    @Test
    void parse_standardLandEarthquake_preserved() {
        LocationInfoDto info = parser.parse(
                "Pazarcık (Kahramanmaraş)",
                "Kahramanmaraş",
                "Pazarcık",
                "Türkiye"
        );

        assertFalse(info.getIsOffshore());
        assertEquals("land", info.getEnvironment());
        assertNull(info.getDistanceToCoastKm());
        assertEquals("Pazarcık (Kahramanmaraş)", info.getDisplayLocation());
    }

    @Test
    void parse_denizliLandNotTreatedAsSea() {
        LocationInfoDto info = parser.parse(
                "Çivril (Denizli)",
                "Denizli",
                "Çivril",
                "Türkiye"
        );

        assertFalse(info.getIsOffshore());
        assertEquals("land", info.getEnvironment());
        assertEquals("Çivril (Denizli)", info.getDisplayLocation());
    }
}
