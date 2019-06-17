package org.fdroid.fdroid.nearby;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class WifiStateChangeServiceTest {

    @Test
    public void testFormatIpAddress() throws UnknownHostException {
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 98273) {
            String ip = WifiStateChangeService.formatIpAddress((int) i);
            InetAddress.getByName(ip);
        }
        InetAddress.getByName(WifiStateChangeService.formatIpAddress(Integer.MAX_VALUE));
        InetAddress.getByName(WifiStateChangeService.formatIpAddress(Integer.MIN_VALUE));
        InetAddress.getByName(WifiStateChangeService.formatIpAddress(0));
    }
}
