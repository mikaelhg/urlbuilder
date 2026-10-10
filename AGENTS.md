## Project

`io.mikael:urlbuilder`: an immutable Java URL builder and parser with **zero runtime dependencies**.

- Entry: `io.mikael.urlbuilder.UrlBuilder` (`empty()`, `fromString/Url/Uri`, `with*`/`add*`/`set*`/`remove*`, `encodeAs`, `toString/Uri/Url`).
- `io.mikael.urlbuilder.util`: `Decoder`, `Encoder` (RFC 3986 pct-encoding), `Rfc3986Util`, `UrlParameterMultimap` (ordered, has `Immutable` view), `Runtime*Exception` wrappers.
- Builders are immutable + thread-safe; every `with*` returns a new instance.
- Standards: RFC 3986 / RFC 1738. WHATWG behaviour is not a goal unless noted.

## Commands

Java 21 (`mise.toml`). Use `./gradlew`.

| Task | Command |
|---|---|
| Tests (+ JaCoCo, report at `build/reports/jacoco/test/html/index.html`) | `./gradlew test` |
| Full CI build | `./gradlew build` |
| Static analysis | `./gradlew spotbugsMain` |
| Javadoc | `./gradlew javadoc` |

## Architecture rules

1. **No runtime deps.** Nothing in `implementation`/`api`. Test deps OK (JUnit Jupiter only for now).
2. **Immutable.** Public `UrlBuilder` fields are `final`, exposed collections unmodifiable, `with*` never mutates the receiver.
3. **Public API is stable** in `io.mikael.urlbuilder[.util]`. `Encoder`/`Decoder` are public on purpose.
4. **No regex in parsing/decoding.** `fromString` is a hand-written single-pass scanner (regex backtracking/stack depth/readability). Don't reintroduce `java.util.regex`.
5. **O(n)** decode/parse (long-input tests exist). No per-step input-sized allocations.
6. **Lenient in, strict out.** Malformed pct-escapes in input stay literal; output is always correctly encoded. Structural authority errors (bad port, unterminated IPv6) throw `IllegalArgumentException`.

## Style

- Match neighbours: 4-space indent, `final` params/locals where they do it, early returns, no wildcard imports in `src/main`.
- `/** */` Javadoc on new code; `Objects.requireNonNull(x, "x")` for public null checks.
- Comments only for non-obvious *why*.

## Testing

- JUnit Jupiter + `junit-jupiter-params`. Add tests to the matching class; new class only for a new unit.
- Name by behaviour (`withPortNullRemovesPort`), not `testFoo`.
- Every bug fix gets a regression test that fails without the fix.
- `@ParameterizedTest` over copy-paste.
- Timing-dependent tests: generous explicit timeouts (`assertTimeoutPreemptively`).

## Workflow

- Work on a topic branch, never directly on `master`. Branches are merged with `--no-ff` merge commits.
- Commits: `feat:|fix:|refactor:|test:|build:|docs:` prefix, imperative subject, body = *why*.
- No commit/merge/push unless asked. No rewriting published history.
- User-visible change → line under `## Unreleased` in `CHANGES.md`; flag behaviour changes.
- Dependabot owns dep bumps; keep GitHub Actions pinned by SHA.
- Run `./gradlew build` before calling it done; report failures as-is.

## Security-sensitive (parser differentials → SSRF, open redirect, allow-list bypass)

Extra care + tests when touching:

- authority split (`@`, `:`, IPv6 brackets) in `fromString`
- pct-decode/re-encode (`Decoder`, `Encoder`): never double-decode/encode
- host handling (`withHost`, IDN)

## Release

`.github/workflows/publish-release.yml` publishes to GitHub Packages. Manual publish to Maven Central.

## Don't

- Add runtime deps, reflection, or JPMS/`module-info` without asking.
- Edit `ITHAKA.md`, `LICENSE`, or license headers.
- Reformat untouched files.
- Commit `build/`, `.gradle/`, `.idea/`.
