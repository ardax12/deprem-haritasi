# SEO Implementation Guide

## Purpose and agreed scope

This document is the implementation handoff for adding a technical SEO baseline and social sharing metadata to the existing Turkish earthquake map.

- Canonical production origin: `https://deprem-haritasi.onrender.com/`
- The existing homepage is the only page to include in the sitemap.
- Event links use the existing `?event=<id>` query parameter and keep the generic site title, description, and social preview.
- No server-side rendering, prerendering, event-specific crawler metadata, new indexable content pages, or API changes are included.

Search engines and social crawlers should be able to identify the public homepage, understand its Turkish purpose, and display a useful link preview. The application remains a client-rendered React SPA; the initial HTML metadata must therefore be useful without executing JavaScript.

## Current delivery architecture

The frontend source lives in `frontend/`. `frontend/vite.config.ts` sets Vite's `build.outDir` to `../src/main/resources/static` and sets `emptyOutDir: true`. A frontend build regenerates the static directory that Spring Boot serves. The Docker build copies `src` into the Java builder stage and packages the application with `bootJar`.

Make authored frontend changes under `frontend/`. In particular, Vite copies files placed in `frontend/public/` to the root of the generated static output. Do not hand-edit generated files under `src/main/resources/static/`, including `index.html` or hashed bundles. A build can replace this directory, so verify its diff after building and retain only expected output changes.

`frontend/src/App.tsx` reads `?event=<id>` and opens the selected event in the browser. Social crawlers generally use the initial HTML response and do not execute this app to derive per-event metadata. This implementation intentionally serves generic homepage metadata for these links.

## File-by-file implementation

### `frontend/index.html`

Update the document head. Keep the existing UTF-8 charset, viewport, favicon, manifest, theme color, and app-install metadata.

Use the following content as the initial metadata copy; keep the wording in Turkish and adjust only when needed to match the actual user-facing product:

- `<html lang="tr">`
- Title: `Türkiye Deprem Haritası ve Güncel Depremler | AFAD Verileri`
- Description: `AFAD verileriyle Türkiye ve yakın çevresindeki güncel depremleri harita ve liste üzerinden takip edin.`
- Canonical URL: `https://deprem-haritasi.onrender.com/`
- Open Graph: `og:type=website`, `og:locale=tr_TR`, matching `og:title` and `og:description`, canonical `og:url`, and an absolute `og:image` URL.
- Twitter: `twitter:card=summary_large_image`, matching `twitter:title` and `twitter:description`, and the same absolute `twitter:image` URL.

Keep the title and description concise, accurate, and consistent with the visible map. Do not claim official affiliation with AFAD; describe the data as AFAD-sourced. Escape ampersands and other special characters correctly in HTML attributes. Do not add a `noindex` directive to the public homepage.

The canonical URL must represent the homepage, not the current event query. Do not make canonical metadata depend on client-side state: `?event=<id>` should still resolve to the same canonical homepage and generic social preview.

### `frontend/public/seo/deprem-haritasi-og.png`

Provide one public social preview image at this path unless an existing image is verified to be suitable and approved for this purpose. Target 1200 × 630 pixels in PNG or JPEG format. It should clearly identify the earthquake map, remain legible at social preview size, and avoid implying government ownership or endorsement. Keep the image static and lightweight enough for crawler fetching.

The metadata must use the absolute deployed URL `https://deprem-haritasi.onrender.com/seo/deprem-haritasi-og.png`. Vite should copy the image to `src/main/resources/static/seo/deprem-haritasi-og.png` during the build. Do not reference a Vite-hashed bundle asset for this image; it is a stable public URL.

If the repository has no suitable image and no image-generation/design decision has been made, leave the asset creation as an explicit implementation prerequisite. Do not publish metadata pointing to a missing image.

### `frontend/public/robots.txt`

Add a root-level crawler policy with these behaviors:

- Allow crawling of public pages by default.
- Disallow `/api/` paths from crawler indexing.
- Reference the sitemap using its absolute canonical-host URL: `https://deprem-haritasi.onrender.com/sitemap.xml`.

Do not block `/`, static assets, the social image, or files required for rendering. Do not use `robots.txt` as an access-control mechanism; API authorization remains the application's responsibility.

### `frontend/public/sitemap.xml`

