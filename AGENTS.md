# AGENTS.md

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/) for all commit messages (for example `feat:`, `fix:`, `chore:`). That keeps history readable and lets [git-cliff](https://github.com/orhun/git-cliff) build [CHANGELOG.md](CHANGELOG.md) correctly. Refresh the changelog locally with:

```bash
git-cliff -o CHANGELOG.md
```

## Cursor Cloud specific instructions

This is a **Minecraft mod** (NeoForge 1.21.1) called **Nutritional Balance**. It adds a diet/nutrition tracking system to Minecraft.

### Key commands

| Task | Command |
|------|---------|
| Build | `./gradlew build` |
| Run client | `./gradlew runClient` |
| Run server (headless) | `./gradlew runServer` |
| Run data generation | `./gradlew runData` |
| Clean build | `./gradlew clean build` |

### Caveats

- **Java 21** is required (the Gradle toolchain auto-resolves it, but the system JDK must be 21+).
- **First build is slow** (~2 min) because Gradle downloads Minecraft, NeoForge, and decompiles sources. Subsequent builds are fast (~5s).
- **No automated tests exist** in the codebase. The `runGameTestServer` task is configured but there are no `@GameTest`-annotated classes.
- **Lint**: There is no dedicated linter. `./gradlew build` compiles with javac and produces 2 deprecation warnings about `EventBusSubscriber.bus()` and `EventBusSubscriber.Bus` being marked for removal — these are non-critical and come from NeoForge API evolution.
- **runClient** requires a display (`DISPLAY=:1` or Xvfb). The ALSA audio errors in logs are harmless (no sound card in cloud VMs).
- **runServer** runs headless (the `--nogui` flag is already configured in `build.gradle`). The EULA is auto-accepted in dev mode. The server starts on port 25565.
- **Gradle daemon** stays resident after builds. This is normal and speeds up subsequent builds.
- The output JAR lands in `build/libs/nourished-<version>.jar`.
