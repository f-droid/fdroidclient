package org.spongycastle.cert.ocsp.jcajce;

import java.security.PublicKey;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.ocsp.OCSPException;
import org.spongycastle.cert.ocsp.RespID;
import org.spongycastle.operator.DigestCalculator;

public class JcaRespID
    extends RespID
{
    public JcaRespID(PublicKey pubKey, DigestCalculator digCalc)
        throws OCSPException
    {
        super(SubjectPublicKeyInfo.getInstance(pubKey.getEncoded()), digCalc);
    }
}
