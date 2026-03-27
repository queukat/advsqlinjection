# Advanced Language Injection

Advanced Language Injection is a JetBrains plugin for projects that keep SQL or other DSL snippets inside structured configuration values such as YAML, JSON, or Properties files.

The plugin lets you define ordered rules that:
- choose any registered IDE language
- match by file name or file path
- match prefixes either at the start of a value or anywhere inside it
- inject one or several non-overlapping segments inside the matched value

## First useful setup

1. Open project settings and search for `Advanced Language Injection`.
2. Add an example rule or create a rule manually.
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
- Marketplace compatibility is limited to build line `223.*`

## Development checks

- `./gradlew.bat test --console=plain`
- `./gradlew.bat buildPlugin --console=plain`
- `./gradlew.bat verifyPlugin --console=plain`
- `./gradlew.bat runPluginVerifier --console=plain`

## Localization

- Runtime UI strings are localized through resource bundles.
- The plugin descriptor and marketplace-style metadata are intentionally English-only to avoid duplicated bilingual descriptions being shown at the same time.
