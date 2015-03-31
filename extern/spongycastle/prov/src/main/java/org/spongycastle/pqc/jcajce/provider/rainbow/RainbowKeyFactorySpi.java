package org.spongycastle.pqc.jcajce.provider.rainbow;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import org.spongycastle.pqc.asn1.RainbowPrivateKey;
import org.spongycastle.pqc.asn1.RainbowPublicKey;
import org.spongycastle.pqc.jcajce.spec.RainbowPrivateKeySpec;
import org.spongycastle.pqc.jcajce.spec.RainbowPublicKeySpec;


/**
 * This class transforms Rainbow keys and Rainbow key specifications.
 *
 * @see BCRainbowPublicKey
 * @see RainbowPublicKeySpec
 * @see BCRainbowPrivateKey
 * @see RainbowPrivateKeySpec
 */
public class RainbowKeyFactorySpi
    extends KeyFactorySpi
    implements AsymmetricKeyInfoConverter
{
    /**
     * Converts, if possible, a key specification into a
     * {@link BCRainbowPrivateKey}. Currently, the following key specifications
     * are supported: {@link RainbowPrivateKeySpec}, {@link PKCS8EncodedKeySpec}.
     * <p/>
     * <p/>
     * <p/>
     * The ASN.1 definition of the key structure is
     * <p/>
     * <pre>
     *   RainbowPrivateKey ::= SEQUENCE {
     *     oid        OBJECT IDENTIFIER         -- OID identifying the algorithm
     *     A1inv      SEQUENCE OF OCTET STRING  -- inversed matrix of L1
     *     b1         OCTET STRING              -- translation vector of L1
     *     A2inv      SEQUENCE OF OCTET STRING  -- inversed matrix of L2
     *     b2         OCTET STRING              -- translation vector of L2
     *     vi         OCTET STRING              -- num of elmts in each Set S
     *     layers     SEQUENCE OF Layer         -- layers of F
     *   }
     *
     *   Layer             ::= SEQUENCE OF Poly
     *   Poly              ::= SEQUENCE {
     *     alpha      SEQUENCE OF OCTET STRING
     *     beta       SEQUENCE OF OCTET STRING
     *     gamma      OCTET STRING
     *     eta        OCTET
     *   }
     * </pre>
     * <p/>
     * <p/>
     *
     * @param keySpec the key specification
     * @return the Rainbow private key
     * @throws InvalidKeySpecException if the KeySpec is not supported.
     */
    public PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof RainbowPrivateKeySpec)
        {
            return new BCRainbowPrivateKey((RainbowPrivateKeySpec)keySpec);
        }
        else if (keySpec instanceof PKCS8EncodedKeySpec)
        {
            // get the DER-encoded Key according to PKCS#8 from the spec
            byte[] encKey = ((PKCS8EncodedKeySpec)keySpec).getEncoded();

            try
            {
                return generatePrivate(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(encKey)));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unsupported key specification: "
            + keySpec.getClass() + ".");
    }

    /**
     * Converts, if possible, a key specification into a
     * {@link BCRainbowPublicKey}. Currently, the following key specifications are
     * supported:{@link X509EncodedKeySpec}.
     * <p/>
     * <p/>
     * <p/>
     * The ASN.1 definition of a public key's structure is
     * <p/>
     * <pre>
     *    RainbowPublicKey ::= SEQUENCE {
     *      oid            OBJECT IDENTIFIER        -- OID identifying the algorithm
     *      docLength      Integer                  -- length of signable msg
     *      coeffquadratic SEQUENCE OF OCTET STRING -- quadratic (mixed) coefficients
     *      coeffsingular  SEQUENCE OF OCTET STRING -- singular coefficients
     *      coeffscalar       OCTET STRING             -- scalar coefficients
     *       }
     * </pre>
     * <p/>
     * <p/>
     *
     * @param keySpec the key specification
     * @return the Rainbow public key
     * @throws InvalidKeySpecException if the KeySpec is not supported.
     */
    public PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof RainbowPublicKeySpec)
        {
            return new BCRainbowPublicKey((RainbowPublicKeySpec)keySpec);
        }
        else if (keySpec instanceof X509EncodedKeySpec)
        {
            // get the DER-encoded Key according to X.509 from the spec
            byte[] encKey = ((X509EncodedKeySpec)keySpec).getEncoded();

            // decode the SubjectPublicKeyInfo data structure to the pki object
            try
            {
                return generatePublic(SubjectPublicKeyInfo.getInstance(encKey));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unknown key specification: " + keySpec + ".");
    }

    /**
     * Converts a given key into a key specification, if possible. Currently the
     * following specs are supported:
     * <ul>
     * <li>for RainbowPublicKey: X509EncodedKeySpec, RainbowPublicKeySpec
     * <li>for RainbowPrivateKey: PKCS8EncodedKeySpec, RainbowPrivateKeySpec
     * </ul>
     *
     * @param key     the key
     * @param keySpec the key specification
     * @return the specification of the CMSS key
     * @throws InvalidKeySpecException if the key type or key specification is not supported.
     */
    public final KeySpec engineGetKeySpec(Key key, Class keySpec)
        throws InvalidKeySpecException
    {
        if (key instanceof BCRainbowPrivateKey)
        {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
            else if (RainbowPrivateKeySpec.class.isAssignableFrom(keySpec))
            {
                BCRainbowPrivateKey privKey = (BCRainbowPrivateKey)key;
                return new RainbowPrivateKeySpec(privKey.getInvA1(), privKey
                    .getB1(), privKey.getInvA2(), privKey.getB2(), privKey
                    .getVi(), privKey.getLayers());
            }
        }
        else if (key instanceof BCRainbowPublicKey)
        {
            if (X509EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new X509EncodedKeySpec(key.getEncoded());
            }
            else if (RainbowPublicKeySpec.class.isAssignableFrom(keySpec))
            {
                BCRainbowPublicKey pubKey = (BCRainbowPublicKey)key;
                return new RainbowPublicKeySpec(pubKey.getDocLength(), pubKey
                    .getCoeffQuadratic(), pubKey.getCoeffSingular(), pubKey
                    .getCoeffScalar());
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
     * Translates a key into a form known by the FlexiProvider. Currently the
     * following key types are supported: RainbowPrivateKey, RainbowPublicKey.
     *
     * @param key the key
     * @return a key of a known key type
     * @throws InvalidKeyException if the key is not supported.
     */
    public final Key engineTranslateKey(Key key)
        throws InvalidKeyException
    {
        if (key instanceof BCRainbowPrivateKey || key instanceof BCRainbowPublicKey)
        {
            return key;
        }

        throw new InvalidKeyException("Unsupported key type");
    }

    public PrivateKey generatePrivate(PrivateKeyInfo keyInfo)
        throws IOException
    {
        RainbowPrivateKey pKey = RainbowPrivateKey.getInstance(keyInfo.parsePrivateKey());

        return new BCRainbowPrivateKey(pKey.getInvA1(), pKey.getB1(), pKey.getInvA2(), pKey.getB2(), pKey.getVi(), pKey.getLayers());
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo keyInfo)
        throws IOException
    {
        RainbowPublicKey pKey = RainbowPublicKey.getInstance(keyInfo.parsePublicKey());

        return new BCRainbowPublicKey(pKey.getDocLength(), pKey.getCoeffQuadratic(), pKey.getCoeffSingular(), pKey.getCoeffScalar());
    }
}
