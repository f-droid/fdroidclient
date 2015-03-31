package org.spongycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Hashtable;

import org.spongycastle.asn1.x9.ECNamedCurveTable;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.Integers;

public class TlsECCUtils
{
    public static final Integer EXT_elliptic_curves = Integers.valueOf(ExtensionType.elliptic_curves);
    public static final Integer EXT_ec_point_formats = Integers.valueOf(ExtensionType.ec_point_formats);

    private static final String[] curveNames = new String[] { "sect163k1", "sect163r1", "sect163r2", "sect193r1",
        "sect193r2", "sect233k1", "sect233r1", "sect239k1", "sect283k1", "sect283r1", "sect409k1", "sect409r1",
        "sect571k1", "sect571r1", "secp160k1", "secp160r1", "secp160r2", "secp192k1", "secp192r1", "secp224k1",
        "secp224r1", "secp256k1", "secp256r1", "secp384r1", "secp521r1",
        "brainpoolP256r1", "brainpoolP384r1", "brainpoolP512r1"};

    public static void addSupportedEllipticCurvesExtension(Hashtable extensions, int[] namedCurves) throws IOException
    {
        extensions.put(EXT_elliptic_curves, createSupportedEllipticCurvesExtension(namedCurves));
    }

    public static void addSupportedPointFormatsExtension(Hashtable extensions, short[] ecPointFormats)
        throws IOException
    {
        extensions.put(EXT_ec_point_formats, createSupportedPointFormatsExtension(ecPointFormats));
    }

    public static int[] getSupportedEllipticCurvesExtension(Hashtable extensions) throws IOException
    {
        byte[] extensionData = TlsUtils.getExtensionData(extensions, EXT_elliptic_curves);
        return extensionData == null ? null : readSupportedEllipticCurvesExtension(extensionData);
    }

    public static short[] getSupportedPointFormatsExtension(Hashtable extensions) throws IOException
    {
        byte[] extensionData = TlsUtils.getExtensionData(extensions, EXT_ec_point_formats);
        return extensionData == null ? null : readSupportedPointFormatsExtension(extensionData);
    }

    public static byte[] createSupportedEllipticCurvesExtension(int[] namedCurves) throws IOException
    {
        if (namedCurves == null || namedCurves.length < 1)
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        return TlsUtils.encodeUint16ArrayWithUint16Length(namedCurves);
    }

    public static byte[] createSupportedPointFormatsExtension(short[] ecPointFormats) throws IOException
    {
        if (ecPointFormats == null)
        {
            ecPointFormats = new short[] { ECPointFormat.uncompressed };
        }
        else if (!Arrays.contains(ecPointFormats, ECPointFormat.uncompressed))
        {
            /*
             * RFC 4492 5.1. If the Supported Point Formats Extension is indeed sent, it MUST
             * contain the value 0 (uncompressed) as one of the items in the list of point formats.
             */

            // NOTE: We add it at the end (lowest preference)
            short[] tmp = new short[ecPointFormats.length + 1];
            System.arraycopy(ecPointFormats, 0, tmp, 0, ecPointFormats.length);
            tmp[ecPointFormats.length] = ECPointFormat.uncompressed;

            ecPointFormats = tmp;
        }

        return TlsUtils.encodeUint8ArrayWithUint8Length(ecPointFormats);
    }

    public static int[] readSupportedEllipticCurvesExtension(byte[] extensionData) throws IOException
    {
        if (extensionData == null)
        {
            throw new IllegalArgumentException("'extensionData' cannot be null");
        }

        ByteArrayInputStream buf = new ByteArrayInputStream(extensionData);

        int length = TlsUtils.readUint16(buf);
        if (length < 2 || (length & 1) != 0)
        {
            throw new TlsFatalAlert(AlertDescription.decode_error);
        }

        int[] namedCurves = TlsUtils.readUint16Array(length / 2, buf);

        TlsProtocol.assertEmpty(buf);

        return namedCurves;
    }

