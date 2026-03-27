# Fix Plan

## Context
- Repository: current workspace
- Audit source: pasted chat audit below this prompt; the pasted block contains a placeholder, so findings were revalidated against the current repository state before execution
- Goal: fix confirmed high-value issues without broad unrelated refactoring

## Audit items review
| ID | Issue | Severity | Audit says | Current status | Notes |
| --- | --- | --- | --- | --- | --- |
| A1 | Product description overclaims current behavior | High | Description says SQL-first and directory-based rules while implementation is narrower | stale / already fixed | Plugin surface is now generic-language and description matches ordered file/path-aware rules |
| A2 | Raw string rule storage is fragile | High | Rules are stored as `prefix=lang=pattern` strings | stale / already fixed | Typed `InjectionRule` model is live; legacy raw strings are kept only for migration input |
| A3 | No path-aware matching / overly coarse host matching | High | Matching is based on file name and raw host text only | stale / already fixed | Matching now runs on value text ranges plus file/path filters |
| A4 | `injectAllOccurrences` can create overlapping or misleading ranges | High | Multiple occurrences inject from each prefix to host end | stale / already fixed | Multiple hits are now split into non-overlapping segments |
| A5 | Settings UI is a raw string list with add/remove only | High | No edit/reorder/validation/clear presentation | stale / already fixed | Replaced with managed table + add/edit/delete/reorder + validation dialog |
| A6 | Fake test / preview UX | Medium | “Test injection” shows a message instead of testing anything | stale / already fixed | Replaced with real preview against the current editor file |
| A7 | Misleading welcome/action surface | Medium | “Installed and ready to use” is misleading when no rules exist | stale / already fixed | Action now opens the setup guide README |
| A8 | Documentation / first-value onboarding are missing | High | No README, examples, or honest setup guidance | stale / already fixed | Added README, example rule flow, and help entry in settings |
| A9 | Tests are missing | High | No tests for rule semantics, migration, persistence | stale / already fixed | Added matcher, planner, migration, and persistence tests |
| A10 | Compatibility / verification discipline is weak | Medium | Open-ended compatibility and limited verification discipline | stale / already fixed | `untilBuild` is constrained to `223.*`; verifier task was run successfully |
| A11 | Searchable options / settings discoverability are weak | Medium | Searchable options were incomplete and settings UX was not standard | partially confirmed | UI is now standard enough for use, but generated searchable options for the plugin still only contain action entries |

## Execution plan

### Phase 1 — Product contract / data model / correctness
- Introduce a typed rule model with migration from legacy raw strings.
- Add file path aware matching and safer value-content based prefix matching.
- Fix multiple occurrence handling to avoid overlapping injections.
- Make plugin wording truthful about generic language injection and supported matching modes.

### Phase 2 — UX / onboarding / discoverability
- Replace raw string list UI with a managed rules table.
- Add add/edit/delete/reorder/validation.
- Replace fake test button with a real preview against the current file.
- Add examples, empty-state guidance, and a help entry that points to repository docs.

### Phase 3 — Tests / verification / compatibility
- Add tests for migration, matching semantics, multiple rules, case sensitivity, invalid language, path matching, multiple occurrences, and persistence round-trip.
- Tighten plugin compatibility configuration and document available verification tasks.
- Run `test`, `buildPlugin`, `verifyPlugin`, and `runPluginVerifier` when the environment allows it.

### Phase 4 — optional polish
- Recheck searchable options after the settings UI rewrite.
- Trim dead or misleading strings and small UX rough edges that remain after core fixes.

## Repair log
- Status: done
  Problem: Audit findings were not yet grounded in a repository plan file.
  Fix: Revalidated the main audit findings against the current code and created this execution plan.
  Files: `docs/fix_plan.md`
  Verification: Re-read current `plugin.xml`, settings state/UI, injector, messages, and Gradle tasks.
  Notes: The prompt’s pasted audit block is a placeholder, so the current thread audit plus fresh code review are treated as the operative source.
- Status: done
  Problem: Rule storage, migration, and matcher semantics were too fragile and over-broad.
  Fix: Added a typed `InjectionRule` model, legacy migration, file/path-aware matching, value-based prefix targeting, an execution planner, and non-overlapping multiple-occurrence handling.
  Files: `src/main/kotlin/com/queukat/advsqlinjection/model/InjectionRule.kt`, `src/main/kotlin/com/queukat/advsqlinjection/model/LegacyRuleMigration.kt`, `src/main/kotlin/com/queukat/advsqlinjection/injection/InjectionRuleMatcher.kt`, `src/main/kotlin/com/queukat/advsqlinjection/injection/InjectionExecutionPlanner.kt`, `src/main/kotlin/com/queukat/advsqlinjection/injection/AdvancedSQLLanguageInjector.kt`, `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsState.kt`
  Verification: New unit tests cover migration, path matching, ordering, invalid languages, and multiple occurrences; `./gradlew.bat test --console=plain` passes.
  Notes: Legacy `prefixLanguagePatterns` remains in persisted state only to import old settings safely.
