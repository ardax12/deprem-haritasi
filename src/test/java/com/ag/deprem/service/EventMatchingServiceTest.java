package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.KoeriRawEventDto;
import com.ag.deprem.model.entity.EventGroup;
import com.ag.deprem.model.entity.EventGroupMember;
import com.ag.deprem.model.entity.EventMatch;
import com.ag.deprem.repository.EventGroupMemberRepository;
import com.ag.deprem.repository.EventGroupRepository;
import com.ag.deprem.repository.EventMatchRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventMatchingServiceTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceReportRepository reportRepository;

    @Autowired
    private SourceReportRevisionRepository revisionRepository;

    @Autowired
    private EventMatchRepository matchRepository;

    @Autowired
    private EventGroupRepository groupRepository;

    @Autowired
    private EventGroupMemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        groupRepository.deleteAll();
        matchRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    void testAutoMatchBetweenAfadAndKoeriForSameEarthquake() {
        // AFAD event: Adana Cukurova at 2026-10-14 11:39:45 UTC, lat 37.1270, lon 35.0500, Mag 2.2 ML
        AfadRawEventDto afad = new AfadRawEventDto();
        afad.setEventId("afad-1001");
        afad.setDate("2026-10-14T11:39:45");
        afad.setLatitude("37.1270");
        afad.setLongitude("35.0500");
        afad.setDepth("10.0");
        afad.setMagnitude("2.2");
        afad.setType("ML");
        afad.setLocation("Cukurova (Adana)");
        afad.setProvince("Adana");
        afad.setDistrict("Cukurova");

        // KOERI event: Adana Cukurova at 14:39:40 TRT (11:39:40 UTC), lat 37.1263, lon 35.0497 (time delta 5s, distance ~0.1 km)
        KoeriRawEventDto koeri = new KoeriRawEventDto(
                "2026.10.14", "14:39:40", "37.1263", "35.0497", "11.2",
                null, "2.1", null, "BOZCALAR-CUKUROVA (ADANA)", "İlksel", "line"
        );

        // 1. Ingest AFAD first
        ingestionService.ingestAfadEvents(List.of(afad));

        // 2. Ingest KOERI second
        ingestionService.ingestKoeriEvents(List.of(koeri));

        assertEquals(2, reportRepository.count(), "Two reports should be in DB (AFAD and KOERI)");

        // 3. Verify match created with AUTO_MATCHED
        List<EventMatch> matches = matchRepository.findByStatus("AUTO_MATCHED");
        assertEquals(1, matches.size(), "Should produce exactly 1 AUTO_MATCHED pair");

        EventMatch match = matches.get(0);
        assertTrue(match.getScore() >= 0.70, "Score should exceed auto-match threshold");
        assertTrue(match.getTimeDeltaSeconds() <= 10, "Time delta should be small (~5s)");
        assertTrue(match.getDistanceKm() < 1.0, "Distance should be very close (< 1km)");

        // 4. Verify unified EventGroup created and both reports linked
        assertEquals(1, groupRepository.count(), "Should create 1 unified EventGroup");
        EventGroup group = groupRepository.findAll().get(0);
        assertEquals("AUTO_CONFIRMED", group.getStatus());

        List<EventGroupMember> members = memberRepository.findByEventGroupId(group.getId());
        assertEquals(2, members.size(), "EventGroup must have both AFAD and KOERI members");
    }

    @Test
    void testNoMatchWhenExceedingThreshold() {
        // AFAD event in Izmir at 10:00:00 UTC
        AfadRawEventDto afad = new AfadRawEventDto();
        afad.setEventId("afad-1002");
        afad.setDate("2026-10-14T10:00:00");
        afad.setLatitude("38.42");
        afad.setLongitude("27.14");
        afad.setDepth("10.0");
        afad.setMagnitude("3.0");
        afad.setType("ML");
        afad.setLocation("Izmir");

        // KOERI event in Van (hundreds of km away)
        KoeriRawEventDto koeri = new KoeriRawEventDto(
                "2026.10.14", "13:00:00", "38.50", "43.38", "10.0",
                null, "3.0", null, "TUSBA (VAN)", "İlksel", "line"
        );

        ingestionService.ingestAfadEvents(List.of(afad));
        ingestionService.ingestKoeriEvents(List.of(koeri));

        assertEquals(0, matchRepository.count(), "No match candidate should be created for distant events");
        assertEquals(0, groupRepository.count(), "No event group should be created");
    }

    @Test
    void testSameSourceNeverMatches() {
        // Two AFAD events close in time and space
        AfadRawEventDto afad1 = new AfadRawEventDto();
        afad1.setEventId("afad-1003");
        afad1.setDate("2026-10-14T10:00:00");
        afad1.setLatitude("38.42");
        afad1.setLongitude("27.14");
        afad1.setMagnitude("3.0");
        afad1.setType("ML");

        AfadRawEventDto afad2 = new AfadRawEventDto();
        afad2.setEventId("afad-1004");
        afad2.setDate("2026-10-14T10:00:05");
        afad2.setLatitude("38.42");
        afad2.setLongitude("27.14");
        afad2.setMagnitude("3.1");
        afad2.setType("ML");

        ingestionService.ingestAfadEvents(List.of(afad1, afad2));

        assertEquals(0, matchRepository.count(), "Reports from the same source must never match each other");
    }
}
