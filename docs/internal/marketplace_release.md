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

## Removing A Bad Marketplace Upload

Use this only when a just-uploaded version must be replaced with the same version number, for example after Marketplace verification reports a problem.

1. Find the Marketplace update id:

   ```powershell
   $headers = @{ Authorization = "Bearer $env:PUBLISH_TOKEN_PLUGIN" }
   Invoke-RestMethod `
     -Uri "https://plugins.jetbrains.com/api/plugins/29252/updateVersions" `
     -Headers $headers
   ```

2. Delete the exact bad update id:

   ```powershell
   Invoke-RestMethod `
     -Uri "https://plugins.jetbrains.com/api/updates/<update-id>" `
     -Method Delete `
     -Headers $headers
   ```

3. Query `https://plugins.jetbrains.com/api/plugins/29252/updateVersions` again and verify that the bad version is gone before republishing.

For the 2026-05-20 `1.1.2` retry, the removed bad update id was `1051887`; the republished update id was `1051889`.

## Marketplace Verification Follow-Up

Marketplace verification can run against newer IDE branches than the local baseline. If Marketplace reports deprecated API usage:

1. Remove the deprecated API from production code.
2. Delete the bad Marketplace update if the same version number must be reused.
3. Run `.\gradlew.bat buildPlugin --console=plain`.
4. Run `.\gradlew.bat publishPlugin --console=plain`.

## GitHub Release Path

After committing and pushing the release changes, create and push a matching tag:

```powershell
git tag v1.1.2
git push origin main
git push origin v1.1.2
```

The release workflow builds, verifies, creates the GitHub Release, and publishes to JetBrains Marketplace when `PUBLISH_TOKEN_PLUGIN` is configured in GitHub secrets.
