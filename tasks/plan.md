# Implementation Plan: Full-Stack Code Quality, Security & Performance Remediations

## Overview
Comprehensive remediation of the findings identified in the multi-axis code review for the `deprem` project. This plan covers database-level aggregation for crowdsourced felt reports to prevent OOM, IP spoofing mitigation, Jakarta Bean Validation at API boundaries, Spring transaction isolation in ingestion matching, XSS hardening in MapLibre popups, frontend fetch deduplication, and code-splitting optimizations.

---

## Architecture & Design Decisions

- **Decision 1: SQL-level aggregation for Felt Reports**  
  *Rationale:* Replace in-memory JVM streaming of entity instances with native database aggregations (`AVG`, `COUNT`, `GROUP BY` with `Pageable`). This reduces heap allocation from $\mathcal{O}(N)$ to $\mathcal{O}(1)$ during seismic traffic spikes, eliminating GC pauses and OOM risks.
- **Decision 2: Strict reverse-proxy IP resolution**  
  *Rationale:* Stop blindly taking the first comma-separated token in `X-Forwarded-For`. Favor Nginx's sanitized `X-Real-IP` (`$remote_addr`) and sanitize proxy chains to prevent duplicate vote bypass.
- **Decision 3: Isolated transaction propagation for matching**  
  *Rationale:* Use `Propagation.REQUIRES_NEW` on `evaluateMatchesForReport` so that transient matching errors do not mark the outer ingestion transaction as rollback-only.
- **Decision 4: Safe DOM construction for MapLibre popups**  
  *Rationale:* Standardize all popup rendering on `document.createElement` + `textContent` instead of `innerHTML` interpolation to completely eliminate XSS vectors.

---

## Tasks & Phases

### Phase 1: Security & Boundary Validation

#### Task 1: Add Jakarta Bean Validation to Felt Reports & Error Handling
**Description:** Add validation constraints (`@Size`, `@Pattern`, `@Min`, `@Max`) to `FeltReportRequestDto`, add `@Valid` to `FeltReportController`, and add `MethodArgumentNotValidException` handler to `GlobalExceptionHandler`.  
**Acceptance Criteria:**
- [ ] Submitting oversized province/district (>64 chars) or invalid felt level returns HTTP 400 Bad Request with ProblemDetail.
- [ ] Submitting invalid coordinates (latitude not in [-90, 90]) returns HTTP 400.
- [ ] Valid requests continue to be accepted.  
**Verification:**
- [ ] Backend tests: `./gradlew.bat test --tests FeltReportControllerTest`  
**Files:**
- `src/main/java/com/ag/deprem/model/dto/FeltReportRequestDto.java`
- `src/main/java/com/ag/deprem/controller/FeltReportController.java`
- `src/main/java/com/ag/deprem/controller/GlobalExceptionHandler.java`
- `src/test/java/com/ag/deprem/controller/FeltReportControllerTest.java`  
**Scope:** Medium (4 files)

#### Task 2: Harden Client IP Resolution against Spoofing
**Description:** Refactor IP extraction in `FeltReportController` to prioritize trusted Nginx header `X-Real-IP`, inspect proxy headers safely, and prevent client-supplied `X-Forwarded-For` injection.  
**Acceptance Criteria:**
- [ ] When `X-Real-IP` is provided by Nginx, it is used as the client IP.
- [ ] Spoofed `X-Forwarded-For` headers with forged leading IPs do not bypass rate limits or allow multiple submissions.  
**Verification:**
- [ ] Backend tests: `./gradlew.bat test --tests FeltReportControllerTest`  
**Files:**
- `src/main/java/com/ag/deprem/controller/FeltReportController.java`
- `src/test/java/com/ag/deprem/controller/FeltReportControllerTest.java`  
**Scope:** Small (2 files)

---

### Checkpoint 1: Security & Boundary Validation
- [ ] `./gradlew.bat test --tests "*FeltReport*"` passes
- [ ] API rejects malformed/overflowing payloads with RFC 7807 problem details

---

### Phase 2: Database Aggregation & Transaction Safety

