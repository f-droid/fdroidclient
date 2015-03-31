package java.util;

import java.lang.reflect.Array;
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @version 1.0
 */


public abstract class AbstractCollection implements Collection
    {
        protected AbstractCollection()
          {
      }

        public abstract Iterator iterator();

        public abstract int size();

        public boolean isEmpty()
            {
                return size()==0;
            }

        public boolean contains(Object o)
            {
                Iterator it=iterator();
                while(it.hasNext())
                    {
                        Object e=it.next();
                        if(o==null)
                            {
                              if(e==null)
                                  return true;
                            }
                        else
                            {
                                if(o.equals(e))
                                    return true;
                            }
                    }
                return false;
            }

        public Object[] toArray()
            {
                Object[] arObjects=new Object[size()];
                Iterator it=iterator();
                int i=0;
                while(it.hasNext())
                    {
                        arObjects[i++]=it.next();
                    }
                return arObjects;
            }

        public Object[] toArray(Object[] a) throws NullPointerException,ArrayStoreException
        //TODO: Check if this is realy compatible to SUN!!!
            {
                if(a==null)
                throw new NullPointerException();

        if (isEmpty()) return a;
                Object[] arObjects=null;
                int size=size();
                if(a.length<size)
                    {
                        Iterator it=iterator();
                        Object o=it.next();
                        if(o==null) //no object or object is null
                            throw new ArrayStoreException(); //correct ?
                        arObjects=(Object[])Array.newInstance(o.getClass(),size);
                    }
                else
                    {
                        arObjects=a;
                         if(a.length>size)
                          arObjects[size]=null;

                    }

                Iterator it=iterator();
                int i=0;
         while(it.hasNext())
                    {
                        Object o=it.next();
            arObjects[i++]=o;
            }
                return arObjects;
            }

        public boolean add(Object o) throws UnsupportedOperationException,NullPointerException,ClassCastException,IllegalArgumentException
            {
                throw new UnsupportedOperationException();
            }

        public boolean remove(Object o) throws UnsupportedOperationException
            {
                Iterator it=iterator();
                while(it.hasNext())
                    {
                        Object e=it.next();
                        if(o==null)
                            {
                                if(e==null)
                                    {
                                      try
                                            {
                                                it.remove();
                                            }
                                        catch(UnsupportedOperationException ue)
                                            {
                                              throw ue;
                                            }
                                        return true;
                                    }
                            }
                        else
                          {
                                if(o.equals(e))
                                    {
                                      try
                                            {
                                                it.remove();
                                            }
                                        catch(UnsupportedOperationException ue)
                                            {
                                              throw ue;
                                            }
                                        return true;
                                    }
                            }
                    }
                return false;
            }

        public boolean containsAll(Collection c)
            {
                Iterator it=c.iterator();
                while(it.hasNext())
                    {
                        if(!contains(it.next()))
                            return false;
                    }
                return true;
            }

        public boolean addAll(Collection c) throws UnsupportedOperationException
            {
                Iterator it=c.iterator();
                boolean ret=false;
                while(it.hasNext())
                    {
                        try
                            {
                                ret|=add(it.next());
                            }
                        catch(UnsupportedOperationException ue)
                            {
                                throw ue;
                            }
                    }
                return ret;
            }

        public boolean removeAll(Collection c) throws UnsupportedOperationException
            {
                Iterator it=iterator();
                boolean ret=false;
                while(it.hasNext())
                    {
                        if(c.contains(it.next()))
                            try
                                {
                                    it.remove();
                                    ret=true;
                                }
                            catch(UnsupportedOperationException ue)
                                {
                                    throw ue;
                                }
                    }
                return ret;
            }

        public boolean retainAll(Collection c) throws UnsupportedOperationException
            {
                Iterator it=iterator();
                boolean ret=false;
                while(it.hasNext())
                    {
                        if(!c.contains(it.next()))
                            try
                                {
                                    it.remove();
                                    ret=true;
                                }
                            catch(UnsupportedOperationException ue)
                                {
                                    throw ue;
                                }
                    }
                return ret;
            }

        public void clear() throws UnsupportedOperationException
            {
                Iterator it=iterator();
                while(it.hasNext())
                    {
                        try
                            {
                                it.next();
                                it.remove();
                            }
                        catch(UnsupportedOperationException ue)
                            {
                                throw ue;
                            }
                    }
            }

        public String toString()
            {
                String ret="[";
                Iterator it=iterator();
                if(it.hasNext())
                    ret+=String.valueOf(it.next());
                while(it.hasNext())
                    {
                        ret+=", ";
                        ret+=String.valueOf(it.next());

                    }
                ret+="]";
                return ret;
            }

}
