package org.spongycastle.x509.examples;

import java.security.cert.*;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.*;
import java.math.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.spongycastle.jce.PrincipalUtil;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.jce.provider.*;
import org.spongycastle.asn1.*;
import org.spongycastle.asn1.misc.MiscObjectIdentifiers;
import org.spongycastle.asn1.misc.NetscapeCertType;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.x509.*;

/**
 * A simple exmple that generates an attribute certificate.
 */
public class AttrCertExample
{
    static X509V1CertificateGenerator  v1CertGen = new X509V1CertificateGenerator();
    static X509V3CertificateGenerator  v3CertGen = new X509V3CertificateGenerator();
    
    /**
     * we generate the CA's certificate
     */
    public static X509Certificate createCaCert(
        PublicKey       pubKey,
        PrivateKey      privKey)
        throws Exception
    {
        //
        // signers name 
        //
        String  issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate";

        //
        // subjects name - the same as we are self signed.
        //
        String  subject = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate";

        //
        // create the certificate - version 1
        //

        v1CertGen.setSerialNumber(BigInteger.valueOf(10));
        v1CertGen.setIssuerDN(new X509Principal(issuer));
        v1CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
        v1CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)));
        v1CertGen.setSubjectDN(new X509Principal(subject));
        v1CertGen.setPublicKey(pubKey);
        v1CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

        X509Certificate cert = v1CertGen.generateX509Certificate(privKey);

        cert.checkValidity(new Date());

        cert.verify(pubKey);

        return cert;
    }
    
    /**
     * we generate a certificate signed by our CA's intermediate certficate
     */
    public static X509Certificate createClientCert(
        PublicKey       pubKey,
        PrivateKey      caPrivKey,
        PublicKey       caPubKey)
        throws Exception
    {
        //
        // issuer
        //
        String  issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate";

        //
        // subjects name table.
        //
        Hashtable                   attrs = new Hashtable();
        Vector                      order = new Vector();

        attrs.put(X509Principal.C, "AU");
        attrs.put(X509Principal.O, "The Legion of the Bouncy Castle");
        attrs.put(X509Principal.L, "Melbourne");
        attrs.put(X509Principal.CN, "Eric H. Echidna");
        attrs.put(X509Principal.EmailAddress, "feedback-crypto@bouncycastle.org");

        order.addElement(X509Principal.C);
        order.addElement(X509Principal.O);
        order.addElement(X509Principal.L);
        order.addElement(X509Principal.CN);
        order.addElement(X509Principal.EmailAddress);

        //
        // create the certificate - version 3
        //
        v3CertGen.reset();

        v3CertGen.setSerialNumber(BigInteger.valueOf(20));
        v3CertGen.setIssuerDN(new X509Principal(issuer));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)));
        v3CertGen.setSubjectDN(new X509Principal(order, attrs));
        v3CertGen.setPublicKey(pubKey);
        v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

        //
        // add the extensions
        //

        v3CertGen.addExtension(
            MiscObjectIdentifiers.netscapeCertType,
            false,
            new NetscapeCertType(NetscapeCertType.objectSigning | NetscapeCertType.smime));

        X509Certificate cert = v3CertGen.generateX509Certificate(caPrivKey);

        cert.checkValidity(new Date());

        cert.verify(caPubKey);

        return cert;
    }
    
    public static void main(String args[])
        throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());

        //
        // personal keys
        //
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(
            new BigInteger("b4a7e46170574f16a97082b22be58b6a2a629798419be12872a4bdba626cfae9900f76abfb12139dce5de56564fab2b6543165a040c606887420e33d91ed7ed7", 16),
            new BigInteger("11", 16));

        RSAPrivateCrtKeySpec privKeySpec = new RSAPrivateCrtKeySpec(
            new BigInteger("b4a7e46170574f16a97082b22be58b6a2a629798419be12872a4bdba626cfae9900f76abfb12139dce5de56564fab2b6543165a040c606887420e33d91ed7ed7", 16),
            new BigInteger("11", 16),
            new BigInteger("9f66f6b05410cd503b2709e88115d55daced94d1a34d4e32bf824d0dde6028ae79c5f07b580f5dce240d7111f7ddb130a7945cd7d957d1920994da389f490c89", 16),
            new BigInteger("c0a0758cdf14256f78d4708c86becdead1b50ad4ad6c5c703e2168fbf37884cb", 16),
            new BigInteger("f01734d7960ea60070f1b06f2bb81bfac48ff192ae18451d5e56c734a5aab8a5", 16),
            new BigInteger("b54bb9edff22051d9ee60f9351a48591b6500a319429c069a3e335a1d6171391", 16),
            new BigInteger("d3d83daf2a0cecd3367ae6f8ae1aeb82e9ac2f816c6fc483533d8297dd7884cd", 16),
            new BigInteger("b8f52fc6f38593dabb661d3f50f8897f8106eee68b1bce78a95b132b4e5b5d19", 16));

        //
        // ca keys
        //
        RSAPublicKeySpec caPubKeySpec = new RSAPublicKeySpec(
            new BigInteger("b259d2d6e627a768c94be36164c2d9fc79d97aab9253140e5bf17751197731d6f7540d2509e7b9ffee0a70a6e26d56e92d2edd7f85aba85600b69089f35f6bdbf3c298e05842535d9f064e6b0391cb7d306e0a2d20c4dfb4e7b49a9640bdea26c10ad69c3f05007ce2513cee44cfe01998e62b6c3637d3fc0391079b26ee36d5", 16),
            new BigInteger("11", 16));

        RSAPrivateCrtKeySpec   caPrivKeySpec = new RSAPrivateCrtKeySpec(
            new BigInteger("b259d2d6e627a768c94be36164c2d9fc79d97aab9253140e5bf17751197731d6f7540d2509e7b9ffee0a70a6e26d56e92d2edd7f85aba85600b69089f35f6bdbf3c298e05842535d9f064e6b0391cb7d306e0a2d20c4dfb4e7b49a9640bdea26c10ad69c3f05007ce2513cee44cfe01998e62b6c3637d3fc0391079b26ee36d5", 16),
            new BigInteger("11", 16),
            new BigInteger("92e08f83cc9920746989ca5034dcb384a094fb9c5a6288fcc4304424ab8f56388f72652d8fafc65a4b9020896f2cde297080f2a540e7b7ce5af0b3446e1258d1dd7f245cf54124b4c6e17da21b90a0ebd22605e6f45c9f136d7a13eaac1c0f7487de8bd6d924972408ebb58af71e76fd7b012a8d0e165f3ae2e5077a8648e619", 16),
            new BigInteger("f75e80839b9b9379f1cf1128f321639757dba514642c206bbbd99f9a4846208b3e93fbbe5e0527cc59b1d4b929d9555853004c7c8b30ee6a213c3d1bb7415d03", 16),
            new BigInteger("b892d9ebdbfc37e397256dd8a5d3123534d1f03726284743ddc6be3a709edb696fc40c7d902ed804c6eee730eee3d5b20bf6bd8d87a296813c87d3b3cc9d7947", 16),
            new BigInteger("1d1a2d3ca8e52068b3094d501c9a842fec37f54db16e9a67070a8b3f53cc03d4257ad252a1a640eadd603724d7bf3737914b544ae332eedf4f34436cac25ceb5", 16),
            new BigInteger("6c929e4e81672fef49d9c825163fec97c4b7ba7acb26c0824638ac22605d7201c94625770984f78a56e6e25904fe7db407099cad9b14588841b94f5ab498dded", 16),
            new BigInteger("dae7651ee69ad1d081ec5e7188ae126f6004ff39556bde90e0b870962fa7b926d070686d8244fe5a9aa709a95686a104614834b0ada4b10f53197a5cb4c97339", 16));

        //
        // set up the keys
        //
        KeyFactory          fact = KeyFactory.getInstance("RSA", "SC");
        PrivateKey          caPrivKey = fact.generatePrivate(caPrivKeySpec);
        PublicKey           caPubKey = fact.generatePublic(caPubKeySpec);
        PrivateKey          privKey = fact.generatePrivate(privKeySpec);
        PublicKey           pubKey = fact.generatePublic(pubKeySpec);

        //
        // note in this case we are using the CA certificate for both the client cetificate
        // and the attribute certificate. This is to make the vcode simpler to read, in practice
        // the CA for the attribute certificate should be different to that of the client certificate
        //
        X509Certificate     caCert = createCaCert(caPubKey, caPrivKey);
        X509Certificate     clientCert = createClientCert(pubKey, caPrivKey, caPubKey);

        // Einen neuen Attributzertifikatsgenerator instantiieren
        X509V2AttributeCertificateGenerator acGen = new X509V2AttributeCertificateGenerator();

        acGen.reset();

        /*
         * Holder setzen hier als IssuerSerial Issuer und Serial sind ein
         * eindeutiger Schl�ssel f�r ein Client Zertifikat!
         */

        acGen.setHolder(new AttributeCertificateHolder(clientCert));

        // Issuer setzen

        acGen.setIssuer(new AttributeCertificateIssuer(PrincipalUtil.getSubjectX509Principal(caCert)));

        // Serial Number (frei gew�hlt)
        acGen.setSerialNumber(new BigInteger("1"));

        // not Before
        acGen.setNotBefore(new Date(System.currentTimeMillis() - 50000));

        // not After
        acGen.setNotAfter(new Date(System.currentTimeMillis() + 50000));

        // signature Algorithmus
        acGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

        // Die eigentlichen Attribute
        GeneralName roleName = new GeneralName(GeneralName.rfc822Name, "DAU123456789");
        ASN1EncodableVector roleSyntax = new ASN1EncodableVector();
        roleSyntax.add(roleName);

        // roleSyntax OID: 2.5.24.72
        X509Attribute attributes = new X509Attribute("2.5.24.72",
                new DERSequence(roleSyntax));

        acGen.addAttribute(attributes);

        // Privaten Schluessel der CA lesen

        // Und noch signieren
        X509V2AttributeCertificate att = (X509V2AttributeCertificate)acGen
                .generateCertificate(caPrivKey, "SC");

        // Hier ist das Attributzertifikat fertig im Speicher, jetzt testen:

        // Holder

        AttributeCertificateHolder h = att.getHolder();
        if (h.match(clientCert))
        {
            System.out.println("Matches original client x509 cert");
        }

        // Issuer
        
        AttributeCertificateIssuer issuer = att.getIssuer();
        if (issuer.match(caCert))
        {
            System.out.println("Matches original ca x509 cert");
        }
        
        // Dates
        System.out.println("valid not before: " + att.getNotBefore());
        System.out.println("valid not before: " + att.getNotAfter());

        // Dates checken (bc wirft NotValid Exception wenn nicht g�ltig)

        try
        {
            att.checkValidity();
            att.checkValidity(new Date());
        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        // verify

        try
        {
            att.verify(caPubKey, "SC");
        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        // Attribute
        X509Attribute[] attribs = att.getAttributes();
        System.out.println("cert has " + attribs.length + " attributes:");
        for (int i = 0; i < attribs.length; i++)
        {
            X509Attribute a = attribs[i];
            System.out.println("OID: " + a.getOID());
            if (a.getOID().equals("2.5.24.72"))
            {
                System.out.println("rolesyntax read from cert!");
            }
        }
    }
}
