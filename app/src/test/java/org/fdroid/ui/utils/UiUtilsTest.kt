package org.fdroid.ui.utils

import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class UiUtilsTest {

  @Test
  fun testAsRelativeTimeString() {
    assertEquals("5 min. ago", ago(MINUTES.toMillis(5)))
    assertEquals("3 hr. ago", ago(HOURS.toMillis(3)))
    assertEquals("Yesterday", ago(DAYS.toMillis(1)))
    assertEquals("2 days ago", ago(DAYS.toMillis(2)))
    assertEquals("1 week ago", ago(DAYS.toMillis(7)))
    assertEquals("2 weeks ago", ago(DAYS.toMillis(14)))
    assertEquals("1 month ago", ago(DAYS.toMillis(30)))
    assertEquals("12 months ago", ago(DAYS.toMillis(364)))
    assertEquals("1 year ago", ago(DAYS.toMillis(365)))
    assertEquals("3 years ago", ago(DAYS.toMillis(365 * 3)))
  }

  @Test
  @Config(qualifiers = "de")
  fun testAsRelativeTimeStringGerman() {
    assertEquals("Vor 5 Min.", ago(MINUTES.toMillis(5)))
    assertEquals("Vor 3 Std.", ago(HOURS.toMillis(3)))
    assertEquals("Gestern", ago(DAYS.toMillis(1)))
    assertEquals("Vorgestern", ago(DAYS.toMillis(2)))
    assertEquals("Vor 3 Tagen", ago(DAYS.toMillis(3)))
    assertEquals("Vor 1 Woche", ago(DAYS.toMillis(7)))
    assertEquals("Vor 2 Wochen", ago(DAYS.toMillis(14)))
    assertEquals("vor 1 Monat", ago(DAYS.toMillis(30)))
    assertEquals("vor 12 Monaten", ago(DAYS.toMillis(364)))
    assertEquals("vor 1 Jahr", ago(DAYS.toMillis(365)))
    assertEquals("vor 3 Jahren", ago(DAYS.toMillis(365 * 3)))
  }

  private fun ago(millis: Long) = (System.currentTimeMillis() - millis).asRelativeTimeString()
}
