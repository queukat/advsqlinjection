# Advanced Language Injection

<!-- public-repo-status -->
> Status: Active JetBrains Marketplace plugin. Issues are open for bugs and focused feature requests.


[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/29252)](https://plugins.jetbrains.com/plugin/29252-advanced-language-injection)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29252)](https://plugins.jetbrains.com/plugin/29252-advanced-language-injection)
[![Rating](https://img.shields.io/jetbrains/plugin/r/stars/29252)](https://plugins.jetbrains.com/plugin/29252-advanced-language-injection)
![Local SonarQube Quality Gate](https://img.shields.io/badge/Local%20SonarQube-Quality%20Gate%20OK-brightgreen)
![Coverage](https://img.shields.io/badge/Coverage-84.0%25-brightgreen)
![Issues](https://img.shields.io/badge/Issues-0-brightgreen)
![Duplications](https://img.shields.io/badge/Duplications-0.0%25-brightgreen)

<details>
<summary><strong>Local SonarQube quality snapshot</strong> (checked 2026-05-16)</summary>

| Metric | Value |
| --- | --- |
| Quality Gate | OK |
| Overall coverage | 84.0% |
| New-code coverage | Not reported |
| Overall duplicated lines | 0.0% |
| Bugs | 0 |
| Vulnerabilities | 0 |
| Code smells | 0 |

These values are a checked local SonarQube API snapshot, not live cloud badges.
</details>

Advanced Language Injection is a JetBrains plugin for projects that keep SQL or other DSL snippets inside structured configuration values such as YAML, JSON, or Properties files.

The plugin lets you define ordered rules that:
- choose any registered IDE language
- match by file name or file path
- match prefixes either at the start of a value or anywhere inside it
- inject one or several non-overlapping segments inside the matched value
- preview rule matches against the current editor file before relying on them

## First useful setup

1. Open project settings and search for `Advanced Language Injection`, or use `Tools | Open Setup Guide`.
2. Click `Add` to create a neutral rule manually, or `Add SQL example` for a ready SQL preset.
3. Start with a safe rule:
   - Prefix: `sql:`
   - Language: `SQL`
   - File pattern: `*.yaml`
   - Path pattern: `config/**/*.yaml`
   - Match scope: `File name + path`
   - Prefix target: `Value starts with prefix`
4. Open a matching file and use `Preview current file` to confirm the rule matches the current editor file.

Example YAML value:

```yaml
query: "sql:select * from users where active = true"
```

For YAML block scalars, match the key/header line and inject into the block body:

```yaml
snippet: |
  SELECT * FROM users WHERE active = true
```

Use prefix `snippet: |` for this style. If your files use the `.yml` extension, set the file pattern to `*.yml`
or use `*.y*ml` to cover both `.yaml` and `.yml`.

## Supported use cases

- SQL embedded in YAML/JSON configuration
- Regex, GraphQL, or other IDE languages embedded in structured values
- Teams with path-based conventions such as `config/**/*.yaml` or `queries/**/*.json`

## Matching model

- Rules are ordered; the first matching rule wins.
- `File name only` ignores `Path pattern`.
- `File name + path` checks both the full path and the project-relative path.
- `Value starts with prefix` is the safer mode for structured config values.
- `Value contains prefix` is kept for migrated legacy rules and broader matching scenarios.
- `Inject every matched prefix segment` splits multiple prefix hits into non-overlapping injected ranges.
- YAML block scalar headers such as `snippet: |` can be used as prefixes; the injected range starts inside the block body.
- Rules that reference a language unavailable in the current IDE are shown as unavailable and skipped.

## Installation from source

1. Clone this repository.
2. Build the plugin distribution archive:

   ```bash
   ./gradlew.bat buildPlugin --console=plain
   ```

3. Install the generated ZIP from `build/distributions` using `Settings | Plugins | gear icon | Install Plugin from Disk`.

## Known limitations

- Injection only works for PSI elements that support IntelliJ language injection hosts.
- Plain text files such as `*.txt` are not supported.
- Matching is value-oriented, not schema-aware. The plugin does not understand domain-specific YAML or JSON schemas.
- Rule preview validates matching against the current file and its value hosts; it does not execute inspections for the injected language.

## Compatibility

- Built against IntelliJ IDEA 2022.3
- Declared compatibility starts at build `223.7571.182`; no explicit upper build bound is set.

## Development checks

- `./gradlew.bat test --console=plain`
- `./gradlew.bat buildSearchableOptions --console=plain`
- `./gradlew.bat buildPlugin --console=plain`
- `./gradlew.bat verifyPlugin --console=plain`
- `./gradlew.bat runPluginVerifier --console=plain`

## CI and release automation

- GitHub Actions CI runs on every push to `main` and on pull requests via [`ci.yml`](.github/workflows/ci.yml).
- CI runs `test`, `buildPlugin`, and `verifyPlugin`, then uploads the built plugin ZIP as an artifact.
- GitHub Releases are created from tags like `v1.1.1` via [`release.yml`](.github/workflows/release.yml).
- Release builds also run `runPluginVerifier`, attach the ZIP and `SHA256SUMS.txt`, and optionally publish to JetBrains Marketplace if the required secrets are configured.
- `CHANGELOG.md` is the single source of truth for release notes. The release workflow uses it for GitHub Releases, and `patchPluginXml` uses the same version section for JetBrains Marketplace change notes.

### Required GitHub secrets for Marketplace publishing

- `PUBLISH_TOKEN_PLUGIN` - JetBrains Marketplace permanent token (`perm-...`), not a GitHub token. Store the token exactly as Marketplace shows it; do not add a `perm:` prefix.
- `CERTIFICATE_CHAIN`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSWORD`

For local uploads on Windows, set the same Marketplace token with:

```powershell
[Environment]::SetEnvironmentVariable("PUBLISH_TOKEN_PLUGIN", "perm-...", "User")
```

## Localization

- Runtime UI strings are localized through resource bundles.
- The plugin descriptor and marketplace-style metadata are intentionally English-only to avoid duplicated bilingual descriptions being shown at the same time.
## License

<!-- commercial-license-policy -->
This project is licensed for non-commercial use under the [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0.txt).
Commercial use, resale, paid distribution, marketplace publication, SaaS hosting, or bundling into a paid product requires separate written permission from the author.
Project names, logos, package identifiers, store listings, screenshots, and other branding assets are not licensed for use in forks or redistributed builds.