#### Task 3: SQL Aggregations in `FeltReportRepository` & Service Refactoring
**Description:** Implement database-level aggregation queries in `FeltReportRepository` for average intensity, total report count, province groupings, reaction counts, and indexed existence check. Refactor `FeltReportService.getSummary()` to consume these projections instead of loading full entity lists into JVM heap.  
**Acceptance Criteria:**
- [ ] `getSummary()` executes SQL aggregation queries without loading all entity instances.
- [ ] `alreadyReportedByClient` is checked via indexed query `existsBySourceReportIdInAndIpHash(...)`.
- [ ] Output `FeltReportSummaryDto` matches existing API contract identically.  
**Verification:**
- [ ] Backend tests: `./gradlew.bat test --tests FeltReportServiceTest`  
**Files:**
- `src/main/java/com/ag/deprem/repository/FeltReportRepository.java`
- `src/main/java/com/ag/deprem/service/FeltReportService.java`
- `src/test/java/com/ag/deprem/service/FeltReportServiceTest.java`  
**Scope:** Medium (3 files)

#### Task 4: Ingestion Matching Transaction Isolation & Shared Resolver Helper
**Description:** Set `Propagation.REQUIRES_NEW` on `EventMatchingService.evaluateMatchesForReport` to prevent rollback-only contamination of `IngestionService.ingestReports`. Extract duplicated `findReportByIdString` and `resolveCurrentRevision` to `SourceReportRepository`.  
**Acceptance Criteria:**
- [ ] Failure in matching evaluation does not abort the outer ingestion transaction.
- [ ] Deduplicated report lookup helper is shared between `EarthquakeQueryService` and `FeltReportService`.  
**Verification:**
- [ ] Backend tests: `./gradlew.bat test --tests IngestionServiceTest` and `./gradlew.bat test --tests EventMatchingServiceTest`  
**Files:**
- `src/main/java/com/ag/deprem/service/EventMatchingService.java`
- `src/main/java/com/ag/deprem/service/IngestionService.java`
- `src/main/java/com/ag/deprem/service/EarthquakeQueryService.java`
- `src/main/java/com/ag/deprem/service/FeltReportService.java`  
**Scope:** Medium (4 files)

---

### Checkpoint 2: Core Backend Engine
- [ ] Complete test suite `./gradlew.bat test` passes (0 failures)
- [ ] Code compiles cleanly without transaction or aggregation warnings

---

### Phase 3: Frontend Hardening & Optimization

#### Task 5: Safe DOM Construction in `EarthquakeMap.tsx` & SVG Lint Cleanups
**Description:** Refactor `displayFaultPopup` in `EarthquakeMap.tsx` to construct DOM nodes with `textContent` instead of `innerHTML`. Fix `stroke-width` SVG attribute in `FeltSummaryCard.tsx` and resolve React Compiler set-state-in-effect warnings.  
**Acceptance Criteria:**
- [ ] Fault popup constructs elements safely without `innerHTML`.
- [ ] `npm run lint` finishes with 0 errors and resolves `stroke-width` warning.  
**Verification:**
- [ ] Frontend linter: `npm run lint`  
**Files:**
- `frontend/src/components/EarthquakeMap.tsx`
- `frontend/src/components/FeltSummaryCard.tsx`
- `frontend/src/components/FilterBar.tsx`  
**Scope:** Small (3 files)

#### Task 6: Deduplicate Data Fetching Logic in `App.tsx`
**Description:** Consolidate duplicate fetch orchestration between filter `useEffect` and `refreshData()` into a unified `loadEarthquakes(filters, page, signal)` callback in `App.tsx`.  
**Acceptance Criteria:**
- [ ] Both filter changes and manual/periodic refresh share the single data fetching and local snapshot workflow.
- [ ] Offline caching and error fallback work identically.  
**Verification:**
- [ ] Frontend build: `npm run build`  
**Files:**
- `frontend/src/App.tsx`  
**Scope:** Small (1 file)

#### Task 7: Lazy-Loading `EarthquakeMap` for Critical Path Optimization
**Description:** Code-split `EarthquakeMap` with React `lazy` and `Suspense` in `App.tsx` to ensure `vendor-maplibre` (~1MB) is not in the blocking critical path for mobile list users.  
**Acceptance Criteria:**
- [ ] Initial bundle size remains compact; MapLibre loads on demand.
- [ ] Map renders seamlessly with suspense boundary.  
**Verification:**
- [ ] Frontend build: `npm run build`  
**Files:**
- `frontend/src/App.tsx`  
**Scope:** Small (1 file)

---

### Checkpoint 3: Full-Stack Verification & Final Regression
- [ ] `./gradlew.bat test` succeeds (all backend unit and integration tests)
- [ ] `npm run lint` passes in `frontend`
- [ ] `npm run build` builds static assets cleanly into Spring Boot static resources
