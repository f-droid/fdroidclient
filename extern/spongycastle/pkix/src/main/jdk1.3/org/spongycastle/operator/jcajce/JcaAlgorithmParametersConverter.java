package org.spongycastle.operator.jcajce;


import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.RSAESOAEPparams;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;

public class JcaAlgorithmParametersConverter
{
    public JcaAlgorithmParametersConverter()
    {
    }

    public AlgorithmIdentifier getAlgorithmIdentifier(ASN1ObjectIdentifier algId, AlgorithmParameters parameters)
        throws InvalidAlgorithmParameterException
    {
        try
        {
            ASN1Encodable params = ASN1Primitive.fromByteArray(parameters.getEncoded());

            return new AlgorithmIdentifier(algId, params);
        }
        catch (IOException e)
        {
            throw new InvalidAlgorithmParameterException("unable to encode parameters object: " + e.getMessage());
        }
    }

    public AlgorithmIdentifier getAlgorithmIdentifier(ASN1ObjectIdentifier algorithm, AlgorithmParameterSpec algorithmSpec)
        throws InvalidAlgorithmParameterException
    {
        throw new InvalidAlgorithmParameterException("unknown parameter spec passed.");
    }
}
