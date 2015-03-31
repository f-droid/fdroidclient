package java.util;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @version 1.0
 */

public interface ListIterator extends Iterator
    {
        public boolean hasPrevious();
        public Object previous() throws NoSuchElementException;
        public int nextIndex();
        public int previousIndex();
        public void set(Object o) throws UnsupportedOperationException, ClassCastException, IllegalArgumentException,IllegalStateException;
        public void add(Object o) throws UnsupportedOperationException, ClassCastException, IllegalArgumentException;
    }
