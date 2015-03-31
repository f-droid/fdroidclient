
package java.util;

public interface Collection
    {
            public boolean add(Object o) throws UnsupportedOperationException,ClassCastException,IllegalArgumentException;
            public boolean addAll(Collection c) throws UnsupportedOperationException,ClassCastException,IllegalArgumentException;
            public void clear()  throws UnsupportedOperationException;
            public boolean contains(Object o);
            public boolean containsAll(Collection c);
            public boolean equals(Object o);
            public int hashCode();
            public boolean isEmpty();
            public Iterator iterator();
            public /*SK13*/boolean remove(Object o) throws UnsupportedOperationException;
            public boolean removeAll(Collection c)  throws UnsupportedOperationException;
            public boolean retainAll(Collection c)  throws UnsupportedOperationException;
            public int size();
            public Object[] toArray();
            public Object[] toArray(Object[] a) throws ArrayStoreException;
    }
