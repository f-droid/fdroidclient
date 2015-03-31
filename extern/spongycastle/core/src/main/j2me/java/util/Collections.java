package java.util;

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
        implements Collection
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
                    throw new RuntimeException();
                }
            };
        }

        public boolean add(Object o)
        {
            throw new RuntimeException();
        }

        public boolean remove(Object o)
        {
            throw new RuntimeException();
        }

        public boolean containsAll(Collection coll)
        {
            return c.containsAll(coll);
        }

        public boolean addAll(Collection coll)
        {
            throw new RuntimeException();
        }

        public boolean removeAll(Collection coll)
        {
            throw new RuntimeException();
        }

        public boolean retainAll(Collection coll)
        {
            throw new RuntimeException();
        }

        public void clear()
        {
            throw new RuntimeException();
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
        implements Set
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

    public static Map unmodifiableMap(Map map)
    {
        return new UnmodifiableMap(map);
    }

    static class UnmodifiableMap
        implements Map
    {
        private Map map;

        UnmodifiableMap(Map map)
        {
            this.map = map;
        }

        public int size()
        {
            return map.size();
        }

        public boolean isEmpty()
        {
            return map.isEmpty();
        }

        public boolean containsKey(Object key)
            throws ClassCastException, NullPointerException
        {
            return map.containsKey(key);
        }

        public boolean containsValue(Object value)
        {
            return map.containsValue(value);
        }

        public Object get(Object key)
            throws ClassCastException, NullPointerException
        {
            return map.get(key);
        }

        public Object put(Object key, Object value)
            throws RuntimeException, ClassCastException, IllegalArgumentException, NullPointerException
        {
            throw new RuntimeException("unsupported operation - map unmodifiable");
        }

        public Object remove(Object key)
            throws RuntimeException
        {
            throw new RuntimeException("unsupported operation - map unmodifiable");
        }

        public void putAll(Map t)
            throws RuntimeException, ClassCastException, IllegalArgumentException, NullPointerException
        {
            throw new RuntimeException("unsupported operation - map unmodifiable");
        }

        public void clear()
            throws RuntimeException
        {
            throw new RuntimeException("unsupported operation - map unmodifiable");
        }

        public Set keySet()
        {
            return map.keySet();
        }

        public Collection values()
        {
            return map.values();
        }

        public Set entrySet()
        {
            return map.entrySet();
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
            throw new RuntimeException();
        }

        public void add(int index, Object element)
        {
            throw new RuntimeException();
        }

        public Object remove(int index)
        {
            throw new RuntimeException();
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
            throw new RuntimeException();
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
                    throw new RuntimeException();
                }

                public void set(Object o)
                {
                    throw new RuntimeException();
                }

                public void add(Object o)
                {
                    throw new RuntimeException();
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
}
