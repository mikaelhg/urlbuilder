# AGENTS.md

Guidance for AI coding agents working in this repository. Humans are welcome to read it too.

## Project

`io.mikael:urlbuilder`: an immutable Java URL builder and parser with **zero runtime dependencies**.

- Entry point: `io.mikael.urlbuilder.UrlBuilder` (`empty()`, `fromString`, `fromUrl`, `fromUri`, `with*`, `add*`, `set*`, `remove*`, `encodeAs`, `toString`, `toUri`, `toUrl`).
- Helpers in `io.mikael.urlbuilder.util`: `Decoder` (percent-decoding, query/path/user-info/fragment parsing), `Encoder` (percent-encoding per RFC 3986), `Rfc3986Util`, `UrlParameterMultimap` (ordered multimap, with an `Immutable` view), and the `Runtime*Exception` wrappers.
- Builder instances are immutable and thread-safe. Every `with*` call returns a new instance.
- Standards: RFC 3986 (URI) and RFC 1738 (URL). Browser (WHATWG) behaviour is *not* a goal except where noted.

## Commands

Java 21, pinned in `mise.toml`. Always use the Gradle wrapper.

| Task | Command |
|---|---|
| Run all tests (also writes the JaCoCo report) | `./gradlew test` |
| Full CI build (what GitHub Actions runs) | `./gradlew build` |
| Static analysis | `./gradlew spotbugsMain` |
| Coverage report | `build/reports/jacoco/test/html/index.html` after `./gradlew test` |
| Javadoc | `./gradlew javadoc` |

## Architecture rules

1. **No runtime dependencies.** Never add anything to `implementation` or `api`. Test dependencies are fine (currently JUnit Jupiter only).
2. **Immutability.** Public fields on `UrlBuilder` are `final`; collections exposed from it must be unmodifiable. New `with*` methods must return a new instance and leave the receiver unchanged.
3. **Public API compatibility.** Public signatures in `io.mikael.urlbuilder` and `io.mikael.urlbuilder.util` are API; `Encoder`/`Decoder` methods were deliberately made public.
4. **Parsing is a hand-written single-pass scanner.** `UrlBuilder.fromString` deliberately does **not** use regular expressions, because of real-world regex problems (backtracking, stack depth, readability). Do not reintroduce `java.util.regex` in parsing or decoding paths.
5. **Linear time.** Decoding and parsing must be O(n); there are tests with very long inputs. Avoid per-step allocation of input-sized buffers.
6. **Lenient on input, strict on output.** Malformed percent-escapes in input are kept as literal text rather than throwing; output is always correctly percent-encoded. Structural errors in the authority (bad port, unterminated IPv6 literal) throw `IllegalArgumentException`.
7. **Java language level.** Source/target follow the Gradle toolchain (Java 21).

## Code style

- Match the surrounding code: 4-space indent, `final` on parameters and locals where the neighbours do it, early returns, no wildcard imports in `src/main`.
- Javadoc: use `/** */` for new code.
- Prefer `Objects.requireNonNull(x, "x")` for public-API null checks.
- Keep comments to the non-obvious *why*; do not restate the code.

## Testing

- Framework: JUnit Jupiter (`org.junit.jupiter`), plus `junit-jupiter-params` for table-driven cases.
  Put new tests in the class that matches what is being tested; create a new class only for a new unit.
- Name tests for the behaviour (`withPortNullRemovesPort`), not `testFoo`.
- Every bug fix gets a regression test that fails without the fix.
- Parameterise with `@ParameterizedTest` instead of copy-pasting similar tests.
- Tests that depend on timing must have generous, explicit timeouts (`assertTimeoutPreemptively`).

## Workflow

- Work on a topic branch, never directly on `master`. Branches are merged with `--no-ff` merge commits.
- Commit messages: conventional-style prefix (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `docs:`), imperative subject, body explains *why*.
- Do not commit, merge or push unless the maintainer asks. Do not rewrite published history.
- Add a line under `## Unreleased` in `CHANGES.md` for any user-visible change (bug fix, behaviour change, API change). Call out behaviour changes explicitly.
- Dependency updates arrive via Dependabot. Do not hand-bump versions unless asked. GitHub Actions are pinned by commit SHA; keep it that way.
- Run `./gradlew build` before saying a change is done, and report failures faithfully.

## Security-sensitive areas

A URL parser sits on trust boundaries (SSRF, open-redirect and allow-list bypasses come from parser differentials). Take extra care and add tests when changing:

- the authority split (`@`, `:`, IPv6 brackets) in `UrlBuilder.fromString`
- percent-decoding and re-encoding (`Decoder`, `Encoder`): double decoding/encoding must never occur
- host handling (`withHost`, IDN conversion)

## Release

Publishing is done by `.github/workflows/publish-release.yml` to GitHub Packages; the README points at Maven Central.

## Things not to do

- Do not add runtime dependencies, reflection, or `module-info`/JPMS changes without asking.
- Do not edit `ITHAKA.md`, `LICENSE` or the license headers.
- Do not reformat files you are not otherwise changing.
- Do not commit anything under `build/`, `.gradle/` or `.idea/`.
