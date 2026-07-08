package org.fdroid.fdroid.nearby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Build;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils6;
import org.fdroid.fdroid.FDroidApp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link SwapWorkflowActivity#isSwapUrl}, which is the security gate
 * that decides whether an incoming swap URL points at a repo on the local
 * subnet.  The host must be a numeric IP literal on our subnet; it must never
 * be resolved via DNS (see merge request !1682 discussion).  The two SDK
 * branches (API 29+ {@code InetAddresses.isNumericAddress} vs. the legacy
 * pattern fallback) are exercised separately.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P) // default: legacy pattern-match branch
public class SwapWorkflowActivityTest {

    private SubnetUtils.SubnetInfo savedSubnetInfo;

    @Before
    public void setUp() {
        savedSubnetInfo = FDroidApp.subnetInfo;
        // pretend the device sits on 192.168.0.0/24(ipv4) 2001:db8:abcd::/64(ipv6)
        FDroidApp.subnetInfo = new SubnetUtils("192.168.0.1/24").getInfo();
        FDroidApp.subnet6Info = new SubnetUtils6("2001:db8:abcd::/64").getInfo();
    }

    @After
    public void tearDown() {
        FDroidApp.subnetInfo = savedSubnetInfo;
    }

    // ------------------------------------------------------------------
    // legacy branch (API < 29, host.matches("[0-9.]+"))
    // ------------------------------------------------------------------

    @Test
    public void testValidSwapUrlLegacy() {
        assertTrue(SwapWorkflowActivity.isSwapUrl("192.168.0.50", 8888));
        assertTrue(SwapWorkflowActivity.isSwapUrl("2001:db8:abcd::1234", 8888));
    }

    @Test
    public void testOutOfSubnetLegacy() {
        assertFalse(SwapWorkflowActivity.isSwapUrl("10.0.0.50", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("2010:db8:abcd::1234", 8888));
    }

    @Test
    public void testHostnameIsNeverResolvedLegacy() {
        // a hostname must be rejected outright, never resolved via DNS
        assertFalse(SwapWorkflowActivity.isSwapUrl("example.com", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("localhost", 8888));
    }

    // removed testIpv6IsRejectedLegacy because ipv6 addresses are now supported

    // ------------------------------------------------------------------
    // modern branch (API 29+, android.net.InetAddresses.isNumericAddress)
    // ------------------------------------------------------------------

    @Test
    @Config(sdk = 30)
    public void testValidSwapUrlModern() {
        assertTrue(SwapWorkflowActivity.isSwapUrl("192.168.0.50", 8888));
        assertTrue(SwapWorkflowActivity.isSwapUrl("2001:db8:abcd::1234", 8888));
    }

    @Test
    @Config(sdk = 30)
    public void testOutOfSubnetModern() {
        assertFalse(SwapWorkflowActivity.isSwapUrl("10.0.0.50", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("2010:db8:abcd::1234", 8888));
    }

    @Test
    @Config(sdk = 30)
    public void testHostnameIsNeverResolvedModern() {
        // a hostname must be rejected outright, never resolved via DNS
        // several of these are considered numeric due to a Robolectric issue, but fail
        // anyway due to an IllegalArgumentException that happens further in the code
        assertFalse(SwapWorkflowActivity.isSwapUrl("foo", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("1", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("example.com", 8888));
        assertFalse(SwapWorkflowActivity.isSwapUrl("localhost", 8888));
    }

    // removed testIpv6IsRejectedModern because ipv6 addresses are now supported

    // ------------------------------------------------------------------
    // port and null-safety (independent of the numeric-address branch)
    // ------------------------------------------------------------------

    @Test
    public void testPrivilegedPortIsRejected() {
        assertFalse(SwapWorkflowActivity.isSwapUrl("192.168.0.50", 80));
        assertFalse(SwapWorkflowActivity.isSwapUrl("192.168.0.50", 1023));
    }

    @Test
    public void testNullHostIsRejected() {
        assertFalse(SwapWorkflowActivity.isSwapUrl((String) null, 8888));
    }

    @Test
    public void testNullUriIsRejected() {
        // regression: checkIfNewRepoOnSameWifi() may pass a null repo Uri
        assertFalse(SwapWorkflowActivity.isSwapUrl((Uri) null));
    }

    @Test
    public void testUriWithoutHostIsRejected() {
        assertFalse(SwapWorkflowActivity.isSwapUrl(Uri.parse("mailto:swap@example.com")));
    }

    @Test
    public void testValidSwapUri() {
        assertTrue(SwapWorkflowActivity.isSwapUrl(
                Uri.parse("http://192.168.0.50:8888/fdroid/repo")));
    }
}
