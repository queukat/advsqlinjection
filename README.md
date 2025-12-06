# Advanced SQL Injection

IntelliJ IDEA plugin that demonstrates advanced language injection for SQL or any other language inside YAML and other structured files, based on configurable prefix rules and file name patterns.

## Features

- Prefix based injection rules of the form `prefix=LANG_ID=glob`
- Supports any language registered in the IDE, not only SQL
- Per project settings, stored separately for each project
- Optional case insensitive prefix search
- Option to inject only the first occurrence or all occurrences within a literal
- UI for managing rules, built with Kotlin UI DSL
- Internationalized messages and settings (English and Russian)

## Requirements

- IntelliJ IDEA 2022.3 or compatible IDE on the IntelliJ Platform
- Java 17
- Tested with the `java` and `yaml` bundled plugins enabled

## Installation from source

1. Clone this repository.
2. Build the plugin distribution archive:

   ```bash
   ./gradlew buildPlugin
   ```

   The resulting zip file will be located under `build/distributions`.

3. In IntelliJ IDEA, open:

   - `Settings` → `Plugins` → gear icon → `Install Plugin from Disk`.
   - Select the generated zip file and restart the IDE when prompted.

## Usage

### Opening settings

After installation, open the plugin settings:

- `Settings` → `Advanced SQL Injection` (project level settings).

There you will see:

- A checkbox **Enable SQL injection in literals**.
- A checkbox **Inject all occurrences**.
- A checkbox **Case insensitive prefix**.
- A list of rules in the **Prefix / language / pattern** group.

### Rule format

Each rule is stored as a single string in the following format:

```text
<prefix>=<LANG_ID>=<file_glob>
```

- `prefix` – text prefix inside a string literal that marks the start of the injected code.
- `LANG_ID` – language identifier as known to IntelliJ IDEA, for example `SQL`, `PostgreSQL`, `JSON`, `XML`.
- `file_glob` – file name pattern where this rule is active, for example `*.yaml` or `*Config.json`.

The plugin converts the glob pattern to a regular expression and applies the rule only when the current file name matches the pattern.

Rules are evaluated from the top of the list. The first rule that matches both the file and prefix wins.

### Example

Suppose you often store SQL queries in YAML files:

```yaml
queries:
  listUsers: "sql:SELECT * FROM users WHERE id = :id"
  searchUsers: "sql:SELECT * FROM users WHERE name ILIKE :name"
```

You can configure a rule:

```text
sql:=SQL=*.yaml
```

With this rule:

- In `*.yaml` files, every string that contains `sql:` will get SQL injection applied to the part after the prefix.
- The IDE will highlight and complete the query as SQL inside the string literal.

### Multiple occurrences

If **Inject all occurrences** is disabled, the plugin injects only the first occurrence of the prefix in a literal and then stops. If it is enabled, the injector scans the literal and applies injection to each found occurrence until the end of the text.

### Case sensitivity

If **Case insensitive prefix** is enabled, the search for the prefix uses case insensitive comparison. For example, with the rule `sql:=SQL=*.yaml`, the prefixes `sql:`, `Sql:` and `SQL:` will all be recognized.

### Limitations

The IntelliJ Platform allows language injection only in file types whose PSI elements support it. That means:

- Works in many structured formats such as YAML, JSON, Properties and various configuration files.
- Does not work in plain text files such as `*.txt`.

## Development

The project uses Gradle with the IntelliJ plugin and Kotlin:

- Plugin id `com.queukat.advsqlinjection`
- Plugin name `Advanced SQL Injection`
- Group `com.queukat`
- Version `1.0.0`

## Localization

The plugin includes English and Russian resource bundles for its actions and settings. The IDE will choose the appropriate language based on the current UI language.
