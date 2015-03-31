package org.spongycastle.cms;

import java.io.IOException;

import org.spongycastle.asn1.cms.KEKIdentifier;
import org.spongycastle.asn1.cms.KEKRecipientInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;

/**
 * the RecipientInfo class for a recipient who has been sent a message
 * encrypted using a secret key known to the other side.
 */
public class KEKRecipientInformation
    extends RecipientInformation
{
    private KEKRecipientInfo      info;

    KEKRecipientInformation(
        KEKRecipientInfo        info,
        AlgorithmIdentifier     messageAlgorithm,
        CMSSecureReadable       secureReadable,
        AuthAttributesProvider  additionalData)
    {
        super(info.getKeyEncryptionAlgorithm(), messageAlgorithm, secureReadable, additionalData);

        this.info = info;

        KEKIdentifier kekId = info.getKekid();

        this.rid = new KEKRecipientId(kekId.getKeyIdentifier().getOctets());
    }

    protected RecipientOperator getRecipientOperator(Recipient recipient)
        throws CMSException, IOException
    {
        return ((KEKRecipient)recipient).getRecipientOperator(keyEncAlg, messageAlgorithm, info.getEncryptedKey().getOctets());
    }
}
