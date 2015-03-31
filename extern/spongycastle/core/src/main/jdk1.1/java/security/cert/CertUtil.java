package java.security.cert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERIA5String;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.OIDTokenizer;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.util.Strings;

class CertUtil
{
    static class Implementation
    {
        Object      engine;
        Provider    provider;

        Implementation(
            Object      engine,
            Provider    provider)
        {
            this.engine = engine;
            this.provider = provider;
        }

        Object getEngine()
        {
            return engine;
        }

        Provider getProvider()
        {
            return provider;
        }
    }

    /**
     * see if we can find an algorithm (or its alias and what it represents) in
     * the property table for the given provider.
     *
     * @return null if no algorithm found, an Implementation if it is.
     */
    static Implementation getImplementation(
        String      baseName,
        String      algorithm,
        Provider    prov)
    {
        if (prov == null)
        {
            Provider[] provider = Security.getProviders();

            //
            // search every provider looking for the algorithm we want.
            //
            for (int i = 0; i != provider.length; i++)
            {
                Implementation imp = getImplementation(baseName, algorithm, provider[i]);
                if (imp != null)
                {
                    return imp;
                }
            }

            return null;
        }

        String      alias;

        while ((alias = prov.getProperty("Alg.Alias." + baseName + "." + algorithm)) != null)
        {
            algorithm = alias;
        }

        String      className = prov.getProperty(baseName + "." + algorithm);

        if (className != null)
        {
            try
            {
                return new Implementation(Class.forName(className).newInstance(), prov);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but no class found!");
            }
            catch (Exception e)
            {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but class inaccessible: " + e.toString());
            }
        }

        return null;
    }

    /**
     * return an implementation for a given algorithm/provider.
     * If the provider is null, we grab the first avalaible who has the required algorithm.
     *
     * @return null if no algorithm found, an Implementation if it is.
     * @exception NoSuchProviderException if a provider is specified and not found.
     */
    static Implementation getImplementation(
        String      baseName,
        String      algorithm,
        String      provider)
        throws NoSuchProviderException
    {
        if (provider == null)
        {
            Provider[] prov = Security.getProviders();

            //
            // search every provider looking for the algorithm we want.
            //
            for (int i = 0; i != prov.length; i++)
            {
                Implementation imp = getImplementation(baseName, algorithm, prov[i]);
                if (imp != null)
                {
                    return imp;
                }
            }
        }
        else
        {
            Provider prov = Security.getProvider(provider);

            if (prov == null)
            {
                throw new NoSuchProviderException("Provider " + provider + " not found");
            }

            return getImplementation(baseName, algorithm, prov);
        }

        return null;
    }

    /**
     * see if we can find an algorithm (or its alias and what it represents) in
     * the property table for the given provider.
     *
     * @return null if no algorithm found, an Implementation if it is.
     */
    static Implementation getImplementation(String baseName, String algorithm,
            Provider prov, Class[] ctorparamtype, Object[] ctorparam)
            throws InvalidAlgorithmParameterException
    {
        String alias;

        while ((alias = prov.getProperty("Alg.Alias." + baseName + "."
                + algorithm)) != null)
        {
            algorithm = alias;
        }

        String className = prov.getProperty(baseName + "." + algorithm);

        if (className != null)
        {
            try
            {
                return new Implementation(Class.forName(className)
                        .getConstructor(ctorparamtype).newInstance(ctorparam),
                        prov);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException("algorithm " + algorithm
                        + " in provider " + prov.getName()
                        + " but no class found!");
            }
            catch (Exception e)
            {
                if (e instanceof InvalidAlgorithmParameterException)
                {
                    throw (InvalidAlgorithmParameterException)e;
                }

                throw new IllegalStateException("algorithm " + algorithm
                        + " in provider " + prov.getName()
                        + " but class inaccessible!");
            }
        }

        return null;
    }

