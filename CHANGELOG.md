# Changelog

All notable changes to this project should be documented in this file.

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
