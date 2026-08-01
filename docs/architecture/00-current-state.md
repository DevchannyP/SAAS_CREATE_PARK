# 00. Current state

## Evidence

- Repository content at discovery: one SSOT HTML file and `.git`.
- No `AGENTS.md`, `README.md`, application source, or build descriptors.
- `codex-cli 0.145.0` is available.
- `git`, Node/npm, Java/Javac, Maven/Gradle, and Docker are not available on PATH.
- Mockup baseline hash is recorded in `docs/ssot/mockup-baseline.md`.

## Mockup contract

The DOM implements a 58px top bar, 265px left directory, central workspace with
198px bottom thread area, 330px right mapping area, architecture layer bar, two
harness tabs, and a 278px/remaining-width harness editor split. State is held in
`localStorage`; run buttons append simulated messages.

The embedded registries contain exactly five screens and fifteen events. Event
kinds are QUERY, COMMAND, NAVIGATION, and EXPORT. The mockup also declares the
five design and five code harness definitions and their fixed file paths.

## Build implication

[설계 가정] Because the repository is otherwise empty, the implementation uses
React/TypeScript/Vite, Java 21/Spring Boot, PostgreSQL/Flyway, and Docker Compose.
Versions are pinned in build files and must be confirmed by CI or a workstation
with the missing toolchain before a release claim.
