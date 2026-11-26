# Changelog

All notable changes to CrateReloaded are documented here. Versions continue the existing release line.

## [2.4.0] — 2026-11-26

Three jars, one per Minecraft line: `CrateReloaded-2.4.0-mc26.jar` (26.1–26.3, Java 25),
`CrateReloaded-2.4.0-mc121.jar` (1.21–1.21.11, Java 21) and `CrateReloaded-2.4.0-mc120.jar` (1.20.1–1.20.6, Java 17).

### Build
- Jars are named `CrateReloaded-<version>-<family>.jar`. The jar now ships the MIT licence texts of the bundled
  EffectLib (`LICENSE_EFFECTLIB`) and CrateReloaded API (`LICENSE_CRATERELOADEDAPI`).
- Removed the binary-only, unlicensed `com.hazebyte:base:0.0.1` jar and the in-project `bukkit/repo` file
  repository. Its only use (`ItemBuilder` in `ConfigConstants`) now uses the in-tree, MIT-licensed
  `com.hazebyte.crate.api.util.ItemBuilder`, which applies the same `&`-code replacement. The build is now
  source-reproducible apart from a few SNAPSHOT dependency pins.

- Three build families from one source tree: `mc26` (Java 25, Paper 26.3, default), `mc121` (Java 21, compiled
  against 1.21.1) and `mc120` (Java 17, compiled against 1.20.1). Select one with `-P<family>`. Jars are named
  `CrateReloaded-<version>-<family>.jar`, and `plugin.yml` `api-version` comes from the profile. The compiler uses
  `release` instead of `source`/`target`.
- Paper API 26.1.2 → 26.3 (build 249, the newest build MockBukkit loads; the main code also compiles against build 261).
  MockBukkit `mockbukkit-v26.1.2` 4.113.3 → `mockbukkit-v26.3` 4.124.0.
- The vendored EffectLib core follows the build family. Its Compatibility backends now target Java 17.
- `-Dcratereloaded.debugRewards=true` (JVM flag) logs every reward paid out (constant flag, item count, commands),
  for support and testing.
- The 1.21 test run now compiles EffectLib against the same 1.21.11 API as the plugin. With 1.21.1 it
  crashed on any Sound effect in tests (`Sound` became an interface in 1.21.3). The shipped jars weren't affected.
- Added `scripts/check-bytecode.py`, which fails the build if any class in a jar, shaded libraries included, needs a newer Java than the family targets.

### Compatibility
- Enchantment and potion-effect aliases are resolved by `minecraft:` key instead of Bukkit constants, which were
  renamed in 1.20.5. The same code now works on 1.20 through 26.x.
- New `compat.PotionCompat`: potion base types resolve by name (`SWIFTNESS` or `SPEED`, and so on), and
  `setBasePotionType` is used where it exists, falling back to `PotionData` otherwise.
- New startup check `compat.BuildFamily`: a jar on the wrong Minecraft line logs the jar to install and disables
  itself (override `-Dcratereloaded.allowUnsupportedVersion=true`). A version outside the tested range logs a warning.
- EffectLib `FireworkEffect` spawns fireworks with `spawn(location, Firework.class)` instead of
  `EntityType.FIREWORK_ROCKET`, which does not exist before 1.20.5.

### Fixed
- **#540: rewards lost when a player leaves mid-animation.** Cause: since 1.16 the server closes the open
  inventory *before* `PlayerQuitEvent`. The quit handler only looked for the animation in the open inventory,
  so it never found it, and the animation then "delivered" its reward to a player who was already offline.
  V2 animations (`CSGO_V2`/`ROULETTE_V2`) did the same when the player went offline. Fix: the new
  `OpeningLedger` writes each animated opening's prize to claim storage as a pending claim **before** the
  animation starts, and settles it exactly once (compare-and-set):
  - normal end: the pending claim is deleted and the reward is delivered as before;
  - quit, kick or plugin disable: online players without claims get the prize directly; everyone else keeps
    it as a claim (`/crate claim`, announced on join);
  - server crash: the pending claim is already on disk.

  A pending claim cannot be redeemed while its animation is still running, so it can't be both claimed and
  won. The animation inventory is no longer reopened for a player who has left.
