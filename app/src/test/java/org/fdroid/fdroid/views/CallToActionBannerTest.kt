package org.fdroid.fdroid.views

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CallToActionBannerTest {

    @Test
    fun testCallToActionExpiry() {
        val notExpired = Calendar.getInstance().apply {
            set(2026, 1, 1)
        }.timeInMillis
        assertFalse(hasCallToActionExpired(now = notExpired))

        val expired = Calendar.getInstance().apply {
            set(2026, 11, 1)
        }.timeInMillis
        assertTrue(hasCallToActionExpired(now = expired))
    }

}
