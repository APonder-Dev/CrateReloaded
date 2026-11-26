#!/usr/bin/env bash
# Builds every CrateReloaded build family and collects the jars in dist/.
#
#   ./build.sh                 all families (mc26, mc121, mc120)
#   ./build.sh mc26 mc121      only the named families
#   OFFLINE=1 ./build.sh       pass -o to Maven
#
# Each family needs its own JDK. Set JAVA25_HOME / JAVA21_HOME / JAVA17_HOME, or let the script look in the
# usual install folders. Per family it runs:
#   mc26   tests at the floor API (26.3 build 249) + a compile check against 26.3 build 261
#   mc121  tests at the ceiling API (1.21.11), then the shipped jar at the floor API (1.21.1)
#   mc120  the shipped jar at the floor API (1.20.1); no MockBukkit 4.x exists for 1.20, so the bukkit
#          test suite is skipped there (api/common tests still run)
# and finally scripts/check-bytecode.py on the jar.
set -euo pipefail
cd "$(dirname "$0")"

MVN=(mvn -B -ntp)
[[ "${OFFLINE:-0}" == "1" ]] && MVN+=(-o)
VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
mkdir -p dist

find_jdk() { # $1 = feature release
  local var="JAVA$1_HOME" dir
  if [[ -n "${!var:-}" ]]; then echo "${!var}"; return; fi
  for dir in "/c/Program Files/Java/jdk-$1"* "/c/Program Files/Eclipse Adoptium/jdk-$1"* \
             "/usr/lib/jvm/java-$1"* "/usr/lib/jvm/temurin-$1"* "/Library/Java/JavaVirtualMachines/"*"-$1"*"/Contents/Home"; do
    [[ -x "$dir/bin/java" || -x "$dir/bin/java.exe" ]] && { echo "$dir"; return; }
  done
  echo "No JDK $1 found: set $var" >&2; return 1
}

build_family() {
  local family=$1 release floor ceiling extra
  case $family in
    mc26)  release=25 ;;
    mc121) release=21; ceiling=1.21.11-R0.1-SNAPSHOT ;;
    mc120) release=17 ;;
    *) echo "Unknown family: $family" >&2; return 1 ;;
  esac
  export JAVA_HOME; JAVA_HOME=$(find_jdk "$release")
  echo "=== $family (JDK $release: $JAVA_HOME)"
  case $family in
    mc26)
      "${MVN[@]}" -P"$family" clean compile -pl bukkit -am -Dpaper.api.version=26.3.build.261-stable \
        -Deffectlib.paper.api.version=26.3.build.261-stable
      "${MVN[@]}" -P"$family" clean verify ;;
    mc121)
      # EffectLib must use the same ceiling API, or its enum-era Sound calls break on the 1.21.3+ interface.
      "${MVN[@]}" -P"$family" clean verify -Dpaper.api.version="$ceiling" -Deffectlib.paper.api.version="$ceiling"
      "${MVN[@]}" -P"$family" clean package -DskipTests ;;
    mc120)
      "${MVN[@]}" -P"$family" clean verify ;;
  esac
  local jar="bukkit/target/crate-bukkit-$VERSION-$family.jar"
  python3 scripts/check-bytecode.py "$jar" "$release" 2>/dev/null || python scripts/check-bytecode.py "$jar" "$release"
  cp "$jar" dist/
}

families=("$@")
[[ ${#families[@]} -eq 0 ]] && families=(mc26 mc121 mc120)
for f in "${families[@]}"; do build_family "$f"; done
echo "=== Done:" dist/*.jar
