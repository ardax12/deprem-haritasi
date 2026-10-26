# SEO Action Plan: deprem-haritasi.onrender.com

Prioritized implementation plan based on the full website SEO audit.

---

## Phase 1: Critical Fixes & Quick Wins (Immediate / Days 1–3)

### 1. Fix Duplicate `<h1>` Heading
- **Severity:** Critical
- **Problem:** Two separate `<h1>` tags exist in the template.
- **Fix:** Keep the first primary descriptive heading as `<h1>` and convert the second one into an `<h2>` or styled `<div>`.
  ```html
  <!-- Before -->
  <h1>Türkiye Deprem Haritası ve Güncel Depremler | AFAD Verileri</h1>
  <h1>Türkiye Deprem Haritası ve Güncel Depremler</h1>

  <!-- After -->
  <h1 class="hero-title">Türkiye Deprem Haritası ve Güncel Depremler</h1>
  <p class="hero-subtitle">AFAD ve Kandilli Rasathanesi Canlı Sismik Takip Platformu</p>
  ```

### 2. Fix HTTP `Content-Language` Response Header
- **Severity:** High
- **Problem:** Server sends `Content-Language: en-US` for a Turkish language site.
- **Fix:** In Spring Boot / Nginx / Render headers, set:
  ```http
  Content-Language: tr
  ```

### 3. Implement Branded HTML 404 Page
- **Severity:** Medium
- **Problem:** Direct visits to broken URLs return raw JSON: `application/problem+json`.
- **Fix:** Add a user-facing `404.html` template in the frontend or Spring Boot `templates/error/404.html` with a prominent "Haritaya Dön" (Return to Map) button.

---

## Phase 2: Content Depth & E-E-A-T (Weeks 1–2)

### 4. Server-Side Pre-render Top Latest Events
- **Severity:** High
- **Problem:** Initial static HTML contains only 153 words.
- **Fix:** Render the latest 5 recorded earthquakes directly into the HTML body inside an accessible semantic table before Leaflet mounts.

### 5. Publish `/llms.txt` for AI Search Engines
- **Severity:** Medium
- **Problem:** AI search crawlers (Perplexity, ChatGPT Search, Gemini) search for `/llms.txt` to quickly index structured factual data.
- **Fix:** Create a clean `/llms.txt` file at the domain root with API endpoint descriptions, update cadence, and source documentation.

### 6. Add E-E-A-T Footer with Institutional Disclaimers
- **Severity:** Medium
- **Problem:** Lack of visible publisher information.
- **Fix:** Add an About/Contact section stating:
  - Data sources: AFAD Deprem Dairesi Başkanlığı & Kandilli Rasathanesi.
  - Project purpose: Open-source seismic awareness tool.
  - Emergency advisory: Official emergency notices must be followed via AFAD (afad.gov.tr).

---

## Phase 3: Programmatic SEO Scaling (Weeks 3–4)

### 7. City Page Enrichment (`/son-depremler/{city}`)
- Ensure all 81 provincial pages indexed in `sitemap.xml` include:
  - City-specific seismicity overview (e.g., active fault lines, historical earthquakes).
  - Breadcrumb markup (`BreadcrumbList` schema).
  - Pre-filtered map view focused on that province's coordinates.
