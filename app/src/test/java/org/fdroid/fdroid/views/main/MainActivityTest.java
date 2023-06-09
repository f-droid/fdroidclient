package org.fdroid.fdroid.views.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class MainActivityTest {

    @Test
    public void testSanitizeSearchTerms() {
        for (String valid : new String[]{"private browser", "πÇÇ", "现代 通用字", "български", "عربي"}) {
            assertEquals(valid, MainActivity.sanitizeSearchTerms(valid));
        }
        for (String invalid : new String[]{
                "Robert'); DROP TABLE Students; --",
                "xxx') OR 1 = 1 -- ]",
                "105 OR 1=1;",
                "\" OR \"=\"",
        }) {
            assertNotEquals(invalid, MainActivity.sanitizeSearchTerms(invalid));
        }
    }

}
