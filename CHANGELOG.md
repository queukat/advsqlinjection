# Changelog

All notable changes to this project should be documented in this file.

## [1.1.2] - 2026-05-20

### Added

- Structured current-file rule preview with copyable diagnostics, matched file/host/segment counts, example values, unavailable-language reporting, and truncation messaging.
- End-to-end fixture coverage for real injected PSI in YAML, JSON, Properties, and YAML block scalar values.
- Focused tests for common glob semantics, unavailable language preservation, and legacy migration compatibility.
- Named plugin verifier matrices so local checks can stay lightweight while release checks cover representative IDE branches.

### Changed

- Rule preview now runs PSI work through non-blocking read actions with committed documents, cancellation checks, a panel-scoped lifecycle, and safer preview button re-enabling.
- File glob matching now treats `**/` as zero or more directories for user-friendly patterns such as `config/**/*.yaml` and `**/*.yaml`.
- New rules start with no hidden default language; SQL remains available only through the explicit SQL example preset.
- Settings apply now refreshes editor highlighting so open files reflect changed injection rules immediately.
- Repository documentation now separates local baseline verification from the broader release verifier matrix.

### Fixed

- Preserved unavailable language IDs while editing existing rules instead of falling back to a placeholder.
- Kept legacy omitted SQL language migration behavior for older persisted rules while moving new-rule defaults to a neutral language selection.
- Cleaned repository hygiene issues including duplicate `.gitignore` content, unused Gradle catalog/repository entries, and misplaced Gradle wrapper properties.
- Moved historical fix-plan notes out of the active docs surface and removed private audit material from the product repository.

## [1.1.1] - 2026-05-03

### Added

- Bundled setup guide available from the Tools menu action and settings UI.
- Platform fixture tests that verify real language injection in YAML, JSON, and Properties values.
- A duplicate-rule button in the settings UI.
- An explicit `Add rule` button next to the SQL example action in settings.
- Support for YAML block scalar headers such as `snippet: |`, matching the header while injecting into the block body.
- User-facing warnings for rules whose language is unavailable in the current IDE.

### Changed

- The default Add action now starts from a neutral empty rule, while SQL remains an explicit example.
- Injector execution now uses a cached prepared rule snapshot with pre-resolved languages and compiled glob patterns.
- Removed the explicit upper IDE build bound while keeping compatibility anchored at IntelliJ Platform build `223.7571.182`.
- Rule preview now counts YAML block scalar header matches the same way as runtime injection.
- Searchable options generation now includes the plugin settings page.
- Settings UI internals were split into dedicated preview, table model, and renderer components.

### Fixed

- Fixed rule dialog initialization so opening `Add` no longer fails while the language selector is being created.
- Removed the hard runtime dependency on YAML PSI classes from YAML block scalar prefix detection.

## [1.1.0] - 2026-03-27

### Added

- Typed injection rules with safe migration from legacy raw-string settings.
- Path-aware rule matching, ordered rule execution, and non-overlapping multi-segment injection.
- Automated tests for migration, matching semantics, ordering, path filters, and persistence round-trips.
- GitHub Actions workflows for CI and tag-based releases.

### Changed

- Reworked the settings UI with add, edit, delete, reorder, validation, example rules, and current-file preview.
- Updated product wording and setup guidance to reflect the plugin's real generic language-injection use case.
- Marketplace badges and release automation documentation now live in the README.
