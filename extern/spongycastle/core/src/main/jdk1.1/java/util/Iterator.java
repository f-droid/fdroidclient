
package java.util;

public interface Iterator
{
    public abstract boolean hasNext();
    public abstract Object next() throws NoSuchElementException;
    public abstract void remove() throws UnsupportedOperationException,IllegalStateException;
}
