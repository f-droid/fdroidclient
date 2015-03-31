package org.spongycastle.util;

public interface Selector
{
    boolean match(Object obj);

    Object clone();
}
