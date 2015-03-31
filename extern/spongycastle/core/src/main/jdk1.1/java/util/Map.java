package java.util;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @version 1.0
 */

public interface Map {

    public static interface Entry
        {
            public Object getKey();
            public Object getValue();
            public Object setValue(Object value) throws UnsupportedOperationException, ClassCastException,IllegalArgumentException,NullPointerException;
          public boolean equals(Object o);
            public int hashCode();
        };

        public int size();
        public boolean isEmpty();
        public boolean containsKey(Object Key) throws ClassCastException,NullPointerException;
        public boolean containsValue(Object value);
        public Object get(Object key)throws ClassCastException,NullPointerException;
        public Object put(Object key,Object value)throws UnsupportedOperationException, ClassCastException,IllegalArgumentException,NullPointerException;
        public Object remove(Object key)throws UnsupportedOperationException;
        public void putAll(Map t)throws UnsupportedOperationException, ClassCastException,IllegalArgumentException,NullPointerException;
        public void clear()throws UnsupportedOperationException;
        public Set keySet();
        public Collection values();
        public Set entrySet();
        public boolean equals(Object o);
        public int hashCode();

 }
