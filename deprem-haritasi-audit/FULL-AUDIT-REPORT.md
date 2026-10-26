# Full SEO Audit Report: deprem-haritasi.onrender.com

**Target URL:** [https://deprem-haritasi.onrender.com/](https://deprem-haritasi.onrender.com/)  
**Audit Date:** 2026-10-26  
**Auditor Engine:** Claude SEO (Universal Engine)  
**Overall SEO Health Score:** **83 / 100** (Grade: **B+**)

---

## Executive Summary

The application is an interactive real-time earthquake mapping platform displaying seismic events sourced from official Turkish agencies (AFAD and Kandilli Rasathanesi). 

### Key Strengths
- **Exemplary Schema Implementation (95/100):** Robust JSON-LD blocks incorporating `WebApplication`, `WebSite`, and a rich `FAQPage` with 5 validated Q&A pairs eligible for Google search rich snippets.
- **Sitemap Architecture:** Valid `sitemap.xml` containing clean programmatic URLs for all 81 Turkish provinces (`/son-depremler/{city}`).
- **Bot & AI Crawler Friendliness:** `robots.txt` explicitly enables indexing and crawling for standard search bots as well as AI crawlers (`GPTBot`, `PerplexityBot`, `ClaudeBot`, `Google-Extended`).
- **Modern Security & Edge Hosting:** SSL/TLS enforced via Cloudflare CDN with HTTP/3 support and fast edge delivery.

### Critical Gaps
1. **Duplicate `<h1>` Tags:** Homepage contains two separate `<h1>` elements.
2. **HTTP Header Locale Mismatch:** Server responds with `content-language: en-US` despite the entire site and metadata targeting Turkish (`tr_TR`).
3. **Thin Initial HTML Content:** Static HTML payload holds only ~153 words before client-side hydration, which limits keyword discovery for basic crawlers.
4. **Missing AI Discovery Layer:** No `/llms.txt` file is present to guide LLMs and AI agentic search bots.

---

## Detailed Category Breakdown

### 1. Technical SEO (Score: 88/100 — Weight: 22%)
| Check | Status | Details |
|---|---|---|
| HTTPS / SSL | **PASS** | Cloudflare edge SSL, valid certificate, HSTS active |
| robots.txt | **PASS** | `200 OK`, allows general access, declares sitemap location |
| XML Sitemap | **PASS** | Valid `urlset` at `/sitemap.xml` listing 81 province sub-pages |
| Canonical Tag | **PASS** | Self-referencing canonical points to `https://deprem-haritasi.onrender.com/` |
| Content-Language Header | **FAIL** | Server sends `en-US` instead of `tr` |
| 404 Error Handling | **WARNING** | Returns Spring Boot `application/problem+json` instead of HTML error page |

### 2. Content Quality & E-E-A-T (Score: 76/100 — Weight: 23%)
- **Experience & Expertise:** Accurate scientific explanations of magnitude measurement metrics (Moment magnitude $M_w$, Local/Richter $M_L$, and Duration $M_d$).
- **Authoritativeness:** Clear attribution to official institutions: AFAD (T.C. İçişleri Bakanlığı Afet ve Acil Durum Yönetimi Başkanlığı) and Boğaziçi Üniversitesi Kandilli Rasathanesi.
- **Thin Content Risk:** Client-side rendered map and table leave very little static body text on the root URL.
- **Trustworthiness:** Needs an explicit "Hakkımızda / İletişim / Veri Politikası" (About / Contact / Data Policy) footer to satisfy Google's E-E-A-T quality rater guidelines for disaster-related tools.

### 3. On-Page SEO (Score: 80/100 — Weight: 20%)
- **Title Tag (69 chars):** `Türkiye Deprem Haritası ve Canlı Depremler | AFAD ve Kandilli Verileri`
  - High relevancy, contains high-intent terms.
- **Meta Description (147 chars):** `AFAD ve Kandilli Rasathanesi verileriyle Türkiye ve yakın çevresindeki güncel depremleri interaktif harita ve anlık liste üzerinden takip edin.`
  - Within the 120-160 character sweet spot.
- **Headings Structure:**
  - `<h1>` **[DUPLICATE ISSUE]**:
    1. `Türkiye Deprem Haritası ve Güncel Depremler | AFAD Verileri`
    2. `Türkiye Deprem Haritası ve Güncel Depremler`
  - `<h2>`: `Canlı ve Açık Sismik Takip`, `Sıkça Sorulan Sorular`
  - `<h3>`: FAQ item questions
- **Open Graph & Twitter Cards:** Configured with `og:image` (`/seo/deprem-haritasi-og.png`, 1200x630) and locale `tr_TR`.

### 4. Schema & Structured Data (Score: 95/100 — Weight: 10%)
- Valid Schema types verified:
  - `WebApplication` (free utility application)
  - `WebSite` (site name, alternate names)
  - `FAQPage` (5 distinct questions and answers)

### 5. Performance & Core Web Vitals (Score: 85/100 — Weight: 10%)
- Cloudflare CDN edge reduces initial TTFB (< 300ms).
- Dynamic data streamed efficiently.
- Static assets gzipped.

### 6. AI Search Readiness (GEO) (Score: 78/100 — Weight: 10%)
- AI crawlers permitted in `robots.txt`.
- FAQ format is optimized for AI extraction.
- Missing `/llms.txt` and `/.well-known/ai-catalog.json`.

---

## Score Summary

$$\text{Health Score} = 0.22(88) + 0.23(76) + 0.20(80) + 0.10(95) + 0.10(85) + 0.10(78) + 0.05(88) = \mathbf{83/100}$$
