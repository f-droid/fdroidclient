package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.RepoTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Date;
import java.util.LinkedList;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;
import static org.fdroid.fdroid.Assert.assertCantDelete;
import static org.fdroid.fdroid.Assert.assertResultCount;
import static org.fdroid.fdroid.Assert.insertApp;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DBHelperTest {
	static final String TAG = "DBHelperTest";

    private List<String> parseXmlStringIntoRepos(String xml) throws IOException, XmlPullParserException {
		String reposXmlPath = "/data/repos.xml";

	    FileOutputStream outputStream = new FileOutputStream(reposXmlPath);
	    outputStream.write(xml.getBytes());
	    outputStream.close();

		// Now parse that xml file
		List<String> additionalRepos = DBHelper.parseXmlRepos(new File(reposXmlPath));
		return additionalRepos;
    }

    @Test
    public void parseXmlReposNegativeTest() {
    	String wrongXml;
    	boolean success = false;

    	// Try parsing invalid xml files
    	wrongXml = "</resources>";
    	try {
    		parseXmlStringIntoRepos(wrongXml);
    	} catch (IOException io) {
    		fail("IOException: " + io.getMessage());
    	} catch (XmlPullParserException xppe) {
    		// good
    	}

    	wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    	                        + "<resources>"
    	                            + "<string-array name=\"default_repos\">"
    	                                +"</item>"
    	                                +"<!-- address -->"
    	                                +"<item>https://www.oem0.com/yeah/repo</item>"
    	                                +"<!-- description -->"
    	                                +"<item>I'm the first oem repo.</item>"
    	                                +"<!-- version -->"
    	                                +"<item>22</item>"
    	                                +"<!-- enabled -->"
    	                                +"<item>1</item>"
    	                                +"<!-- push requests -->"
    	                                +"<item>ignore</item>"
    	                                +"<!-- pubkey -->"
    	                                +"<item>fffff2313aaaaabcccc111</item>"
    	                            +"</string-array>"
    	                        +"</resources>";
    	try {
    		parseXmlStringIntoRepos(wrongXml);
    		fail("Invalid xml read successfully --> Wrong");
    	} catch (IOException io) {
    		fail("IOException: " + io.getMessage());
    	} catch (XmlPullParserException xppe) {
    		// good
    	}

    	wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    	                        + "<resources>"
    	                            + "<string-array name=\"default_repos\">"
    	                                +"<!-- address -->"
    	                                +"<item><item>https://www.oem0.com/yeah/repo</item>"
    	                                +"<!-- description -->"
    	                                +"<item>I'm the first oem repo.</item>"
    	                                +"<!-- version -->"
    	                                +"<item>22</item>"
    	                                +"<!-- enabled -->"
    	                                +"<item>1</item>"
    	                                +"<!-- push requests -->"
    	                                +"<item>ignore</item>"
    	                                +"<!-- pubkey -->"
    	                                +"<item>fffff2313aaaaabcccc111</item>"
    	                            +"</string-array>"
    	                        +"</resources>";
    	try {
    		parseXmlStringIntoRepos(wrongXml);
    		fail("Invalid xml read successfully --> Wrong");
    	} catch (IOException io) {
    		fail("IOException: " + io.getMessage());
    	} catch (XmlPullParserException xppe) {
    		// good
    	}

    	wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    	                        + "<resources>"
    	                            + "<string-array name=\"default_repos\">"
    	                        	+"</resources><item></item>";
    	try {
    		parseXmlStringIntoRepos(wrongXml);
    		fail("Invalid xml read successfully --> Wrong");
    	} catch (IOException io) {
    		fail("IOException: " + io.getMessage());
    	} catch (XmlPullParserException xppe) {
    		// good
    	}

    	wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    				+"<item>https://www.oem0.com/yeah/repo</item>"
                    +"<!-- description -->"
                    +"<item>I'm the first oem repo.</item>"
                    +"<!-- version -->"
                    +"<item>22</item>"
                    +"<!-- enabled -->"
                    +"<item>1</item>"
                    +"<!-- push requests -->"
                    +"<item>ignore</item>"
                    +"<!-- pubkey -->"
                    +"<item>fffff2313aaaaabcccc111</item>"
                    +"</string-array>"
	                +"</resources>";
    	try {
    		parseXmlStringIntoRepos(wrongXml);
    		fail("Invalid xml read successfully --> Wrong");
    	} catch (IOException io) {
    		fail("IOException: " + io.getMessage());
    	} catch (XmlPullParserException xppe) {
    		// good
    	}

    	// Parse valid xml but make sure that only <item> tags are parsed
    	String validXml = "<root>"
    				+"<ite>HAHA</ite>"
                    +"<item>https://www.oem0.com/yeah/repo</item>"
                    +"<item>I'm the first oem repo.</item>"
                    +"<item>22</item>"
                    +"<item>1</item>"
                    +"<item>ignore</item>"
                    +"<item>fffff2313aaaaabcccc111</item>"
                    +"</root>";

        List<String> repos;
        try {
    		repos = parseXmlStringIntoRepos(validXml);
	    	assertEquals(repos.size(), 6);
	    	assertEquals(repos.get(0), "https://www.oem0.com/yeah/repo");
        } catch (IOException io) {
    		fail("IOException. Valid xml did not get parsed!");
        } catch (XmlPullParserException xppe) {
    		fail("XmlPullParserException. Valid xml did not get parsed!");
        }
    }

    @Test
    public void parseXmlReposPositiveTest() {
    	// Parse valid xml file
    	String reposXmlContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    	                        + "<resources>"
    	                            + "<string-array name=\"default_repos\">"
    	                                +"<!-- name -->"
    	                                +"<item>oem0Name</item>"
    	                                +"<!-- address -->"
    	                                +"<item>https://www.oem0.com/yeah/repo</item>"
    	                                +"<!-- description -->"
    	                                +"<item>I'm the first oem repo.</item>"
    	                                +"<!-- version -->"
    	                                +"<item>22</item>"
    	                                +"<!-- enabled -->"
    	                                +"<item>1</item>"
    	                                +"<!-- push requests -->"
    	                                +"<item>ignore</item>"
    	                                +"<!-- pubkey -->"
    	                                +"<item>fffff2313aaaaabcccc111</item>"

    	                                +"<!-- name -->"
    	                                +"<item>oem1MyNameIs</item>"
    	                                +"<!-- address -->"
    	                                +"<item>https://www.mynameis.com/rapper/repo</item>"
    	                                +"<!-- description -->"
    	                                +"<item>Who is the first repo?</item>"
    	                                +"<!-- version -->"
    	                                +"<item>22</item>"
    	                                +"<!-- enabled -->"
    	                                +"<item>0</item>"
    	                                +"<!-- push requests -->"
    	                                +"<item>ignore</item>"
    	                                +"<!-- pubkey -->"
    	                                +"<item>ddddddd2313aaaaabcccc111</item>"
    	                            +"</string-array>"
    	                        +"</resources>";
    	
    	// Now parse that xml file
    	List<String> additionalRepos;
    	try {
    		additionalRepos = parseXmlStringIntoRepos(reposXmlContent);
    	} catch (IOException io) {
    		fail("IOException. Failed parsing xml string into repos.");
    		return;
    	} catch (XmlPullParserException xppe) {
    		fail("XmlPullParserException. Failed parsing xml string into repos.");
    		return;
    	}

    	// We should have loaded these repos
    	List<String> oem0 = Arrays.asList("oem0Name", "https://www.oem0.com/yeah/repo", "I'm the first oem repo.",
    	                                    "22", "1", "ignore", "fffff2313aaaaabcccc111");
    	List<String> oem1 = Arrays.asList("oem1MyNameIs", "https://www.mynameis.com/rapper/repo", "Who is the first repo?",
    	                                    "22", "0", "ignore", "ddddddd2313aaaaabcccc111");
    	List<String> shouldBeRepos = new LinkedList<>();
    	shouldBeRepos.addAll(oem0);
    	shouldBeRepos.addAll(oem1);

    	assertEquals(additionalRepos.size(), shouldBeRepos.size());
    	for (int i = 0; i < additionalRepos.size(); i++) {
    	    assertEquals(shouldBeRepos.get(i), additionalRepos.get(i));
    	}
    }
}
