package org.spongycastle.cms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.BEROctetString;
import org.spongycastle.asn1.BERSet;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.EncryptedContentInfo;
import org.spongycastle.asn1.cms.EnvelopedData;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OutputEncryptor;

/**
 * General class for generating a CMS enveloped-data message.
 *
 * A simple example of usage.
 *
 * <pre>
 *       CMSTypedData msg     = new CMSProcessableByteArray("Hello World!".getBytes());
 *
 *       CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
 *
 *       edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(recipientCert).setProvider("SC"));
 *
 *       CMSEnvelopedData ed = edGen.generate(
 *                                       msg,
 *                                       new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC)
 *                                              .setProvider("SC").build());
 *
 * </pre>
 */
public class CMSEnvelopedDataGenerator
    extends CMSEnvelopedGenerator
{
    /**
     * base constructor
     */
    public CMSEnvelopedDataGenerator()
    {
    }

    private CMSEnvelopedData doGenerate(
        CMSTypedData content,
        OutputEncryptor contentEncryptor)
        throws CMSException
    {
        if (!oldRecipientInfoGenerators.isEmpty())
        {
            throw new IllegalStateException("can only use addRecipientGenerator() with this method");
        }

        ASN1EncodableVector     recipientInfos = new ASN1EncodableVector();
        AlgorithmIdentifier     encAlgId;
        ASN1OctetString         encContent;

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        try
        {
            OutputStream cOut = contentEncryptor.getOutputStream(bOut);

            content.write(cOut);

            cOut.close();
        }
        catch (IOException e)
        {
            throw new CMSException("");
        }

        byte[] encryptedContent = bOut.toByteArray();

        encAlgId = contentEncryptor.getAlgorithmIdentifier();

        encContent = new BEROctetString(encryptedContent);

        GenericKey encKey = contentEncryptor.getKey();

        for (Iterator it = recipientInfoGenerators.iterator(); it.hasNext();)
        {
            RecipientInfoGenerator recipient = (RecipientInfoGenerator)it.next();

            recipientInfos.add(recipient.generate(encKey));
        }

        EncryptedContentInfo  eci = new EncryptedContentInfo(
                        content.getContentType(),
                        encAlgId,
                        encContent);

        ASN1Set unprotectedAttrSet = null;
        if (unprotectedAttributeGenerator != null)
        {
            AttributeTable attrTable = unprotectedAttributeGenerator.getAttributes(new HashMap());

            unprotectedAttrSet = new BERSet(attrTable.toASN1EncodableVector());
        }

        ContentInfo contentInfo = new ContentInfo(
                CMSObjectIdentifiers.envelopedData,
                new EnvelopedData(originatorInfo, new DERSet(recipientInfos), eci, unprotectedAttrSet));

        return new CMSEnvelopedData(contentInfo);
    }

    /**
     * generate an enveloped object that contains an CMS Enveloped Data
     * object using the given provider.
     *
     * @param content the content to be encrypted
     * @param contentEncryptor the symmetric key based encryptor to encrypt the content with.
     */
    public CMSEnvelopedData generate(
        CMSTypedData content,
        OutputEncryptor contentEncryptor)
        throws CMSException
    {
        return doGenerate(content, contentEncryptor);
    }
}
