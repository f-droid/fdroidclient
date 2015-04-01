package org.spongycastle.cert.selector;

import java.io.IOException;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.util.Pack;

class MSOutlookKeyIdCalculator
{
    // This is less than ideal, but it seems to be the best way of supporting this without exposing SHA-1
    // as the class is only used to workout the MSOutlook Key ID, you can think of the fact it's SHA-1 as
    // a coincidence...
    static byte[] calculateKeyId(SubjectPublicKeyInfo info)
    {
        SHA1Digest dig = new SHA1Digest();
        byte[] hash = new byte[dig.getDigestSize()];
        byte[] spkiEnc = new byte[0];
        try
        {
            spkiEnc = info.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            return new byte[0];
        }

        // try the outlook 2010 calculation
        dig.update(spkiEnc, 0, spkiEnc.length);

        dig.doFinal(hash, 0);

        return hash;
    }

    private static abstract class GeneralDigest
    {
        private static final int BYTE_LENGTH = 64;
        private byte[]  xBuf;
        private int     xBufOff;

        private long    byteCount;

        /**
         * Standard constructor
         */
        protected GeneralDigest()
        {
            xBuf = new byte[4];
            xBufOff = 0;
        }

        /**
         * Copy constructor.  We are using copy constructors in place
         * of the Object.clone() interface as this interface is not
         * supported by J2ME.
         */
        protected GeneralDigest(GeneralDigest t)
        {
            xBuf = new byte[t.xBuf.length];

            copyIn(t);
        }

        protected void copyIn(GeneralDigest t)
        {
            System.arraycopy(t.xBuf, 0, xBuf, 0, t.xBuf.length);

            xBufOff = t.xBufOff;
            byteCount = t.byteCount;
        }

        public void update(
            byte in)
        {
            xBuf[xBufOff++] = in;

            if (xBufOff == xBuf.length)
            {
                processWord(xBuf, 0);
                xBufOff = 0;
            }

            byteCount++;
        }

        public void update(
            byte[]  in,
            int     inOff,
            int     len)
        {
            //
            // fill the current word
            //
            while ((xBufOff != 0) && (len > 0))
            {
                update(in[inOff]);

                inOff++;
                len--;
            }

            //
            // process whole words.
            //
            while (len > xBuf.length)
            {
                processWord(in, inOff);

                inOff += xBuf.length;
                len -= xBuf.length;
                byteCount += xBuf.length;
            }

            //
            // load in the remainder.
            //
            while (len > 0)
            {
                update(in[inOff]);

                inOff++;
                len--;
            }
        }

        public void finish()
        {
            long    bitLength = (byteCount << 3);

            //
            // add the pad bytes.
            //
            update((byte)128);

            while (xBufOff != 0)
            {
                update((byte)0);
            }

            processLength(bitLength);

            processBlock();
        }

        public void reset()
        {
            byteCount = 0;

            xBufOff = 0;
            for (int i = 0; i < xBuf.length; i++)
            {
                xBuf[i] = 0;
            }
        }

        protected abstract void processWord(byte[] in, int inOff);

        protected abstract void processLength(long bitLength);

        protected abstract void processBlock();
    }

