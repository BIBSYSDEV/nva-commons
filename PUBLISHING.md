# Publishing

Merging to `main` does not publish anything.
A release to Maven Central happens only when a GitHub release is published.

## Publishing a release

1. Update `VERSION_NAME` in `gradle.properties`.
   This can be its own small pull request, covering everything merged since the previous release.
2. Merge to `main`.
3. Create a GitHub release with a tag matching that version, prefixed with `v`:
   ```bash
   gh release create v2.11.3 --title v2.11.3 --generate-notes --target main
   ```
4. The `publish.yml` workflow checks that the tag matches `VERSION_NAME`, then builds, tests, signs, and publishes all modules to Maven Central.
5. Artifacts appear on Maven Central within 10 to 30 minutes.
6. Optional: edit the generated release notes on GitHub.

If you use [mise](https://mise.jdx.dev), `mise run publish:release` does step 3 for you.
It shows the latest release and the `VERSION_NAME` on `main`, then creates the release for that version once you confirm.
`mise run publish:versions` shows the same two versions on its own.

## Testing locally

Publish all modules to your local Maven repository:

```bash
./gradlew publishToMavenLocal -PlocalBuild
```

`-PlocalBuild` adds a `-local` suffix to the version (e.g. `2.11.3-local`), so the local build cannot be mistaken for a release on Maven Central.
The `nva-libs` catalog is published with the same suffix, so it points at the local modules.
`mise run publish:local` runs the same command.

In the consuming project, add `mavenLocal()` to `dependencyResolutionManagement.repositories` in `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

Then set the nva-commons version to the `-local` version, typically `nvaCommonsVersion` in `gradle.properties`.
Both the `versions` and `nva-libs` catalogs are resolved from that version.
Remember to revert both changes before committing.

## GitHub repository secrets

These are already configured.
If they need to be rotated:

| Secret             | Description                               |
| ------------------ | ----------------------------------------- |
| `OSSRH_USERNAME`   | Sonatype Central user token username      |
| `OSSRH_PASSWORD`   | Sonatype Central user token password      |
| `SIGNING_KEY`      | ASCII-armored GPG private key for signing |
| `SIGNING_PASSWORD` | Passphrase for the GPG key                |

When pasting the GPG key, ensure there is a trailing newline after `-----END PGP PRIVATE KEY BLOCK-----`.
