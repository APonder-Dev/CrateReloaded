#!/usr/bin/env python3
"""Fail if any class in a plugin jar needs a newer Java than the build family targets.

Usage: check-bytecode.py <jar> <max-java-release>

Shaded libraries keep their own class-file versions, so compiling our code with --release 17 does not
by itself make the jar loadable on Java 17. META-INF/versions/* entries (multi-release jars) are
ignored: the JVM only loads them on newer runtimes.
"""
import struct
import sys
import zipfile
from collections import Counter


def main() -> int:
    jar, max_release = sys.argv[1], int(sys.argv[2])
    max_major = max_release + 44
    too_new = []
    majors = Counter()
    with zipfile.ZipFile(jar) as zf:
        for name in zf.namelist():
            if not name.endswith(".class") or name.startswith("META-INF/versions/"):
                continue
            head = zf.read(name)[:8]
            if head[:4] != b"\xca\xfe\xba\xbe":
                continue
            major = struct.unpack(">H", head[6:8])[0]
            majors[major] += 1
            if major > max_major:
                too_new.append((name, major - 44))
    summary = ", ".join(f"Java {m - 44}: {c}" for m, c in sorted(majors.items()))
    print(f"{jar}: {summary}")
    if too_new:
        print(f"FAIL: {len(too_new)} classes need a Java newer than {max_release}, e.g.:")
        for name, release in too_new[:10]:
            print(f"  {name} (Java {release})")
        return 1
    print(f"OK: every class loads on Java {max_release}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