    private static class SHA1Digest
        extends GeneralDigest
    {
        private static final int    DIGEST_LENGTH = 20;

        private int     H1, H2, H3, H4, H5;

        private int[]   X = new int[80];
        private int     xOff;

        /**
         * Standard constructor
         */
        public SHA1Digest()
        {
            reset();
        }

        public String getAlgorithmName()
        {
            return "SHA-1";
        }

        public int getDigestSize()
        {
            return DIGEST_LENGTH;
        }

        protected void processWord(
            byte[]  in,
            int     inOff)
        {
            // Note: Inlined for performance
    //        X[xOff] = Pack.bigEndianToInt(in, inOff);
            int n = in[  inOff] << 24;
            n |= (in[++inOff] & 0xff) << 16;
            n |= (in[++inOff] & 0xff) << 8;
            n |= (in[++inOff] & 0xff);
            X[xOff] = n;

            if (++xOff == 16)
            {
                processBlock();
            }
        }

        protected void processLength(
            long    bitLength)
        {
            if (xOff > 14)
            {
                processBlock();
            }

            X[14] = (int)(bitLength >>> 32);
            X[15] = (int)(bitLength & 0xffffffff);
        }

        public int doFinal(
            byte[]  out,
            int     outOff)
        {
            finish();

            Pack.intToBigEndian(H1, out, outOff);
            Pack.intToBigEndian(H2, out, outOff + 4);
            Pack.intToBigEndian(H3, out, outOff + 8);
            Pack.intToBigEndian(H4, out, outOff + 12);
            Pack.intToBigEndian(H5, out, outOff + 16);

            reset();

            return DIGEST_LENGTH;
        }

        /**
         * reset the chaining variables
         */
        public void reset()
        {
            super.reset();

            H1 = 0x67452301;
            H2 = 0xefcdab89;
            H3 = 0x98badcfe;
            H4 = 0x10325476;
            H5 = 0xc3d2e1f0;

            xOff = 0;
            for (int i = 0; i != X.length; i++)
            {
                X[i] = 0;
            }
        }

        //
        // Additive constants
        //
        private static final int    Y1 = 0x5a827999;
        private static final int    Y2 = 0x6ed9eba1;
        private static final int    Y3 = 0x8f1bbcdc;
        private static final int    Y4 = 0xca62c1d6;

        private int f(
            int    u,
            int    v,
            int    w)
        {
            return ((u & v) | ((~u) & w));
        }

        private int h(
            int    u,
            int    v,
            int    w)
        {
            return (u ^ v ^ w);
        }

        private int g(
            int    u,
            int    v,
            int    w)
        {
            return ((u & v) | (u & w) | (v & w));
        }

        protected void processBlock()
        {
            //
            // expand 16 word block into 80 word block.
            //
            for (int i = 16; i < 80; i++)
            {
                int t = X[i - 3] ^ X[i - 8] ^ X[i - 14] ^ X[i - 16];
                X[i] = t << 1 | t >>> 31;
            }

            //
            // set up working variables.
            //
            int     A = H1;
            int     B = H2;
            int     C = H3;
            int     D = H4;
            int     E = H5;

            //
            // round 1
            //
            int idx = 0;

            for (int j = 0; j < 4; j++)
            {
                // E = rotateLeft(A, 5) + f(B, C, D) + E + X[idx++] + Y1
                // B = rotateLeft(B, 30)
                E += (A << 5 | A >>> 27) + f(B, C, D) + X[idx++] + Y1;
                B = B << 30 | B >>> 2;

                D += (E << 5 | E >>> 27) + f(A, B, C) + X[idx++] + Y1;
                A = A << 30 | A >>> 2;

                C += (D << 5 | D >>> 27) + f(E, A, B) + X[idx++] + Y1;
                E = E << 30 | E >>> 2;

                B += (C << 5 | C >>> 27) + f(D, E, A) + X[idx++] + Y1;
                D = D << 30 | D >>> 2;

                A += (B << 5 | B >>> 27) + f(C, D, E) + X[idx++] + Y1;
                C = C << 30 | C >>> 2;
            }

            //
            // round 2
            //
            for (int j = 0; j < 4; j++)
            {
                // E = rotateLeft(A, 5) + h(B, C, D) + E + X[idx++] + Y2
                // B = rotateLeft(B, 30)
                E += (A << 5 | A >>> 27) + h(B, C, D) + X[idx++] + Y2;
                B = B << 30 | B >>> 2;

                D += (E << 5 | E >>> 27) + h(A, B, C) + X[idx++] + Y2;
                A = A << 30 | A >>> 2;

                C += (D << 5 | D >>> 27) + h(E, A, B) + X[idx++] + Y2;
                E = E << 30 | E >>> 2;

                B += (C << 5 | C >>> 27) + h(D, E, A) + X[idx++] + Y2;
                D = D << 30 | D >>> 2;

                A += (B << 5 | B >>> 27) + h(C, D, E) + X[idx++] + Y2;
                C = C << 30 | C >>> 2;
            }

            //
            // round 3
            //
            for (int j = 0; j < 4; j++)
            {
                // E = rotateLeft(A, 5) + g(B, C, D) + E + X[idx++] + Y3
                // B = rotateLeft(B, 30)
                E += (A << 5 | A >>> 27) + g(B, C, D) + X[idx++] + Y3;
                B = B << 30 | B >>> 2;

                D += (E << 5 | E >>> 27) + g(A, B, C) + X[idx++] + Y3;
                A = A << 30 | A >>> 2;

                C += (D << 5 | D >>> 27) + g(E, A, B) + X[idx++] + Y3;
                E = E << 30 | E >>> 2;

                B += (C << 5 | C >>> 27) + g(D, E, A) + X[idx++] + Y3;
                D = D << 30 | D >>> 2;

                A += (B << 5 | B >>> 27) + g(C, D, E) + X[idx++] + Y3;
                C = C << 30 | C >>> 2;
            }

            //
            // round 4
            //
            for (int j = 0; j <= 3; j++)
            {
                // E = rotateLeft(A, 5) + h(B, C, D) + E + X[idx++] + Y4
                // B = rotateLeft(B, 30)
                E += (A << 5 | A >>> 27) + h(B, C, D) + X[idx++] + Y4;
                B = B << 30 | B >>> 2;

                D += (E << 5 | E >>> 27) + h(A, B, C) + X[idx++] + Y4;
                A = A << 30 | A >>> 2;

                C += (D << 5 | D >>> 27) + h(E, A, B) + X[idx++] + Y4;
                E = E << 30 | E >>> 2;

                B += (C << 5 | C >>> 27) + h(D, E, A) + X[idx++] + Y4;
                D = D << 30 | D >>> 2;

                A += (B << 5 | B >>> 27) + h(C, D, E) + X[idx++] + Y4;
                C = C << 30 | C >>> 2;
            }


            H1 += A;
            H2 += B;
            H3 += C;
            H4 += D;
            H5 += E;

            //
            // reset start of the buffer.
            //
            xOff = 0;
            for (int i = 0; i < 16; i++)
            {
                X[i] = 0;
            }
        }
    }
}
