package org.spongycastle.jcajce.provider.asymmetric.x509;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Null;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.RSASSAPSSparams;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;

class X509SignatureUtil
{
    private static final ASN1Null       derNull = new DERNull();
    
    static void setSignatureParameters(
        Signature signature,
        ASN1Encodable params)
        throws NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {
        if (params != null && !derNull.equals(params))
        {
            /*
            AlgorithmParameters  sigParams = AlgorithmParameters.getInstance(signature.getAlgorithm(), signature.getProvider());
            
            try
            {
                sigParams.init(params.getDERObject().getDEREncoded());
            }
            catch (IOException e)
            {
                throw new SignatureException("IOException decoding parameters: " + e.getMessage());
            }

            try
            {
                signature.setParameters(sigParams.getParameterSpec(PSSParameterSpec.class));
            }
            catch (GeneralSecurityException e)
            {
                throw new SignatureException("Exception extracting parameters: " + e.getMessage());
            }
            */
        }
    }
    
    static String getSignatureName(
        AlgorithmIdentifier sigAlgId) 
    {
        ASN1Encodable params = sigAlgId.getParameters();
        
        if (params != null && !derNull.equals(params))
        {
            if (sigAlgId.getObjectId().equals(PKCSObjectIdentifiers.id_RSASSA_PSS))
            {
                RSASSAPSSparams rsaParams = RSASSAPSSparams.getInstance(params);
                
                return getDigestAlgName(rsaParams.getHashAlgorithm().getObjectId()) + "withRSAandMGF1";
            }
        }

        return sigAlgId.getObjectId().getId();
    }
    
    /**
     * Return the digest algorithm using one of the standard JCA string
     * representations rather the the algorithm identifier (if possible).
     */
    private static String getDigestAlgName(
        DERObjectIdentifier digestAlgOID)
    {
        if (PKCSObjectIdentifiers.md5.equals(digestAlgOID))
        {
            return "MD5";
        }
        else if (OIWObjectIdentifiers.idSHA1.equals(digestAlgOID))
        {
            return "SHA1";
        }
        else if (NISTObjectIdentifiers.id_sha224.equals(digestAlgOID))
        {
            return "SHA224";
        }
        else if (NISTObjectIdentifiers.id_sha256.equals(digestAlgOID))
        {
            return "SHA256";
        }
        else if (NISTObjectIdentifiers.id_sha384.equals(digestAlgOID))
        {
            return "SHA384";
        }
        else if (NISTObjectIdentifiers.id_sha512.equals(digestAlgOID))
        {
            return "SHA512";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd128.equals(digestAlgOID))
        {
            return "RIPEMD128";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd160.equals(digestAlgOID))
        {
            return "RIPEMD160";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd256.equals(digestAlgOID))
        {
            return "RIPEMD256";
        }
        else if (CryptoProObjectIdentifiers.gostR3411.equals(digestAlgOID))
        {
            return "GOST3411";
        }
        else
        {
            return digestAlgOID.getId();            
        }
    }
}
