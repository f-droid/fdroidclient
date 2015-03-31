package org.spongycastle.jcajce.provider.asymmetric.dh;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.DHParameterSpec;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.pkcs.DHParameter;

public class AlgorithmParametersSpi
    extends java.security.AlgorithmParametersSpi
{
    DHParameterSpec     currentSpec;

    protected boolean isASN1FormatString(String format)
    {
        return format == null || format.equals("ASN.1");
    }

    protected AlgorithmParameterSpec engineGetParameterSpec(
        Class paramSpec)
        throws InvalidParameterSpecException
    {
        if (paramSpec == null)
        {
            throw new NullPointerException("argument to getParameterSpec must not be null");
        }

        return localEngineGetParameterSpec(paramSpec);
    }




        /**
         * Return the PKCS#3 ASN.1 structure DHParameter.
         * <p>
         * <pre>
         *  DHParameter ::= SEQUENCE {
         *                   prime INTEGER, -- p
         *                   base INTEGER, -- g
         *                   privateValueLength INTEGER OPTIONAL}
         * </pre>
         */
        protected byte[] engineGetEncoded() 
        {
            DHParameter dhP = new DHParameter(currentSpec.getP(), currentSpec.getG(), currentSpec.getL());

            try
            {
                return dhP.getEncoded(ASN1Encoding.DER);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error encoding DHParameters");
            }
        }

        protected byte[] engineGetEncoded(
            String format) 
        {
            if (isASN1FormatString(format))
            {
                return engineGetEncoded();
            }

            return null;
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(
            Class paramSpec) 
            throws InvalidParameterSpecException
        {
            if (paramSpec == DHParameterSpec.class)
            {
                return currentSpec;
            }

            throw new InvalidParameterSpecException("unknown parameter spec passed to DH parameters object.");
        }

        protected void engineInit(
            AlgorithmParameterSpec paramSpec) 
            throws InvalidParameterSpecException
        {
            if (!(paramSpec instanceof DHParameterSpec))
            {
                throw new InvalidParameterSpecException("DHParameterSpec required to initialise a Diffie-Hellman algorithm parameters object");
            }

            this.currentSpec = (DHParameterSpec)paramSpec;
        }

        protected void engineInit(
            byte[] params) 
            throws IOException
        {
            try
            {
                DHParameter dhP = DHParameter.getInstance(params);

                if (dhP.getL() != null)
                {
                    currentSpec = new DHParameterSpec(dhP.getP(), dhP.getG(), dhP.getL().intValue());
                }
                else
                {
                    currentSpec = new DHParameterSpec(dhP.getP(), dhP.getG());
                }
            }
            catch (ClassCastException e)
            {
                throw new IOException("Not a valid DH Parameter encoding.");
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                throw new IOException("Not a valid DH Parameter encoding.");
            }
        }

        protected void engineInit(
            byte[] params,
            String format) 
            throws IOException
        {
            if (isASN1FormatString(format))
            {
                engineInit(params);
            }
            else
            {
                throw new IOException("Unknown parameter format " + format);
            }
        }

        protected String engineToString() 
        {
            return "Diffie-Hellman Parameters";
        }
}
