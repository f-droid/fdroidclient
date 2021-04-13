package org.fdroid.fdroid.data;

import android.os.Build;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.TestUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.os.LocaleListCompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LocalVariableName")
public class LocaleSelectionTest {

    private static final String KEY = "summary";

    private static final String EN_US_NAME = "Checkey: info on local apps";
    private static final String EN_US_FEATURE_GRAPHIC = "en-US/featureGraphic.png";
    private static final String EN_US_PHONE_SCREENSHOT = "en-US/phoneScreenshots/First.png";
    private static final String EN_US_SEVEN_INCH_SCREENSHOT = "en-US/sevenInchScreenshots/checkey-tablet.png";
    private static final String FR_FR_NAME = "Checkey : infos applis locales";
    private static final String FR_CA_FEATURE_GRAPHIC = "fr-CA/featureGraphic.png";
    private static final String FR_FR_FEATURE_GRAPHIC = "fr-FR/featureGraphic.png";
    private static final String FR_FR_SEVEN_INCH_SCREENSHOT = "fr-FR/sevenInchScreenshots/checkey-tablet.png";

    @Before
    public final void setUp() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void localeSelection() throws Exception {

        App app = new App();
        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-US,de-DE");

        //no metadata present
        Map<String, Map<String, Object>> localized = new HashMap<>();
        app.setLocalized(localized);
        assertEquals("Unknown application", app.summary);

        HashMap<String, Object> en_US = new HashMap<>();
        en_US.put(KEY, "summary-en_US");
        HashMap<String, Object> en_GB = new HashMap<>();
        en_GB.put(KEY, "summary-en_GB");
        HashMap<String, Object> de = new HashMap<>();
        de.put(KEY, "summary-de");
        HashMap<String, Object> de_AT = new HashMap<>();
        de_AT.put(KEY, "summary-de_AT");
        HashMap<String, Object> de_DE = new HashMap<>();
        de_DE.put(KEY, "summary-de_DE");
        HashMap<String, Object> es_ES = new HashMap<>();
        es_ES.put(KEY, "summary-es_ES");
        HashMap<String, Object> fr_FR = new HashMap<>();
        fr_FR.put(KEY, "summary-fr_FR");
        HashMap<String, Object> it_IT = new HashMap<>();
        it_IT.put(KEY, "summary-it_IT");

        app.summary = "reset";
        localized.put("de-AT", de_AT);
        localized.put("de-DE", de_DE);
        localized.put("en-US", en_US);
        app.setLocalized(localized);
        // just select the matching en-US locale, nothing special here
        assertEquals(en_US.get(KEY), app.summary);

        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-SE,de-DE");
        app.setLocalized(localized);
        // Fall back to another en locale before de
        assertEquals(en_US.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("de-AT", de_AT);
        localized.put("de-DE", de_DE);
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        App.systemLocaleList = LocaleListCompat.forLanguageTags("de-AT,de-DE");
        app.setLocalized(localized);
        // full match against a non-default locale
        assertEquals(de_AT.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("de-AT", de_AT);
        localized.put("de", de);
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        App.systemLocaleList = LocaleListCompat.forLanguageTags("de-CH,en-US");
        app.setLocalized(localized);
        assertEquals(de.get(KEY), app.summary);

        app.summary = "reset";
        localized.clear();
        localized.put("en-GB", en_GB);
        localized.put("en-US", en_US);

        Locale.setDefault(new Locale("en", "AU"));
        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-AU");
        app.setLocalized(localized);
        assertEquals(en_US.get(KEY), app.summary);

        app.summary = "reset";
        App.systemLocaleList = LocaleListCompat.forLanguageTags("zh-Hant-TW,zh-Hans-CN");
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

        localized.clear();
        localized.put("en-US", en_US);
        localized.put("zh-CN", zh_CN);
        app.setLocalized(localized);
        assertEquals(zh_CN.get(KEY), app.summary);

        // https://developer.android.com/guide/topics/resources/multilingual-support#resource-resolution-examples
        App.systemLocaleList = LocaleListCompat.forLanguageTags("fr-CH");
        localized.clear();
        localized.put("en-US", en_US);
        localized.put("de-DE", de_DE);
        localized.put("es-ES", es_ES);
        localized.put("fr-FR", fr_FR);
        localized.put("it-IT", it_IT);
        app.setLocalized(localized);
        assertEquals(fr_FR.get(KEY), app.summary);

        // https://developer.android.com/guide/topics/resources/multilingual-support#t-2d-choice
        App.systemLocaleList = LocaleListCompat.forLanguageTags("fr-CH,it-CH");
        localized.clear();
        localized.put("en-US", en_US);
        localized.put("de-DE", de_DE);
        localized.put("es-ES", es_ES);
        localized.put("it-IT", it_IT);
        app.setLocalized(localized);
        assertEquals(it_IT.get(KEY), app.summary);
    }

    @Test
    public void testSetLocalized() throws IOException {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 24);

        File f = TestUtils.copyResourceToTempFile("localized.json");
        Map<String, Object> result = new ObjectMapper().readValue(
                FileUtils.readFileToString(f, (String) null), HashMap.class);
        List<Map<String, Object>> apps = (List<Map<String, Object>>) result.get("apps");
        Map<String, Map<String, Object>> localized = (Map<String, Map<String, Object>>) apps.get(0).get("localized");
        App app = new App();

        App.systemLocaleList = LocaleListCompat.create(Locale.US);
        app.setLocalized(localized);
        assertEquals(EN_US_NAME, app.name);
        assertEquals(EN_US_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(EN_US_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);

        // choose the language when there is an exact locale match
        App.systemLocaleList = LocaleListCompat.forLanguageTags("fr-FR");
        app.setLocalized(localized);
        assertEquals(FR_FR_NAME, app.name);
        assertEquals(FR_FR_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(FR_FR_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);

        // choose the language from a different country when the preferred country is not available,
        // while still choosing featureGraphic from exact match
        App.systemLocaleList = LocaleListCompat.create(Locale.CANADA_FRENCH);
        app.setLocalized(localized);
        assertEquals(FR_FR_NAME, app.name);
        assertEquals(FR_CA_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(FR_FR_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);

        // choose the third preferred language when first and second lack translations
        App.systemLocaleList = LocaleListCompat.forLanguageTags("bo-IN,sr-RS,fr-FR");
        app.setLocalized(localized);
        assertEquals(FR_FR_NAME, app.name);
        assertEquals(FR_FR_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(FR_FR_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);

        // choose first language from different country, rather than 2nd full lang/country match
        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-GB,fr-FR");
        app.setLocalized(localized);
        assertEquals(EN_US_NAME, app.name);
        assertEquals(EN_US_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(EN_US_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);

        // choose en_US when no match, and mark as not localized
        App.systemLocaleList = LocaleListCompat.forLanguageTags("bo-IN,sr-RS");
        app.setLocalized(localized);
        assertEquals(EN_US_NAME, app.name);
        assertEquals(EN_US_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(EN_US_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertFalse(app.isLocalized);

        // When English is the preferred language and the second language has no entries
        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-US,sr-RS");
        app.setLocalized(localized);
        assertEquals(EN_US_NAME, app.name);
        assertEquals(EN_US_FEATURE_GRAPHIC, app.featureGraphic);
        assertEquals(EN_US_PHONE_SCREENSHOT, app.phoneScreenshots[0]);
        assertEquals(EN_US_SEVEN_INCH_SCREENSHOT, app.sevenInchScreenshots[0]);
        assertTrue(app.isLocalized);
    }

    @Test
    public void testIsLocalized() {
        final String enSummary = "utility for getting information about the APKs that are installed on your device";
        HashMap<String, Object> en = new HashMap<>();
        en.put("summary", enSummary);

        final String esSummary = "utilidad para obtener información sobre los APKs instalados en su dispositivo";
        HashMap<String, Object> es = new HashMap<>();
        es.put("summary", esSummary);

        final String frSummary = "utilitaire pour obtenir des informations sur les APKs qui sont installés sur vot";
        HashMap<String, Object> fr = new HashMap<>();
        fr.put("summary", frSummary);

        final String nlSummary = "hulpprogramma voor het verkrijgen van informatie over de APK die zijn geïnstalle";
        HashMap<String, Object> nl = new HashMap<>();
        nl.put("summary", nlSummary);

        App app = new App();
        Map<String, Map<String, Object>> localized = new HashMap<>();
        localized.put("es", es);
        localized.put("fr", fr);

        App.systemLocaleList = LocaleListCompat.forLanguageTags("nl-NL");
        app.setLocalized(localized);
        assertFalse(app.isLocalized);

        localized.put("nl", nl);
        app.setLocalized(localized);
        assertTrue(app.isLocalized);
        assertEquals(nlSummary, app.summary);

        app = new App();
        localized.clear();
        localized.put("nl", nl);
        app.setLocalized(localized);
        assertTrue(app.isLocalized);

        app = new App();
        localized.clear();
        localized.put("en-US", en);
        app.setLocalized(localized);
        assertFalse(app.isLocalized);

        App.systemLocaleList = LocaleListCompat.forLanguageTags("en-US");
        app = new App();
        localized.clear();
        localized.put("en-US", en);
        app.setLocalized(localized);
        assertTrue(app.isLocalized);
    }
}
