package org.spongycastle.pkcs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.operator.OutputEncryptor;

/**
 * A class for creating EncryptedPrivateKeyInfo structures.
 * <pre>
 * EncryptedPrivateKeyInfo ::= SEQUENCE {
 *      encryptionAlgorithm AlgorithmIdentifier {{KeyEncryptionAlgorithms}},
 *      encryptedData EncryptedData
 * }
 *
 * EncryptedData ::= OCTET STRING
 *
 * KeyEncryptionAlgorithms ALGORITHM-IDENTIFIER ::= {
 *          ... -- For local profiles
 * }
 * </pre>
 */
public class PKCS8EncryptedPrivateKeyInfoBuilder
{
    private PrivateKeyInfo privateKeyInfo;

    public PKCS8EncryptedPrivateKeyInfoBuilder(PrivateKeyInfo privateKeyInfo)
    {
        this.privateKeyInfo = privateKeyInfo;
    }

    public PKCS8EncryptedPrivateKeyInfo build(
        OutputEncryptor encryptor)
    {
        try
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            OutputStream cOut = encryptor.getOutputStream(bOut);

            cOut.write(privateKeyInfo.getEncoded());

            cOut.close();

            return new PKCS8EncryptedPrivateKeyInfo(new EncryptedPrivateKeyInfo(encryptor.getAlgorithmIdentifier(), bOut.toByteArray()));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("cannot encode privateKeyInfo");
        }
    }
}
