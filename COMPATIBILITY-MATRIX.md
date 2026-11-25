# Compatibility Matrix

Last updated: 2026-11-24 (Milestone B). A version is marked Supported only after the plugin has been played on that real server version. **Paper 1.20.4,
1.21.11 and 26.1.2 have now passed in-game bot tests** (8/8 scenarios each, below) and are marked **Supported with
limitations**. The limitations are listed under "Known gaps". 26.3 has only a console smoke test so far (the bot
library cannot join 26.3 yet), so it stays Experimental.
The "Evidence" column records what has actually been checked.

## Build families

One repository, three jars. Each jar is compiled against the **lowest** Paper API it claims, for the Java
release that Minecraft line requires. Build them with `build.sh` / `build.bat`, or `mvn -P<family> package`.

| Family | Jar | Minecraft | Java (bytecode) | Compiled against | `api-version` | Test suite runs against |
|---|---|---|---|---|---|---|
| `mc26` (default) | `crate-bukkit-<v>-mc26.jar` | 26.1 – 26.3 | 25 | Paper API 26.3.build.249-stable | `26.1` | MockBukkit v26.3 4.124.0 on 26.3.build.249 |
| `mc121` | `crate-bukkit-<v>-mc121.jar` | 1.21 – 1.21.11 | 21 | Paper API 1.21.1 | `1.21` | MockBukkit v1.21 4.116.3 on 1.21.11 |
| `mc120` | `crate-bukkit-<v>-mc120.jar` | 1.20.1 – 1.20.6 | 17 | Paper API 1.20.1 | `1.20` | none: no MockBukkit 4.x exists for 1.20 (api/common unit tests only) |

Notes:
- `mc26` compiles against build 249 because MockBukkit 4.124.0 cannot load build 261 (build 261 adds a
  `minecraft:world_clock` registry that MockBukkit lacks). `build.sh` also compiles the main code against
  `26.3.build.261-stable` to show it still builds against the newest 26.3.
- `scripts/check-bytecode.py` confirms that no class in a jar, shaded libraries included, needs a newer Java than the family
  targets.
- At startup, `compat.BuildFamily` compares the server version with the jar's family. A jar from the wrong
  family logs which jar to install and disables itself (override: `-Dcratereloaded.allowUnsupportedVersion=true`).
  A version on the right line but outside the tested range logs a warning.
- Version-specific code is isolated in `com.hazebyte.crate.cratereloaded.compat`. Enchantments and potion effects resolve
  by `minecraft:` key, which works on every family. Potion base types use `PotionCompat`, which detects
  `setBasePotionType` (1.20.2+) at runtime and otherwise falls back to `PotionData`.

## Per-version status

Status values follow the master prompt §4.3. "Mock" means the MockBukkit suite, not a real server.

| Minecraft | Family | Java | Status | Evidence |
|---|---|---|---|---|
| 26.3 | mc26 | 25 | Experimental | **Real server: Paper 26.3-262 started, plugin enabled, 5 crates loaded, reload OK.** Compiles (builds 249 and 261); 195 bukkit + 22 other tests pass (mock, build 249) |
| 26.2 | mc26 | 25 | Not yet tested | Covered by the 26.1-floor `api-version`; no 26.2-specific check |
| 26.1.2 | mc26 | 25 | Supported with limitations | **In-game bot tests 8/8 on Paper 26.1.2-74** |
| 26.1.1 / 26.1 | mc26 | 25 | Not yet tested | Same jar as 26.1.2 |
| 1.21.11 | mc121 | 21 | Supported with limitations | **In-game bot tests 8/8 on Paper 1.21.11-132.** Console smoke test OK. Mock suite passes at the 1.21.11 API |
| 1.21.10 / .8 / .5 / .4 / .3 | mc121 | 21 | Not yet tested | Within the floor/ceiling compile range only |
| 1.21.1 / 1.21 | mc121 | 21 | Not yet tested | Shipped jar compiles against 1.21.1 |
| 1.20.6 / 1.20.5 | mc120 | 21 (server) | Not yet tested | Plugin bytecode is Java 17. The server applies its pre-1.20.5 plugin compatibility rewriting (`api-version: 1.20`) |
| 1.20.4 | mc120 | 17 | Supported with limitations | **In-game bot tests 8/8 on Paper 1.20.4-499 (JDK 17).** Console smoke test OK. No server-mock tests |
| 1.20.2 / 1.20.1 | mc120 | 17 | Not yet tested | Shipped jar compiles against 1.20.1 with JDK 17 |
| 1.19.4, 1.18.2, 1.17.1, 1.16.5 | — | — | Unsupported | No build family. Startup check reports "No CrateReloaded build supports this version" |
| 1.8.8 – 1.15.2 | — | — | Unsupported | Legacy code paths (`ServerVersion` checks for 1.8/1.9/1.13) remain but are not built or tested |

