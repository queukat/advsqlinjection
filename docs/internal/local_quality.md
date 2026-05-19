# Local Quality Notes

This repository uses a local SonarQube instance for quality checks. The public README keeps static local SonarQube badges plus a collapsible local quality snapshot; refresh both only after running the analysis locally and querying SonarQube again.

## Environment

The local machine provides these environment variables:

- `SONAR_HOME`
- `SONAR_HOST_URL`
- `SONAR_JAVA_PATH`
- `SONAR_TOKEN`

Do not commit token values. The last verified host value was `http://localhost:9000`.

## Last Local SonarQube Run

- Date: 2026-05-19
- Command: `.\gradlew.bat sonar --console=plain`
- Quality Gate: OK
- Coverage: 84.1%
- Bugs: 0
- Vulnerabilities: 0
- Code smells: 0
- Duplicated lines density: 0.0%
- NCLOC: 1689

Gradle/Sonar may warn about missing blame information while files are uncommitted. That warning does not mean the analysis failed.

## Plugin Verifier IDE Cache Size

The default local verifier run uses only `IC-2022.3.3` to reduce disk usage. The broader pre-release matrix is named `release` and can be run with:

```powershell
.\gradlew.bat runPluginVerifier "-PpluginVerifierMatrix=release" --console=plain
```

Use `-PpluginVerifierIdeVersions=...` or `PLUGIN_VERIFIER_IDE_VERSIONS` only for one-off custom checks.

The `release` matrix is:

- `IC-2022.3.3`
- `IC-2024.3.6`
- `IU-2025.3`
- `IU-2026.1.2`

Use `IU` for `2025.3` and later because IntelliJ IDEA Community (`IC`) builds are no longer published as a separate IDE line starting with 2025.3.

Local cache status after cleanup:

| IDE | Approx. size |
| --- | ---: |
| `IC-2022.3.3` | 2.69 GB kept locally |
| `IC-2024.3.6` | deleted; was about 2.71 GB |
| `IU-2025.3` | not currently cached or measured locally |
| `IU-2026.1.2` | deleted; was about 4.14 GB |

The default baseline-only cache is about 2.69 GB in `C:\Users\User\.cache\pluginVerifier\ides`. The release matrix downloads the missing IDEs on demand.
