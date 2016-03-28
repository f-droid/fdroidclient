package org.fdroid.fdroid;

import android.test.AndroidTestCase;

import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;

public class SanitizedFileTest extends AndroidTestCase {

    public void testSanitizedFile() {

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
        assertEquals("shadow;", evilNotSanitized.getName()); // Should be ;rm /etc/shadow; but the forward slashes are naughty.

        SanitizedFile safeSanitized = new SanitizedFile(directory, safeFile);
        SanitizedFile nonEvilSanitized = new SanitizedFile(directory, nonEvilFile);
        SanitizedFile evilSanitized = new SanitizedFile(directory, evilFile);

        assertEquals("/tmp/blah/safe", safeSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/safe-and_bleh.boo", nonEvilSanitized.getAbsolutePath());
        assertEquals("/tmp/blah/rmetcshadow", evilSanitized.getAbsolutePath());

        assertEquals("safe", safeSanitized.getName());
        assertEquals("safe-and_bleh.boo", nonEvilSanitized.getName());
        assertEquals("rmetcshadow", evilSanitized.getName());

    }

}
