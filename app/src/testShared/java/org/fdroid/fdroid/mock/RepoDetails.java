package org.belmarket.shop.mock;

import android.support.annotation.NonNull;

import org.belmarket.shop.RepoXMLHandler;
import org.belmarket.shop.data.Apk;
import org.belmarket.shop.data.App;
import org.belmarket.shop.data.RepoPushRequest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static org.junit.Assert.fail;

public class RepoDetails implements RepoXMLHandler.IndexReceiver {
    public static final String TAG = "RepoDetails";

    public String name;
    public String description;
    public String signingCert;
    public int maxAge;
    public int version;
    public long timestamp;

    public List<Apk> apks = new ArrayList<>();
    public List<App> apps = new ArrayList<>();
    public List<RepoPushRequest> repoPushRequestList = new ArrayList<>();

    @Override
    public void receiveRepo(String name, String description, String signingCert, int maxage, int version, long timestamp) {
        this.name = name;
        this.description = description;
        this.signingCert = signingCert;
        this.maxAge = maxage;
        this.version = version;
        this.timestamp = timestamp;
    }

    @Override
    public void receiveApp(App app, List<Apk> packages) {
        apks.addAll(packages);
        apps.add(app);
    }

    @Override
    public void receiveRepoPushRequest(RepoPushRequest repoPushRequest) {
        repoPushRequestList.add(repoPushRequest);
    }

    @NonNull
    public static RepoDetails getFromFile(InputStream inputStream, int pushRequests) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            RepoDetails repoDetails = new RepoDetails();
            MockRepo mockRepo = new MockRepo(100, pushRequests);
            RepoXMLHandler handler = new RepoXMLHandler(mockRepo, repoDetails);
            reader.setContentHandler(handler);
            InputSource is = new InputSource(new BufferedInputStream(inputStream));
            reader.parse(is);
            return repoDetails;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            fail();

            // Satisfies the compiler, but fail() will always throw a runtime exception so we never
            // reach this return statement.
            return null;
        }
    }

}

