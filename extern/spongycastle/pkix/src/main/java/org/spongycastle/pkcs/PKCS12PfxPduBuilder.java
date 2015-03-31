package org.spongycastle.pkcs;

import java.io.IOException;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.DLSequence;
import org.spongycastle.asn1.pkcs.AuthenticatedSafe;
import org.spongycastle.asn1.pkcs.ContentInfo;
import org.spongycastle.asn1.pkcs.MacData;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.Pfx;
import org.spongycastle.cms.CMSEncryptedDataGenerator;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.operator.OutputEncryptor;

/**
 * A builder for the PKCS#12 Pfx key and certificate store.
 * <p>
 * For example: you can build a basic key store for the user owning privKey as follows:
 * </p>
 * <pre>
 *      X509Certificate[] chain = ....
 *      PublicKey         pubKey = ....
 *      PrivateKey        privKey = ....
 *      JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
 *
 *      PKCS12SafeBagBuilder taCertBagBuilder = new JcaPKCS12SafeBagBuilder(chain[2]);
 *
 *      taCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("Bouncy Primary Certificate"));
 *
 *      PKCS12SafeBagBuilder caCertBagBuilder = new JcaPKCS12SafeBagBuilder(chain[1]);
 *
 *      caCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("Bouncy Intermediate Certificate"));
 *
 *      PKCS12SafeBagBuilder eeCertBagBuilder = new JcaPKCS12SafeBagBuilder(chain[0]);
 *
 *      eeCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("Eric's Key"));
 *      eeCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, extUtils.createSubjectKeyIdentifier(pubKey));
 *
 *      PKCS12SafeBagBuilder keyBagBuilder = new JcaPKCS12SafeBagBuilder(privKey, new BcPKCS12PBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, new CBCBlockCipher(new DESedeEngine())).build(passwd));
 *
 *      keyBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("Eric's Key"));
 *      keyBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, extUtils.createSubjectKeyIdentifier(pubKey));
 *
 *      //
 *      // construct the actual key store
 *      //
 *      PKCS12PfxPduBuilder pfxPduBuilder = new PKCS12PfxPduBuilder();
 *
 *      PKCS12SafeBag[] certs = new PKCS12SafeBag[3];
 *
 *      certs[0] = eeCertBagBuilder.build();
 *      certs[1] = caCertBagBuilder.build();
 *      certs[2] = taCertBagBuilder.build();
 *
 *      pfxPduBuilder.addEncryptedData(new BcPKCS12PBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC, new CBCBlockCipher(new RC2Engine())).build(passwd), certs);
 *
 *      pfxPduBuilder.addData(keyBagBuilder.build());
 *
 *      PKCS12PfxPdu pfx = pfxPduBuilder.build(new BcPKCS12MacCalculatorBuilder(), passwd);
 * </pre>
 *
 */
public class PKCS12PfxPduBuilder
{
    private ASN1EncodableVector dataVector = new ASN1EncodableVector();

    /**
     * Add a SafeBag that is to be included as is.
     *
     * @param data the SafeBag to add.
     * @return this builder.
     * @throws IOException
     */
    public PKCS12PfxPduBuilder addData(PKCS12SafeBag data)
        throws IOException
    {
        dataVector.add(new ContentInfo(PKCSObjectIdentifiers.data, new DEROctetString(new DLSequence(data.toASN1Structure()).getEncoded())));

        return this;
    }

    /**
     * Add a SafeBag that is to be wrapped in a EncryptedData object.
     *
     * @param dataEncryptor the encryptor to use for encoding the data.
     * @param data the SafeBag to include.
     * @return this builder.
     * @throws IOException if a issue occurs processing the data.
     */
    public PKCS12PfxPduBuilder addEncryptedData(OutputEncryptor dataEncryptor, PKCS12SafeBag data)
        throws IOException
    {
        return addEncryptedData(dataEncryptor, new DERSequence(data.toASN1Structure()));
    }

    /**
     * Add a set of SafeBags that are to be wrapped in a EncryptedData object.
     *
     * @param dataEncryptor the encryptor to use for encoding the data.
     * @param data the SafeBags to include.
     * @return this builder.
     * @throws IOException if a issue occurs processing the data.
     */
    public PKCS12PfxPduBuilder addEncryptedData(OutputEncryptor dataEncryptor, PKCS12SafeBag[] data)
        throws IOException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        for (int i = 0; i != data.length; i++)
        {
            v.add(data[i].toASN1Structure());
        }

        return addEncryptedData(dataEncryptor, new DLSequence(v));
    }

    private PKCS12PfxPduBuilder addEncryptedData(OutputEncryptor dataEncryptor, ASN1Sequence data)
        throws IOException
    {
        CMSEncryptedDataGenerator envGen = new CMSEncryptedDataGenerator();

        try
        {
            dataVector.add(envGen.generate(new CMSProcessableByteArray(data.getEncoded()), dataEncryptor).toASN1Structure());
        }
        catch (CMSException e)
        {
            throw new PKCSIOException(e.getMessage(), e.getCause());
        }

        return this;
    }

    /**
     * Build the Pfx structure, protecting it with a MAC calculated against the passed in password.
     *
     * @param macCalcBuilder a builder for a PKCS12 mac calculator.
     * @param password the password to use.
     * @return a Pfx object.
     * @throws PKCSException on a encoding or processing error.
     */
    public PKCS12PfxPdu build(PKCS12MacCalculatorBuilder macCalcBuilder, char[] password)
        throws PKCSException
    {
        AuthenticatedSafe auth = AuthenticatedSafe.getInstance(new DLSequence(dataVector));
        byte[]            encAuth;

        try
        {
            encAuth = auth.getEncoded();
        }
        catch (IOException e)
        {
            throw new PKCSException("unable to encode AuthenticatedSafe: " + e.getMessage(), e);
        }

        ContentInfo       mainInfo = new ContentInfo(PKCSObjectIdentifiers.data, new DEROctetString(encAuth));
        MacData           mData = null;

        if (macCalcBuilder != null)
        {
            MacDataGenerator mdGen = new MacDataGenerator(macCalcBuilder);

            mData = mdGen.build(password, encAuth);
        }

        //
        // output the Pfx
        //
        Pfx pfx = new Pfx(mainInfo, mData);

        return new PKCS12PfxPdu(pfx);
    }
}
