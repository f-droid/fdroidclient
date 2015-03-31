package java.util;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @version 1.0
 */

public abstract class AbstractList extends AbstractCollection implements List

{
  protected AbstractList al = this;


  protected AbstractList()
        {
    }

    public boolean add(Object o) throws UnsupportedOperationException, ClassCastException, IllegalArgumentException
        {
            try
                {
                    add(size(),o);
                  return true;
                }
            catch(UnsupportedOperationException ue)
                {
                    throw ue;
                }
        }

    public abstract Object get(int index) throws IndexOutOfBoundsException;

    public Object set(int index,Object element) throws UnsupportedOperationException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException
        {
            throw new UnsupportedOperationException();
        }

    public void add(int index,Object element)  throws UnsupportedOperationException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException
        {
            throw new UnsupportedOperationException();
        }

    public Object remove(int index)  throws UnsupportedOperationException, IndexOutOfBoundsException
        {
            Object o = get(index);

            removeRange(index,index+1);
            return o;
        }

    public int indexOf(Object o)
        {
             ListIterator li = listIterator();
      Object e;
          while(li.hasNext())
                {
          int index=li.nextIndex();
                    e=li.next();
          System.out.println(e);
                    if(o==null)
                        {
                            if(e==null)
                                return index;
                        }
                    else
                        {
                            if(o.equals(e))
                                return index;
                        }
                }
            return -1;
        }

    public int lastIndexOf(Object o)
        {
            ListIterator li=listIterator(size());
          while(li.hasPrevious())
                {
                    int index=li.previousIndex();
                    Object e=li.previous();
                    if(o==null)
                        {
                            if(e==null)
                                return index;
                        }
                    else
                        {
                            if(o.equals(e))
                                return index;
                        }
                }
            return -1;
        }

    public void clear() throws UnsupportedOperationException
        {
            try
                {
                    removeRange(0,size());
                }
            catch(UnsupportedOperationException ue)
                {
                    throw ue;
                }
        }

    public boolean addAll(int index,Collection c) throws UnsupportedOperationException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException
        {
            Iterator it=c.iterator();
            boolean ret=false;
            while(it.hasNext())
                {
                    try
                        {
                            add(index++,it.next());
                            ret=true;
                        }
                    catch(UnsupportedOperationException ue)
                        {
                            throw ue;
                        }
                }
            return ret;
        }

    public Iterator iterator()
        {
            return new AbstractListIterator(this,0);
        }

    public ListIterator listIterator()
        {
            return listIterator(0);
        }

    public ListIterator listIterator(int index) throws IndexOutOfBoundsException
        {
             if(index<0||index>size())    throw new IndexOutOfBoundsException();
            return new AbstractListListIterator(this,index);
        }

    public List subList(int fromIndex,int toIndex) throws IndexOutOfBoundsException,IllegalArgumentException
        {
          if(fromIndex < 0 || toIndex > size())
                throw new IndexOutOfBoundsException();
            if(fromIndex>toIndex)
                throw new IllegalArgumentException();
              return (List) new Sublist(this,fromIndex,toIndex);
        }

    public boolean equals(Object o)
        {
            if(o==this)
                return true;
            if(!(o instanceof List))
                return false;
            Iterator it1=iterator();
            Iterator it2=((List)o).iterator();
            while(it1.hasNext())
                {
                    if(!it2.hasNext())
                        return false;
                    Object e1=it1.next();
                    Object e2=it2.next();
                    if(e1==null)
                        {
                            if(e2!=null)
                                return false;
                        }
                    if(!e1.equals(e2))
                        return false;
                }
            return true;
        }

    public int hashCode()
        {
          int hashCode = 1;
            Iterator it = iterator();
            while (it.hasNext())
                {
                    Object o = it.next();
                    hashCode = 31*hashCode + (o==null ? 0 : o.hashCode());
                }
            return hashCode;
    }

    protected void removeRange(int fromIndex,int toIndex)
        {
    System.out.println("breakpoint 1");
            if(fromIndex==toIndex) return;
          System.out.println("breakpoint 2");
             ListIterator li=listIterator(fromIndex);
          System.out.println("breakpoint 3");
            int i=fromIndex;
            do
              {
                    li.next();
                    li.remove();
                    i++;
                }while(li.hasNext()&&i<toIndex);
        }

////////////////////////////////////////////////////////////
/////////////innere Klasse AbstractIterator/////////////////
////////////////////////////////////////////////////////////

private class AbstractListIterator implements Iterator
    {
        AbstractList m_al=null;
        int m_nextIndex=0;
        public AbstractListIterator(AbstractList al,int index)
            {
                m_al=al;
                m_nextIndex=index;
            }

        public boolean hasNext()
            {
                return m_nextIndex < m_al.size();
            }

    public Object next()
            {
        return m_al.get(m_nextIndex++);
            }

        public void remove()
          {
              m_al.remove(m_nextIndex - 1);
            }
    }

///////////////////////////////////////////////////////////////////////////////
//////////// innere Klasse AbstraktListListIterator
///////////////////////////////////////////////////////////////////////////////


private class AbstractListListIterator extends AbstractListIterator implements ListIterator
    {
        public AbstractListListIterator(AbstractList al,int index)
            {
                super(al,index);
            }

        public boolean hasPrevious()
            {
                return m_nextIndex>0;
            }

        public Object previous()// throws NoSuchElementException;
          {
                return m_al.get(--m_nextIndex);
            }

        public int nextIndex()
            {
                return m_nextIndex;
            }

        public int previousIndex()
            {
                return m_nextIndex-1;
            }

        public void set(Object o) //throws UnsupportedOperationException, ClassCastException, IllegalArgumentException,IllegalStateException;
          {
                m_al.set(m_nextIndex-1,o);
            }

        public void add(Object o)// throws UnsupportedOperationException, ClassCastException, IllegalArgumentException;
          {
                m_al.add(m_nextIndex-1,o);
            }
    }


}
