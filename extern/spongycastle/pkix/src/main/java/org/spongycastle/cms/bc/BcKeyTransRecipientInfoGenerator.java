package org.spongycastle.cms.bc;

import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.KeyTransRecipientInfoGenerator;
import org.spongycastle.operator.bc.BcAsymmetricKeyWrapper;

public abstract class BcKeyTransRecipientInfoGenerator
    extends KeyTransRecipientInfoGenerator
{
    public BcKeyTransRecipientInfoGenerator(X509CertificateHolder recipientCert, BcAsymmetricKeyWrapper wrapper)
    {
        super(new IssuerAndSerialNumber(recipientCert.toASN1Structure()), wrapper);
    }

    public BcKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, BcAsymmetricKeyWrapper wrapper)
    {
        super(subjectKeyIdentifier, wrapper);
    }
}