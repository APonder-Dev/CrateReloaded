# CrateReloaded

> **Upstream:** [Hazebyte/CratePlugin](https://github.com/Hazebyte/CratePlugin) (original plugin), via the
> [Fruit-Servers/CratePlugin](https://github.com/Fruit-Servers/CratePlugin) fork. Issue history:
> [Hazebyte/CrateReloaded](https://github.com/Hazebyte/CrateReloaded/issues).

[![Version](https://img.shields.io/badge/version-2.4.0-green.svg)](CHANGELOG.md)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20%20%7C%201.21%20%7C%2026.x-orange.svg)](https://github.com/APonder-Dev/CrateReloaded/wiki/Compatibility)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-blue.svg)](#build-families)

A crate plugin for Paper servers: key, mystery and supply crates with animated openings (CSGO, Roulette and
more), weighted rewards, previews, holograms and a claim system for rewards that can't be delivered right away.

This is a maintained modernization of [Hazebyte's CrateReloaded](https://github.com/Hazebyte/CratePlugin), built on
the [Fruit-Servers fork](https://github.com/Fruit-Servers/CratePlugin). It keeps the plugin name, commands, data
folder and configuration format, so existing `config.yml`, crate files (`crates/*.yml`) and claim files keep working.

---

## Which jar do I need?

One source tree builds three jars. Install **one** of them, for your server's Minecraft line:

| Server | Jar | Java on the server |
|---|---|---|
| Minecraft 26.1 – 26.3 | `CrateReloaded-2.4.0-mc26.jar` | 25 |
| Minecraft 1.21 – 1.21.11 | `CrateReloaded-2.4.0-mc121.jar` | 21 |
| Minecraft 1.20.1 – 1.20.6 | `CrateReloaded-2.4.0-mc120.jar` | 17 (21 for 1.20.5+) |

If you install the wrong one, the plugin says which jar you need and disables itself. Start the server with
`-Dcratereloaded.allowUnsupportedVersion=true` to override this. Older versions (1.19.4 and below) are not
supported.

### Tested status

| Version | Status |
|---|---|
| Paper 1.20.4, 1.21.11, 26.1.2 | **Supported with limitations**: passed in-game tests (crate openings of every type, an attempt to take items from an animation, quitting mid-animation, offline gives) |
| Paper 26.3 | Experimental: starts, loads crates and reloads cleanly; not yet played in game |
| Other 1.20.x / 1.21.x / 26.x releases | Not yet tested; the jar is compiled against the lowest API of its family |
| Spigot | Not yet tested |

Details and known gaps: [Compatibility](https://github.com/APonder-Dev/CrateReloaded/wiki/Compatibility).

---

## What changed in 2.4.0

Highlights. The full list is in [CHANGELOG.md](CHANGELOG.md).

- **No more lost or doubled rewards.** Each animated opening's prize is saved as a claim before the animation
  starts and paid out exactly once, even if the player quits, rejoins, the plugin reloads or the server crashes
  (#540, #496).
- **No item duplication from crate menus.** Clicks and drags in every crate GUI are blocked, even when another
  plugin un-cancels them (#475, #573).
- **Claims are safer.** Claims written by 2.3.8 can be claimed again (#600). Claim file errors after
  `/cr claim all` are gone (#601), and a claim that can't be read is kept on disk instead of deleted.
- **Correct stacks.** Unstackable rewards (totems, armour, shulker boxes) go one per slot (#486).
- **`/cr give all <crate> [n] offline`** now puts the crates in each online player's `/cr claim` (#445, #524).
- **Version fixes.** `hide:true` items no longer crash on 26.x. Potions and enchantments resolve on every
  version. The NBT library that warned on every crate open was removed (#599).
- **Smaller fixes.** `{player-name}` works in `item:` rewards (#586). `{date}` works in the claim menu (#610).
  `{month}` is 1–12. The CSGO_REVERSE start frame is fixed (#536). There is an optional `crate.private-sounds`
  setting (#597), and the preview-menu title is read from `crate.menu`.

> **Upgrading from 2.3.x:** your configuration is not rewritten. One visible change: the preview menu title now
> comes from `crate.menu` in `config.yml`, as documented. Older versions read an undocumented `crate.name` key
> and ignored `crate.menu`.

---

## Configuration and commands

Everything is configured in `plugins/CrateReloaded/config.yml` and the crate files in `crates/`. Full documentation:
[the wiki](https://github.com/APonder-Dev/CrateReloaded/wiki). The original documentation still
applies: [crate.hazebyte.com](https://crate.hazebyte.com/).

New or changed options in 2.4.0:

| Key | Default | Meaning |
|---|---|---|
| `crate.menu` | `'Menu'` | Preview menu title (now actually used) |
| `crate.hotbar` | `false` | Allow moving crate items with hotbar number keys |
| `crate.private-sounds` | `false` | Crate sounds are heard only by the player opening the crate |

Changed command:

```
/cr give all <crate> [amount] [online|offline]   # offline = into each online player's /cr claim
```

---

## Building from source

### Build families

| Profile | Java bytecode | Compiled against | Tests |
|---|---|---|---|
| `mc26` (default) | 25 | Paper API 26.3 (build 249; also compile-checked against build 261) | MockBukkit v26.3 |
| `mc121` | 21 | Paper API 1.21.1 | MockBukkit v1.21, run against the 1.21.11 API |
| `mc120` | 17 | Paper API 1.20.1 | unit tests only (no MockBukkit 4.x exists for 1.20) |

### Requirements

- Maven 3.9+
- The JDK for the family you build: 25 (`mc26`), 21 (`mc121`) or 17 (`mc120`)
- Python 3 for the bytecode check (optional)

### Build a jar

```bash
mvn clean verify                # mc26 (JDK 25), with tests
mvn clean package -Pmc121       # 1.21 jar (JDK 21)
mvn clean package -Pmc120       # 1.20 jar (JDK 17)
```

The jar is written to `bukkit/target/CrateReloaded-<version>-<family>.jar`. To confirm it loads on the family's
Java version (bundled libraries included), run:

```bash
python3 scripts/check-bytecode.py bukkit/target/CrateReloaded-2.4.0-mc121.jar 21
```

The 1.21 test suite needs the newest 1.21 API at test time:
`mvn -Pmc121 verify -Dpaper.api.version=1.21.11-R0.1-SNAPSHOT -Deffectlib.paper.api.version=1.21.11-R0.1-SNAPSHOT`.
CI runs the lint, all three test suites and all three builds on every push and pull request.

### Code style

[Spotless](https://github.com/diffplug/spotless) with Palantir Java Format:

```bash
mvn -pl bukkit,common,nms spotless:check   # check (what CI runs)
mvn -pl bukkit,common,nms spotless:apply   # fix
```

---

## Project layout

| Module | Contents |
|---|---|
| `bukkit` | The plugin itself; version differences live in `com.hazebyte.crate.cratereloaded.compat` |
| `api` | CrateReloadedAPI, the public developer API (MIT), see [api/README.md](api/README.md) |
| `common`, `utils` | Shared code bundled into the plugin |
| `effectlib` | Vendored [EffectLib](https://github.com/elBukkit/EffectLib) (MIT) for crate effects |
| `cmi-stub` | Compile-time stubs for optional CMI hologram support (not shipped) |
| `nms` | Placeholder for server-internal adapters; currently empty |

Documentation: [the wiki](https://github.com/APonder-Dev/CrateReloaded/wiki).

---

## Licence

The CrateReloaded plugin repositories (Hazebyte and Fruit-Servers) contain **no licence file**. The original README
showed a GPLv3 badge, but no licence text was ever published. The `api` module is MIT-licensed
(© 2019 Hazebyte, see `api/LICENSE`), and the vendored EffectLib is MIT (`effectlib/LICENSE`). Both licence
texts ship inside the plugin jar.

## Credits

Original plugin by [Hazebyte](https://github.com/Hazebyte). Paper 26.x modernization by RebornIV
([Fruit-Servers](https://github.com/Fruit-Servers/CratePlugin)). Multi-version builds, fixes and test tooling in
this fork by Anthony Ponder / APonder.dev.
