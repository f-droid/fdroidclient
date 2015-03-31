package java.util;

import java.io.Serializable;

public class Collections
{
    public static List EMPTY_LIST = new ArrayList();

    private Collections()
    {
    }

    public static Collection unmodifiableCollection(Collection c)
    {
        return new UnmodifiableCollection(c);
    }

    static class UnmodifiableCollection
        implements Collection, Serializable
    {
        Collection c;

        UnmodifiableCollection(Collection c)
        {
            this.c = c;
        }

        public int size()
        {
            return c.size();
        }

        public boolean isEmpty()
        {
            return c.isEmpty();
        }

        public boolean contains(Object o)
        {
            return c.contains(o);
        }

        public Object[] toArray()
        {
            return c.toArray();
        }

        public Object[] toArray(Object[] a)
        {
            return c.toArray(a);
        }

        public Iterator iterator()
        {
            return new Iterator()
            {
                Iterator i = c.iterator();

                public boolean hasNext()
                {
                    return i.hasNext();
                }

                public Object next()
                {
                    return i.next();
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean add(Object o)
        {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection coll)
        {
            return c.containsAll(coll);
        }

        public boolean addAll(Collection coll)
        {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection coll)
        {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection coll)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        public String toString()
        {
            return c.toString();
        }
    }

    public static Set unmodifiableSet(Set s)
    {
        return new UnmodifiableSet(s);
    }

    static class UnmodifiableSet
        extends UnmodifiableCollection
        implements Set, Serializable
    {
        UnmodifiableSet(Set s)
        {
            super(s);
        }

        public boolean equals(Object o)
        {
            return c.equals(o);
        }

        public int hashCode()
        {
            return c.hashCode();
        }
    }

    public static List unmodifiableList(List list)
    {
        return new UnmodifiableList(list);
    }

    static class UnmodifiableList
        extends UnmodifiableCollection
        implements List
    {
        private List list;

        UnmodifiableList(List list)
        {
            super(list);
            this.list = list;
        }

        public boolean equals(Object o)
        {
            return list.equals(o);
        }

        public int hashCode()
        {
            return list.hashCode();
        }

        public Object get(int index)
        {
            return list.get(index);
        }

        public Object set(int index, Object element)
        {
            throw new UnsupportedOperationException();
        }

        public void add(int index, Object element)
        {
            throw new UnsupportedOperationException();
        }

        public Object remove(int index)
        {
            throw new UnsupportedOperationException();
        }

        public int indexOf(Object o)
        {
            return list.indexOf(o);
        }

        public int lastIndexOf(Object o)
        {
            return list.lastIndexOf(o);
        }

        public boolean addAll(int index, Collection c)
        {
            throw new UnsupportedOperationException();
        }

        public ListIterator listIterator()
        {
            return listIterator(0);
        }

        public ListIterator listIterator(final int index)
        {
            return new ListIterator()
            {
                ListIterator i = list.listIterator(index);

                public boolean hasNext()
                {
                    return i.hasNext();
                }

                public Object next()
                {
                    return i.next();
                }

                public boolean hasPrevious()
                {
                    return i.hasPrevious();
                }

                public Object previous()
                {
                    return i.previous();
                }

                public int nextIndex()
                {
                    return i.nextIndex();
                }

                public int previousIndex()
                {
                    return i.previousIndex();
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

                public void set(Object o)
                {
                    throw new UnsupportedOperationException();
                }

                public void add(Object o)
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public List subList(int fromIndex, int toIndex)
        {
            return new UnmodifiableList(list.subList(fromIndex, toIndex));
        }
    }

    public static Enumeration enumeration(final Collection c)
    {
        return new Enumeration()
        {
            Iterator i = c.iterator();

            public boolean hasMoreElements()
            {
                return i.hasNext();
            }

            public Object nextElement()
            {
                return i.next();
            }
        };
    }

    public static Map unmodifiableMap(Map s)
    {
        return new UnmodifiableMap(s);
    }

    static class UnmodifiableMap
        implements Map
    {
        private Map c;

        UnmodifiableMap(Map map)
        {
            this.c = map;
        }

        public int size()
        {
            return c.size();
        }

        public boolean isEmpty()
        {
            return c.isEmpty();
        }

        public boolean containsKey(Object o)
        {
            return c.containsKey(o);
        }

        public boolean containsValue(Object o)
        {
            return c.containsValue(o);
        }

        public Object get(Object o)
        {
            return c.get(o);
        }

        public Object put(Object o, Object o2)
        {
            throw new UnsupportedOperationException();
        }

        public Object remove(Object o)
        {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map map)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        public Set keySet()
        {
            return Collections.unmodifiableSet(c.keySet());
        }

        public Collection values()
        {
            return new UnmodifiableCollection(c.values());
        }

        public Set entrySet()
        {
            return Collections.unmodifiableSet(c.entrySet());
        }

        public boolean equals(Object o)
        {
            return c.equals(o);
        }

        public int hashCode()
        {
            return c.hashCode();
        }

        public String toString()
        {
            return c.toString();
        }
    }
}
