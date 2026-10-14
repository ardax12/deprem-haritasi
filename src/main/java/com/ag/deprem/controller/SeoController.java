package com.ag.deprem.controller;

import com.ag.deprem.service.IndexNowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
public class SeoController {

    private final IndexNowService indexNowService;

    @Value("${app.site.base-url:https://depremizleme.com}")
    private String baseUrl;

    public static final List<String> TURKISH_PROVINCE_SLUGS = List.of(
            "adana", "adiyaman", "afyonkarahisar", "agri", "amasya", "ankara", "antalya", "artvin", "aydin", "balikesir",
            "bilecik", "bingol", "bitlis", "bolu", "burdur", "bursa", "canakkale", "cankiri", "corum", "denizli",
            "diyarbakir", "edirne", "elazig", "erzincan", "erzurum", "eskisehir", "gaziantep", "giresun", "gumushane", "hakkari",
            "hatay", "isparta", "mersin", "istanbul", "izmir", "kars", "kastamonu", "kayseri", "kirklareli", "kirsehir",
            "kocaeli", "konya", "kutahya", "malatya", "manisa", "kahramanmaras", "mardin", "mugla", "mus", "nevsehir",
            "nigde", "ordu", "rize", "sakarya", "samsun", "siirt", "sinop", "sivas", "tekirdag", "tokat",
            "trabzon", "tunceli", "sanliurfa", "usak", "van", "yozgat", "zonguldak", "aksaray", "bayburt", "karaman",
            "kirikkale", "batman", "sirnak", "bartin", "ardahan", "igdir", "yalova", "karabuk", "kilis", "osmaniye", "duzce"
    );

    public SeoController(IndexNowService indexNowService) {
        this.indexNowService = indexNowService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        String cleanBase = baseUrl.replaceAll("/+$", "");
        String nowIso = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Main entrypoint
        xml.append("  <url>\n");
        xml.append("    <loc>").append(cleanBase).append("/</loc>\n");
        xml.append("    <lastmod>").append(nowIso).append("</lastmod>\n");
        xml.append("    <changefreq>always</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 81 Provinces programmatically generated
        for (String slug : TURKISH_PROVINCE_SLUGS) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(cleanBase).append("/son-depremler/").append(slug).append("</loc>\n");
            xml.append("    <lastmod>").append(nowIso).append("</lastmod>\n");
            xml.append("    <changefreq>hourly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>\n");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

        return new ResponseEntity<>(xml.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() {
        String cleanBase = baseUrl.replaceAll("/+$", "");

        StringBuilder robots = new StringBuilder();
        robots.append("User-agent: *\n");
        robots.append("Allow: /\n");
        robots.append("Disallow: /api/v1/archive/\n\n");

        // Explicit allow rules for Generative AI search crawlers (GEO)
        robots.append("# Generative Engine Optimization (GEO) Bot Access\n");
        robots.append("User-agent: Google-Extended\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: GPTBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: PerplexityBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: Claude-Web\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: CCBot\n");
        robots.append("Allow: /\n\n");

        robots.append("Sitemap: ").append(cleanBase).append("/sitemap.xml\n");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");

        return new ResponseEntity<>(robots.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{key}.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getIndexNowKeyFile(@PathVariable String key) {
        if (key.equals(indexNowService.getIndexNowKey())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(key);
        }
        return ResponseEntity.notFound().build();
    }
}