- **First start ran on built-in defaults.** Settings loaded `config.yml` before the bundled default was written, so a
  fresh install logged a "Configuration key … is not set" warning per key and ignored the shipped values (prefix,
  timezone, hologram plugin, claim settings…) until the first `/cr reload`. The default `config.yml` is now
  written first, only if it is missing. Found on a Paper 26.3 test server with 20 other plugins.
- **#494 / #397: `always:()` rewards were never given, on any crate.** The crate parser kept `always:()` lines in
  the random pool (flagged constant, but with `chance:(0)`, so never drawn), and the constant-reward list stayed
  empty. They are now split into the constant list when `crate.yml` loads. They are given on every instant,
  supply and mystery opening, and, as a second fix, on animated openings too: those used to pay only the reward
  the animation landed on. Animated `always:()` rewards are part of the pending claim, so an interrupted
  animation pays them too.
- **V2 reward permissions were inverted.** The legacy `permission:(node)` tag excludes players who have the node,
  and the mapper stores it as `exclusive-permissions`. The V2 prize rule, however, *required* those permissions
  and ignored `inclusive-permissions`. Now `exclusive-permissions` excludes a player who has any of them, and
  `inclusive-permissions` requires all of them. This only affected rewards drawn through the V2 animation API
  (`CSGO_V2`/`ROULETTE_V2` aren't selectable from `crate.yml`).
- **P1 batch.** #536: CSGO / CSGO_REVERSE now fill all seven visible slots, so no glass pane scrolls through
  the row. #586: `{player}` / `{player-name}` are now replaced in `item:` rewards (normal, V2 and supply delivery).
  #610: `{date}` in the claim menu is filled from the claim timestamp. #611: not reproducible with documented
  syntax; custom variables defined without braces now also match `{name}`, and longest keys are replaced first.
  `{month}` is 1–12 (was 0–11). #597: new optional `crate.private-sounds` (default false) plays crate sounds only
  to the opener; previously the target player was never applied to sound effects. #555-class: the preview menu
  title is read from the documented `crate.menu` (fallback `crate.name`), and `crate.hotbar` is in the bundled
  config. #604 (partial): a crate-item lookup that matched now returns the crate instead of `null`, and removing
  a crate no longer breaks other crates that share its material.
- **#599: "There was an error with NBT serialization" on every crate open.** It came from
  `ItemSerialization.appendNbtData`, which used `item-nbt-api` (NMS reflection) and broke on newer servers.
  The call was already disabled in this fork. The dead method and the **`item-nbt-api` dependency are now
  removed**, so the jar no longer ships a library that depends on server internals and would need checking on
  every Minecraft version.
- **#445 / #524: `/cr give all <crate> [n] offline` ignored `offline`.** The command had no online/offline
  argument, so keys went straight to inventories. It now takes `[online/offline]` like `/cr give to`.
  `offline` puts the crates in every **online** player's `/cr claim`, the meaning agreed in #445 (requires
  claims to be enabled, as for `give to … offline`). Also, a `CrateGiveEvent` cancelled for one player used to
  abort the give for everyone after them; it now skips only that player.
- **`hide:true` crashed on 26.x.** `ItemBuilder.hideAll` named `ItemFlag.HIDE_POTION_EFFECTS`, which 26.x
  removed. The API module compiles against an old Spigot API, so the build didn't catch it, and any reward or
  display with `hide:true` threw `NoSuchFieldError` when parsed. It now adds every `ItemFlag` the server
  knows. `hide:false` no longer hides everything. A compile of the `api` and `utils` sources against Paper
  26.3 and 1.20.1 found no other reference to an API member that has since been removed.
- **#600: keys in claims written by 2.3.8 could not be claimed.** 2.3.8 appended raw item NBT, which contains
  spaces, to serialised display items. The item parser split it on spaces and rejected the fragments
  (`Enchantment is invalid: [{Enchantments]`), so the reward came out `null`, and claiming threw a
  `NullPointerException`. The parser now ignores that appended NBT; it only repeats what the plain tokens
  already say. Single-token `{...}` NBT is unchanged. Claim reading is also hardened:
  - unparseable rewards are filtered out completely (`remove(null)` dropped only the first one), and
    `ClaimExecutor` skips `null` rewards when paying, logging and messaging;
  - a claim whose stored rewards all fail to parse is **kept on disk** with a warning; it used to be deleted
    as "empty", destroying the player's reward for good;
  - V1 → V2 migration no longer rewrites a claim if any of its rewards failed to parse (it used to drop them).
