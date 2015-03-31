package org.spongycastle.cms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.BEROctetString;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.SignedData;
import org.spongycastle.asn1.cms.SignerInfo;

/**
 * general class for generating a pkcs7-signature message.
 * <p>
 * A simple example of usage, generating a detached signature.
 *
 * <pre>
 *      List             certList = new ArrayList();
 *      CMSTypedData     msg = new CMSProcessableByteArray("Hello world!".getBytes());
 *
 *      certList.add(signCert);
 *
 *      Store           certs = new JcaCertStore(certList);
 *
 *      CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
 *      ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("SC").build(signKP.getPrivate());
 *
 *      gen.addSignerInfoGenerator(
 *                new JcaSignerInfoGeneratorBuilder(
 *                     new JcaDigestCalculatorProviderBuilder().setProvider("SC").build())
 *                     .build(sha1Signer, signCert));
 *
 *      gen.addCertificates(certs);
 *
 *      CMSSignedData sigData = gen.generate(msg, false);
 * </pre>
 */
public class CMSSignedDataGenerator
    extends CMSSignedGenerator
{
    private List signerInfs = new ArrayList();

    /**
     * base constructor
     */
    public CMSSignedDataGenerator()
    {
    }

    /**
     * Generate a CMS Signed Data object carrying a detached CMS signature.
     *
     * @param content the content to be signed.
     */
    public CMSSignedData generate(
        CMSTypedData content)
        throws CMSException
    {
        return generate(content, false);
    }

    /**
     * Generate a CMS Signed Data object which can be carrying a detached CMS signature, or have encapsulated data, depending on the value
     * of the encapsulated parameter.
     *
     * @param content the content to be signed.
     * @param encapsulate true if the content should be encapsulated in the signature, false otherwise.
     */
    public CMSSignedData generate(
        // FIXME Avoid accessing more than once to support CMSProcessableInputStream
        CMSTypedData content,
        boolean encapsulate)
        throws CMSException
    {
        if (!signerInfs.isEmpty())
        {
            throw new IllegalStateException("this method can only be used with SignerInfoGenerator");
        }

                // TODO
//        if (signerInfs.isEmpty())
//        {
//            /* RFC 3852 5.2
//             * "In the degenerate case where there are no signers, the
//             * EncapsulatedContentInfo value being "signed" is irrelevant.  In this
//             * case, the content type within the EncapsulatedContentInfo value being
//             * "signed" MUST be id-data (as defined in section 4), and the content
//             * field of the EncapsulatedContentInfo value MUST be omitted."
//             */
//            if (encapsulate)
//            {
//                throw new IllegalArgumentException("no signers, encapsulate must be false");
//            }
//            if (!DATA.equals(eContentType))
//            {
//                throw new IllegalArgumentException("no signers, eContentType must be id-data");
//            }
//        }
//
//        if (!DATA.equals(eContentType))
//        {
//            /* RFC 3852 5.3
//             * [The 'signedAttrs']...
//             * field is optional, but it MUST be present if the content type of
//             * the EncapsulatedContentInfo value being signed is not id-data.
//             */
//            // TODO signedAttrs must be present for all signers
//        }

        ASN1EncodableVector  digestAlgs = new ASN1EncodableVector();
        ASN1EncodableVector  signerInfos = new ASN1EncodableVector();

        digests.clear();  // clear the current preserved digest state

        //
        // add the precalculated SignerInfo objects.
        //
        for (Iterator it = _signers.iterator(); it.hasNext();)
        {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSSignedHelper.INSTANCE.fixAlgID(signer.getDigestAlgorithmID()));

            // TODO Verify the content type and calculated digest match the precalculated SignerInfo
            signerInfos.add(signer.toASN1Structure());
        }

        //
        // add the SignerInfo objects
        //
        ASN1ObjectIdentifier contentTypeOID = content.getContentType();

        ASN1OctetString octs = null;

        if (content != null)
        {
            ByteArrayOutputStream bOut = null;

            if (encapsulate)
            {
                bOut = new ByteArrayOutputStream();
            }

            OutputStream cOut = CMSUtils.attachSignersToOutputStream(signerGens, bOut);

            // Just in case it's unencapsulated and there are no signers!
            cOut = CMSUtils.getSafeOutputStream(cOut);

            try
            {
                content.write(cOut);

                cOut.close();
            }
            catch (IOException e)
            {
                throw new CMSException("data processing exception: " + e.getMessage(), e);
            }

            if (encapsulate)
            {
                octs = new BEROctetString(bOut.toByteArray());
            }
        }

        for (Iterator it = signerGens.iterator(); it.hasNext();)
        {
            SignerInfoGenerator sGen = (SignerInfoGenerator)it.next();
            SignerInfo inf = sGen.generate(contentTypeOID);

            digestAlgs.add(inf.getDigestAlgorithm());
            signerInfos.add(inf);

            byte[] calcDigest = sGen.getCalculatedDigest();

            if (calcDigest != null)
            {
                digests.put(inf.getDigestAlgorithm().getAlgorithm().getId(), calcDigest);
            }
        }

        ASN1Set certificates = null;

        if (certs.size() != 0)
        {
            certificates = CMSUtils.createBerSetFromList(certs);
        }

        ASN1Set certrevlist = null;

        if (crls.size() != 0)
        {
            certrevlist = CMSUtils.createBerSetFromList(crls);
        }

        ContentInfo encInfo = new ContentInfo(contentTypeOID, octs);

        SignedData  sd = new SignedData(
                                 new DERSet(digestAlgs),
                                 encInfo,
                                 certificates,
                                 certrevlist,
                                 new DERSet(signerInfos));

        ContentInfo contentInfo = new ContentInfo(
            CMSObjectIdentifiers.signedData, sd);

        return new CMSSignedData(content, contentInfo);
    }

    /**
     * generate a set of one or more SignerInformation objects representing counter signatures on
     * the passed in SignerInformation object.
     *
     * @param signer the signer to be countersigned
     * @return a store containing the signers.
     */
    public SignerInformationStore generateCounterSigners(SignerInformation signer)
        throws CMSException
    {
        return this.generate(new CMSProcessableByteArray(null, signer.getSignature()), false).getSignerInfos();
    }
}

