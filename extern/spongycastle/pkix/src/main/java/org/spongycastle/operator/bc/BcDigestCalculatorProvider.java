package org.spongycastle.operator.bc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.ExtendedDigest;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;

public class BcDigestCalculatorProvider
    implements DigestCalculatorProvider
{
    private BcDigestProvider digestProvider = BcDefaultDigestProvider.INSTANCE;

    public DigestCalculator get(final AlgorithmIdentifier algorithm)
        throws OperatorCreationException
    {
        Digest dig = digestProvider.get(algorithm);

        final DigestOutputStream stream = new DigestOutputStream(dig);

        return new DigestCalculator()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return algorithm;
            }

            public OutputStream getOutputStream()
            {
                return stream;
            }

            public byte[] getDigest()
            {
                return stream.getDigest();
            }
        };
    }

    private class DigestOutputStream
        extends OutputStream
    {
        private Digest dig;

        DigestOutputStream(Digest dig)
        {
            this.dig = dig;
        }

        public void write(byte[] bytes, int off, int len)
            throws IOException
        {
            dig.update(bytes, off, len);
        }

        public void write(byte[] bytes)
            throws IOException
        {
            dig.update(bytes, 0, bytes.length);
        }

        public void write(int b)
            throws IOException
        {
            dig.update((byte)b);
        }

        byte[] getDigest()
        {
            byte[] d = new byte[dig.getDigestSize()];

            dig.doFinal(d, 0);

            return d;
        }
    }
}