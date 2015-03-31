package org.spongycastle.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Provider;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;

public class JcaDigestCalculatorProviderBuilder
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());

    public JcaDigestCalculatorProviderBuilder()
    {
    }

    public JcaDigestCalculatorProviderBuilder setProvider(Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public JcaDigestCalculatorProviderBuilder setProvider(String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public DigestCalculatorProvider build()
        throws OperatorCreationException
    {
        return new DigestCalculatorProvider()
        {
            public DigestCalculator get(final AlgorithmIdentifier algorithm)
                throws OperatorCreationException
            {
                final DigestOutputStream stream;

                try
                {
                    MessageDigest dig = helper.createDigest(algorithm);

                    stream = new DigestOutputStream(dig);
                }
                catch (GeneralSecurityException e)
                {
                    throw new OperatorCreationException("exception on setup: " + e, e);
                }

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
        };
    }

    private class DigestOutputStream
        extends OutputStream
    {
        private MessageDigest dig;

        DigestOutputStream(MessageDigest dig)
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
           dig.update(bytes);
        }

        public void write(int b)
            throws IOException
        {
           dig.update((byte)b);
        }

        byte[] getDigest()
        {
            return dig.digest();
        }
    }
}