    public static short[] readSupportedPointFormatsExtension(byte[] extensionData) throws IOException
    {
        if (extensionData == null)
        {
            throw new IllegalArgumentException("'extensionData' cannot be null");
        }

        ByteArrayInputStream buf = new ByteArrayInputStream(extensionData);

        short length = TlsUtils.readUint8(buf);
        if (length < 1)
        {
            throw new TlsFatalAlert(AlertDescription.decode_error);
        }

        short[] ecPointFormats = TlsUtils.readUint8Array(length, buf);

        TlsProtocol.assertEmpty(buf);

        if (!Arrays.contains(ecPointFormats, ECPointFormat.uncompressed))
        {
            /*
             * RFC 4492 5.1. If the Supported Point Formats Extension is indeed sent, it MUST
             * contain the value 0 (uncompressed) as one of the items in the list of point formats.
             */
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }

        return ecPointFormats;
    }

    public static String getNameOfNamedCurve(int namedCurve)
    {
        return isSupportedNamedCurve(namedCurve) ? curveNames[namedCurve - 1] : null;
    }

    public static ECDomainParameters getParametersForNamedCurve(int namedCurve)
    {
        String curveName = getNameOfNamedCurve(namedCurve);
        if (curveName == null)
        {
            return null;
        }

        // Lazily created the first time a particular curve is accessed
        X9ECParameters ecP = ECNamedCurveTable.getByName(curveName);

        if (ecP == null)
        {
            return null;
        }

        // It's a bit inefficient to do this conversion every time
        return new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
    }

    public static boolean hasAnySupportedNamedCurves()
    {
        return curveNames.length > 0;
    }

