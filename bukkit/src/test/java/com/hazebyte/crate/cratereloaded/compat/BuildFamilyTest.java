package com.hazebyte.crate.cratereloaded.compat;

import com.hazebyte.crate.api.ServerVersion;
import com.hazebyte.crate.cratereloaded.compat.BuildFamily.Verdict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Plain unit test (no MockBukkit): ServerVersion.of only returns real versions when no mock server is set. */
class BuildFamilyTest {

    private static ServerVersion v(String version) {
        return ServerVersion.of(version);
    }

    @Test
    void familyFor_mapsVersionLinesToArtifacts() {
        Assertions.assertEquals("mc120", BuildFamily.familyFor(v("1.20.1")));
        Assertions.assertEquals("mc120", BuildFamily.familyFor(v("1.20.6")));
        Assertions.assertEquals("mc121", BuildFamily.familyFor(v("1.21")));
        Assertions.assertEquals("mc121", BuildFamily.familyFor(v("1.21.11")));
        Assertions.assertEquals("mc26", BuildFamily.familyFor(v("26.1.2")));
        Assertions.assertEquals("mc26", BuildFamily.familyFor(v("26.3")));
        Assertions.assertNull(BuildFamily.familyFor(v("1.19.4")));
    }

    @Test
    void check_classifiesServerAgainstFamily() {
        BuildFamily mc120 = new BuildFamily("mc120", v("1.20.1"), v("1.20.6"));
        Assertions.assertEquals(Verdict.SUPPORTED, mc120.check(v("1.20.4")));
        Assertions.assertEquals(Verdict.WRONG_ARTIFACT, mc120.check(v("1.21.4")));
        Assertions.assertEquals(Verdict.WRONG_ARTIFACT, mc120.check(v("1.19.4")));

        BuildFamily mc26 = new BuildFamily("mc26", v("26.1"), v("26.3"));
        Assertions.assertEquals(Verdict.SUPPORTED, mc26.check(v("26.1.2")));
        Assertions.assertEquals(Verdict.UNTESTED, mc26.check(v("26.4")));
        Assertions.assertEquals(Verdict.WRONG_ARTIFACT, mc26.check(v("1.21.11")));

        Assertions.assertEquals(Verdict.UNKNOWN, new BuildFamily("unknown", null, null).check(v("1.21.4")));
    }

    @Test
    void describe_namesTheRequiredArtifact() {
        BuildFamily mc120 = new BuildFamily("mc120", v("1.20.1"), v("1.20.6"));
        String message = mc120.describe(v("1.21.4"), Verdict.WRONG_ARTIFACT);
        Assertions.assertTrue(message.contains("server runs 1.21.4"), message);
        Assertions.assertTrue(message.contains("Install the mc121 build"), message);
        Assertions.assertTrue(
                mc120.describe(v("1.19.4"), Verdict.WRONG_ARTIFACT).contains("No CrateReloaded build supports"));
        Assertions.assertNull(mc120.describe(v("1.20.4"), Verdict.SUPPORTED));
    }

    @Test
    void load_readsFilteredBuildProperties() {
        BuildFamily build = BuildFamily.load();
        Assertions.assertFalse(build.getFamily().startsWith("${"), "crate-build.properties was not filtered");
        Assertions.assertTrue(build.getFamily().startsWith("mc"), build.getFamily());
    }
}
