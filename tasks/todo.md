# Task List: Full-Stack Code Quality, Security & Performance Remediations

- [x] **Phase 1: Security & Boundary Validation**
  - [x] Task 1: Add Jakarta Bean Validation to Felt Reports (`FeltReportRequestDto`, `FeltReportController`, `GlobalExceptionHandler`)
  - [x] Task 2: Harden Client IP Resolution against Spoofing in `FeltReportController`
  - [x] **Checkpoint 1:** Run `./gradlew.bat test --tests "*FeltReport*"` to verify boundary validation and anti-spoofing

- [x] **Phase 2: Database Aggregation & Transaction Safety**
  - [x] Task 3: SQL Aggregations in `FeltReportRepository` and refactor `FeltReportService.getSummary()`
  - [x] Task 4: Ingestion Transaction Isolation (`REQUIRES_NEW` on matching) and deduplicate report lookup helper
  - [x] **Checkpoint 2:** Run `./gradlew.bat test` (all 15 test suites must pass)

- [x] **Phase 3: Frontend Hardening & Optimization**
  - [x] Task 5: Safe DOM Construction in `EarthquakeMap.tsx` and SVG / effect lint cleanups
  - [x] Task 6: Deduplicate data fetching orchestration in `frontend/src/App.tsx`
  - [x] Task 7: Lazy-load `EarthquakeMap` in `App.tsx` for critical path optimization
  - [x] **Checkpoint 3:** Run `npm run lint` and `npm run build` in `frontend`

- [x] **Final Checkpoint: Full-Stack Regression**
  - [x] Execute `./gradlew.bat test` (All 61+ tests passing)
  - [x] Verify frontend build synchronizes to `src/main/resources/static`
