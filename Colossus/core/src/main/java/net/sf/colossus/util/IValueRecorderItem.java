/*
 */

package net.sf.colossus.util;


/**
 *
 * @author dolbeau
 */
public interface IValueRecorderItem
{

    /**
     * Get the value.
     * @return The current value.
     */
    int getValue();

    String getWhy(String prefix);

    String getFull(String prefix);

    boolean isReset();

}
