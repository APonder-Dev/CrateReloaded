# Dependency & Licence Audit

Audit date: 2026-11-24. Baseline: Fruit-Servers/CratePlugin @ `e2e5067`.

## Repository licences (GitHub API `license.spdx_id`, plus file inspection)

| Repository | Licence file | Detected | Notes |
|---|---|---|---|
| Hazebyte/CratePlugin | **none** | none | README shows a "GPL v3" badge, but no LICENSE/COPYING file exists. |
| Fruit-Servers/CratePlugin | **none** | none | Inherits the same situation. |
| Hazebyte/CrateReloadedAPI | `LICENSE` | **MIT** (© 2019 Hazebyte) | Same file is present at `api/LICENSE` and must be kept. |
| Hazebyte/CrateReloadedNPC | **none** | none | — |
| Hazebyte/crate.hazebyte.com | **none** | none | Documentation text and images. |
| Hazebyte/utils (submodule) | — | — | Repo returns 404 (private or deleted). Sources are vendored in `utils/`. |

### ⚠ Licensing blocker (needs owner decision)

The main plugin, the NPC extension and the docs site have **no licence file**. A README badge is a statement of
intent, not a grant. Under default copyright, public visibility does not authorise redistribution.

Consequences until resolved:
- Modifying and running the plugin **privately on your own server** is the lowest-risk use.
- **Do not publish** jars, a public fork, or the migrated Wiki as if GPLv3 applied, unless Hazebyte confirms
  the licence (or you choose to rely on the README badge and accept that risk).
- New code written for this project is attributed to Anthony Ponder / APonder.dev. Its licence follows whatever
  licence the combined work ends up under.

## Build dependencies

| Dependency | Version | Scope | Shaded / relocated | Licence | Finding |
|---|---|---|---|---|---|
| `io.papermc.paper:paper-api` | 26.1.2.build.72-stable | provided | no | GPL-3.0 (API) | Bump to 26.3.build.261-stable. |
| `com.hazebyte:base` | 0.0.1 | compile | (unrelocated) | **unknown, binary only** | **Remove.** See below. |
| `com.hazebyte:utils` | 1.0.0 | compile | — | unknown (upstream 404) | In-tree source; keep, licence unclear. |
| `com.hazebyte:CrateReloadedAPI` | 2.2.1 | compile | no | MIT | In-tree module. |
| `co.aikar:acf-core` / `acf-bukkit` | **0.5.1-SNAPSHOT** | compile | → `com.hazebyte.libs.aikar.acf` | MIT | SNAPSHOT is not reproducible; pin a timestamped snapshot. |
| `co.aikar:locales` | **1.0-SNAPSHOT** | compile | → `com.hazebyte.libs.aikar.locales` | MIT | Same. |
| `com.elmakers.mine.bukkit:EffectLib` | **11.1-SNAPSHOT** | compile | `de.slikey` → `com.hazebyte.libs.slikey` | MIT | Vendored in `effectlib/`; label is a SNAPSHOT. |
| `com.google.dagger:dagger` | 2.60 | compile | → `com.hazebyte.libs.dagger` | Apache-2.0 | OK. |
| `org.mapstruct:mapstruct` | managed | compile | → `com.hazebyte.libs.org.mapstruct` | Apache-2.0 | OK. |
| ~~`de.tr7zw:item-nbt-api`~~ | ~~2.15.7~~ | — | — | MIT | **Removed (Milestone B, #599).** Its only caller was the disabled `ItemSerialization.appendNbtData`. |
| `org.bstats:bstats-bukkit` | 3.2.1 | compile | → `com.hazebyte.bstats` | MIT | Sends anonymous metrics; make sure it can be disabled. |
| `me.clip:placeholderapi` | 2.12.2 | provided | no | GPL-3.0 | OK. |
| `net.milkbowl.vault:VaultAPI` | 1.7 | provided | no | LGPL-3.0 | OK. |
| `com.github.decentsoftware-eu:decentholograms` | 2.10.1 | provided | no | GPL-3.0 | OK. |
| `com.hazebyte.lib:cmi` | 8.7.8.2 | provided | no | proprietary CMI | Satisfied by the `cmi-stub` module (57 lines of signature stubs, not shipped). |
| `com.mojang:authlib` | 9.0.75 | provided | no | Mojang | Provided by the server. Never shade it. |
| `json-simple`, `commons-lang` | 1.1.1 / 2.6 | provided | no | Apache-2.0 | `commons-lang` 2.x is not on modern Paper's classpath, so verify each use. |
| `org.testng:testng` (utils) | **RELEASE** | test | — | Apache-2.0 | Unpinned version; pin it, or remove it in favour of JUnit. |
| `junit-bom` | 6.1.0 | test | — | EPL-2.0 | OK. |
| `mockito` | 5.23.0 | test | — | MIT | OK. |
| `mockbukkit-v26.1.2` | 4.113.3 | test | — | MIT | Move to `mockbukkit-v26.3`. |

## `com.hazebyte:base:0.0.1`

- Location: `bukkit/repo/com/hazebyte/base/0.0.1/base-0.0.1.jar` (35 entries, classes dated 2021-12-05).
- POM: generated with `install:install-file`. No source, licence or SCM information.
- Contents: `Base`, `Component`, `InventoryListener`, `Size`, `Var`, `util/ItemBuilder`, `foundation/*Button`,
  `event/*Event`.
- Usage in the codebase: **a single import**, `com.hazebyte.base.util.ItemBuilder` in
  `bukkit/.../util/ConfigConstants.java` (6 static button items).
- The deployed 2.3.9 jar on the server does not contain `com/hazebyte/base`, so it was never shipped.
- **Decision:** replace the 6 usages with `com.hazebyte.crate.api.util.ItemBuilder` (MIT, in-tree), then delete
  the binary and the `in-project-libs` repository. This makes the build fully source-reproducible.

## Repository declarations

`aikar`, `extendedclip`, `codemc`, `spigot`, `papermc`, `jitpack.io`, `minecraft-libraries`, plus the file repo
`in-project-libs`. jitpack builds artifacts on demand from Git tags, so pinned versions remain mutable in
principle. Prefer the upstream repo for each artifact where one exists.
