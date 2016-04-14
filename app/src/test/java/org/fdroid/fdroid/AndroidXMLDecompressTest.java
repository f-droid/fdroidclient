package org.fdroid.fdroid;

import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AndroidXMLDecompressTest {

    String[] testDirNames = {
            System.getProperty("user.dir") + "/src/test/assets",
            System.getProperty("user.dir") + "/build/outputs/apk",
            System.getenv("HOME") + "/fdroid/repo",
    };

    FilenameFilter apkFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.endsWith(".apk");
        }
    };

    @Test
    public void testParseVersionCode() throws IOException {
        for (File f : getFilesToTest()) {
            System.out.println("\n" + f);
            Map<String, Object> map = AndroidXMLDecompress.getManifestHeaderAttributes(f.getAbsolutePath());
            for (String key : map.keySet()) {
                System.out.println(key + "=\"" + map.get(key) + "\"");
            }
        }
    }

    private List<File> getFilesToTest() {
        ArrayList<File> apkFiles = new ArrayList<File>(5);
        for (String dirName : testDirNames) {
            System.out.println("looking in " + dirName);
            File dir = new File(dirName);
            File[] files = dir.listFiles(apkFilter);
            if (files != null) {
                apkFiles.addAll(Arrays.asList(files));
            }
        }
        return apkFiles;
    }
}
