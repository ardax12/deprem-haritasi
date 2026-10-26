package com.ag.deprem.controller;

import com.ag.deprem.service.IndexNowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeoController {

    private final IndexNowService indexNowService;

    @Value("${app.site.base-url:}")
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
    public ResponseEntity<String> getSitemap(HttpServletRequest request) {
        String cleanBase = resolveBaseUrl(request);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Main entrypoint
        xml.append("  <url>\n");
        xml.append("    <loc>").append(cleanBase).append("/</loc>\n");
        xml.append("    <changefreq>always</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 81 Provinces programmatically generated
        for (String slug : TURKISH_PROVINCE_SLUGS) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(cleanBase).append("/son-depremler/").append(slug).append("</loc>\n");
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
    public ResponseEntity<String> getRobotsTxt(HttpServletRequest request) {
        String cleanBase = resolveBaseUrl(request);

        StringBuilder robots = new StringBuilder();
        robots.append("User-agent: *\n");
        robots.append("Allow: /\n");
        robots.append("Disallow: /api/\n\n");

        // Explicit allow rules for Generative AI search crawlers (GEO)
        robots.append("# Generative Engine Optimization (GEO) Bot Access\n");
        robots.append("User-agent: Google-Extended\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: GPTBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: OAI-SearchBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: ChatGPT-User\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: ClaudeBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: Claude-SearchBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: Claude-Web\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: PerplexityBot\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: Applebot-Extended\n");
        robots.append("Allow: /\n\n");

        robots.append("User-agent: CCBot\n");
        robots.append("Allow: /\n\n");

        robots.append("Sitemap: ").append(cleanBase).append("/sitemap.xml\n");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");

        return new ResponseEntity<>(robots.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/llms.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLlmsTxt(HttpServletRequest request) {
        String cleanBase = resolveBaseUrl(request);
        StringBuilder sb = new StringBuilder();
        sb.append("# Türkiye Deprem Haritası\n\n");
        sb.append("> AFAD ve Boğaziçi Üniversitesi Kandilli Rasathanesi açık verileriyle Türkiye ve yakın çevresindeki depremleri gerçek zamanlı harita ve liste üzerinde takip eden bağımsız sismik izleme platformu.\n\n");
        sb.append("## Veri Kaynakları & Entegrasyonlar\n");
        sb.append("- AFAD Deprem Dairesi Başkanlığı (T.C. İçişleri Bakanlığı Afet ve Acil Durum Yönetimi Başkanlığı)\n");
        sb.append("- Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü (KOERI-BDTİM)\n");
        sb.append("- Senkronizasyon: 5 dakikada bir otomatik arka plan taraması ve anlık soket/yenileme kontrolleri\n\n");
        sb.append("## Temel Bağlantılar\n");
        sb.append("- Ana Sayfa / Canlı Harita: ").append(cleanBase).append("/\n");
        sb.append("- XML Sitemap: ").append(cleanBase).append("/sitemap.xml\n");
        sb.append("- Ayrıntılı LLM Dokümanı: ").append(cleanBase).append("/llms-full.txt\n\n");
        sb.append("## Şehir Sayfaları (81 İl)\n");
        sb.append("Türkiye'nin tüm illerine ait güncel deprem kayıtları ve yerel fay hattı analizleri:\n");
        sb.append("- İstanbul: ").append(cleanBase).append("/son-depremler/istanbul\n");
        sb.append("- İzmir: ").append(cleanBase).append("/son-depremler/izmir\n");
        sb.append("- Ankara: ").append(cleanBase).append("/son-depremler/ankara\n");
        sb.append("- Hatay: ").append(cleanBase).append("/son-depremler/hatay\n");
        sb.append("- Kahramanmaraş: ").append(cleanBase).append("/son-depremler/kahramanmaras\n");
        sb.append("- Malatya: ").append(cleanBase).append("/son-depremler/malatya\n");
        sb.append("- Diğer tüm iller: ").append(cleanBase).append("/son-depremler/{il-slug}\n\n");
        sb.append("## API Referansı\n");
        sb.append("- GET /api/v1/earthquakes (Filtreler: minMagnitude, maxMagnitude, place, source, start, end, bbox)\n");
        sb.append("- GET /api/v1/earthquakes/{id} (Tekil deprem detayları ve revizyon geçmişi)\n");
        sb.append("- GET /api/v1/status (AFAD ve Kandilli kaynak sağlık ve tazelik durumları)\n");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/llms-full.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLlmsFullTxt(HttpServletRequest request) {
        String cleanBase = resolveBaseUrl(request);
        StringBuilder sb = new StringBuilder();
        sb.append("# Türkiye Deprem Haritası — Kapsamlı Bilgi ve Veri Rehberi\n\n");
        sb.append("> Türkiye ve komşu bölgelerdeki sismik aktiviteleri AFAD ve Kandilli Rasathanesi açık veri servislerinden alarak interaktif harita, filtrelenebilir liste, derinlik profili ve deprem revizyon geçmişiyle sunan açık kaynaklı web uygulamasıdır.\n\n");
        sb.append("## 1. Sismik Büyüklük Türleri ve Anlamları\n");
        sb.append("- Mw (Moment Büyüklüğü): Fay kırılmasıyla açığa çıkan toplam fiziksel mekanik enerjiyi hesaplar. En güvenilir ölçümdür.\n");
        sb.append("- ML (Yerel Büyüklük / Richter): Klasik sismografların ibre genliğine dayalı yerel büyüklük ölçümüdür. Küçük ve orta şiddetteki sarsıntılarda hızlı ilk değerlendirme sunar.\n");
        sb.append("- Md (Süreye Bağlı Büyüklük): Sismograf kaydındaki sarsıntı sinyalinin toplam süresinden türetilir.\n\n");
        sb.append("## 2. Türkiye Tektonik Fay Kuşakları\n");
        sb.append("- Kuzey Anadolu Fay Hattı (KAF): Saros Körfezi'nden başlayıp Marmara Denizi, Düzce, Bolu, Amasya, Tokat, Erzincan üzerinden Karlıova'ya uzanan ~1500 km uzunluğunda sağ-yanal doğrultu atımlı aktif fay kuşağıdır.\n");
        sb.append("- Doğu Anadolu Fay Hattı (DAF): Antakya, Maraş, Gölbaşı, Elazığ ve Bingöl hatlarını kapsayan sol-yanal doğrultu atımlı levha sınırıdır.\n");
        sb.append("- Batı Anadolu Açılma Rejimi: Ege kıyıları, Gediz, Büyük ve Küçük Menderes graben sistemlerini kapsayan normal fay ağırlıklı sismik bölgedir.\n\n");
        sb.append("## 3. Kurumsal Veri Kaynakları & E-E-A-T Şeffaflığı\n");
        sb.append("- T.C. İçişleri Bakanlığı Afet ve Acil Durum Yönetimi Başkanlığı (AFAD) Deprem Dairesi: https://deprem.afad.gov.tr\n");
        sb.append("- Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü Bölgesel Deprem-Tsunami İzleme ve Değerlendirme Merkezi (KOERI-BDTİM): http://www.koeri.boun.edu.tr\n");
        sb.append("- Bu platform bağımsız bir kamu bilgilendirme aracıdır. Resmi acil durum yönlendirmeleri için AFAD'ın duyuruları esastır.\n\n");
        sb.append("## 4. Tüm Şehir Sayfaları Listesi\n");
        for (String slug : TURKISH_PROVINCE_SLUGS) {
            sb.append("- ").append(cleanBase).append("/son-depremler/").append(slug).append("\n");
        }
        sb.append("\n## 5. API Uç Noktaları\n");
        sb.append("- ").append(cleanBase).append("/api/v1/earthquakes?minMagnitude=3.0\n");
        sb.append("- ").append(cleanBase).append("/api/v1/status\n");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
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

    private String resolveBaseUrl(HttpServletRequest request) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.replaceAll("/+$", "");
        }

        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank() || (!"http".equalsIgnoreCase(proto) && !"https".equalsIgnoreCase(proto))) {
            proto = request.getScheme();
            if (proto == null || (!"http".equalsIgnoreCase(proto) && !"https".equalsIgnoreCase(proto))) {
                proto = "https";
            }
        }
        proto = proto.toLowerCase();

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443 && port > 0) {
                host = host + ":" + port;
            }
        }

        if (host != null && host.contains(",")) {
            host = host.split(",")[0].trim();
        }

        if (host == null || !host.matches("^[a-zA-Z0-9.-]+(:[0-9]{1,5})?$")) {
            host = "localhost:8080";
        }

        if (host.endsWith(":80") && "http".equalsIgnoreCase(proto)) {
            host = host.substring(0, host.length() - 3);
        } else if (host.endsWith(":443") && "https".equalsIgnoreCase(proto)) {
            host = host.substring(0, host.length() - 4);
        }

        return proto + "://" + host;
    }
}
