# Fork Comparison — Hazebyte/CratePlugin vs Fruit-Servers/CratePlugin

Audit date: 2026-11-24. All SHAs re-verified against freshly cloned repositories at audit time.

| Repository | Branch | HEAD SHA |
|---|---|---|
| Hazebyte/CratePlugin (original) | `main` | `505d98845faf37aa15bbebe6fe3b56266753dbbc` |
| Fruit-Servers/CratePlugin (fork) | `main` | `e2e506797c0fc6889c2bf3b5c6f368f9c60792de` |
| Hazebyte/CrateReloadedAPI | `main` | `c0afc95e4588b69cc10ab4ee1d04f21b05a8f6ce` |
| Hazebyte/CrateReloadedNPC | `main` | `43cef238346b21f259741c2c732944b3503f6c00` |
| Hazebyte/crate.hazebyte.com | `main` | `cf2f9ffa242387022ef49909a9f784cdf9292b2c` |

`git rev-list --left-right --count fruit/main...hazebyte/main` → **10 ahead, 0 behind**. The fork is a strict
fast-forward of the original; there is no divergent Hazebyte work to reconcile.

Overall diff: 213 files changed, +11,754 / −1,123 (most of the additions are the vendored EffectLib source and
the in-tree `com.hazebyte:base` binary repository).

## Per-commit evaluation

All 10 commits are authored by `RebornIV` (2026-06-24 → 2026-06-29).

### 1. `3bdf5bf` — Java 17, dependency bumps, EffectLib relocation, concurrent-open guard
- **Files:** 51 (CorePlugin, claim storage, Give/Open crate components, `Animation`, `AnimationManager`,
  `CrateHandler`, `CrateValidatorImpl`, parsers, pagination, `SlikeyEffectNames`, …).
- **Original behaviour:** `Animation.players` was a static `ArrayList` that every new `Animation` instance
  cleared (`players.clear()` in each constructor), which silently disabled the "already opening" guard. Keys were
  removed even when the open was rejected.
- **New behaviour:** concurrent `Set`; constructors no longer clear it; a stale entry is recovered only if the
  player is not actually viewing an `AnimationHolder`; `CrateOpenResponse.opened` is propagated and
  `CrateHandler` only consumes the key when `response.isOpened()`; `Animation.clearTracked()` on disable;
  `closeCrate` captures the crate up front and handles a null crate.
- **Compatibility:** no config or data format change. **Security:** positive — closes a key-loss path and a
  double-open window (relevant to #496/#475/#573 class of issues). **License:** n/a. **Tests:** existing suite.
- **Decision: ADOPT.** Reward selection still happens at animation end (see open risks below).

### 2. `ce15a66` — drop private `m2.hazebyte.com` repo
- **Files:** 100. Vendors EffectLib source as a module and commits `bukkit/repo/com/hazebyte/base/0.0.1/*.jar`
  as a file-based Maven repo.
- **Reason:** `m2.hazebyte.com` no longer resolves (verified: connection fails), so the original cannot build.
- **License:** EffectLib is MIT (licence file retained in `effectlib/LICENSE`). `com.hazebyte:base` is a
  **binary-only jar with no source and no licence** — see `DEPENDENCY_AUDIT.md`.
- **Decision: ADAPT.** Keep EffectLib vendoring; **remove** the `base` binary (only one import site, replaceable
  by the API's own `ItemBuilder`).

### 3. `f6db66b` — JDK 25 toolchain
- **Files:** 15. Sets `java.version=25` globally.
- **Compatibility:** produces class-file v69 for every module, so the jar cannot load on any server older than
  MC 26.x (which requires Java 25). This conflicts with the multi-version goal (prompt §4.4).
- **Decision: ADAPT.** Keep 25 for the 26.x artifact; introduce per-artifact `release` levels (17/21/25).

### 4. `32f88a5` — rebase vendored EffectLib to 11.x
- **Files:** 111 (almost all under `effectlib/`). Adds Compatibility modules v1_12 / v1_13 / v1_17.
- **Decision: ADOPT**, but pin EffectLib to a non-SNAPSHOT version label for reproducibility.

### 5. `02e5cfd` — `spigot-api` → `paper-api 26.1.2.build.72-stable`
- **Files:** 7. **Compatibility:** compiles against Paper-only API; Spigot runtime support is no longer verified.
- **Decision: ADAPT.** Bump to `26.3.build.261-stable` (verified published) for the 26.x artifact; older
  artifacts compile against their own API version.

### 6. `7e3f854` — MockBukkit 26.x + JUnit 6
- **Files:** 15 (tests only). **Decision: ADOPT**; move to `mockbukkit-v26.3` (verified published).

### 7. `5c40b35` — soft-dep bumps, HolographicDisplays retired for DecentHolograms
- **Files:** 9, −253 lines. HolographicDisplays provider deleted.
- **Compatibility:** servers that still use HolographicDisplays lose holograms. HD has no 26.x build, so this is
  reasonable for the 26.x artifact. **Decision: ADOPT for 26.x; DEFER** restoring HD for legacy artifacts.

### 8. `53f0844` — 26.1 runtime fixes
- **Files:** 13. `ServerVersion` parsing for `26.x` version strings, jakarta.inject relocation, `HeadTexture`
  rewritten to `PlayerProfile` (directly addresses **#612**), legacy potion / enchantment / material aliases
  (`PotionUtil`, `Enchantments`, `Mat`). Adds 4 test classes.
- **Decision: ADOPT.** Needs cross-version verification: `PlayerProfile` texture API is unavailable before
  ~1.18.1, so the legacy path must sit behind an adapter.

### 9. `97b084d` — drop ProGuard obfuscation
- **Files:** 16, removes `lib/retrace.jar` (7.6 MB) and `scripts/obfuscate.sh`.
- **Decision: ADOPT.** Obfuscation has no place in a maintained source build.

### 10. `e2e5067` — CI/docs alignment, version 2.4.0
- **Files:** 9. **Decision: ADOPT.** Continue versioning from 2.4.0.

## PR Hazebyte/CratePlugin#1

Closed unmerged. Its content corresponds to the work that landed in the Fruit fork (commits 1–10 above); no
additional changes found that are not already represented. Decision: covered by the fork evaluation above.

## Baseline decision

**Selected baseline: Fruit-Servers/CratePlugin @ `e2e506797c0fc6889c2bf3b5c6f368f9c60792de`**
(comparison reference: Hazebyte/CratePlugin @ `505d98845faf37aa15bbebe6fe3b56266753dbbc`).

Reasons:
1. The original cannot be built today: its private repo is offline, and `placeholderapi:2.10.10` and
   `com.hazebyte:base` / `utils` are unresolvable (`hazebyte-baseline-build.log`).
2. The fork is a pure fast-forward (0 commits behind), so nothing from the original is lost.
3. The fork builds and its 206 tests pass on JDK 25 (see `BASELINE.md`).

Working copy: `CratePlugin/`, branch `modernization`, remotes `origin` = Fruit-Servers, `hazebyte` = original.

## Open risks carried into Milestone B
- Reward is chosen and given at animation **end** (`Animation.closeCrate`), after the key is already consumed.
  A disconnect or crash mid-animation can lose the reward (**#540**). Needs a persisted opening record.
- Whole jar is Java 25 bytecode — blocks every pre-26 target until build families are split.
- `item-nbt-api` relies on NMS reflection, so each server version needs separate verification (**#599**).
