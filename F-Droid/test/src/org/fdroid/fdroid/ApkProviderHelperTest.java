package org.fdroid.fdroid;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.mock.MockApk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ApkProviderHelperTest extends BaseApkProviderTest {

    public void testKnownApks() {

        for (int i = 0; i < 7; i ++)
            TestUtils.insertApk(this, "org.fdroid.fdroid", i);

        for (int i = 0; i < 9; i ++)
            TestUtils.insertApk(this, "org.example", i);

        for (int i = 0; i < 3; i ++)
            TestUtils.insertApk(this, "com.example", i);

        TestUtils.insertApk(this, "com.apk.thingo", 1);

        Apk[] known = {
            new MockApk("org.fdroid.fdroid", 1),
            new MockApk("org.fdroid.fdroid", 3),
            new MockApk("org.fdroid.fdroid", 5),

            new MockApk("com.example", 1),
            new MockApk("com.example", 2),
        };

        Apk[] unknown = {
            new MockApk("org.fdroid.fdroid", 7),
            new MockApk("org.fdroid.fdroid", 9),
            new MockApk("org.fdroid.fdroid", 11),
            new MockApk("org.fdroid.fdroid", 13),

            new MockApk("com.example", 3),
            new MockApk("com.example", 4),
            new MockApk("com.example", 5),

            new MockApk("info.example", 1),
            new MockApk("info.example", 2),
        };

        List<Apk> apksToCheck = new ArrayList<Apk>(known.length + unknown.length);
        Collections.addAll(apksToCheck, known);
        Collections.addAll(apksToCheck, unknown);

        String[] projection = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION_CODE
        };

        List<Apk> knownApks = ApkProvider.Helper.knownApks(getMockContext(), apksToCheck, projection);

        assertResultCount(known.length, knownApks);

        for (Apk knownApk : knownApks)
            assertContains(knownApks, knownApk);
    }

    public void testFindByApp() {

        for (int i = 0; i < 7; i ++)
            TestUtils.insertApk(this, "org.fdroid.fdroid", i);

        for (int i = 0; i < 9; i ++)
            TestUtils.insertApk(this, "org.example", i);

        for (int i = 0; i < 3; i ++)
            TestUtils.insertApk(this, "com.example", i);

        TestUtils.insertApk(this, "com.apk.thingo", 1);

        assertTotalApkCount(7 + 9 + 3 + 1);

        List<Apk> fdroidApks = ApkProvider.Helper.findByApp(getMockContext(), "org.fdroid.fdroid");
        assertResultCount(7, fdroidApks);
        assertBelongsToApp(fdroidApks, "org.fdroid.fdroid");

        List<Apk> exampleApks = ApkProvider.Helper.findByApp(getMockContext(), "org.example");
        assertResultCount(9, exampleApks);
        assertBelongsToApp(exampleApks, "org.example");

        List<Apk> exampleApks2 = ApkProvider.Helper.findByApp(getMockContext(), "com.example");
        assertResultCount(3, exampleApks2);
        assertBelongsToApp(exampleApks2, "com.example");

        List<Apk> thingoApks = ApkProvider.Helper.findByApp(getMockContext(), "com.apk.thingo");
        assertResultCount(1, thingoApks);
        assertBelongsToApp(thingoApks, "com.apk.thingo");
    }

    public void testUpdate() {

        Uri apkUri = TestUtils.insertApk(this, "com.example", 10);

        String[] allFields = ApkProvider.DataColumns.ALL;
        Cursor cursor = getMockContentResolver().query(apkUri, allFields, null, null, null);
        assertResultCount(1, cursor);

        cursor.moveToFirst();
        Apk apk = new Apk(cursor);

        assertEquals("com.example", apk.id);
        assertEquals(10, apk.vercode);

        assertNull(apk.features);
        assertNull(apk.added);
        assertNull(apk.hashType);

        apk.features = Utils.CommaSeparatedList.make("one,two,three");
        long dateTimestamp = System.currentTimeMillis();
        apk.added = new Date(dateTimestamp);
        apk.hashType = "i'm a hash type";

        ApkProvider.Helper.update(getMockContext(), apk);

        // Should not have inserted anything else, just updated the already existing apk.
        Cursor allCursor = getMockContentResolver().query(ApkProvider.getContentUri(), allFields, null, null, null);
        assertResultCount(1, allCursor);

        Cursor updatedCursor = getMockContentResolver().query(apkUri, allFields, null, null, null);
        assertResultCount(1, updatedCursor);

        updatedCursor.moveToFirst();
        Apk updatedApk = new Apk(updatedCursor);

        assertEquals("com.example", updatedApk.id);
        assertEquals(10, updatedApk.vercode);

        assertNotNull(updatedApk.features);
        assertNotNull(updatedApk.added);
        assertNotNull(updatedApk.hashType);

        assertEquals("one,two,three", updatedApk.features.toString());
        assertEquals(new Date(dateTimestamp).getYear(), updatedApk.added.getYear());
        assertEquals(new Date(dateTimestamp).getMonth(), updatedApk.added.getMonth());
        assertEquals(new Date(dateTimestamp).getDay(), updatedApk.added.getDay());
        assertEquals("i'm a hash type", updatedApk.hashType);
    }

    public void testFind() {

        // Insert some random apks either side of the "com.example", so that
        // the Helper.find() method doesn't stumble upon the app we are interested
        // in by shear dumb luck...
        for (int i = 0; i < 10; i ++)
            TestUtils.insertApk(this, "org.fdroid.apk." + i, i);

        ContentValues values = new ContentValues();
        values.put(ApkProvider.DataColumns.VERSION, "v1.1");
        values.put(ApkProvider.DataColumns.HASH, "xxxxyyyy");
        values.put(ApkProvider.DataColumns.HASH_TYPE, "a hash type");
        TestUtils.insertApk(this, "com.example", 11, values);

        // ...and a few more for good measure...
        for (int i = 15; i < 20; i ++)
            TestUtils.insertApk(this, "com.other.thing." + i, i);

        Apk apk = ApkProvider.Helper.find(getMockContext(), "com.example", 11);

        assertNotNull(apk);

        // The find() method populates ALL fields if you don't specify any,
        // so we expect to find each of the ones we inserted above...
        assertEquals("com.example", apk.id);
        assertEquals(11, apk.vercode);
        assertEquals("v1.1", apk.version);
        assertEquals("xxxxyyyy", apk.hash);
        assertEquals("a hash type", apk.hashType);

        String[] projection = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.HASH
        };

        Apk apkLessFields = ApkProvider.Helper.find(getMockContext(), "com.example", 11, projection);

        assertNotNull(apkLessFields);

        assertEquals("com.example", apkLessFields.id);
        assertEquals("xxxxyyyy", apkLessFields.hash);

        // Didn't ask for these fields, so should be their default values...
        assertNull(apkLessFields.hashType);
        assertNull(apkLessFields.version);
        assertEquals(0, apkLessFields.vercode);

        Apk notFound = ApkProvider.Helper.find(getMockContext(), "com.doesnt.exist", 1000);
        assertNull(notFound);
    }

}
