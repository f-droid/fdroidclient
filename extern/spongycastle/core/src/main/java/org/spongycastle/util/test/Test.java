package org.spongycastle.util.test;

public interface Test
{
    String getName();

    TestResult perform();
}
