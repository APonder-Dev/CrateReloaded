# nms

Reserved for adapters that touch server internals (NMS). It is **currently empty**, and the plugin has no NMS
dependency: since 2.4.0 the last one, `item-nbt-api`, has been removed.

Version differences that the Bukkit/Paper API can express are handled in
`bukkit/src/main/java/com/hazebyte/crate/cratereloaded/compat` (`PotionCompat`, `BuildFamily`) or by
key-based lookups. Add code here only when no supported API exists. Keep it per-version, behind a capability
check, and never load it before the server version is known.
