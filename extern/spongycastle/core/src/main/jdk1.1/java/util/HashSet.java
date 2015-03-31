package java.util;

import java.io.*;
///*sk13*/import java.util.Hashtable;

public class HashSet extends /*sk13*/AbstractSet
    /*sk13*/ /*extends Hashmap*/

{
        private HashMap m_HashMap=null;

        public HashSet()
            {
                m_HashMap=new HashMap();

            }

        public HashSet(Collection c)
            {
                m_HashMap=new HashMap(Math.max(11,c.size()*2));
                addAll(c);

            }

        public HashSet(int initialCapacity, float loadFactor)
            {
                m_HashMap=new HashMap(initialCapacity,loadFactor);

            }

        public HashSet(int initialCapacity)
            {
                m_HashMap=new HashMap(initialCapacity);

            }

    public Iterator iterator()
      {
         return (m_HashMap.keySet()).iterator();
            }

        public int size()
            {
                return m_HashMap.size();
            }

       public boolean contains(Object o)
      {
              return m_HashMap.containsKey(o);
            }

    public boolean add(Object  o)
    {
        if (!m_HashMap.containsValue(o))
        {
            m_HashMap.put(o, o);

            return true;

        }

        return false;
    }

        public boolean remove(Object o)
            {
                return (m_HashMap.remove(o)!=null);
            }

        public void clear()
            {
                m_HashMap.clear();
            }


        public Object clone()
            {
                HashSet hs=new HashSet();
                hs.m_HashMap=(HashMap)m_HashMap.clone();
              return hs;
            }

}