    /**
     * return an implementation for a given algorithm/provider. If the provider
     * is null, we grab the first avalaible who has the required algorithm.
     * 
     * @return null if no algorithm found, an Implementation if it is.
     * 
     * @exception NoSuchProviderException
     *                if a provider is specified and not found.
     */
    static Implementation getImplementation(String baseName, String algorithm,
            String provider, Class[] ctorparamtype, Object[] ctorparam)
            throws NoSuchProviderException, InvalidAlgorithmParameterException
    {
        if (provider == null)
        {
            Provider[] prov = Security.getProviders();

            //
            // search every provider looking for the algorithm we want.
            //
            for (int i = 0; i != prov.length; i++)
            {
                Implementation imp = getImplementation(baseName, algorithm,
                        prov[i], ctorparamtype, ctorparam);
                if (imp != null)
                {
                    return imp;
                }
            }
        }
        else
        {
            Provider prov = Security.getProvider(provider);

            if (prov == null)
            {
                throw new NoSuchProviderException("Provider " + provider
                        + " not found");
            }

            return getImplementation(baseName, algorithm, prov, ctorparamtype,
                    ctorparam);
        }

        return null;
    }

    static byte[] parseGeneralName(int type, String data) throws IOException
    {
        byte[] encoded = null;

        switch (type)
        {
        case 0:
            throw new IOException(
                    "unable to parse OtherName String representation");
        case 1:
            encoded = parseRfc822(data.trim());
            break;
        case 2:
            encoded = parseDNSName(data.trim());
            break;
        case 3:
            throw new IOException(
                    "unable to parse ORAddress String representation");
        case 4:
            encoded = parseX509Name(data.trim());
            break;
        case 5:
            throw new IOException(
                    "unable to parse EDIPartyName String representation");
        case 6:
            encoded = parseURI(data.trim());
            break;
        case 7:
            encoded = parseIP(data.trim());
            break;
        case 8:
            encoded = parseOID(data.trim());
            break;
        default:
            throw new IOException(
                    "unable to parse unkown type String representation");
        }
        return encoded;
    }

    /**
     * Check the format of an OID.<br />
     * Throw an IOException if the first component is not 0, 1 or 2 or the
     * second component is greater than 39.<br />
     * <br />
     * User {@link org.spongycastle.asn1.OIDTokenizer OIDTokenizer}
     * 
     * @param the
     *            OID to be checked.
     * 
     * @exception IOException
     *                if the first component is not 0, 1 or 2 or the second
     *                component is greater than 39.
     */
    static byte[] parseOID(String oid) throws IOException
    {
        OIDTokenizer tokenizer = new OIDTokenizer(oid);
        String token;
        if (!tokenizer.hasMoreTokens())
        {
            throw new IOException("OID contains no tokens");
        }
        token = tokenizer.nextToken();
        if (token == null)
        {
            throw new IOException("OID contains no tokens");
        }
        try
        {
            int test = (Integer.valueOf(token)).intValue();
            if (test < 0 || test > 2)
            {
                throw new IOException("first token is not >= 0 and <=2");
            }
            if (!tokenizer.hasMoreTokens())
            {
                throw new IOException("OID contains only one token");
            }
            token = tokenizer.nextToken();
            if (token == null)
            {
                throw new IOException("OID contains only one token");
            }
            test = (Integer.valueOf(token)).intValue();
            if (test < 0 || test > 39)
            {
                throw new IOException("secon token is not >= 0 and <=39");
            }
        }
        catch (NumberFormatException ex)
        {
            throw new IOException("token: " + token + ": " + ex.toString());
        }
        ASN1Object derData = new ASN1ObjectIdentifier(oid);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DEROutputStream derOutStream = new DEROutputStream(outStream);
        derOutStream.writeObject(derData);
        derOutStream.close();
        return outStream.toByteArray();
    }

    /**
     * Parse the given IPv4 or IPv6 into DER encoded byte array representation.
     * 
     * @param the
     *            IP in well known String format
     * 
     * @return the IP as byte array
     * 
     * @exception IOException
     *                if the String could not be parsed
     */
    private static byte[] parseIP(String data) throws IOException
    {
        byte[] encoded = parseIPv4(data);

        if (encoded == null)
        {
            encoded = parseIPv6(data);
        }

        if (encoded == null)
        {
            throw new IOException(
                    "unable to parse IP to DER encoded byte array");
        }

        return encoded;
    }

