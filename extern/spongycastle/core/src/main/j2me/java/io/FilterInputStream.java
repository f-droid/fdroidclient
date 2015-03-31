package java.io;

public class FilterInputStream extends InputStream
{
    protected InputStream in;

    protected FilterInputStream(InputStream underlying)
    {
        in = underlying;
    }

    public int read() throws IOException
    {
        return in.read();
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int offset, int length) throws IOException
    {
        return in.read(b, offset, length);
    }

    public long skip(long n) throws IOException
    {
        return in.skip(n);
    }

    public int available() throws IOException
    {
        return in.available();
    }

    public void close() throws IOException
    {
        in.close();
    }

    public void mark(int readlimit)
    {
        in.mark(readlimit);
    }

    public void reset() throws IOException
    {
        in.reset();
    }

    public boolean markSupported()
    {
        return in.markSupported();
    }
}
