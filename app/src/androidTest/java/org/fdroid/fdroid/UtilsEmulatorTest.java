package org.fdroid.fdroid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class UtilsEmulatorTest {

    @Test(expected = SAXParseException.class)
    public void testXMLReaderXXEFileAccess() throws ParserConfigurationException, SAXException, IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "  <!DOCTYPE foo [  \n" +
                "    <!ELEMENT foo ANY >\n" +
                "    <!ENTITY xxe SYSTEM \"file:///etc/hosts\" >]><foo>&xxe;</foo>";
        final XMLReader reader = Utils.newXMLReaderInstance();
        reader.parse(new InputSource(new StringReader(xml)));
    }

    @Test(expected = SAXParseException.class)
    public void testXMLReaderXXEUrlAccess() throws ParserConfigurationException, SAXException, IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "  <!DOCTYPE foo [  \n" +
                "    <!ELEMENT foo ANY >\n" +
                "    <!ENTITY xxe SYSTEM \"https://example.com\" >]><foo>&xxe;</foo>";
        final XMLReader reader = Utils.newXMLReaderInstance();
        reader.parse(new InputSource(new StringReader(xml)));
    }
}