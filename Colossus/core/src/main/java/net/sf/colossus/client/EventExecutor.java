package net.sf.colossus.client;


public interface EventExecutor
{
    public abstract void retriggerEvent();

    public abstract boolean isThereALastEvent();

    public abstract boolean getRetriggeredEventOngoing();
}
