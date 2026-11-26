# Releasing

A release is only made from a commit where **every CI job passed**: Lint, Tests (mc26, mc121, mc120) and
Build (mc26, mc121, mc120).

1. Update `CHANGELOG.md` and the version in the POMs.
2. Push and wait for CI on that exact commit:
   `gh run list --branch modernization --limit 1` must show `completed success`.
3. Use the jars from that run's artifacts (`CrateReloaded-mc26`, `-mc121`, `-mc120`), or rebuild locally from the
   same commit with the commands in the README. Each must pass `scripts/check-bytecode.py`.
4. Tag that commit `v<version>` and create the GitHub release with the three
   `CrateReloaded-<version>-<family>.jar` files.

If CI fails after a release has been published, fix it on a new commit before the next release.
