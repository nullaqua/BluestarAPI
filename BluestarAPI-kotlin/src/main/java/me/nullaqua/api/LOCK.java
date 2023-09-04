package me.nullaqua.api;

/**
 * LOCK类,通过需要不空的LOCK实例,并且其没有任何可用构造函数,从而使对应方法无法被调用
 */
public final class LOCK
{
    private LOCK()
    {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public LOCK check()
    {
        return this;
    }
}