Add a valid XML sitemap with exactly one `<url>` entry for `https://deprem-haritasi.onrender.com/`. Do not add event query URLs, API routes, hashed assets, or non-existent location pages. Use the sitemap namespace `http://www.sitemaps.org/schemas/sitemap/0.9`. Do not add a fabricated `lastmod`; omit optional timestamps unless a reliable homepage-content modification date is available and maintained.

### Generated production files

After source changes, run the frontend build so Vite emits the HTML, robots file, sitemap, and image into `src/main/resources/static/`. These are generated outputs, not additional authoring locations. Confirm that the generated HTML and public assets are present before building or deploying the Spring Boot application.

## Safeguards and edge cases

- Keep the canonical origin exactly `https://deprem-haritasi.onrender.com`; avoid `http`, alternate hosts, trailing-path inconsistencies, and development URLs in published metadata.
- Ensure the canonical URL and sitemap contain only the homepage. Query-string event links remain shareable but are not separate sitemap entries.
- Use the same title, description, and image across Open Graph and Twitter fields to avoid inconsistent previews.
- Verify the referenced image exists and can be fetched anonymously at its public URL before shipping metadata that points to it.
- Do not add event-specific previews in React alone. That would update a browser DOM after JavaScript runs, which is insufficient for many social crawlers.
- Do not add broad crawler disallows, remove app-shell assets, or block resources needed to render the public page.
- The service worker currently precaches the application shell and caches other same-origin assets on demand. If the SEO files or preview image are ever added to its explicit precache list, verify cache versioning and offline behavior; this SEO change does not require changing the service worker.
- Avoid unrelated generated-file churn from Vite's `emptyOutDir` behavior. Review `git status` and the diff after the build, especially hashed asset names and existing static files.
- Do not add structured data unless its claims and data freshness can be kept accurate. It is not required for this baseline.

## Implementation sequence

1. Confirm the public origin and inspect the candidate social image; create or approve the 1200 × 630 image if needed.
2. Edit `frontend/index.html` and add `robots.txt`, `sitemap.xml`, and the preview image under `frontend/public/`.
3. Run `npm run build` from `frontend/`. This runs TypeScript project builds and Vite, which writes production assets to `src/main/resources/static/`.
4. Inspect the generated output and Git diff. Confirm only expected generated artifacts changed; do not manually patch generated files.
5. Build and deploy through the existing Docker/Render flow, then verify the public URLs below.

The optional `npm run build:sync` script also copies static output into `build/resources/main/static` when that local Gradle output directory exists. It is not needed for the Docker deployment path, which packages the resources from `src` during `bootJar`.

## Acceptance checks

### Local source and generated output

- `frontend/index.html` contains one meaningful title, description, canonical link, Open Graph set, and Twitter card set.
- All social fields use the agreed Turkish copy and the same stable, absolute preview-image URL.
- The generated `src/main/resources/static/index.html` contains the same metadata as the source HTML.
- Generated output contains `/robots.txt`, `/sitemap.xml`, and the referenced social image at the expected root-relative paths.
- Sitemap XML parses successfully, names the canonical origin, and contains only the homepage URL.
- `robots.txt` allows public crawling, excludes the API path from crawling, and advertises the sitemap.
- Opening the homepage and a valid `?event=<id>` link still loads the app and opens the event detail modal as before.

### Deployed origin

After deployment, verify that these URLs return successful responses over HTTPS and the expected content types:

- `https://deprem-haritasi.onrender.com/`
- `https://deprem-haritasi.onrender.com/robots.txt`
- `https://deprem-haritasi.onrender.com/sitemap.xml`
- `https://deprem-haritasi.onrender.com/seo/deprem-haritasi-og.png`

Inspect the homepage response HTML itself (not only the browser's post-JavaScript DOM) to confirm metadata is present for crawlers. Check that the canonical link, `og:url`, and sitemap `<loc>` agree. Confirm the image URL is publicly accessible and renders at a useful social preview size.

Finally, use a social platform's link-preview debugger or equivalent crawler preview tool to check the homepage preview. Event query links should show the same generic preview by design.

## Out of scope

- Server-side rendering or prerendering.
- Unique titles, descriptions, canonical URLs, or social cards for individual earthquakes.
- Additional public pages, location landing pages, educational content, or changes to the app's routing model.
- Backend API changes, analytics integrations, or promises about search ranking or indexing speed.
