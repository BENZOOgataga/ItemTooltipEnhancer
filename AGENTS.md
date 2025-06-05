# AGENT Guidelines for ItemTooltipEnhancer

This document defines how automated agents and contributors should interact with this repository.

## Repository Overview
- The project is a Forge mod for Minecraft 1.20.1.
- Java sources live in `src/main/java`.
- Resources and configuration files are under `src/main/resources`.
- Gradle build scripts (`build.gradle`, `settings.gradle`, `gradle.properties`) handle compilation and packaging.

## Code Style
- Use **4 spaces** for Java and Gradle files; do not use tabs.
- JSON and TOML resources use **2 spaces** for indentation.
- Keep lines under **120 characters** where possible.
- Opening braces for classes, methods and control structures go on the **same line**.
- End files with a newline.
- Use SLF4J `Logger` for logging.

## Commit Messages
- Write concise commit messages in the **imperative mood** (e.g. "Add new feature").
- Separate unrelated changes into different commits.
- Do **not** commit build output or IDE-specific files.

## Working With Versions
- The version in `gradle.properties` (`mod_version`) is bumped automatically by the CI workflow. Avoid editing it manually unless instructed.

## Build & Test
- Ensure JDK 17 is installed.
- Run `./gradlew build` before committing to verify the project compiles.
- If you need IDE run configs, execute `./gradlew IntelliJRuns`.

## Pull Requests
- Include a summary of the change and any gameplay impact.
- Confirm in the PR description that `./gradlew build` completed successfully.

## Licensing
- This project uses the MIT License. New source files should include an appropriate license header if required.

