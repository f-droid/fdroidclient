package org.spongycastle.openssl;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;

public class PEMKeyPair
{
    private final SubjectPublicKeyInfo publicKeyInfo;
    private final PrivateKeyInfo privateKeyInfo;

    public PEMKeyPair(SubjectPublicKeyInfo publicKeyInfo, PrivateKeyInfo privateKeyInfo)
    {
        this.publicKeyInfo = publicKeyInfo;
        this.privateKeyInfo = privateKeyInfo;
    }

    public PrivateKeyInfo getPrivateKeyInfo()
    {
        return privateKeyInfo;
    }

    public SubjectPublicKeyInfo getPublicKeyInfo()
    {
        return publicKeyInfo;
    }
}
