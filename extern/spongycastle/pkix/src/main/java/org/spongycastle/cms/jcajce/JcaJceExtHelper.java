package org.spongycastle.cms.jcajce;

import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.operator.SymmetricKeyUnwrapper;
import org.spongycastle.operator.jcajce.JceAsymmetricKeyUnwrapper;

public interface JcaJceExtHelper
    extends JcaJceHelper
{
    JceAsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey);

    SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey);
}