- Status: done
  Problem: The settings UI was raw, misleading, and could not get a user to first value safely.
  Fix: Replaced the raw list with a managed rules table, add/edit/delete/reorder flows, validation dialog, example rule, README opener, and real preview against the current file.
  Files: `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsPanel.kt`, `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionRuleDialog.kt`, `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionHelp.kt`, `src/main/kotlin/com/queukat/advsqlinjection/actions/ShowWelcomeAction.kt`, `src/main/resources/message.properties`, `src/main/resources/message_ru.properties`
  Verification: Compiles and packages successfully; preview flow is implemented against the active editor file and no longer shows a fake success message.
  Notes: The UI stays project-scoped to preserve per-project rule storage.
- Status: done
  Problem: Product wording, documentation, and compatibility signals were misleading or too weak.
  Fix: Renamed the product surface to `Advanced Language Injection`, updated plugin metadata, added a repository README, tightened `untilBuild`, and refreshed release notes.
  Files: `src/main/resources/META-INF/plugin.xml`, `build.gradle.kts`, `README.md`
  Verification: `./gradlew.bat buildPlugin verifyPlugin --console=plain` passes; `./gradlew.bat runPluginVerifier --console=plain` reports the plugin as compatible and dynamically eligible.
  Notes: The plugin ID and package names stay unchanged to avoid breaking continuity and unnecessary refactors.
- Status: partially done
  Problem: Searchable options were incomplete for the plugin settings.
  Fix: Reworked the settings UI, implemented `SearchableConfigurable`, and re-ran searchable-options generation.
  Files: `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsConfigurable.kt`, `src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsPanel.kt`
  Verification: `buildSearchableOptions` completes, but the generated plugin searchable-options XML still contains only action entries.
  Notes: This remains open; the likely cause is how project-level configurables are indexed by the searchable-options traversal, and changing the plugin to application-level settings would be a broader product change.
- Status: done
  Problem: The plugin descriptor showed both English and Russian text at once, which made the metadata look duplicated instead of locale-aware.
  Fix: Removed the Russian block from the plugin descriptor description and kept a single English metadata description; runtime localized bundles remain in place for actual UI strings.
  Files: `src/main/resources/META-INF/plugin.xml`
  Verification: Re-read the descriptor after the edit to confirm the description is now single-language.
  Notes: This change affects the plugin description surface only; `message_ru.properties` stays because it is used for locale-specific UI messages.
- Status: done
  Problem: The repository had no CI or release automation, so build health and tagged releases depended on manual local steps.
  Fix: Added GitHub Actions workflows for CI and tag-based releases, wired Gradle signing/publishing to environment-backed secrets, and documented the release secrets plus Marketplace badges in the README.
  Files: `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `build.gradle.kts`, `README.md`
  Verification: Re-read the workflow files and ran local Gradle checks to confirm the referenced tasks exist and pass in the current workspace.
  Notes: Marketplace publishing remains conditional on the required GitHub secrets being configured in the repository settings.

## Verification summary
- Revalidated current source structure and main audit claims against repository code.
- Listed current Gradle tasks to confirm availability of `test`, `buildPlugin`, `verifyPlugin`, and `runPluginVerifier`.
- Ran `./gradlew.bat test --console=plain` multiple times during implementation; final run passed.
- Ran `./gradlew.bat buildPlugin verifyPlugin --console=plain`; both passed.
- Ran `./gradlew.bat runPluginVerifier --console=plain`; verifier reported `Compatible` against `IC-223.8836.26` and said the plugin can probably be enabled or disabled without IDE restart.
- Re-ran searchable-options generation as part of packaging/verifier flows; generation succeeds, but plugin settings entries are still incomplete.

## Remaining issues / not fixed
- Searchable options for the plugin settings are still incomplete: the generated plugin XML still contains only action entries, even after moving to `SearchableConfigurable` and rewriting the UI. This is documented honestly instead of being claimed as fixed.
- Matching is safer and path-aware now, but it is still value-based rather than schema-aware. Deep YAML/JSON key/schema targeting was intentionally not added to avoid a broad and risky platform-specific expansion.