## Real-server smoke tests (2026-11-24)

Harness: `../smoke/` (`prepare.sh` lays out the servers, `run_smoke.py` starts one, runs console commands, stops it
and saves `smoke.log`). Official Paper builds were downloaded from `fill.papermc.io` and SHA-256-verified. The
deployed 2.3.9 `config.yml`, `crate.yml` and `messages/` were **copied** into each server, not changed. The
Minecraft EULA was accepted by the server owner for these local test servers. Commands run on each:
`plugins`, `crate version`, `crate list`, `crate reload`, `crate list`.

| Case | Server | JDK | Jar | Result |
|---|---|---|---|---|
| 26.3 | Paper 26.3-262 | 25.0.3 | mc26 | Enabled; `FoodChest, FoodPack, FoodKeyT3, FoodKey, FoodKeyT2` loaded; reload re-loads all 5; clean stop |
| 1.21.11 | Paper 1.21.11-132 | 21.0.12 | mc121 | Same as above; clean stop |
| 1.20.4 | Paper 1.20.4-499 | 17.0.16 | mc120 | Same as above. The server stalls at "Closing Thread Pool" after `stop`, but a control run **with no plugins** stalls the same way, so the plugin is not the cause |
| wrong artifact | Paper 1.21.11-132 | 21.0.12 | mc120 | Startup check logged "This CrateReloaded jar is the mc120 build (Minecraft 1.20.1 - 1.20.6), but the server runs 1.21.11. Install the mc121 build …" and disabled the plugin, as designed |

Log output found in every case and still to be dealt with:
- `Invalid input provided: HOLOGRAPHICDISPLAYS`: the deployed config sets `hologram-plugin: 'HolographicDisplays'`,
  and support for that plugin was removed in the fork. The plugin carries on without holograms. It needs a clear
  warning, or a migration to `DecentHolograms`.
- `Use of the three-letter time zone ID "PST" is deprecated`: from `timezone: 'PST'` in the config. A future JDK
  will remove these IDs; map them to region IDs such as `America/Los_Angeles`.
- `No key layers in MapLike[{}]` (ERROR, logged by the server itself before any plugin loads): caused by the test
  harness's `level-type=flat` with no generator settings, not by CrateReloaded.

## In-game bot tests (2026-11-24)

Harness: `../smoke/bot/scenario.js` with `mineflayer` 4.39.0 (pinned, installed with `--ignore-scripts`). The script
starts the server, places three chests, has an offline-mode bot register them with `/cr set` (opped only for that,
then deopped) and plays these scenarios against the deployed `crate.yml`. The servers are peaceful, offline-mode
and bound to `127.0.0.1`. Results are in each server folder's `bot-results.json`.

| # | Scenario | 1.20.4 (mc120) | 1.21.11 (mc121) | 26.1.2 (mc26) |
|---|---|---|---|---|
| 1 | Register crate blocks with `/cr set` | PASS | PASS | PASS |
| 2 | Open `FoodKey` (KEY, no animation): key consumed, rewards received | PASS | PASS | PASS |
| 3 | Open `FoodKeyT2` (ROULETTE) to the end: window opens and closes, reward paid | PASS | PASS | PASS |
| 4 | 7 shift-click / pick-up attempts during the `FoodKeyT3` (CSGO) animation take nothing (#475/#573) | PASS | PASS | PASS |
| 5 | …and CSGO then pays exactly one reward | PASS | PASS | PASS |
| 6 | Quit 1.5 s into ROULETTE, rejoin 1.5 s later: exactly **one** claim, **no** second payout (#540/#496) | PASS | PASS | PASS |
| 7 | Open `FoodPack` (MYSTERY) from the hand | PASS | PASS | PASS |
| 8 | `/cr give all FoodKey 1 offline` goes to claims, not the inventory (#445/#524) | PASS | PASS | PASS |

There were no CrateReloaded errors and no `com.hazebyte` stack frames in any server log.

## Known gaps before a version can be marked fully Supported

1. **Coverage of the bot tests:** one configuration (your `crate.yml`), Paper only, no Vault, PlaceholderAPI,
   holograms or Citizens, and no SUPPLY crate (`FoodChest`). Redeeming a claim through the `/cr claim` GUI is
   not yet scripted: claims are checked on disk.
2. ~~`item-nbt-api` NMS reflection (#599)~~: library removed; no NMS or reflection-on-internals dependency remains in the jar.
3. **Spigot** (non-Paper) runtime is untested for every family.
4. **1.20.x** has no automated server-mock tests.
