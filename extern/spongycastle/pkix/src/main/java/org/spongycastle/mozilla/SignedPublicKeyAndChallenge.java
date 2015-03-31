package org.spongycastle.mozilla;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.mozilla.PublicKeyAndChallenge;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * This is designed to parse the SignedPublicKeyAndChallenge created by the
 * KEYGEN tag included by Mozilla based browsers.
 *  <pre>
 *  PublicKeyAndChallenge ::= SEQUENCE {
 *    spki SubjectPublicKeyInfo,
 *    challenge IA5STRING
 *  }
 *
 *  SignedPublicKeyAndChallenge ::= SEQUENCE {
 *    publicKeyAndChallenge PublicKeyAndChallenge,
 *    signatureAlgorithm AlgorithmIdentifier,
 *    signature BIT STRING
 *  }
 *  </pre>
 */
public class SignedPublicKeyAndChallenge
    extends ASN1Object
{
    private static ASN1Sequence toDERSequence(byte[]  bytes)
    {
        try
        {
            ByteArrayInputStream    bIn = new ByteArrayInputStream(bytes);
            ASN1InputStream         aIn = new ASN1InputStream(bIn);

            return (ASN1Sequence)aIn.readObject();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("badly encoded request");
        }
    }

    private ASN1Sequence          spkacSeq;
    private PublicKeyAndChallenge pkac;
    private AlgorithmIdentifier   signatureAlgorithm;
    private DERBitString          signature;

    public SignedPublicKeyAndChallenge(byte[] bytes)
    {
        spkacSeq = toDERSequence(bytes);
        pkac = PublicKeyAndChallenge.getInstance(spkacSeq.getObjectAt(0));
        signatureAlgorithm = 
            AlgorithmIdentifier.getInstance(spkacSeq.getObjectAt(1));
        signature = (DERBitString)spkacSeq.getObjectAt(2);
    }

    public ASN1Primitive toASN1Primitive()
    {
        return spkacSeq;
    }

    public PublicKeyAndChallenge getPublicKeyAndChallenge()
    {
        return pkac;
    }

    public boolean verify()
        throws NoSuchAlgorithmException, SignatureException, 
               NoSuchProviderException, InvalidKeyException
    {
        return verify(null);
    }

    public boolean verify(String provider)
        throws NoSuchAlgorithmException, SignatureException, 
               NoSuchProviderException, InvalidKeyException
    {
        Signature sig = null;
        if (provider == null)
        {
            sig = Signature.getInstance(signatureAlgorithm.getAlgorithm().getId());
        }
        else
        {
            sig = Signature.getInstance(signatureAlgorithm.getAlgorithm().getId(), provider);
        }
        PublicKey pubKey = this.getPublicKey(provider);
        sig.initVerify(pubKey);
        try
        {
            DERBitString pkBytes = new DERBitString(pkac);
            sig.update(pkBytes.getBytes());

            return sig.verify(signature.getBytes());
        }
        catch (Exception e)
        {
            throw new InvalidKeyException("error encoding public key");
        }
    }

    public PublicKey getPublicKey(String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException, 
               InvalidKeyException
    {
        SubjectPublicKeyInfo subjectPKInfo = pkac.getSubjectPublicKeyInfo();
        try
        {
            DERBitString bStr = new DERBitString(subjectPKInfo);
            X509EncodedKeySpec xspec = new X509EncodedKeySpec(bStr.getBytes());
            

            AlgorithmIdentifier keyAlg = subjectPKInfo.getAlgorithm();

            KeyFactory factory =
                KeyFactory.getInstance(keyAlg.getAlgorithm().getId(),provider);

            return factory.generatePublic(xspec);
                           
        }
        catch (Exception e)
        {
            throw new InvalidKeyException("error encoding public key");
        }
    }
}
