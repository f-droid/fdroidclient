package org.spongycastle.openssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.operator.OutputEncryptor;
import org.spongycastle.util.io.pem.PemGenerationException;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemObjectGenerator;

public class PKCS8Generator
    implements PemObjectGenerator
{
    public static final ASN1ObjectIdentifier AES_128_CBC = NISTObjectIdentifiers.id_aes128_CBC;
    public static final ASN1ObjectIdentifier AES_192_CBC = NISTObjectIdentifiers.id_aes192_CBC;
    public static final ASN1ObjectIdentifier AES_256_CBC = NISTObjectIdentifiers.id_aes256_CBC;

    public static final ASN1ObjectIdentifier DES3_CBC = PKCSObjectIdentifiers.des_EDE3_CBC;

    public static final ASN1ObjectIdentifier PBE_SHA1_RC4_128 = PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4;
    public static final ASN1ObjectIdentifier PBE_SHA1_RC4_40 = PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4;
    public static final ASN1ObjectIdentifier PBE_SHA1_3DES = PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC;
    public static final ASN1ObjectIdentifier PBE_SHA1_2DES = PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC;
    public static final ASN1ObjectIdentifier PBE_SHA1_RC2_128 = PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC2_CBC;
    public static final ASN1ObjectIdentifier PBE_SHA1_RC2_40 = PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC;

    private PrivateKeyInfo key;
    private OutputEncryptor outputEncryptor;

    /**
     * Base constructor.
     */
    public PKCS8Generator(PrivateKeyInfo key, OutputEncryptor outputEncryptor)
    {
        this.key = key;
        this.outputEncryptor = outputEncryptor;
    }

    public PemObject generate()
        throws PemGenerationException
    {
        if (outputEncryptor != null)
        {
            return generate(key, outputEncryptor);
        }
        else
        {
            return generate(key, null);
        }
    }

    private PemObject generate(PrivateKeyInfo key, OutputEncryptor encryptor)
        throws PemGenerationException
    {
        try
        {
            byte[] keyData = key.getEncoded();

            if (encryptor == null)
            {
                return new PemObject("PRIVATE KEY", keyData);
            }

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            OutputStream cOut = encryptor.getOutputStream(bOut);

            cOut.write(key.getEncoded());

            cOut.close();

            EncryptedPrivateKeyInfo info = new EncryptedPrivateKeyInfo(encryptor.getAlgorithmIdentifier(), bOut.toByteArray());

            return new PemObject("ENCRYPTED PRIVATE KEY", info.getEncoded());
        }
        catch (IOException e)
        {
            throw new PemGenerationException("unable to process encoded key data: " + e.getMessage(), e);
        }
    }
}
