package org.spongycastle.jce.provider;

import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.jcajce.provider.symmetric.util.PBE;

public class JCEPBEKey
    implements SecretKey
{
    String              algorithm;
    ASN1ObjectIdentifier oid;
    int                 type;
    int                 digest;
    int                 keySize;
    int                 ivSize;
    CipherParameters    param;
    PBEKeySpec          pbeKeySpec;
    boolean             tryWrong = false;

    /**
     * @param param
     */
    public JCEPBEKey(
        String              algorithm,
        ASN1ObjectIdentifier oid,
        int                 type,
        int                 digest,
        int                 keySize,
        int                 ivSize,
        PBEKeySpec          pbeKeySpec,
        CipherParameters    param)
    {
        this.algorithm = algorithm;
        this.oid = oid;
        this.type = type;
        this.digest = digest;
        this.keySize = keySize;
        this.ivSize = ivSize;
        this.pbeKeySpec = pbeKeySpec;
        this.param = param;
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public String getFormat()
    {
        return "RAW";
    }

    public byte[] getEncoded()
    {
        if (param != null)
        {
            KeyParameter    kParam;
            
            if (param instanceof ParametersWithIV)
            {
                kParam = (KeyParameter)((ParametersWithIV)param).getParameters();
            }
            else
            {
                kParam = (KeyParameter)param;
            }
            
            return kParam.getKey();
        }
        else
        {
            if (type == PBE.PKCS12)
            {
                return PBEParametersGenerator.PKCS12PasswordToBytes(pbeKeySpec.getPassword());
            }
            else
            {   
                return PBEParametersGenerator.PKCS5PasswordToBytes(pbeKeySpec.getPassword());
            }
        }
    }
    
    int getType()
    {
        return type;
    }
    
    int getDigest()
    {
        return digest;
    }
    
    int getKeySize()
    {
        return keySize;
    }
    
    int getIvSize()
    {
        return ivSize;
    }
    
    CipherParameters getParam()
    {
        return param;
    }
    
    /**
     * these should never be called.
     */
    int getIterationCount()
    {
        return 0;
    }
    
    byte[] getSalt()
    {
        return null;
    }
    
    /**
     * Return the object identifier associated with this algorithm
     * 
     * @return the oid for this PBE key
     */
    public ASN1ObjectIdentifier getOID()
    {
        return oid;
    }
    
    void setTryWrongPKCS12Zero(boolean tryWrong)
    {
        this.tryWrong = tryWrong; 
    }
    
    boolean shouldTryWrongPKCS12()
    {
        return tryWrong;
    }
}
