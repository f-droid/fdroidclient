package org.spongycastle.cms.jcajce;

import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.operator.SymmetricKeyUnwrapper;
import org.spongycastle.operator.jcajce.JceAsymmetricKeyUnwrapper;
import org.spongycastle.operator.jcajce.JceSymmetricKeyUnwrapper;

class NamedJcaJceExtHelper
    extends NamedJcaJceHelper
    implements JcaJceExtHelper
{
    public NamedJcaJceExtHelper(String providerName)
    {
        super(providerName);
    }

    public JceAsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey)
    {
        return new JceAsymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey).setProvider(providerName);
    }

    public SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey)
    {
        return new JceSymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey).setProvider(providerName);
    }
}