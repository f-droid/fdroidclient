package org.fdroid.fdroid.nearby;

/*
 * #%L
 * NanoHttpd-Webserver
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Synced from NanoHTTPD's {@code TestHttpServer.java}
 *
 * @see <a href="https://github.com/NanoHttpd/nanohttpd/blob/nanohttpd-project-2.3.1/webserver/src/test/java/fi/iki/elonen/TestHttpServer.java">webserver/src/test/java/fi/iki/elonen/LocalHTTPDTest.java</a>
 */
@SuppressWarnings("LineLength")
@RunWith(RobolectricTestRunner.class)
public class LocalHTTPDTest {

    private static final String HEADER_FIELD_ETAG = "ETag";

    private static ClassLoader classLoader;
    private static LocalHTTPD localHttpd;
    private static Thread serverStartThread;
    private static File webRoot;

    private final int port = 38723;
    private final String baseUrl = "http://localhost:" + port;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        classLoader = getClass().getClassLoader();

        assertFalse(Utils.isServerSocketInUse(port));

        final Context context = ApplicationProvider.getApplicationContext();
        webRoot = context.getFilesDir();
        FileUtils.deleteDirectory(webRoot);
        assertTrue(webRoot.mkdir());
        assertTrue(webRoot.isDirectory());

        final File testdir = new File(webRoot, "testdir");
        assertTrue(testdir.mkdir());
        IOUtils.copy(classLoader.getResourceAsStream("test.html"),
                new FileOutputStream(new File(testdir, "test.html")));

        serverStartThread = new Thread(() -> {
            localHttpd = new LocalHTTPD(
                    context,
                    "localhost",
                    port,
                    webRoot,
                    false);
            try {
                localHttpd.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertTrue(localHttpd.isAlive());
        });
        serverStartThread.start();
        // give the server some tine to start.
        do {
            Thread.sleep(100);
        } while (!Utils.isServerSocketInUse(port));
    }

    @After
    public void tearDown() throws Exception {
        localHttpd.stop();
        serverStartThread.join(5000);
        assertFalse(localHttpd.isAlive());
        assertFalse(serverStartThread.isAlive());
    }

    @Test
    public void doTest404() throws Exception {
        HttpURLConnection connection = getNoKeepAliveConnection(baseUrl + "/xxx/yyy.html");
        connection.setReadTimeout(5000);
        connection.connect();
        Assert.assertEquals(404, connection.getResponseCode());
        connection.disconnect();
    }

    @Test
    public void doSomeBasicTest() throws Exception {
        URL url = new URL(baseUrl + "/testdir/test.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());
        String string = IOUtils.toString(connection.getInputStream(), "UTF-8");
        Assert.assertEquals("<html>\n<head>\n<title>dummy</title>\n</head>\n<body>\n\t<h1>it works</h1>\n</body>\n</html>", string);
        connection.disconnect();

        url = new URL(baseUrl + "/");
        connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());
        string = IOUtils.toString(connection.getInputStream(), "UTF-8");
        System.out.println("REPLY: " + string);
        assertTrue(string.indexOf("testdir") > 0);
        connection.disconnect();

        url = new URL(baseUrl + "/testdir");
        connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());
        string = IOUtils.toString(connection.getInputStream(), "UTF-8");
        assertTrue(string.indexOf("test.html") > 0);
        connection.disconnect();

