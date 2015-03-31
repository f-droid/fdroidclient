package java.util;

/*************
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @version 1.0
 */

public abstract class AbstractMap implements Map{

  protected AbstractMap()
        {
    }

    public int size()
        {
            return entrySet().size();
        }

    public boolean isEmpty()
        {
            return size()==0;
        }

    public boolean containsValue(Object value)
        {
            Iterator it=entrySet().iterator();
            while(it.hasNext())
                {
                    Map.Entry v=(Map.Entry)it.next();
                    if(value==null)
                        {
                            if(v.getValue()==null)
                                return true;
                        }
                    else
                        {
                            if(value.equals(v.getValue()))
                                return true;
                        }
                }
            return false;
        }

    public boolean containsKey(Object key) throws ClassCastException,NullPointerException
        {
            Iterator it=entrySet().iterator();
            while(it.hasNext())
                {
                    Map.Entry v=(Map.Entry)it.next();
                    if(key==null)
                        {
                            if(v.getKey()==null)
                                return true;
                        }
                    else
                        {
                            if(key.equals(v.getKey()))
                                return true;
                        }
                }
            return false;
        }

    public Object get(Object key)throws ClassCastException,NullPointerException
        {
            Iterator it=entrySet().iterator();
            while(it.hasNext())
                {
                    Map.Entry v=(Map.Entry)it.next();
                    if(key==null)
                        {
                            if(v.getKey()==null)
                                return v.getValue();
                        }
                    else
                        {
                            if(key.equals(v.getKey()))
                                return v.getValue();
                        }
                }
            return null;
        }

    public Object put(Object key,Object value) throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

    public Object remove(Object key)
        {
            Iterator it=entrySet().iterator();
            Object o=null;
            while(it.hasNext())
                    {
                        Map.Entry v=(Map.Entry)it.next();
                        if(key==null)
                            {
                                if(v.getKey()==null)
                                    {
                                        o=v.getValue();
                                        it.remove();
                                        return o;
                                    }
                            }
                        else
                            {
                                if(key.equals(v.getKey()))
                                    {
                                        o=v.getValue();
                                        it.remove();
                                        return o;
                                    }
                            }
                    }
          return null;
        }

    public void putAll(Map t)
        {
            Iterator it=t.entrySet().iterator();
            while(it.hasNext())
                    {
                        Map.Entry v=(Map.Entry)it.next();
                        put(v.getKey(),v.getValue());
                    }
        }

    public void clear()
        {
            entrySet().clear();
        }

    public Set keySet()
        {
            throw new UnsupportedOperationException("no keySet in AbstractMap()");
        }

    public Collection values()
        {
            throw new UnsupportedOperationException("no values in AbstractMap()");
        }

    public abstract Set entrySet();

    public boolean equals(Object o)
        {
            throw new UnsupportedOperationException("no equals in AbstractMap()");
        }

    public int hashCode()
        {
            throw new UnsupportedOperationException("no hashCode in AbstractMap()");
        }

    public String toString()
        {
            throw new UnsupportedOperationException("no toString in AbstractMap()");
        }


}