    public static boolean containsECCCipherSuites(int[] cipherSuites)
    {
        for (int i = 0; i < cipherSuites.length; ++i)
        {
            if (isECCCipherSuite(cipherSuites[i]))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isECCCipherSuite(int cipherSuite)
    {
        switch (cipherSuite)
        {
        /*
         * RFC 4492
         */
        case CipherSuite.TLS_ECDH_ECDSA_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA:
        case CipherSuite.TLS_ECDH_RSA_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA:
        case CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA:
        case CipherSuite.TLS_ECDHE_RSA_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA:
        case CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA:
        case CipherSuite.TLS_ECDH_anon_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDH_anon_WITH_RC4_128_SHA:
        case CipherSuite.TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDH_anon_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDH_anon_WITH_AES_256_CBC_SHA:

        /*
         * RFC 5289
         */
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384:

        /*
         * RFC 5489
         */
        case CipherSuite.TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA:
        case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA:
        case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256:
        case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA:
        case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384:
        case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA:
        case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA256:
        case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384:
        case CipherSuite.TLS_ECDHE_PSK_WITH_RC4_128_SHA:

        /*
         * draft-josefsson-salsa20-tls-02 
         */
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_ESTREAM_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_ESTREAM_SALSA20_UMAC96:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_ECDSA_WITH_SALSA20_UMAC96:
        case CipherSuite.TLS_ECDHE_PSK_WITH_ESTREAM_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_PSK_WITH_ESTREAM_SALSA20_UMAC96:
        case CipherSuite.TLS_ECDHE_PSK_WITH_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_PSK_WITH_SALSA20_UMAC96:
        case CipherSuite.TLS_ECDHE_RSA_WITH_ESTREAM_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_RSA_WITH_ESTREAM_SALSA20_UMAC96:
        case CipherSuite.TLS_ECDHE_RSA_WITH_SALSA20_SHA1:
        case CipherSuite.TLS_ECDHE_RSA_WITH_SALSA20_UMAC96:

            return true;

        default:
            return false;
        }
    }

    public static boolean areOnSameCurve(ECDomainParameters a, ECDomainParameters b)
    {
        // TODO Move to ECDomainParameters.equals() or other utility method?
        return a.getCurve().equals(b.getCurve()) && a.getG().equals(b.getG()) && a.getN().equals(b.getN())
            && a.getH().equals(b.getH());
    }

    public static boolean isSupportedNamedCurve(int namedCurve)
    {
        return (namedCurve > 0 && namedCurve <= curveNames.length);
    }

    public static boolean isCompressionPreferred(short[] ecPointFormats, short compressionFormat)
    {
        if (ecPointFormats == null)
        {
            return false;
        }
        for (int i = 0; i < ecPointFormats.length; ++i)
        {
            short ecPointFormat = ecPointFormats[i];
            if (ecPointFormat == ECPointFormat.uncompressed)
            {
                return false;
            }
            if (ecPointFormat == compressionFormat)
            {
                return true;
            }
        }
        return false;
    }

    public static byte[] serializeECFieldElement(int fieldSize, BigInteger x) throws IOException
    {
        return BigIntegers.asUnsignedByteArray((fieldSize + 7) / 8, x);
    }

    public static byte[] serializeECPoint(short[] ecPointFormats, ECPoint point) throws IOException
    {
        ECCurve curve = point.getCurve();

        /*
         * RFC 4492 5.7. ...an elliptic curve point in uncompressed or compressed format. Here, the
         * format MUST conform to what the server has requested through a Supported Point Formats
         * Extension if this extension was used, and MUST be uncompressed if this extension was not
         * used.
         */
        boolean compressed = false;
        if (curve instanceof ECCurve.F2m)
        {
            compressed = isCompressionPreferred(ecPointFormats, ECPointFormat.ansiX962_compressed_char2);
        }
        else if (curve instanceof ECCurve.Fp)
        {
            compressed = isCompressionPreferred(ecPointFormats, ECPointFormat.ansiX962_compressed_prime);
        }
        return point.getEncoded(compressed);
    }

    public static byte[] serializeECPublicKey(short[] ecPointFormats, ECPublicKeyParameters keyParameters)
        throws IOException
    {
        return serializeECPoint(ecPointFormats, keyParameters.getQ());
    }

    public static BigInteger deserializeECFieldElement(int fieldSize, byte[] encoding) throws IOException
    {
        int requiredLength = (fieldSize + 7) / 8;
        if (encoding.length != requiredLength)
        {
            throw new TlsFatalAlert(AlertDescription.decode_error);
        }
        return new BigInteger(1, encoding);
    }

    public static ECPoint deserializeECPoint(short[] ecPointFormats, ECCurve curve, byte[] encoding) throws IOException
    {
        /*
         * NOTE: Here we implicitly decode compressed or uncompressed encodings. DefaultTlsClient by
         * default is set up to advertise that we can parse any encoding so this works fine, but
         * extra checks might be needed here if that were changed.
         */
        // TODO Review handling of infinity and hybrid encodings
        return curve.decodePoint(encoding);
    }

    public static ECPublicKeyParameters deserializeECPublicKey(short[] ecPointFormats, ECDomainParameters curve_params,
        byte[] encoding) throws IOException
    {
        try
        {
            ECPoint Y = deserializeECPoint(ecPointFormats, curve_params.getCurve(), encoding);
            return new ECPublicKeyParameters(Y, curve_params);
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }

    public static byte[] calculateECDHBasicAgreement(ECPublicKeyParameters publicKey, ECPrivateKeyParameters privateKey)
    {
        ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(privateKey);
        BigInteger agreementValue = basicAgreement.calculateAgreement(publicKey);

        /*
         * RFC 4492 5.10. Note that this octet string (Z in IEEE 1363 terminology) as output by
         * FE2OSP, the Field Element to Octet String Conversion Primitive, has constant length for
         * any given field; leading zeros found in this octet string MUST NOT be truncated.
         */
        return BigIntegers.asUnsignedByteArray(basicAgreement.getFieldSize(), agreementValue);
    }

    public static AsymmetricCipherKeyPair generateECKeyPair(SecureRandom random, ECDomainParameters ecParams)
    {
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        keyPairGenerator.init(new ECKeyGenerationParameters(ecParams, random));
        return keyPairGenerator.generateKeyPair();
    }

    public static ECPrivateKeyParameters generateEphemeralClientKeyExchange(SecureRandom random, short[] ecPointFormats,
        ECDomainParameters ecParams, OutputStream output) throws IOException
    {
        AsymmetricCipherKeyPair kp = TlsECCUtils.generateECKeyPair(random, ecParams);

        ECPublicKeyParameters ecPublicKey = (ECPublicKeyParameters) kp.getPublic();
        writeECPoint(ecPointFormats, ecPublicKey.getQ(), output);

        return (ECPrivateKeyParameters) kp.getPrivate();
    }

    public static ECPublicKeyParameters validateECPublicKey(ECPublicKeyParameters key) throws IOException
    {
        // TODO Check RFC 4492 for validation
        return key;
    }

    public static int readECExponent(int fieldSize, InputStream input) throws IOException
    {
        BigInteger K = readECParameter(input);
        if (K.bitLength() < 32)
        {
            int k = K.intValue();
            if (k > 0 && k < fieldSize)
            {
                return k;
            }
        }
        throw new TlsFatalAlert(AlertDescription.illegal_parameter);
    }

    public static BigInteger readECFieldElement(int fieldSize, InputStream input) throws IOException
    {
        return deserializeECFieldElement(fieldSize, TlsUtils.readOpaque8(input));
    }

    public static BigInteger readECParameter(InputStream input) throws IOException
    {
        // TODO Are leading zeroes okay here?
        return new BigInteger(1, TlsUtils.readOpaque8(input));
    }

    public static ECDomainParameters readECParameters(int[] namedCurves, short[] ecPointFormats, InputStream input)
        throws IOException
    {
        try
        {
            short curveType = TlsUtils.readUint8(input);

            switch (curveType)
            {
            case ECCurveType.explicit_prime:
            {
                checkNamedCurve(namedCurves, NamedCurve.arbitrary_explicit_prime_curves);

                BigInteger prime_p = readECParameter(input);
                BigInteger a = readECFieldElement(prime_p.bitLength(), input);
                BigInteger b = readECFieldElement(prime_p.bitLength(), input);
                ECCurve curve = new ECCurve.Fp(prime_p, a, b);
                ECPoint base = deserializeECPoint(ecPointFormats, curve, TlsUtils.readOpaque8(input));
                BigInteger order = readECParameter(input);
                BigInteger cofactor = readECParameter(input);
                return new ECDomainParameters(curve, base, order, cofactor);
            }
            case ECCurveType.explicit_char2:
            {
                checkNamedCurve(namedCurves, NamedCurve.arbitrary_explicit_char2_curves);

                int m = TlsUtils.readUint16(input);
                short basis = TlsUtils.readUint8(input);
                ECCurve curve;
                switch (basis) {
                case ECBasisType.ec_basis_trinomial:
                {
                    int k = readECExponent(m, input);
                    BigInteger a = readECFieldElement(m, input);
                    BigInteger b = readECFieldElement(m, input);
                    curve = new ECCurve.F2m(m, k, a, b);
                    break;
                }
                case ECBasisType.ec_basis_pentanomial:
                {
                    int k1 = readECExponent(m, input);
                    int k2 = readECExponent(m, input);
                    int k3 = readECExponent(m, input);
                    BigInteger a = readECFieldElement(m, input);
                    BigInteger b = readECFieldElement(m, input);
                    curve = new ECCurve.F2m(m, k1, k2, k3, a, b);
                    break;
                }
                default:
                    throw new TlsFatalAlert(AlertDescription.illegal_parameter);
                }
                ECPoint base = deserializeECPoint(ecPointFormats, curve, TlsUtils.readOpaque8(input));
                BigInteger order = readECParameter(input);
                BigInteger cofactor = readECParameter(input);
                return new ECDomainParameters(curve, base, order, cofactor);
            }
            case ECCurveType.named_curve:
            {
                int namedCurve = TlsUtils.readUint16(input);
                if (!NamedCurve.refersToASpecificNamedCurve(namedCurve))
                {
                    /*
                     * RFC 4492 5.4. All those values of NamedCurve are allowed that refer to a
                     * specific curve. Values of NamedCurve that indicate support for a class of
                     * explicitly defined curves are not allowed here [...].
                     */
                    throw new TlsFatalAlert(AlertDescription.illegal_parameter);
                }

                checkNamedCurve(namedCurves, namedCurve);

                return TlsECCUtils.getParametersForNamedCurve(namedCurve);
            }
            default:
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }

    private static void checkNamedCurve(int[] namedCurves, int namedCurve) throws IOException
    {
        if (namedCurves != null && !Arrays.contains(namedCurves, namedCurve))
        {
            /*
             * RFC 4492 4. [...] servers MUST NOT negotiate the use of an ECC cipher suite
             * unless they can complete the handshake while respecting the choice of curves
             * and compression techniques specified by the client.
             */
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }

    public static void writeECExponent(int k, OutputStream output) throws IOException
    {
        BigInteger K = BigInteger.valueOf(k);
        writeECParameter(K, output);
    }

    public static void writeECFieldElement(ECFieldElement x, OutputStream output) throws IOException
    {
        TlsUtils.writeOpaque8(x.getEncoded(), output);
    }

    public static void writeECFieldElement(int fieldSize, BigInteger x, OutputStream output) throws IOException
    {
        TlsUtils.writeOpaque8(serializeECFieldElement(fieldSize, x), output);
    }

    public static void writeECParameter(BigInteger x, OutputStream output) throws IOException
    {
        TlsUtils.writeOpaque8(BigIntegers.asUnsignedByteArray(x), output);
    }

    public static void writeExplicitECParameters(short[] ecPointFormats, ECDomainParameters ecParameters,
        OutputStream output) throws IOException
    {
        ECCurve curve = ecParameters.getCurve();
        if (curve instanceof ECCurve.Fp)
        {
            TlsUtils.writeUint8(ECCurveType.explicit_prime, output);

            ECCurve.Fp fp = (ECCurve.Fp) curve;
            writeECParameter(fp.getQ(), output);
        }
        else if (curve instanceof ECCurve.F2m)
        {
            TlsUtils.writeUint8(ECCurveType.explicit_char2, output);

            ECCurve.F2m f2m = (ECCurve.F2m) curve;
            int m = f2m.getM();
            TlsUtils.checkUint16(m);
            TlsUtils.writeUint16(m, output);

            if (f2m.isTrinomial())
            {
                TlsUtils.writeUint8(ECBasisType.ec_basis_trinomial, output);
                writeECExponent(f2m.getK1(), output);
            }
            else
            {
                TlsUtils.writeUint8(ECBasisType.ec_basis_pentanomial, output);
                writeECExponent(f2m.getK1(), output);
                writeECExponent(f2m.getK2(), output);
                writeECExponent(f2m.getK3(), output);
            }

        }
        else
        {
            throw new IllegalArgumentException("'ecParameters' not a known curve type");
        }

        writeECFieldElement(curve.getA(), output);
        writeECFieldElement(curve.getB(), output);
        TlsUtils.writeOpaque8(serializeECPoint(ecPointFormats, ecParameters.getG()), output);
        writeECParameter(ecParameters.getN(), output);
        writeECParameter(ecParameters.getH(), output);
    }

    public static void writeECPoint(short[] ecPointFormats, ECPoint point, OutputStream output) throws IOException
    {
        TlsUtils.writeOpaque8(TlsECCUtils.serializeECPoint(ecPointFormats, point), output);
    }

    public static void writeNamedECParameters(int namedCurve, OutputStream output) throws IOException
    {
        if (!NamedCurve.refersToASpecificNamedCurve(namedCurve))
        {
            /*
             * RFC 4492 5.4. All those values of NamedCurve are allowed that refer to a specific
             * curve. Values of NamedCurve that indicate support for a class of explicitly defined
             * curves are not allowed here [...].
             */
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        TlsUtils.writeUint8(ECCurveType.named_curve, output);
        TlsUtils.checkUint16(namedCurve);
        TlsUtils.writeUint16(namedCurve, output);
    }
}
