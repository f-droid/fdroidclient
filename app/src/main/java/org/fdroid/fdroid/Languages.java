package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class Languages {
    public static final String TAG = "Languages";

    public static final String USE_SYSTEM_DEFAULT = "";

    private static final Locale DEFAULT_LOCALE;
    private static final Locale TIBETAN = new Locale("bo");
    private static final Locale CHINESE_HONG_KONG = new Locale("zh", "HK");

    private static Locale locale;
    private static Languages singleton;
    private static Map<String, String> tmpMap = new TreeMap<>();
    private static Map<String, String> nameMap;

    static {
        DEFAULT_LOCALE = Locale.getDefault();
    }

    private Languages(Activity activity) {
        Set<Locale> localeSet = new LinkedHashSet<>();
        localeSet.addAll(Arrays.asList(LOCALES_TO_TEST));

        for (Locale locale : localeSet) {
            if (locale.equals(TIBETAN)) {
                // include English name for devices without Tibetan font support
                tmpMap.put(TIBETAN.getLanguage(), "Tibetan བོད་སྐད།"); // Tibetan
            } else if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
                tmpMap.put(Locale.SIMPLIFIED_CHINESE.toString(), "中文 (中国)"); // Chinese (China)
            } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
                tmpMap.put(Locale.TRADITIONAL_CHINESE.toString(), "中文 (台灣)"); // Chinese (Taiwan)
            } else if (locale.equals(CHINESE_HONG_KONG)) {
                tmpMap.put(CHINESE_HONG_KONG.toString(), "中文 (香港)"); // Chinese (Hong Kong)
            } else {
                tmpMap.put(locale.getLanguage(), capitalize(locale.getDisplayLanguage(locale)));
            }
        }

        // remove the current system language from the menu
        tmpMap.remove(Locale.getDefault().getLanguage());

        /* SYSTEM_DEFAULT is a fake one for displaying in a chooser menu. */
        tmpMap.put(USE_SYSTEM_DEFAULT, activity.getString(R.string.pref_language_default));
        nameMap = Collections.unmodifiableMap(tmpMap);
    }

    /**
     * @param activity the {@link Activity} this is working as part of
     * @return the singleton to work with
     */
    public static Languages get(Activity activity) {
        if (singleton == null) {
            singleton = new Languages(activity);
        }
        return singleton;
    }

    /**
     * Handles setting the language if it is different than the current language,
     * or different than the current system-wide locale.  The preference is cleared
     * if the language matches the system-wide locale or "System Default" is chosen.
     */
    @TargetApi(17)
    public static void setLanguage(final ContextWrapper contextWrapper) {
        if (Build.VERSION.SDK_INT >= 24) {
            Utils.debugLog(TAG, "Languages.setLanguage() ignored on >= android-24");
            Preferences.get().clearLanguage();
            return;
        }
        String language = Preferences.get().getLanguage();
        if (TextUtils.equals(language, DEFAULT_LOCALE.getLanguage())) {
            Preferences.get().clearLanguage();
            locale = DEFAULT_LOCALE;
        } else if (locale != null && TextUtils.equals(locale.getLanguage(), language)) {
            return; // already configured
        } else if (language == null || language.equals(USE_SYSTEM_DEFAULT)) {
            Preferences.get().clearLanguage();
            locale = DEFAULT_LOCALE;
        } else {
            /* handle locales with the country in it, i.e. zh_CN, zh_TW, etc */
            String[] localeSplit = language.split("_");
            if (localeSplit.length > 1) {
                locale = new Locale(localeSplit[0], localeSplit[1]);
            } else {
                locale = new Locale(language);
            }
        }
        Locale.setDefault(locale);

        final Resources resources = contextWrapper.getBaseContext().getResources();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /**
     * Force reload the {@link Activity to make language changes take effect.}
     *
     * @param activity the {@code Activity} to force reload
     */
    public static void forceChangeLanguage(Activity activity) {
        if (Build.VERSION.SDK_INT >= 24) {
            Utils.debugLog(TAG, "Languages.forceChangeLanguage() ignored on >= android-24");
            return;
        }
        Intent intent = activity.getIntent();
        if (intent == null) { // when launched as LAUNCHER
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    /**
     * @return an array of the names of all the supported languages, sorted to
     * match what is returned by {@link Languages#getSupportedLocales()}.
     */
    public String[] getAllNames() {
        return nameMap.values().toArray(new String[nameMap.size()]);
    }

    /**
     * @return sorted list of supported locales.
     */
    public String[] getSupportedLocales() {
        Set<String> keys = nameMap.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    public static final Locale[] LOCALES_TO_TEST = {
            Locale.ENGLISH,
            Locale.FRENCH,
            Locale.GERMAN,
            Locale.ITALIAN,
            Locale.JAPANESE,
            Locale.KOREAN,
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            CHINESE_HONG_KONG,
            TIBETAN,
            new Locale("af"),
            new Locale("ar"),
            new Locale("be"),
            new Locale("bg"),
            new Locale("ca"),
            new Locale("cs"),
            new Locale("da"),
            new Locale("el"),
            new Locale("es"),
            new Locale("eo"),
            new Locale("et"),
            new Locale("eu"),
            new Locale("fa"),
            new Locale("fi"),
            new Locale("he"),
            new Locale("hi"),
            new Locale("hu"),
            new Locale("hy"),
            new Locale("id"),
            new Locale("is"),
            new Locale("it"),
            new Locale("ml"),
            new Locale("my"),
            new Locale("nb"),
            new Locale("nl"),
            new Locale("pl"),
            new Locale("pt"),
            new Locale("ro"),
            new Locale("ru"),
            new Locale("sc"),
            new Locale("sk"),
            new Locale("sn"),
            new Locale("sr"),
            new Locale("sv"),
            new Locale("th"),
            new Locale("tr"),
            new Locale("uk"),
            new Locale("vi"),
    };

}
