
package java.util;

public interface Collection
    {
            public boolean add(Object o) throws RuntimeException,ClassCastException,IllegalArgumentException;
            public boolean addAll(Collection c) throws RuntimeException,ClassCastException,IllegalArgumentException;
            public void clear()  throws RuntimeException;
            public boolean contains(Object o);
            public boolean containsAll(Collection c);
            public boolean equals(Object o);
            public int hashCode();
            public boolean isEmpty();
            public Iterator iterator();
            public /*SK13*/boolean remove(Object o) throws RuntimeException;
            public boolean removeAll(Collection c)  throws RuntimeException;
            public boolean retainAll(Collection c)  throws RuntimeException;
            public int size();
            public Object[] toArray();
            public Object[] toArray(Object[] a) throws ArrayStoreException;
    }
