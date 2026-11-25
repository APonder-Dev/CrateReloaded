# CrateReloaded API

The public API for the CrateReloaded plugin: crates, rewards, claims, effects and events (`CrateGiveEvent`,
`CrateRewardEvent`, `ClaimGiveEvent` and others), reached through `CrateAPI`. Licensed MIT (© 2019 Hazebyte), see
[LICENSE](LICENSE).

- Plugin and build instructions: [../README.md](../README.md)
- Javadoc for the original API: [hazebyte.github.io/CrateReloadedAPI](https://hazebyte.github.io/CrateReloadedAPI/)

## Using it in your plugin

The old Hazebyte Maven repository (`m2.hazebyte.com`) is offline, and this fork does not publish to a public
repository yet. Install the API into your local Maven repository from this source tree:

```bash
mvn -pl api install
```

Then depend on it with `provided` scope; the CrateReloaded plugin supplies the classes at runtime:

```xml
<dependency>
    <groupId>com.hazebyte</groupId>
    <artifactId>CrateReloadedAPI</artifactId>
    <version>2.2.1</version>
    <scope>provided</scope>
</dependency>
```

Add `softdepend: [CrateReloaded]` (or `depend`) to your `plugin.yml`.

```java
Crate crate = CrateAPI.getCrateRegistrar().getCrate("FoodKey");
```

## Notes for 2.4.0

- The API is compiled for Java 8 bytecode, against an older Spigot API, so it works with every build family.
  Code that runs inside the plugin is now checked against the 1.20.1 and 26.3 APIs. `ItemBuilder.hideAll` was the
  one method that referenced a member 26.x removed. It is fixed, and `hideAll(false)` no longer hides flags.
- Behaviour that API users may notice:
  - with `/cr give all … offline`, `CrateGiveEvent` fires once per player, and cancelling it for one player
    no longer skips the rest;
  - an interrupted animation fires `ClaimGiveEvent` once, for the single prize.
