# Baseline Build & Test Record (prompt §3.2 / §18.1)

Executed locally on 2026-11-24 (Windows 11, Maven 3.9.11). These results come from builds run that day, not
historical CI.

## Fruit-Servers/CratePlugin @ `e2e5067` — selected baseline

- JDK: `C:\Program Files\Java\jdk-25.0.3`
- Command: `mvn -B -ntp clean verify`
- Result: **BUILD SUCCESS** (4 min 47 s)

| Module | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| api (CrateReloadedAPI 2.2.1) | 13 | 0 | 0 | 0 |
| common | 9 | 0 | 0 | 0 |
| utils | 0 | 0 | 0 | 0 |
| bukkit | 184 | 0 | 0 | 0 |
| **Total** | **206** | **0** | **0** | **0** |

`utils` declares a TestNG dependency, but its 4 test classes don't run under the Surefire/JUnit 6 setup. Recorded
as "0 run" and not counted as passing.

All of these tests are unit or MockBukkit tests. **No real-server test exists in the baseline.**

## Hazebyte/CratePlugin @ `505d988` — original

- JDK: Temurin 17.0.16. Command: `mvn -B -ntp -DskipTests compile`
- Result: **FAILED** during dependency resolution:
  `me.clip:placeholderapi:jar:2.10.10` is no longer published, and `https://m2.hazebyte.com` is unreachable
  (also needed for `com.hazebyte:base` and `utils`).
- The original cannot be built reproducibly today.

## Server currently deployed

The unpacked jar in `../CrateReloaded/` is **CrateReloaded 2.3.9** (`api-version: 1.13`, main
`com.hazebyte.crate.cratereloaded.CorePlugin`). Its `config.yml`, `crate.yml` and `messages/` are the
configuration that migration must keep working.
