package com.hazebyte.crate.cratereloaded.compat;

import com.hazebyte.crate.api.ServerVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Which build family this jar was built for (mc120 / mc121 / mc26), read from {@code crate-build.properties},
 * and whether the running server belongs to it.
 *
 * <p>Each family is compiled against the lowest Paper API it claims and for that line's Java release. A
 * jar from another family can load and then fail later on a missing method or renamed constant, so the
 * plugin checks this at startup and names the artifact the server needs.
 */
public final class BuildFamily {

    /** JVM flag that lets the plugin start on a version line it was not built for. */
    public static final String ALLOW_UNSUPPORTED_PROPERTY = "cratereloaded.allowUnsupportedVersion";

    private final String family;
    private final ServerVersion min;
    private final ServerVersion max;

    BuildFamily(String family, ServerVersion min, ServerVersion max) {
        this.family = family;
        this.min = min;
        this.max = max;
    }

    public static BuildFamily load() {
        Properties properties = new Properties();
        try (InputStream in = BuildFamily.class.getClassLoader().getResourceAsStream("crate-build.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
            // Fall through to "unknown".
        }
        String family = properties.getProperty("family", "unknown");
        String min = properties.getProperty("minecraft.min", "");
        String max = properties.getProperty("minecraft.max", "");
        if (min.isEmpty() || max.isEmpty() || min.startsWith("${")) {
            return new BuildFamily(family, null, null);
        }
        return new BuildFamily(family, ServerVersion.of(min), ServerVersion.of(max));
    }

    public String getFamily() {
        return family;
    }

    /**
     * The build family a server version needs, or {@code null} if no family covers it.
     *
     * @param version the server version
     * @return {@code mc120}, {@code mc121}, {@code mc26} or {@code null}
     */
    public static String familyFor(ServerVersion version) {
        if (version.getMajor() == 1 && version.getMinor() == 20) {
            return "mc120";
        }
        if (version.getMajor() == 1 && version.getMinor() == 21) {
            return "mc121";
        }
        if (version.getMajor() == 26) {
            return "mc26";
        }
        return null;
    }

    /** Result of comparing the running server with this jar's family. */
    public enum Verdict {
        /** Inside the family's tested range. */
        SUPPORTED,
        /** Same version line, outside the range this release was tested on. */
        UNTESTED,
        /** A different version line: the wrong jar. */
        WRONG_ARTIFACT,
        /** No family information (e.g. an IDE build without resource filtering). */
        UNKNOWN
    }

    public Verdict check(ServerVersion server) {
        if (min == null || max == null) {
            return Verdict.UNKNOWN;
        }
        String required = familyFor(server);
        if (required == null || !required.equals(family)) {
            return Verdict.WRONG_ARTIFACT;
        }
        if (server.lt(min) || server.gt(max)) {
            return Verdict.UNTESTED;
        }
        return Verdict.SUPPORTED;
    }

    /**
     * Human-readable explanation for a verdict, for the startup log.
     *
     * @param server the server version
     * @param verdict the result of {@link #check(ServerVersion)}
     * @return the message, or {@code null} when nothing needs to be said
     */
    public String describe(ServerVersion server, Verdict verdict) {
        switch (verdict) {
            case WRONG_ARTIFACT:
                String required = familyFor(server);
                return String.format(
                        "This CrateReloaded jar is the %s build (Minecraft %s - %s), but the server runs %s. %s",
                        family,
                        format(min),
                        format(max),
                        format(server),
                        required == null
                                ? "No CrateReloaded build supports this version."
                                : "Install the " + required + " build (CrateReloaded-<version>-" + required + ".jar).");
            case UNTESTED:
                return String.format(
                        "Minecraft %s is outside the range this %s build was tested on (%s - %s).",
                        format(server), family, format(min), format(max));
            default:
                return null;
        }
    }

    /** {@code 1.20.1} rather than {@link ServerVersion#toString()}'s {@code 1_20_R1}. */
    static String format(ServerVersion version) {
        return version.getRevision() == 0
                ? version.getMajor() + "." + version.getMinor()
                : version.getMajor() + "." + version.getMinor() + "." + version.getRevision();
    }
}
