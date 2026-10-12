package com.ag.deprem.service;

import com.ag.deprem.model.dto.LocationInfoDto;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocationParser {

    private static final Pattern BRACKETED_PATTERN = Pattern.compile(
            "^(.*?)\\s*-\\s*\\[([0-9.,]+)\\s*km\\]\\s*(.*?)(?:\\s*\\((.*?)\\))?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SEA_KEYWORDS = Pattern.compile(
            "(?i).*(deniz|k[oö]rfez|akdeniz|karadeniz|ege|marmara).*"
    );

    public LocationInfoDto parse(String place, String province, String district, String country) {
        LocationInfoDto info = new LocationInfoDto();

        if (place == null || place.isBlank()) {
            info.setIsOffshore(false);
            info.setEnvironment("land");
            info.setDisplayLocation(buildDefaultPlace(province, district, country));
            return info;
        }

        String trimmed = place.trim();
        Matcher matcher = BRACKETED_PATTERN.matcher(trimmed);

        if (matcher.matches()) {
            String region = matcher.group(1).trim();
            String distStr = matcher.group(2).trim().replace(',', '.');
            Double distanceKm = null;
            try {
                distanceKm = Double.parseDouble(distStr);
            } catch (NumberFormatException ignored) {}

            String nearDist = matcher.group(3).trim();
            String nearProv = (matcher.group(4) != null) ? matcher.group(4).trim() : null;

            StringBuilder nearest = new StringBuilder(nearDist);
            if (nearProv != null && !nearProv.isBlank()) {
                nearest.append(" (").append(nearProv).append(")");
            }

            info.setDistanceToCoastKm(distanceKm);
            info.setNearestSettlement(nearest.toString());

            boolean isSea = SEA_KEYWORDS.matcher(region).matches();
            if (isSea) {
                info.setIsOffshore(true);
                info.setEnvironment("offshore");
                String distText = (distanceKm != null) ? (distanceKm + " km") : "";
                info.setDisplayLocation(region + " (Türkiye kıyısına • " + nearDist + " açıkları • " + distText + ")");
            } else {
                info.setIsOffshore(false);
                info.setEnvironment("cross_border");
                String distText = (distanceKm != null) ? (distanceKm + " km") : "";
                info.setDisplayLocation(region + " (" + nearDist + " sınırında • " + distText + ")");
            }
            return info;
        }

        // Check if pure sea without bracketed distance (e.g. "Ege Denizi", "Akdeniz")
        if (SEA_KEYWORDS.matcher(trimmed).matches() && !isDenizliLand(trimmed, province)) {
            info.setIsOffshore(true);
            info.setEnvironment("offshore");
            info.setDisplayLocation(trimmed + " (Açık Deniz)");
            if (district != null && !district.isBlank()) {
                info.setNearestSettlement(district + (province != null ? " (" + province + ")" : ""));
            }
            return info;
        }

        // Standard land earthquake
        info.setIsOffshore(false);
        info.setEnvironment("land");
        info.setDisplayLocation(trimmed);
        return info;
    }

    private boolean isDenizliLand(String place, String province) {
        String p = (province != null) ? province.toLowerCase() : "";
        String pl = place.toLowerCase();
        return (p.contains("denizli") || pl.contains("denizli")) && !pl.contains("denizi") && !pl.contains("körfez");
    }

    private String buildDefaultPlace(String province, String district, String country) {
        if (district != null && province != null) {
            return district + " (" + province + ")";
        }
        if (province != null) return province;
        if (country != null) return country;
        return "Bilinmeyen Konum";
    }
}
