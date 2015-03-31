package java.util;

public class ArrayList extends AbstractList
    implements List
    {
        Vector m_Vector=null;

        public ArrayList()
            {
                m_Vector=new Vector();
            }

        public ArrayList(Collection c)
            {
                m_Vector=new Vector((int)(c.size()*1.1));
                addAll(c);
            }

        public ArrayList(int initialCapacity)
          {
                m_Vector=new Vector(initialCapacity);
            }

        public void trimToSize()
            {
                m_Vector.trimToSize();
            }

        public void ensureCapacity(int minCapacity)
            {
                m_Vector.ensureCapacity(minCapacity);
            }

        public int size()
            {
                return m_Vector.size();
            }

        public boolean contains(Object elem)
            {
                return m_Vector.contains(elem);
            }

        public int indexOf(Object elem)
            {
                return m_Vector.indexOf(elem);
            }

        public int lastIndexOf(Object elem)
            {
                return m_Vector.lastIndexOf(elem);
            }

        public Object clone()
            {
                ArrayList al=new ArrayList();
                al.m_Vector=(Vector)m_Vector.clone();
              return al;
            }

        public Object[] toArray()
            {
                Object[] o=new Object[m_Vector.size()];
                m_Vector.copyInto(o);
                return o;
            }

        public Object get(int index)
            {
                return m_Vector.elementAt(index);
            }

        public Object set(int index,Object elem)
            {
                Object o=m_Vector.elementAt(index);
                m_Vector.setElementAt(elem,index);
                return o;
            }

        public boolean add(Object o)
            {
                m_Vector.addElement(o);
                return true;
            }

        public void add(int index,Object elem)
            {
                m_Vector.insertElementAt(elem,index);
            }

        public Object remove(int index)
            {
                Object o=m_Vector.elementAt(index);
                m_Vector.removeElementAt(index);
                return o;
            }

        public void clear()
            {
                m_Vector.removeAllElements();
            }





    }
