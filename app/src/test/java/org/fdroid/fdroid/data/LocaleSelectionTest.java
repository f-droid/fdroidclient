package org.fdroid.fdroid.data;


import android.os.Build;
import android.os.LocaleList;
import org.fdroid.fdroid.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LocaleSelectionTest {

    private static final String KEY = "summary";

    @Test
    public void correctLocaleSelectionBeforeSDK24() throws Exception {
        TestUtils.setFinalStatic(Build.VERSION.class.getDeclaredField("SDK_INT"), 19);
        assertTrue(Build.VERSION.SDK_INT < 24);
        App app;

        Map<String, Map<String, Object>> localized = new HashMap<>();
        HashMap<String, Object> en_US = new HashMap<>();
        en_US.put(KEY, "summary-en_US");
        HashMap<String, Object> de_AT = new HashMap<>();
        de_AT.put(KEY, "summary-de_AT");
        HashMap<String, Object> de_DE = new HashMap<>();
        de_DE.put(KEY, "summary-de_DE");
        HashMap<String, Object> sv = new HashMap<>();
        sv.put(KEY, "summary-sv");
        HashMap<String, Object> sv_FI = new HashMap<>();
        sv_FI.put(KEY, "summary-sv_FI");

        localized.put("de-AT", de_AT);
        localized.put("de-DE", de_DE);
        localized.put("en-US", en_US);
        localized.put("sv", sv);
        localized.put("sv-FI", sv_FI);

        // Easy mode. en-US metadata with an en-US locale
        Locale.setDefault(new Locale("en", "US"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(en_US.get(KEY), app.summary);

        // Fall back to en-US locale, when we have a different en locale
        Locale.setDefault(new Locale("en", "UK"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(en_US.get(KEY), app.summary);

        // Fall back to language only
        Locale.setDefault(new Locale("en", "UK"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(en_US.get(KEY), app.summary);

        // select the correct one out of multiple language locales
        Locale.setDefault(new Locale("de", "DE"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(de_DE.get(KEY), app.summary);

        // Even when we have a non-exact matching locale, we should fall back to the same language
        Locale.setDefault(new Locale("de", "CH"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(de_AT.get(KEY), app.summary);

        // Test fallback to base lang with not exact matching locale
        Locale.setDefault(new Locale("sv", "SE"));
        app = new App();
        app.setLocalized(localized);
        assertEquals(sv.get(KEY), app.summary);
    }

    @Test
    public void correctLocaleSelectionFromSDK24() throws Exception {

        TestUtils.setFinalStatic(Build.VERSION.class.getDeclaredField("SDK_INT"), 29);
        assertTrue(Build.VERSION.SDK_INT >= 24);

        App app = spy(new App());
        LocaleList localeList = mock(LocaleList.class);

        // we mock both the getLocales call and the conversion to a language tag string.
        doReturn(localeList).when(app).getLocales();
        // Set both default locale as well as the locale list, because the algorithm uses both...
        Locale.setDefault(new Locale("en", "US"));
        when(localeList.toLanguageTags()).thenReturn("en-US,de-DE");

        //no metadata present
        Map<String, Map<String, Object>> localized = new HashMap<>();
        app.setLocalized(localized);
        assertEquals("Unknown application", app.summary);

        HashMap<String, Object> en_US = new HashMap<>();
        en_US.put(KEY, "summary-en_US");
        HashMap<String, Object> en_GB = new HashMap<>();
        en_GB.put(KEY, "summary-en_GB");
        HashMap<String, Object> de_AT = new HashMap<>();
        de_AT.put(KEY, "summary-de_AT");
        HashMap<String, Object> de_DE = new HashMap<>();
        de_DE.put(KEY, "summary-de_DE");

        app.summary = "reset";
        localized.put("de-AT", de_AT);
        localized.put("de-DE", de_DE);
        localized.put("en-US", en_US);
        app.setLocalized(localized);
        // just select the matching en-US locale, nothing special here
        assertEquals(en_US.get(KEY), app.summary);

        Locale.setDefault(new Locale("en", "SE"));
        when(localeList.toLanguageTags()).thenReturn("en-SE,de-DE");
        app.setLocalized(localized);
        // Fall back to another en locale before de
        assertEquals(en_US.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("de-AT", de_AT);
        localized.put("de-DE", de_DE);
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        Locale.setDefault(new Locale("de", "AT"));
        when(localeList.toLanguageTags()).thenReturn("de-AT,de-DE");
        app.setLocalized(localized);
        // full match against a non-default locale
        assertEquals(de_AT.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("de-AT", de_AT);
        localized.put("de", de_DE);
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        Locale.setDefault(new Locale("de", "CH"));
        when(localeList.toLanguageTags()).thenReturn("de-CH,en-US");
        app.setLocalized(localized);
        assertEquals(de_DE.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        Locale.setDefault(new Locale("en", "AU"));
        when(localeList.toLanguageTags()).thenReturn("en-AU");
        app.setLocalized(localized);
        assertEquals(en_US.get(KEY), app.summary);

        app.summary = "reset";
        Locale.setDefault(new Locale("zh", "TW", "#Hant"));
        when(localeList.toLanguageTags()).thenReturn("zh-Hant-TW,zh-Hans-CN");
        localized.clear();
        localized.put("en", en_GB);
        localized.put("en-US", en_US);
        app.setLocalized(localized);
        //No match at all, fall back to an english locale
        assertEquals(en_US.get(KEY), app.summary);

        app.summary = "reset";
        HashMap<String, Object> zh_TW = new HashMap<>();
        zh_TW.put(KEY, "summary-zh_TW");
        HashMap<String, Object> zh_CN = new HashMap<>();
        zh_CN.put(KEY, "summary-zh_CN");
        HashMap<String, Object> zh_HK = new HashMap<>();
        zh_HK.put(KEY, "summary-zh_HK");

        localized.clear();
        localized.put("en-US", en_US);
        localized.put("zh-CN", zh_CN);
        localized.put("zh-HK", zh_HK);
        localized.put("zh-TW", zh_TW);
        app.setLocalized(localized);
        assertEquals(zh_TW.get(KEY), app.summary);

        localized.clear();
        localized.put("en-US", en_US);
        localized.put("zh-CN", zh_CN);
        app.setLocalized(localized);
        assertEquals(zh_CN.get(KEY), app.summary);
    }
}