        IOUtils.copy(classLoader.getResourceAsStream("additional_repos.xml"),
                new FileOutputStream(new File(webRoot, "additional_repos.xml")));
        url = new URL(baseUrl + "/additional_repos.xml");
        connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());
        byte[] actual = IOUtils.toByteArray(connection.getInputStream());
        byte[] expected = IOUtils.toByteArray(classLoader.getResourceAsStream("additional_repos.xml"));
        Assert.assertArrayEquals(expected, actual);
        connection.disconnect();
    }

    @Test
    public void testAPKMimeType() throws IOException {
        String fileName = "urzip.apk";
        String mimeType = "application/vnd.android.package-archive";
        IOUtils.copy(classLoader.getResourceAsStream(fileName),
                new FileOutputStream(new File(webRoot, fileName)));
        URL url = new URL(baseUrl + "/" + fileName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        assertEquals(200, connection.getResponseCode());
        Assert.assertEquals(mimeType, connection.getContentType());
        connection.disconnect();
    }

    @Test
    public void testHeadRequest() throws IOException, InterruptedException {
        File indexFile = new File(webRoot, "index.html");
        IOUtils.copy(classLoader.getResourceAsStream("index.html"),
                new FileOutputStream(indexFile));

        URL url = new URL(baseUrl + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        String mimeType = "text/html";
        System.out.println(mimeType + " " + connection.getContentType());
        assertEquals(mimeType, connection.getContentType());
        assertEquals(indexFile.length(), connection.getContentLength());
        assertNotEquals(0, connection.getContentLength());

        String etag = connection.getHeaderField(HEADER_FIELD_ETAG);
        assertFalse(TextUtils.isEmpty(etag));

        assertEquals(200, connection.getResponseCode());
        connection.disconnect();
    }

    @Test
    public void testPostRequest() throws IOException {
        URL url = new URL(baseUrl + "/request-swap");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        writer.write("repo=" + baseUrl);
        writer.flush();
        writer.close();
        outputStream.close();

        assertEquals(200, connection.getResponseCode());
        connection.disconnect();
    }

    @Test
    public void testBadPostRequest() throws IOException {
        URL url = new URL(baseUrl + "/request-swap");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStream outputStream = connection.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        writer.write("repolkasdfkj" + baseUrl);
        writer.flush();
        writer.close();
        outputStream.close();
        assertEquals(400, connection.getResponseCode());
        connection.disconnect();
    }

    @Test
    public void doArgumentTest() throws InterruptedException, UnsupportedEncodingException, IOException {
        final int testPort = 9458;
        Thread testServer = new Thread(() -> {
            LocalHTTPD localHttpd = new LocalHTTPD(
                    ApplicationProvider.getApplicationContext(),
                    "localhost",
                    testPort,
                    webRoot,
                    false);
            try {
                localHttpd.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertTrue(localHttpd.isAlive());
        });

        testServer.start();
        Thread.sleep(200);

        URL url = new URL("http://localhost:" + testPort + "/");

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
            String str = IOUtils.toString(connection.getInputStream(), "UTF-8");
            assertTrue("The response entity didn't contain the string 'testdir'", str.contains("testdir"));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            testServer.join(5000);
        }
    }

    @Test
    public void testURLContainsParentDirectory() throws IOException {
        HttpURLConnection connection = null;
        URL url = new URL(baseUrl + "/testdir/../index.html");
        try {
            connection = (HttpURLConnection) url.openConnection();
            Assert.assertEquals("The response status should be 403(Forbidden), " + "since the server won't serve requests with '../' due to security reasons",
                    403, connection.getResponseCode());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testIndexFileIsShownWhenURLEndsWithDirectory() throws IOException {
        HttpURLConnection connection = null;
        try {
            String dirName = "indexDir";
            File indexDir = new File(webRoot, dirName);
            assertTrue(indexDir.mkdir());
            IOUtils.copy(classLoader.getResourceAsStream("index.html"),
                    new FileOutputStream(new File(indexDir, "index.html")));
            URL url = new URL(baseUrl + "/" + dirName);
            connection = (HttpURLConnection) url.openConnection();
            String responseString = IOUtils.toString(connection.getInputStream(), "UTF-8");
            Assert.assertThat("When the URL ends with a directory, and if an index.html file is present in that directory," + " the server should respond with that file",
                    responseString, containsString("Simple index file"));

            IOUtils.copy(classLoader.getResourceAsStream("index.html"),
                    new FileOutputStream(new File(webRoot, "index.html")));
            url = new URL(baseUrl + "/");
            connection = (HttpURLConnection) url.openConnection();
            responseString = IOUtils.toString(connection.getInputStream(), "UTF-8");
            Assert.assertThat("When the URL ends with a directory, and if an index.html file is present in that directory,"
                            + " the server should respond with that file",
                    responseString, containsString("Simple index file"));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testRangeHeaderWithStartPositionOnly() throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = getNoKeepAliveConnection(baseUrl + "/testdir/test.html");
            connection.addRequestProperty("range", "bytes=10-");
            connection.setReadTimeout(5000);
            String responseString = IOUtils.toString(connection.getInputStream(), "UTF-8");
            System.out.println("responseString " + responseString);
            Assert.assertThat("The data from the beginning of the file should have been skipped as specified in the 'range' header", responseString,
                    not(containsString("<head>")));
            Assert.assertThat("The response should contain the data from the end of the file since end position was not given in the 'range' header", responseString,
                    containsString("</head>"));
            Assert.assertEquals("The content length should be the length starting from the requested byte", "74", connection.getHeaderField("Content-Length"));
            Assert.assertEquals("The 'Content-Range' header should contain the correct lengths and offsets based on the range served", "bytes 10-83/84",
                    connection.getHeaderField("Content-Range"));
            Assert.assertEquals("Response status for a successful range request should be PARTIAL_CONTENT(206)",
                    206, connection.getResponseCode());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testRangeStartGreaterThanFileLength() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/testdir/test.html");
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("range", "bytes=1000-");
            connection.connect();
            Assert.assertEquals("Response status for a request with 'range' header value which exceeds file length should be RANGE_NOT_SATISFIABLE(416)",
                    416, connection.getResponseCode());
            Assert.assertEquals("The 'Content-Range' header should contain the correct lengths and offsets based on the range served",
                    "bytes */84", connection.getHeaderField("Content-Range"));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testRangeHeaderWithStartAndEndPosition() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/testdir/test.html");
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("range", "bytes=10-40");
            String responseString = IOUtils.toString(connection.getInputStream(), "UTF-8");
            Assert.assertThat("The data from the beginning of the file should have been skipped as specified in the 'range' header",
                    responseString, not(containsString("<head>")));
            Assert.assertThat("The data from the end of the file should have been skipped as specified in the 'range' header",
                    responseString, not(containsString("</head>")));
            Assert.assertEquals("The 'Content-Length' should be the length from the requested start position to end position",
                    "31", connection.getHeaderField("Content-Length"));
            Assert.assertEquals("The 'Contnet-Range' header should contain the correct lengths and offsets based on the range served",
                    "bytes 10-40/84", connection.getHeaderField("Content-Range"));
            Assert.assertEquals("Response status for a successful request with 'range' header should be PARTIAL_CONTENT(206)",
                    206, connection.getResponseCode());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testIfNoneMatchHeader() throws IOException {
        HttpURLConnection connection = null;
        int status = -1;
        while (status == -1) {
            System.out.println("testIfNoneMatchHeader connect attempt");
            try {
                connection = getNoKeepAliveConnection(baseUrl + "/testdir/test.html");
                connection.setRequestProperty("if-none-match", "*");
                connection.connect();
                status = connection.getResponseCode();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        Assert.assertEquals("The response status to a request with 'if-non-match=*' header should be NOT_MODIFIED(304), if the file exists",
                304, status);
    }

    @Test
    public void testRangeHeaderAndIfNoneMatchHeader() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/testdir/test.html");
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("range", "bytes=10-20");
            connection.addRequestProperty("if-none-match", "*");
            Assert.assertEquals("The response status to a request with 'if-non-match=*' header and 'range' header should be NOT_MODIFIED(304),"
                    + " if the file exists, because 'if-non-match' header should be given priority", 304, connection.getResponseCode());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection getNoKeepAliveConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Connection", "Close"); // avoid keep-alive
        return connection;
    }
}
