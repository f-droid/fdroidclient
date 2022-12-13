package cc.mvdan.accesspoint;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class WifiApControlTest {

    @Test
    public void testMacAddressToByteArrayRandomCrap() {
        assertArrayEquals(
                new byte[]{0, 0, 0, 0, 0, 0},
                WifiApControl.macAddressToByteArray("nothingH-x"));
    }

    @Test
    public void testMacAddressToByteArrayDots() {
        assertArrayEquals(
                new byte[]{(byte) 0x20, (byte) 0x82, (byte) 0xc0, (byte) 0xff, (byte) 0x33, (byte) 0xe9},
                WifiApControl.macAddressToByteArray("20.82.c0.ff.33.e9"));
    }

    @Test
    public void testMacAddressToByteArrayColons() {
        assertArrayEquals(
                new byte[]{(byte) 0x20, (byte) 0x82, (byte) 0xc0, (byte) 0xff, (byte) 0x33, (byte) 0xe9},
                WifiApControl.macAddressToByteArray("20:82:c0:ff:33:e9"));
    }

    @Test
    public void testMacAddressToByteArraySpaces() {
        assertArrayEquals(
                new byte[]{(byte) 0x20, (byte) 0x82, (byte) 0xc0, (byte) 0xff, (byte) 0x33, (byte) 0xe9},
                WifiApControl.macAddressToByteArray("20 82 c0 ff 33 e9"));
    }

    @Test
    public void testMacAddressToByteArrayDashes() {
        assertArrayEquals(
                new byte[]{(byte) 0x20, (byte) 0x82, (byte) 0xc0, (byte) 0xff, (byte) 0x33, (byte) 0xe9},
                WifiApControl.macAddressToByteArray("20-82-c0-ff-33-e9"));
    }

    @Test
    public void testMacAddressToByteArrayWhiteSpaceCruft() {
        assertArrayEquals(
                new byte[]{(byte) 0x20, (byte) 0x82, (byte) 0xc0, (byte) 0xff, (byte) 0x33, (byte) 0xe9},
                WifiApControl.macAddressToByteArray(" 20:82:c0:ff:33:e9\t"));
    }
}