    /**
     * Parse the given IPv4 into DER encoded byte array representation.
     * 
     * @param the
     *            IP in well known String format
     * 
     * @return the IP as byte array or <code>null</code> if not parseable
     */
    private static byte[] parseIPv4(String data)
    {
        if (data.length() == 0)
        {
            return null;
        }

        int octet;
        int octets = 0;
        byte[] dst = new byte[4];

        int pos = 0;
        int start = 0;
        while (start < data.length()
                && (pos = data.indexOf('.', start)) > start && pos - start > 3)
        {
            try
            {
                octet = (Integer.valueOf(data.substring(start, pos - start)))
                        .intValue();
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
            if (octet < 0 || octet > 255)
            {
                return null;
            }
            dst[octets++] = (byte)(octet & 0xff);

            start = pos + 1;
        }

        if (octets < 4)
        {
            return null;
        }

        return dst;
    }

    /**
     * Parse the given IPv6 into DER encoded byte array representation.<br />
     * <br />
     * <b>TODO: implement this</b>
     * 
     * @param the
     *            IP in well known String format
     * 
     * @return the IP as byte array or <code>null</code> if not parseable
     */
    private static byte[] parseIPv6(String data)
    {
        return null;
    }

    /**
     * Parse the given URI into DER encoded byte array representation.
     * 
     * @param the
     *            URI in well known String format
     * 
     * @return the URI as byte array
     * 
     * @exception IOException
     *                if the String could not be parsed
     */
    private static byte[] parseURI(String data) throws IOException
    {
        // TODO do parsing test
        ASN1Object derData = new DERIA5String(data);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DEROutputStream derOutStream = new DEROutputStream(outStream);
        derOutStream.writeObject(derData);
        derOutStream.close();
        return outStream.toByteArray();
    }

    /**
     * Parse the given rfc822 addr-spec into DER encoded byte array
     * representation.
     * 
     * @param the
     *            rfc822 addr-spec in well known String format
     * 
     * @return the rfc822 addr-spec as byte array
     * 
     * @exception IOException
     *                if the String could not be parsed
     */
    private static byte[] parseRfc822(String data) throws IOException
    {
        int tmpInt = data.indexOf('@');
        if (tmpInt < 0 || tmpInt >= data.length() - 1)
        {
            throw new IOException("wrong format of rfc822Name:" + data);
        }
        // TODO more test for illegal charateers
        ASN1Object derData = new DERIA5String(data);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DEROutputStream derOutStream = new DEROutputStream(outStream);
        derOutStream.writeObject(derData);
        derOutStream.close();
        return outStream.toByteArray();
    }

    /**
     * Parse the given DNS name into DER encoded byte array representation. The
     * String must be in den preffered name syntax as defined in RFC 1034.
     * 
     * @param the
     *            DNS name in well known String format
     * 
     * @return the DNS name as byte array
     * 
     * @exception IOException
     *                if the String could not be parsed
     */
    private static byte[] parseDNSName(String data) throws IOException
    {
        // TODO more test for illegal charateers
        ASN1Object derData = new DERIA5String(data);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DEROutputStream derOutStream = new DEROutputStream(outStream);
        derOutStream.writeObject(derData);
        derOutStream.close();
        return outStream.toByteArray();
    }

    /**
     * Parse the given X.509 name into DER encoded byte array representation.
     * 
     * @param the
     *            X.509 name in well known String format
     * 
     * @return the X.509 name as byte array
     * 
     * @exception IOException
     *                if the String could not be parsed
     */
    private static byte[] parseX509Name(String data) throws IOException
    {
        // TODO more test for illegal charateers
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DEROutputStream derOutStream = new DEROutputStream(outStream);
        derOutStream.writeObject(new X509Name(trimX509Name(data)));
        derOutStream.close();
        return outStream.toByteArray();
    }

    /**
     * Returns the given name converted to upper case and all multi spaces squezed
     * to one space.
     **/
    static String trimX509Name(String name)
    {
        String data = Strings.toUpperCase(name.trim());
        int pos;
        while ((pos = data.indexOf("  ")) >= 0)
        {
            data = data.substring(0, pos) + data.substring(pos + 1);
        }
        while ((pos = data.indexOf(" =")) >= 0)
        {
            data = data.substring(0, pos) + data.substring(pos + 1);
        }
        while ((pos = data.indexOf("= ")) >= 0)
        {
            data = data.substring(0, pos + 1) + data.substring(pos + 2);
        }
        return data;
    }
}
