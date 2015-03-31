package java.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class StringTokenizer
    implements Enumeration
{
    private String s;
    private String delims;
    private boolean retDelims;
    private int     maxPos;

    private int pos;

    public StringTokenizer(String s, String delims)
    {
        this(s, delims, false);
    }

    public StringTokenizer(String s, String delims, boolean retDelims)
    {
        this.s = s;
        this.delims = delims;
        this.retDelims = retDelims;
        this.maxPos = s.length();
    }

    public boolean hasMoreTokens()
    {
        if (retDelims)
        {
            return pos < maxPos; 
        }
        else
        {
            int next = pos;
            while (next < maxPos && isDelim(next))
            {
                next++;
            }

            return next < maxPos;
        }
    }

    public String nextToken()
    {
        String tok;

        if (pos == maxPos)
        {
            throw new NoSuchElementException("no more tokens");
        }

        if (retDelims)
        {
            if (isDelim(pos))
            {
                tok = s.substring(pos, pos + 1);
                pos++;

                return tok;
            }
        }

        while (pos < maxPos && isDelim(pos))
        {
            pos++;
        }

        int start = pos;
        
        while (pos < maxPos && !isDelim(pos))
        {
            pos++;
        }

        if (pos < maxPos)
        {
            tok = s.substring(start, pos);
        }
        else
        {
            tok = s.substring(start);
        }

        return tok;
    }

    public boolean hasMoreElements()
    {
        return hasMoreTokens();
    }

    public Object nextElement()
    {
        return nextToken();
    }

    private boolean isDelim(int index)
    {
        char c = s.charAt(index);

        for (int i = 0; i != delims.length(); i++)
        {
            if (delims.charAt(i) == c)
            {
                return true;
            }
        }

        return false;
    }
}
