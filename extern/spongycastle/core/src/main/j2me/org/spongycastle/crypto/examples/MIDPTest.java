package org.spongycastle.crypto.examples;

import java.io.*;
import java.lang.*;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;

import org.spongycastle.util.test.*;
import org.spongycastle.util.encoders.*;

import org.spongycastle.crypto.*;
import org.spongycastle.crypto.paddings.*;
import org.spongycastle.crypto.engines.*;
import org.spongycastle.crypto.modes.*;
import org.spongycastle.crypto.params.*;

/**
 * MIDP is a simple graphics application for the J2ME CLDC/MIDP.
 * 
 * It has hardcoded values for the key and plain text. It also performs the
 * standard testing for the chosen cipher, and displays the results.
 * 
 * This example shows how to use the light-weight API and a symmetric cipher.
 * 
 */
public class MIDPTest extends MIDlet
{
    private Display             d           = null;

    private boolean             doneEncrypt = false;

    private String              key         = "0123456789abcdef0123456789abcdef";
    private String              plainText   = "www.bouncycastle.org";
    private byte[]              keyBytes    = null;
    private byte[]              cipherText  = null;
    private BufferedBlockCipher cipher      = null;

    private String[]            cipherNames = {"DES", "DESede", "IDEA", "Rijndael", "Twofish"};

    private Form                output      = null;

    public void startApp()
    {
        Display.getDisplay(this).setCurrent(output);
    }

    public void pauseApp()
    {

    }

    public void destroyApp(boolean unconditional)
    {

    }

    public MIDPTest()
    {
        output = new Form("BouncyCastle");
        output.append("Key: " + key.substring(0, 7) + "...\n");
        output.append("In : " + plainText.substring(0, 7) + "...\n");

        cipherText = performEncrypt(Hex.decode(key.getBytes()), plainText);
        String ctS = new String(Hex.encode(cipherText));

        output.append("\nCT : " + ctS.substring(0, 7) + "...\n");

        String decryptText = performDecrypt(Hex.decode(key.getBytes()), cipherText);

        output.append("PT : " + decryptText.substring(0, 7) + "...\n");

        if (decryptText.compareTo(plainText) == 0)
        {
            output.append("Success");
        }
        else
        {
            output.append("Failure");
            message("[" + plainText + "]");
            message("[" + decryptText + "]");
        }

    }

    private byte[] performEncrypt(byte[] key, String plainText)
    {
        byte[] ptBytes = plainText.getBytes();

        cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(getEngineInstance()));

        String name = cipher.getUnderlyingCipher().getAlgorithmName();
        message("Using " + name);

        cipher.init(true, new KeyParameter(key));

        byte[] rv = new byte[cipher.getOutputSize(ptBytes.length)];

        int oLen = cipher.processBytes(ptBytes, 0, ptBytes.length, rv, 0);
        try
        {
            cipher.doFinal(rv, oLen);
        }
        catch (CryptoException ce)
        {
            message("Ooops, encrypt exception");
            status(ce.toString());
        }
        return rv;
    }

    private String performDecrypt(byte[] key, byte[] cipherText)
    {
        cipher.init(false, new KeyParameter(key));

        byte[] rv = new byte[cipher.getOutputSize(cipherText.length)];

        int oLen = cipher.processBytes(cipherText, 0, cipherText.length, rv, 0);
        try
        {
            cipher.doFinal(rv, oLen);
        }
        catch (CryptoException ce)
        {
            message("Ooops, decrypt exception");
            status(ce.toString());
        }
        return new String(rv).trim();
    }

    private int whichCipher()
    {
        return 4; // DES
    }

    private BlockCipher getEngineInstance()
    {
        // returns a block cipher according to the current
        // state of the radio button lists. This is only
        // done prior to encryption.
        BlockCipher rv = null;

        switch (whichCipher())
        {
            case 0 :
                rv = new DESEngine();
                break;
            case 1 :
                rv = new DESedeEngine();
                break;
            case 2 :
                rv = new IDEAEngine();
                break;
            case 3 :
                rv = new RijndaelEngine();
                break;
            case 4 :
                rv = new TwofishEngine();
                break;
            default :
                rv = new DESEngine();
                break;
        }
        return rv;
    }

    public void message(String s)
    {
        System.out.println("M:" + s);
    }

    public void status(String s)
    {
        System.out.println("S:" + s);
    }

}
