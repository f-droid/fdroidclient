package org.spongycastle.tsp;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.tsp.Accuracy;
import org.spongycastle.asn1.tsp.TSTInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.GeneralName;

public class TimeStampTokenInfo
{
    TSTInfo tstInfo;
    Date    genTime;
    
    TimeStampTokenInfo(TSTInfo tstInfo)
        throws TSPException, IOException
    {
        this.tstInfo = tstInfo;
        this.genTime = tstInfo.getGenTime().getDate();
    }

    public boolean isOrdered()
    {
        return tstInfo.getOrdering().isTrue();
    }

    public Accuracy getAccuracy()
    {
        return tstInfo.getAccuracy();
    }

    public Date getGenTime()
    {
        return genTime;
    }

    public GenTimeAccuracy getGenTimeAccuracy()
    {
        if (this.getAccuracy() != null)
        {
            return new GenTimeAccuracy(this.getAccuracy());
        }
        
        return null;
    }
    
    public ASN1ObjectIdentifier getPolicy()
    {
        return tstInfo.getPolicy();
    }
    
    public BigInteger getSerialNumber()
    {
        return tstInfo.getSerialNumber().getValue();
    }

    public GeneralName getTsa()
    {
        return tstInfo.getTsa();
    }

    /**
     * @return the nonce value, null if there isn't one.
     */
    public BigInteger getNonce()
    {
        if (tstInfo.getNonce() != null)
        {
            return tstInfo.getNonce().getValue();
        }

        return null;
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return tstInfo.getMessageImprint().getHashAlgorithm();
    }

    public ASN1ObjectIdentifier getMessageImprintAlgOID()
    {
        return tstInfo.getMessageImprint().getHashAlgorithm().getAlgorithm();
    }

    public byte[] getMessageImprintDigest()
    {
        return tstInfo.getMessageImprint().getHashedMessage();
    }

    public byte[] getEncoded() 
        throws IOException
    {
        return tstInfo.getEncoded();
    }

    /**
     * @deprecated use toASN1Structure
     * @return
     */
    public TSTInfo toTSTInfo()
    {
        return tstInfo;
    }

    public TSTInfo toASN1Structure()
    {
        return tstInfo;
    }
}