- **#601: "Unable to parse claim data … key is not found in config"** after `/cr claim all` or with an empty
  claim menu. Claims were being deleted on one common-pool thread while the menu re-read the same YAML on
  another, so a key could disappear between listing and parsing. All claim I/O now runs on one ordered thread
  (see #540), and a key removed since the listing is skipped quietly.
- **Duplication: #475 / #573, items taken out of crate GUIs.** The per-menu listeners cancelled clicks at
  LOW priority with `ignoreCancelled = true`. When another plugin had cancelled the click first, they skipped
  it, and if a later plugin un-cancelled it, the item moved. V2 GUIs, including the `CSGO_V2`/`ROULETTE_V2`
  animations, only cancelled clicks on button slots, so display items could be taken. No crate GUI handled
  `InventoryDragEvent`. The new `CrateGuiGuardListener` runs at HIGHEST, after all button handlers, and cancels
  every click and drag while a CrateReloaded GUI is open, whatever earlier plugins did. It then resends the
  player's inventory a tick later, which clears the "ghost item" a cancelled shift-click briefly showed (#573).
  The V2 button handler moved from HIGHEST to HIGH so it still runs first.
- **Duplication: #496, quit and rejoin mid-animation for two rewards.** Closed by the #540 `OpeningLedger`: the
  quit settles the opening (prize kept as a claim), and the animation's late ending pays nothing, even when
  the player has already rejoined. Covered by a quit-and-rejoin regression test.
- **#486: unstackable rewards were handed out as one stack** (e.g. 5 totems, shulker boxes or chestplates in
  a single slot). Item delivery split every reward into stacks of 64 regardless of the item. It now uses the
  item's own max stack size (1, 16, 64, or a custom `max_stack_size` on 1.20.5+) for:
  - inventory delivery (`ItemCalculatorUtil.putItemsIntoInventory`);
  - overflow drops (`ItemCalculatorUtil.dropNaturally`: an oversized dropped stack was picked up as one stack);
  - `PlayerUtil.giveItemOrDrop`.

  Items that `Inventory#addItem` could not place were also silently discarded; they are now returned as
  leftover and go to claims or the ground like any other overflow.
- With claims enabled, an interrupted legacy animation stored the **entire** pre-generated reward list
  (`numberOfPrizes + 1` rewards, about 20) as claims for one key. It now stores the single prize.
- Claim storage operations ran concurrently on the common ForkJoin pool against one unsynchronised YAML
  object, with no ordering between them. They now run in order on a single `CrateReloaded-claims` thread, which
  is flushed to disk on plugin disable.
- `PotionUtil.toItemStack` swapped the "extended" and "upgraded" flags, so a level II potion became a
  long-duration potion and vice versa.
- `ItemSerialization` wrote base-potion durations in ticks while the parser reads seconds, so a re-parsed
  potion lasted 20× longer than intended. It also threw a `NullPointerException` on potions with no effect
  (water, awkward, mundane).
- The `multishot` enchantment alias never resolved because of the misspelling `MULTSHOT`. `sweeping_edge` now
  also resolves on pre-1.20.5 servers, where its key is `sweeping`.
- `onDisable` no longer throws when startup stopped early.

### Documentation
- The bundled `crate.yml` now has an example for every animation: CSGO_REVERSE, RECTANGLE, RECTANGLE_REVERSE and
  HEART, CSGO with the RANDOM end animation, and an animated mystery crate (`FoodBox`). Existing installs only get
  them by copying the new crates in. All of them were tested in game on 1.20.4, 1.21.11 and 26.1.2.
- Added the wiki (installation, crates, rewards, item format, variables, effects, claims, compatibility, API).

### Included from the Fruit-Servers fork
- Java 25 / Paper API 26.1.2, MockBukkit 26.x + JUnit 6, EffectLib 11.x vendored, DecentHolograms replaces
  HolographicDisplays, ProGuard removed, 26.x version parsing, player-head textures via `PlayerProfile` (#612),
  concurrent-open guard and no key consumption on rejected opens.
