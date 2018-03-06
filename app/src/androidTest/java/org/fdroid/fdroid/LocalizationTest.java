package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs through all of the translated strings and tests them with the same format
 * values that the source strings expect.  This is to ensure that the formats in
 * the translations are correct in number and in type (e.g. {@code s} or {@code s}.
 * It reads the source formats and then builds {@code formats} to represent the
 * position and type of the formats.  Then it runs through all of the translations
 * with formats of the correct number and type.
 */
@RunWith(AndroidJUnit4.class)
public class LocalizationTest {
    public static final String TAG = "LocalizationTest";

    private final Pattern androidFormat = Pattern.compile("(%[a-z0-9]\\$?[a-z]?)");
    private final Locale[] locales = Locale.getAvailableLocales();
    private final HashSet<String> localeNames = new HashSet<>(locales.length);

    private AssetManager assets;
    private Configuration config;
    private Resources resources;

    @Before
    public void setUp() {
        for (Locale locale : Languages.LOCALES_TO_TEST) {
            localeNames.add(locale.toString());
        }
        for (Locale locale : locales) {
            localeNames.add(locale.toString());
        }

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        assets = context.getAssets();
        config = context.getResources().getConfiguration();
        config.locale = Locale.ENGLISH;
        // Resources() requires DisplayMetrics, but they are only needed for drawables
        resources = new Resources(assets, new DisplayMetrics(), config);
    }

    @Test
    public void testLoadAllPlural() throws IllegalAccessException {
        Field[] fields = R.plurals.class.getDeclaredFields();

        HashMap<String, String> haveFormats = new HashMap<>();
        for (Field field : fields) {
            //Log.i(TAG, field.getName());
            int resId = field.getInt(int.class);
            CharSequence string = resources.getQuantityText(resId, 4);
            //Log.i(TAG, field.getName() + ": '" + string + "'");
            Matcher matcher = androidFormat.matcher(string);
            int matches = 0;
            char[] formats = new char[5];
            while (matcher.find()) {
                String match = matcher.group(0);
                char formatType = match.charAt(match.length() - 1);
                switch (match.length()) {
                    case 2:
                        formats[matches] = formatType;
                        matches++;
                        break;
                    case 4:
                        formats[Integer.parseInt(match.substring(1, 2)) - 1] = formatType;
                        break;
                    case 5:
                        formats[Integer.parseInt(match.substring(1, 3)) - 1] = formatType;
                        break;
                    default:
                        throw new IllegalStateException(field.getName() + " has bad format: " + match);
                }
            }
            haveFormats.put(field.getName(), new String(formats).trim());
        }

        for (Locale locale : locales) {
            config.locale = locale;
            // Resources() requires DisplayMetrics, but they are only needed for drawables
            resources = new Resources(assets, new DisplayMetrics(), config);
            for (Field field : fields) {
                String formats = null;
                try {
                    int resId = field.getInt(int.class);
                    for (int quantity = 0; quantity < 567; quantity++) {
                        resources.getQuantityString(resId, quantity);
                    }

                    formats = haveFormats.get(field.getName());
                    switch (formats) {
                        case "d":
                            resources.getQuantityString(resId, 1, 1);
                            break;
                        case "s":
                            resources.getQuantityString(resId, 1, "ONE");
                            break;
                        case "ds":
                            resources.getQuantityString(resId, 2, 1, "TWO");
                            break;
                        default:
                            if (!TextUtils.isEmpty(formats)) {
                                throw new IllegalStateException("Pattern not included in tests: " + formats);
                            }
                    }
                } catch (IllegalFormatException | Resources.NotFoundException e) {
                    Log.i(TAG, locale + " " + field.getName());
                    throw new IllegalArgumentException("Bad '" + formats + "' format in " + locale + " "
                            + field.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Test
    public void testLoadAllStrings() throws IllegalAccessException {
        Field[] fields = R.string.class.getDeclaredFields();

        HashMap<String, String> haveFormats = new HashMap<>();
        for (Field field : fields) {
            String string = resources.getString(field.getInt(int.class));
            Matcher matcher = androidFormat.matcher(string);
            int matches = 0;
            char[] formats = new char[5];
            while (matcher.find()) {
                String match = matcher.group(0);
                char formatType = match.charAt(match.length() - 1);
                switch (match.length()) {
                    case 2:
                        formats[matches] = formatType;
                        matches++;
                        break;
                    case 4:
                        formats[Integer.parseInt(match.substring(1, 2)) - 1] = formatType;
                        break;
                    case 5:
                        formats[Integer.parseInt(match.substring(1, 3)) - 1] = formatType;
                        break;
                    default:
                        throw new IllegalStateException(field.getName() + " has bad format: " + match);
                }
            }
            haveFormats.put(field.getName(), new String(formats).trim());
        }

        for (Locale locale : locales) {
            config.locale = locale;
            // Resources() requires DisplayMetrics, but they are only needed for drawables
            resources = new Resources(assets, new DisplayMetrics(), config);
            for (Field field : fields) {
                int resId = field.getInt(int.class);
                resources.getString(resId);

                String formats = haveFormats.get(field.getName());
                try {
                    switch (formats) {
                        case "d":
                            resources.getString(resId, 1);
                            break;
                        case "dd":
                            resources.getString(resId, 1, 2);
                            break;
                        case "dds":
                            resources.getString(resId, 1, 2, "THREE");
                            break;
                        case "sds":
                            resources.getString(resId, "ONE", 2, "THREE");
                            break;
                        case "s":
                            resources.getString(resId, "ONE");
                            break;
                        case "ss":
                            resources.getString(resId, "ONE", "TWO");
                            break;
                        case "sss":
                            resources.getString(resId, "ONE", "TWO", "THREE");
                            break;
                        case "ssss":
                            resources.getString(resId, "ONE", "TWO", "THREE", "FOUR");
                            break;
                        case "ssd":
                            resources.getString(resId, "ONE", "TWO", 3);
                            break;
                        case "sssd":
                            resources.getString(resId, "ONE", "TWO", "THREE", 4);
                            break;
                        default:
                            if (!TextUtils.isEmpty(formats)) {
                                throw new IllegalStateException("Pattern not included in tests: " + formats);
                            }
                    }
                } catch (Exception e) {
                    Log.i(TAG, locale + " " + field.getName());
                    throw new IllegalArgumentException("Bad format in '" + locale + "' '" + field.getName() + "': "
                            + e.getMessage());
                }
            }
        }
    }
}
