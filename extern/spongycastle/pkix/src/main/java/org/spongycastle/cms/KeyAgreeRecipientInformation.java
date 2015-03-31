package org.spongycastle.cms;

import java.io.IOException;
import java.util.List;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.cms.KeyAgreeRecipientIdentifier;
import org.spongycastle.asn1.cms.KeyAgreeRecipientInfo;
import org.spongycastle.asn1.cms.OriginatorIdentifierOrKey;
import org.spongycastle.asn1.cms.OriginatorPublicKey;
import org.spongycastle.asn1.cms.RecipientEncryptedKey;
import org.spongycastle.asn1.cms.RecipientKeyIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * the RecipientInfo class for a recipient who has been sent a message
 * encrypted using key agreement.
 */
public class KeyAgreeRecipientInformation
    extends RecipientInformation
{
    private KeyAgreeRecipientInfo info;
    private ASN1OctetString       encryptedKey;

    static void readRecipientInfo(List infos, KeyAgreeRecipientInfo info,
        AlgorithmIdentifier messageAlgorithm, CMSSecureReadable secureReadable, AuthAttributesProvider additionalData)
    {
        ASN1Sequence s = info.getRecipientEncryptedKeys();

        for (int i = 0; i < s.size(); ++i)
        {
            RecipientEncryptedKey id = RecipientEncryptedKey.getInstance(
                s.getObjectAt(i));

            RecipientId rid;

            KeyAgreeRecipientIdentifier karid = id.getIdentifier();
            IssuerAndSerialNumber iAndSN = karid.getIssuerAndSerialNumber();

            if (iAndSN != null)
            {
                rid = new KeyAgreeRecipientId(iAndSN.getName(), iAndSN.getSerialNumber().getValue());
            }
            else
            {
                RecipientKeyIdentifier rKeyID = karid.getRKeyID();

                // Note: 'date' and 'other' fields of RecipientKeyIdentifier appear to be only informational

                rid = new KeyAgreeRecipientId(rKeyID.getSubjectKeyIdentifier().getOctets());
            }

            infos.add(new KeyAgreeRecipientInformation(info, rid, id.getEncryptedKey(), messageAlgorithm,
                secureReadable, additionalData));
        }
    }

    KeyAgreeRecipientInformation(
        KeyAgreeRecipientInfo   info,
        RecipientId             rid,
        ASN1OctetString         encryptedKey,
        AlgorithmIdentifier     messageAlgorithm,
        CMSSecureReadable       secureReadable,
        AuthAttributesProvider  additionalData)
    {
        super(info.getKeyEncryptionAlgorithm(), messageAlgorithm, secureReadable, additionalData);

        this.info = info;
        this.rid = rid;
        this.encryptedKey = encryptedKey;
    }

    private SubjectPublicKeyInfo getSenderPublicKeyInfo(AlgorithmIdentifier recKeyAlgId,
        OriginatorIdentifierOrKey originator)
        throws CMSException, IOException
    {
        OriginatorPublicKey opk = originator.getOriginatorKey();
        if (opk != null)
        {
            return getPublicKeyInfoFromOriginatorPublicKey(recKeyAlgId, opk);
        }

        OriginatorId origID;

        IssuerAndSerialNumber iAndSN = originator.getIssuerAndSerialNumber();
        if (iAndSN != null)
        {
            origID = new OriginatorId(iAndSN.getName(), iAndSN.getSerialNumber().getValue());
        }
        else
        {
            SubjectKeyIdentifier ski = originator.getSubjectKeyIdentifier();

            origID = new OriginatorId(ski.getKeyIdentifier());
        }

        return getPublicKeyInfoFromOriginatorId(origID);
    }

    private SubjectPublicKeyInfo getPublicKeyInfoFromOriginatorPublicKey(AlgorithmIdentifier recKeyAlgId,
            OriginatorPublicKey originatorPublicKey)
    {
        SubjectPublicKeyInfo pubInfo = new SubjectPublicKeyInfo(
            recKeyAlgId,
            originatorPublicKey.getPublicKey().getBytes());

        return pubInfo;
    }

    private SubjectPublicKeyInfo getPublicKeyInfoFromOriginatorId(OriginatorId origID)
            throws CMSException
    {
        // TODO Support all alternatives for OriginatorIdentifierOrKey
        // see RFC 3852 6.2.2
        throw new CMSException("No support for 'originator' as IssuerAndSerialNumber or SubjectKeyIdentifier");
    }

    protected RecipientOperator getRecipientOperator(Recipient recipient)
        throws CMSException, IOException
    {
        KeyAgreeRecipient agreeRecipient = (KeyAgreeRecipient)recipient;
                AlgorithmIdentifier    recKeyAlgId = agreeRecipient.getPrivateKeyAlgorithmIdentifier();

        return ((KeyAgreeRecipient)recipient).getRecipientOperator(keyEncAlg, messageAlgorithm, getSenderPublicKeyInfo(recKeyAlgId,
                        info.getOriginator()), info.getUserKeyingMaterial(), encryptedKey.getOctets());
    }
}
