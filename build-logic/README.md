# Build Logic

This subproject contains Gradle convention plugins that centralize common build configuration
for the NVA project. These are published as artifacts to Maven Central alongside the Java
libraries in this repository.

## Convention Plugins

The core concept is to bundle relevant Gradle configuration into "convention" plugins.
These can be split up further without requiring changes from consumers, as long as the main
plugin IDs stay the same.

### `nva.root-module-conventions`

Configuration and utilities specific to the root project:

- Aggregates code coverage reports from all subprojects
- Enforces 100% test coverage on method and class level
- Custom tasks for convenience:
    - `verifyCoverage`: Verifies aggregated coverage meets requirements
    - `showCoverageReport`: Displays clickable link to coverage report
    - `dependencyUpdates`: Shows available dependency updates

Applied only in the root project's `build.gradle`.

### `nva.java-conventions`

Standard Java project configuration for all NVA Commons modules:

- Sets up Java toolchain with Amazon Corretto vendor
- Configures quality tools: PMD, Jacoco, ErrorProne
- Applies formatting conventions
- Sets up testing with JUnit platform
- Configures code coverage reporting

Applied directly in each Java module's `build.gradle`:

```groovy
plugins {
    id 'nva.java-conventions'
}
```

### `nva.formatting-conventions`

Provides consistent code formatting using Spotless:

- Java code formatting with Google Java Format
- Groovy Gradle script formatting
- Formatting for miscellaneous files (`.gitignore`, `.gitattributes`, `.editorconfig`, `*.md`)
- Automatically applies formatting during `build` and `test` tasks
- Supports `spotless:off` / `spotless:on` toggle comments

Applied automatically by either `nva.java-conventions` or `nva.root-module-conventions` plugins.

## Project Structure

```
build-logic/
├── build.gradle              # Plugin configuration and dependencies
├── settings.gradle           # Project settings
├── src/main/groovy/          # Convention plugins to be published
├── src/main/resources/       # Plugin resources
│   └── pmd-ruleset.xml       # PMD rules bundled with the plugin
└── README.md                 # This file
```

## How It Works

1. **Plugin Compilation**: Gradle compiles the `.gradle` files in `src/main/groovy/` into proper
   Gradle plugins
2. **Plugin Registration**: Each `.gradle` file becomes a plugin with an ID matching its filename
3. **Plugin Application**: Other subprojects apply these plugins using the standard `plugins {}`
   block

## Usage in Other Projects

See our [example repository](https://github.com/BIBSYSDEV/nva-gradle-template) for a complete
example on how to use these convention plugins.

## Customization

To modify shared build logic:

1. Edit the relevant `.gradle` file in `src/main/groovy/`
2. The changes will automatically apply to all projects using that convention plugin
3. Test changes by running `./gradlew build` from the root project

## PMD Ruleset

The `nva.java-conventions` plugin bundles a customized PMD ruleset (`pmd-ruleset.xml`) that enforces
NVA coding standards. To modify PMD rules for all projects, edit
`src/main/resources/pmd-ruleset.xml` in the build-logic project.
