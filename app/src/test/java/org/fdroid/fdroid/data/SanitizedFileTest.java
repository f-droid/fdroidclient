package org.fdroid.fdroid.data;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class SanitizedFileTest {

    @Test
    public void testSanitizedFile() {
        assumeTrue("/".equals(System.getProperty("file.separator")));

        File directory = new File("/tmp/blah");

        String safeFile = "safe";
        String nonEvilFile = "$%^safe-and_bleh.boo*@~";
        String evilFile = ";rm /etc/shadow;";

        File safeNotSanitized = new File(directory, safeFile);
        File nonEvilNotSanitized = new File(directory, nonEvilFile);
        File evilNotSanitized = new File(directory, evilFile);

        assertEquals("/tmp/blah/safe", safeNotSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/$%^safe-and_bleh.boo*@~", nonEvilNotSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/;rm /etc/shadow;", evilNotSanitized.getAbsolutePath());

        assertEquals("safe", safeNotSanitized.getName());
        assertEquals("$%^safe-and_bleh.boo*@~", nonEvilNotSanitized.getName());
        assertEquals("shadow;", evilNotSanitized.getName());

        SanitizedFile safeSanitized = new SanitizedFile(directory, safeFile);
        SanitizedFile nonEvilSanitized = new SanitizedFile(directory, nonEvilFile);
        SanitizedFile evilSanitized = new SanitizedFile(directory, evilFile);

        assertEquals("/tmp/blah/safe", safeSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/safe-and_bleh.boo", nonEvilSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/rm etcshadow", evilSanitized.getAbsolutePath());

        assertEquals("safe", safeSanitized.getName());
        assertEquals("safe-and_bleh.boo", nonEvilSanitized.getName());
        assertEquals("rm etcshadow", evilSanitized.getName());

    }

}
