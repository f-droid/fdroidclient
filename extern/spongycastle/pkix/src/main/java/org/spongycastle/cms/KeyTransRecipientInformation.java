package org.spongycastle.cms;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.cms.KeyTransRecipientInfo;
import org.spongycastle.asn1.cms.RecipientIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;

/**
 * the KeyTransRecipientInformation class for a recipient who has been sent a secret
 * key encrypted using their public key that needs to be used to
 * extract the message.
 */
public class KeyTransRecipientInformation
    extends RecipientInformation
{
    private KeyTransRecipientInfo info;

    KeyTransRecipientInformation(
        KeyTransRecipientInfo   info,
        AlgorithmIdentifier     messageAlgorithm,
        CMSSecureReadable       secureReadable,
        AuthAttributesProvider  additionalData)
    {
        super(info.getKeyEncryptionAlgorithm(), messageAlgorithm, secureReadable, additionalData);

        this.info = info;

        RecipientIdentifier r = info.getRecipientIdentifier();

        if (r.isTagged())
        {
            ASN1OctetString octs = ASN1OctetString.getInstance(r.getId());

            rid = new KeyTransRecipientId(octs.getOctets());
        }
        else
        {
            IssuerAndSerialNumber   iAnds = IssuerAndSerialNumber.getInstance(r.getId());

            rid = new KeyTransRecipientId(iAnds.getName(), iAnds.getSerialNumber().getValue());
        }
    }

    protected RecipientOperator getRecipientOperator(Recipient recipient)
        throws CMSException
    {
        return ((KeyTransRecipient)recipient).getRecipientOperator(keyEncAlg, messageAlgorithm, info.getEncryptedKey().getOctets());
    }
}
