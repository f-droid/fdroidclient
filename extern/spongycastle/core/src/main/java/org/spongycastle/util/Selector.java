package org.spongycastle.util;

public interface Selector
    extends Cloneable
{
    boolean match(Object obj);

    Object clone();
}
