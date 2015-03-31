package java.util;

public class Sublist
    extends AbstractList
{
    AbstractList m_al = null;
    int m_fromIndex = 0;
    int m_toIndex = 0;
    int size = 0;

    public Sublist(AbstractList ali, int fromIndex, int toIndex)
    {
        m_al = ali;
        m_toIndex = toIndex;
        m_fromIndex = fromIndex;
        size = size();
    }

    public Object set(int index, Object o)
    {
        if (index < size)
        {
            o = m_al.set(index + m_fromIndex, o);
            if (o != null)
            {
                size++;
                m_toIndex++;
            }
            return o;
        }
        else
        {
            throw new IndexOutOfBoundsException();
        }
    }

    public Object get(int index)
        throws IndexOutOfBoundsException
    {
        if (index < size)
        {
            return m_al.get(index + m_fromIndex);
        }
        else
        {
            throw new IndexOutOfBoundsException();
        }
    }

    public void add(int index, Object o)
    {

        if (index <= size)
        {
            m_al.add(index + m_fromIndex, o);
            m_toIndex++;
            size++;

        }
        else
        {
            throw new IndexOutOfBoundsException();
        }

    }

    public Object remove(int index, Object o)
    {
        if (index < size)
        {
            Object ob = m_al.remove(index + m_fromIndex);
            if (ob != null)
            {
                m_toIndex--;
                size--;
            }
            return ob;
        }
        else
        {
            throw new IndexOutOfBoundsException();
        }
    }

    public boolean addAll(int index, Collection c)
    {
        if (index < size)
        {
            boolean bool = m_al.addAll(index + m_fromIndex, c);
            if (bool)
            {
                int lange = c.size();
                m_toIndex = m_toIndex + lange;
                size = size + lange;
            }
            return bool;
        }
        else
        {
            throw new IndexOutOfBoundsException();
        }
    }

    public boolean addAll(Collection c)
    {
        boolean bool = m_al.addAll(m_toIndex, c);
        if (bool)
        {
            int lange = c.size();
            m_toIndex = m_toIndex + lange;
            size = size + lange;
        }
        return bool;
    }

    public void removeRange(int from, int to)
    {
        if ((from <= to) && (from <= size) && (to <= size))
        {
            m_al.removeRange(from, to);
            int lange = to - from;
            m_toIndex = m_toIndex - lange;
            size = size - lange;
        }
        else
        {
            if (from > to)
            {
                throw new IllegalArgumentException();
            }
            else
            {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    public int size()
    {
        return (m_toIndex - m_fromIndex);
    }
}
