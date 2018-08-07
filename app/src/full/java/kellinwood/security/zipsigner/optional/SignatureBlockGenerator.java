
package kellinwood.security.zipsigner.optional;

import kellinwood.security.zipsigner.KeySet;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SignatureBlockGenerator {

    /**
     * Sign the given content using the private and public keys from the keySet, and return the encoded CMS (PKCS#7) data.
     * Use of direct signature and DER encoding produces a block that is verifiable by Android recovery programs.
     */
    public static byte[] generate(KeySet keySet, byte[] content) {
        try {
            List certList = new ArrayList();
            CMSTypedData msg = new CMSProcessableByteArray(content);

            certList.add(keySet.getPublicKey());

            Store certs = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder(keySet.getSignatureAlgorithm()).setProvider("BC");
            ContentSigner sha1Signer = jcaContentSignerBuilder.build(keySet.getPrivateKey());

            JcaDigestCalculatorProviderBuilder jcaDigestCalculatorProviderBuilder = new JcaDigestCalculatorProviderBuilder().setProvider("BC");
            DigestCalculatorProvider digestCalculatorProvider = jcaDigestCalculatorProviderBuilder.build();

            JcaSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider);
            jcaSignerInfoGeneratorBuilder.setDirectSignature(true);
            SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(sha1Signer, keySet.getPublicKey());

            gen.addSignerInfoGenerator(signerInfoGenerator);

            gen.addCertificates(certs);

            CMSSignedData sigData = gen.generate(msg, false);
            return sigData.toASN1Structure().getEncoded("DER");

        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

}
