package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.pqc.asn1.McEliecePrivateKey;
import org.spongycastle.pqc.asn1.McEliecePublicKey;
import org.spongycastle.pqc.jcajce.spec.McEliecePrivateKeySpec;
import org.spongycastle.pqc.jcajce.spec.McEliecePublicKeySpec;

/**
 * This class is used to translate between McEliece keys and key specifications.
 *
 * @see BCMcEliecePrivateKey
 * @see McEliecePrivateKeySpec
 * @see BCMcEliecePublicKey
 * @see McEliecePublicKeySpec
 */
public class McElieceKeyFactorySpi
    extends KeyFactorySpi
{
    /**
     * The OID of the algorithm.
     */
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.1";

    /**
     * Converts, if possible, a key specification into a
     * {@link BCMcEliecePublicKey}. Currently, the following key specifications
     * are supported: {@link McEliecePublicKeySpec}, {@link X509EncodedKeySpec}.
     *
     * @param keySpec the key specification
     * @return the McEliece public key
     * @throws InvalidKeySpecException if the key specification is not supported.
     */
    public PublicKey generatePublic(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof McEliecePublicKeySpec)
        {
            return new BCMcEliecePublicKey((McEliecePublicKeySpec)keySpec);
        }
        else if (keySpec instanceof X509EncodedKeySpec)
        {
            // get the DER-encoded Key according to X.509 from the spec
            byte[] encKey = ((X509EncodedKeySpec)keySpec).getEncoded();

            // decode the SubjectPublicKeyInfo data structure to the pki object
            SubjectPublicKeyInfo pki;
            try
            {
                pki = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(encKey));
            }
            catch (IOException e)
            {
                throw new InvalidKeySpecException(e.toString());
            }

            try
            {
                // --- Build and return the actual key.
                ASN1Primitive innerType = pki.parsePublicKey();
                ASN1Sequence publicKey = (ASN1Sequence)innerType;

                // decode oidString (but we don't need it right now)
                String oidString = ((ASN1ObjectIdentifier)publicKey.getObjectAt(0))
                    .toString();

                // decode <n>
                BigInteger bigN = ((ASN1Integer)publicKey.getObjectAt(1)).getValue();
                int n = bigN.intValue();

                // decode <t>
                BigInteger bigT = ((ASN1Integer)publicKey.getObjectAt(2)).getValue();
                int t = bigT.intValue();

                // decode <matrixG>
                byte[] matrixG = ((ASN1OctetString)publicKey.getObjectAt(3)).getOctets();


                return new BCMcEliecePublicKey(new McEliecePublicKeySpec(OID, t, n,
                    matrixG));
            }
            catch (IOException cce)
            {
                throw new InvalidKeySpecException(
                    "Unable to decode X509EncodedKeySpec: "
                        + cce.getMessage());
            }
        }

        throw new InvalidKeySpecException("Unsupported key specification: "
            + keySpec.getClass() + ".");
    }

    /**
     * Converts, if possible, a key specification into a
     * {@link BCMcEliecePrivateKey}. Currently, the following key specifications
     * are supported: {@link McEliecePrivateKeySpec},
     * {@link PKCS8EncodedKeySpec}.
     *
     * @param keySpec the key specification
     * @return the McEliece private key
     * @throws InvalidKeySpecException if the KeySpec is not supported.
     */
    public PrivateKey generatePrivate(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof McEliecePrivateKeySpec)
        {
            return new BCMcEliecePrivateKey((McEliecePrivateKeySpec)keySpec);
        }
        else if (keySpec instanceof PKCS8EncodedKeySpec)
        {
            // get the DER-encoded Key according to PKCS#8 from the spec
            byte[] encKey = ((PKCS8EncodedKeySpec)keySpec).getEncoded();

            // decode the PKCS#8 data structure to the pki object
            PrivateKeyInfo pki;

            try
            {
                pki = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(encKey));
            }
            catch (IOException e)
            {
                throw new InvalidKeySpecException("Unable to decode PKCS8EncodedKeySpec: " + e);
            }

            try
            {
                ASN1Primitive innerType = pki.parsePrivateKey().toASN1Primitive();

                // build and return the actual key
                ASN1Sequence privKey = (ASN1Sequence)innerType;

                // decode oidString (but we don't need it right now)
                String oidString = ((ASN1ObjectIdentifier)privKey.getObjectAt(0))
                    .toString();

                // decode <n>
                BigInteger bigN = ((ASN1Integer)privKey.getObjectAt(1)).getValue();
                int n = bigN.intValue();

                // decode <k>
                BigInteger bigK = ((ASN1Integer)privKey.getObjectAt(2)).getValue();
                int k = bigK.intValue();

                // decode <fieldPoly>
                byte[] encFieldPoly = ((ASN1OctetString)privKey.getObjectAt(3))
                    .getOctets();
                // decode <goppaPoly>
                byte[] encGoppaPoly = ((ASN1OctetString)privKey.getObjectAt(4))
                    .getOctets();

                // decode <sInv>
                byte[] encSInv = ((ASN1OctetString)privKey.getObjectAt(5)).getOctets();
                // decode <p1>
                byte[] encP1 = ((ASN1OctetString)privKey.getObjectAt(6)).getOctets();
                // decode <p2>
                byte[] encP2 = ((ASN1OctetString)privKey.getObjectAt(7)).getOctets();

                //decode <h>
                byte[] encH = ((ASN1OctetString)privKey.getObjectAt(8)).getOctets();

                // decode <qInv>
                ASN1Sequence qSeq = (ASN1Sequence)privKey.getObjectAt(9);
                byte[][] encQInv = new byte[qSeq.size()][];
                for (int i = 0; i < qSeq.size(); i++)
                {
                    encQInv[i] = ((ASN1OctetString)qSeq.getObjectAt(i)).getOctets();
                }

                return new BCMcEliecePrivateKey(new McEliecePrivateKeySpec(OID, n, k,
                    encFieldPoly, encGoppaPoly, encSInv, encP1, encP2,
                    encH, encQInv));

            }
            catch (IOException cce)
            {
                throw new InvalidKeySpecException(
                    "Unable to decode PKCS8EncodedKeySpec.");
            }
        }

        throw new InvalidKeySpecException("Unsupported key specification: "
            + keySpec.getClass() + ".");
    }

    /**
     * Converts, if possible, a given key into a key specification. Currently,
     * the following key specifications are supported:
     * <ul>
     * <li>for McEliecePublicKey: {@link X509EncodedKeySpec},
     * {@link McEliecePublicKeySpec}</li>
     * <li>for McEliecePrivateKey: {@link PKCS8EncodedKeySpec},
     * {@link McEliecePrivateKeySpec}</li>.
     * </ul>
     *
     * @param key     the key
     * @param keySpec the key specification
     * @return the specification of the McEliece key
     * @throws InvalidKeySpecException if the key type or the key specification is not
     * supported.
     * @see BCMcEliecePrivateKey
     * @see McEliecePrivateKeySpec
     * @see BCMcEliecePublicKey
     * @see McEliecePublicKeySpec
     */
    public KeySpec getKeySpec(Key key, Class keySpec)
        throws InvalidKeySpecException
    {
        if (key instanceof BCMcEliecePrivateKey)
        {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
            else if (McEliecePrivateKeySpec.class.isAssignableFrom(keySpec))
            {
                BCMcEliecePrivateKey privKey = (BCMcEliecePrivateKey)key;
                return new McEliecePrivateKeySpec(OID, privKey.getN(), privKey
                    .getK(), privKey.getField(), privKey.getGoppaPoly(),
                    privKey.getSInv(), privKey.getP1(), privKey.getP2(),
                    privKey.getH(), privKey.getQInv());
            }
        }
        else if (key instanceof BCMcEliecePublicKey)
        {
            if (X509EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new X509EncodedKeySpec(key.getEncoded());
            }
            else if (McEliecePublicKeySpec.class.isAssignableFrom(keySpec))
            {
                BCMcEliecePublicKey pubKey = (BCMcEliecePublicKey)key;
                return new McEliecePublicKeySpec(OID, pubKey.getN(), pubKey.getT(),
                    pubKey.getG());
            }
        }
        else
        {
            throw new InvalidKeySpecException("Unsupported key type: "
                + key.getClass() + ".");
        }

        throw new InvalidKeySpecException("Unknown key specification: "
            + keySpec + ".");
    }

    /**
     * Translates a key into a form known by the FlexiProvider. Currently, only
     * the following "source" keys are supported: {@link BCMcEliecePrivateKey},
     * {@link BCMcEliecePublicKey}.
     *
     * @param key the key
     * @return a key of a known key type
     * @throws InvalidKeyException if the key type is not supported.
     */
    public Key translateKey(Key key)
        throws InvalidKeyException
    {
        if ((key instanceof BCMcEliecePrivateKey)
            || (key instanceof BCMcEliecePublicKey))
        {
            return key;
        }
        throw new InvalidKeyException("Unsupported key type.");

    }

    public PublicKey generatePublic(SubjectPublicKeyInfo pki)
        throws InvalidKeySpecException
    {
        // get the inner type inside the BIT STRING
        try
        {
            ASN1Primitive innerType = pki.parsePublicKey();
            McEliecePublicKey key = McEliecePublicKey.getInstance(innerType);
            return new BCMcEliecePublicKey(key.getOID().getId(), key.getN(), key.getT(), key.getG());
        }
        catch (IOException cce)
        {
            throw new InvalidKeySpecException("Unable to decode X509EncodedKeySpec");
        }
    }

    public PrivateKey generatePrivate(PrivateKeyInfo pki)
        throws InvalidKeySpecException
    {
        // get the inner type inside the BIT STRING
        try
        {
            ASN1Primitive innerType = pki.parsePrivateKey().toASN1Primitive();
            McEliecePrivateKey key = McEliecePrivateKey.getInstance(innerType);
            return new BCMcEliecePrivateKey(key.getOID().getId(), key.getN(), key.getK(), key.getField(), key.getGoppaPoly(), key.getSInv(), key.getP1(), key.getP2(), key.getH(), key.getQInv());
        }
        catch (IOException cce)
        {
            throw new InvalidKeySpecException("Unable to decode PKCS8EncodedKeySpec");
        }
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected KeySpec engineGetKeySpec(Key key, Class tClass)
        throws InvalidKeySpecException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected Key engineTranslateKey(Key key)
        throws InvalidKeyException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
