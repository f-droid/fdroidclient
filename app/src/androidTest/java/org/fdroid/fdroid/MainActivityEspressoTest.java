package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import android.view.View;
import org.fdroid.fdroid.views.BannerUpdatingRepos;
import org.fdroid.fdroid.views.main.MainActivity;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@Ignore
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {
    public static final String TAG = "MainActivityEspressoTest";

    /**
     * ARM emulators are too slow to run these tests in a useful way.  The sad
     * thing is that it would probably work if Android didn't put up the ANR
     * "Process system isn't responding" on boot each time.  There seems to be no
     * way to increase the ANR timeout.
     */
    @BeforeClass
    public static void classSetUp() {
        Log.i(TAG, "setUp " + isEmulator() + " " + Build.SUPPORTED_ABIS[0]);
        if (Build.SUPPORTED_ABIS[0].startsWith("arm") && isEmulator()) {
            Log.e(TAG, "SKIPPING TEST: ARM emulators are too slow to run these tests in a useful way");
            org.junit.Assume.assumeTrue(false);
            return;
        }

        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.MINUTES);
        IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.MINUTES);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        try {
            UiDevice.getInstance(instrumentation)
                    .executeShellCommand("pm grant "
                            + instrumentation.getTargetContext().getPackageName()
                            + " android.permission.SET_ANIMATION_SCALE");
        } catch (IOException e) {
            e.printStackTrace();
        }
        SystemAnimations.disableAll(InstrumentationRegistry.getTargetContext());

        // dismiss the ANR or any other system dialogs that might be there
        UiObject button = new UiObject(new UiSelector().text("Wait").enabled(true));
        try {
            button.click();
        } catch (UiObjectNotFoundException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
        new UiWatchers().registerAnrAndCrashWatchers();
    }

    @AfterClass
    public static void classTearDown() {
        SystemAnimations.enableAll(InstrumentationRegistry.getTargetContext());
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    /**
     * Placate {@link android.os.StrictMode}
     *
     * @see <a href="https://github.com/aosp-mirror/platform_frameworks_base/commit/6f3a38f3afd79ed6dddcef5c83cb442d6749e2ff"> Run finalizers before counting for StrictMode</a>
     */
    @After
    public void tearDown() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void bottomNavFlavorCheck() {
        onView(withText(R.string.updates)).check(matches(isDisplayed()));
        onView(withText(R.string.menu_settings)).check(matches(isDisplayed()));
        onView(withText("THIS SHOULD NOT SHOW UP ANYWHERE!!!")).check(doesNotExist());

        assertTrue(BuildConfig.FLAVOR.startsWith("full") || BuildConfig.FLAVOR.startsWith("basic"));

        if (BuildConfig.FLAVOR.startsWith("basic")) {
            onView(withText(R.string.main_menu__latest_apps)).check(matches(isDisplayed()));
            onView(withText(R.string.main_menu__categories)).check(doesNotExist());
            onView(withText(R.string.main_menu__swap_nearby)).check(doesNotExist());
        }

        if (BuildConfig.FLAVOR.startsWith("full")) {
            onView(withText(R.string.main_menu__latest_apps)).check(matches(isDisplayed()));
            onView(withText(R.string.main_menu__categories)).check(matches(isDisplayed()));
            onView(withText(R.string.main_menu__swap_nearby)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void showSettings() {
        ViewInteraction settingsBottonNavButton = onView(
                allOf(withText(R.string.menu_settings), isDisplayed()));
        settingsBottonNavButton.perform(click());
        onView(withText(R.string.preference_manage_installed_apps)).check(matches(isDisplayed()));
        if (BuildConfig.FLAVOR.startsWith("basic") && BuildConfig.APPLICATION_ID.endsWith(".debug")) {
            // TODO fix me by sorting out the flavor applicationId for debug builds in app/build.gradle
            Log.i(TAG, "Skipping the remainder of showSettings test because it just crashes on basic .debug builds");
            return;
        }
        ViewInteraction manageInstalledAppsButton = onView(
                allOf(withText(R.string.preference_manage_installed_apps), isDisplayed()));
        manageInstalledAppsButton.perform(click());
        onView(withText(R.string.installed_apps__activity_title)).check(matches(isDisplayed()));
    }

    @Test
    public void showUpdates() {
        ViewInteraction updatesBottonNavButton = onView(allOf(withText(R.string.updates), isDisplayed()));
        updatesBottonNavButton.perform(click());
        onView(withText(R.string.updates)).check(matches(isDisplayed()));
    }

    @Test
    public void startSwap() {
        if (!BuildConfig.FLAVOR.startsWith("full")) {
            return;
        }
        ViewInteraction nearbyBottonNavButton = onView(
                allOf(withText(R.string.main_menu__swap_nearby), isDisplayed()));
        nearbyBottonNavButton.perform(click());
        ViewInteraction findPeopleButton = onView(
                allOf(withId(R.id.button), withText(R.string.nearby_splash__find_people_button), isDisplayed()));
        findPeopleButton.perform(click());
        onView(withText(R.string.swap_send_fdroid)).check(matches(isDisplayed()));
    }

    @Test
    public void showCategories() {
        if (!BuildConfig.FLAVOR.startsWith("full")) {
            return;
        }
        onView(allOf(withText(R.string.menu_settings), isDisplayed())).perform(click());
        onView(allOf(withText(R.string.main_menu__categories), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.swipe_to_refresh), isDisplayed()))
                .perform(swipeDown())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeDown())
                .perform(swipeDown())
                .perform(swipeRight())
                .perform(swipeLeft())
                .perform(swipeLeft())
                .perform(swipeLeft())
                .perform(swipeLeft())
                .perform(click());
    }

    @Test
    public void showLatest() {
        if (!BuildConfig.FLAVOR.startsWith("full")) {
            return;
        }
        onView(Matchers.<View>instanceOf(BannerUpdatingRepos.class)).check(matches(not(isDisplayed())));
        onView(allOf(withText(R.string.menu_settings), isDisplayed())).perform(click());
        onView(allOf(withText(R.string.main_menu__latest_apps), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.swipe_to_refresh), isDisplayed()))
                .perform(swipeDown())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeUp())
                .perform(swipeDown())
                .perform(swipeUp())
                .perform(swipeDown())
                .perform(swipeDown())
                .perform(swipeDown())
                .perform(swipeDown())
                .perform(click());
    }

    @Test
    public void showSearch() {
        onView(allOf(withText(R.string.menu_settings), isDisplayed())).perform(click());
        onView(withId(R.id.fab_search)).check(doesNotExist());
        if (!BuildConfig.FLAVOR.startsWith("full")) {
            return;
        }
        onView(allOf(withText(R.string.main_menu__latest_apps), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.fab_search), isDisplayed())).perform(click());
        onView(withId(R.id.sort)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.search), isDisplayed()))
                .perform(click())
                .perform(typeText("test"));
        onView(allOf(withId(R.id.sort), isDisplayed())).perform(click());
    }
}