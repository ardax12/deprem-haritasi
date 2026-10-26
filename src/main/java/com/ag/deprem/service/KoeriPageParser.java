package com.ag.deprem.service;

import com.ag.deprem.model.dto.KoeriRawEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KoeriPageParser {

    private static final Logger log = LoggerFactory.getLogger(KoeriPageParser.class);

    private static final Pattern PRE_TAG_PATTERN = Pattern.compile("<pre[^>]*>(.*?)</pre>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_LINE_PATTERN = Pattern.compile(
            "^(\\d{4}\\.\\d{2}\\.\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+([\\d\\.\\-]+)\\s+([\\d\\.\\-]+)\\s+([\\d\\.\\-]+)\\s+([\\d\\.\\-]+)\\s+([\\d\\.\\-]+)\\s+([\\d\\.\\-]+)\\s+(.*)$"
    );

    public static class ParseResult {
        private final List<KoeriRawEventDto> events;
        private final int totalLines;
        private final int parsedCount;
        private final int skippedCount;

        public ParseResult(List<KoeriRawEventDto> events, int totalLines, int parsedCount, int skippedCount) {
            this.events = events;
            this.totalLines = totalLines;
            this.parsedCount = parsedCount;
            this.skippedCount = skippedCount;
        }

        public List<KoeriRawEventDto> getEvents() { return events; }
        public int getTotalLines() { return totalLines; }
        public int getParsedCount() { return parsedCount; }
        public int getSkippedCount() { return skippedCount; }
    }

    public ParseResult parse(byte[] bytes, Charset charset) {
        if (bytes == null || bytes.length == 0) {
            return new ParseResult(List.of(), 0, 0, 0);
        }
        Charset effectiveCharset = (charset != null) ? charset : Charset.forName("windows-1254");
        String content = new String(bytes, effectiveCharset);
        return parse(content);
    }

    public ParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            return new ParseResult(List.of(), 0, 0, 0);
        }

        String textToParse = content;
        Matcher preMatcher = PRE_TAG_PATTERN.matcher(content);
        if (preMatcher.find()) {
            textToParse = preMatcher.group(1);
        }

        String[] lines = textToParse.split("\\r?\\n");
        List<KoeriRawEventDto> events = new ArrayList<>();
        int parsedCount = 0;
        int skippedCount = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("...") || line.startsWith("---") || line.startsWith("Tarih") || line.startsWith("Date")) {
                continue;
            }

            // Check if line starts with date
            Matcher matcher = EVENT_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                // If it's another non-event header/footer line, skip gracefully
                skippedCount++;
                continue;
            }

            try {
                String date = matcher.group(1).trim();
                String time = matcher.group(2).trim();
                String lat = matcher.group(3).trim();
                String lon = matcher.group(4).trim();
                String depth = cleanValue(matcher.group(5));
                String md = cleanValue(matcher.group(6));
                String ml = cleanValue(matcher.group(7));
                String mw = cleanValue(matcher.group(8));
                String remaining = matcher.group(9).trim();

                String location;
                String quality;

                // Split remaining into Location and Solution Quality
                // Check if fixed column layout applies: in lst0.asp, quality is separated by 2 or more spaces or at col >= 121
                String[] parts = remaining.split("\\s{2,}");
                if (parts.length >= 2) {
                    location = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1)).trim();
                    quality = parts[parts.length - 1].trim();
                } else {
                    location = remaining;
                    quality = null;
                }

                KoeriRawEventDto dto = new KoeriRawEventDto(
                        date, time, lat, lon, depth, md, ml, mw, location, quality, rawLine
                );
                events.add(dto);
                parsedCount++;
            } catch (Exception e) {
                log.warn("Failed to parse KOERI line: '{}': {}", line, e.getMessage());
                skippedCount++;
            }
        }

        log.info("KOERI parsed: totalLines={}, parsed={}, skipped={}", lines.length, parsedCount, skippedCount);
        return new ParseResult(events, lines.length, parsedCount, skippedCount);
    }

    private String cleanValue(String val) {
        if (val == null) {
            return null;
        }
        String trimmed = val.trim();
        if (trimmed.equals("-.-") || trimmed.equals("-") || trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
