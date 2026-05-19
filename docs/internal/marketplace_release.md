# Marketplace Release Notes

Use this flow for direct JetBrains Marketplace uploads from the local machine.

## Variables

- `PUBLISH_TOKEN_PLUGIN` is the JetBrains Marketplace permanent token (`perm-...`). It is required for upload.
- `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` are signing variables. They are optional and are not required for the direct Marketplace upload flow.
- Do not commit token or key values.

## Version And Notes

1. Update `version` in `build.gradle.kts`.
2. Add the matching section to `CHANGELOG.md`, for example `## [1.1.2] - 2026-05-20`.
3. `patchPluginXml` reads that changelog section and uses it as JetBrains Marketplace change notes.

## Local Publish

Build the plugin ZIP, then publish it:

```powershell
.\gradlew.bat buildPlugin --console=plain
.\gradlew.bat publishPlugin --console=plain
```

`publishPlugin` uses `PUBLISH_TOKEN_PLUGIN` and uploads to the `default` Marketplace channel. The `signPlugin` task is available for explicit signed builds, but `publishPlugin` does not depend on it.

## GitHub Release Path

After committing and pushing the release changes, create and push a matching tag:

```powershell
git tag v1.1.2
git push origin main
git push origin v1.1.2
```

The release workflow builds, verifies, creates the GitHub Release, and publishes to JetBrains Marketplace when `PUBLISH_TOKEN_PLUGIN` is configured in GitHub secrets.
