package net.sf.colossus.util;


import java.util.Collection;
import java.util.Collections;


/**
 * A collection of static methods to help with using java.util.Collection derivates.
 * 
 * This is an addition to {@link Collections}.
 */
public class CollectionHelper
{
    /**
     * Copies all elements that match a predicate.
     * 
     * Every element of the first collection for which the predicate holds (i.e. returns
     * true) is added to the second collection.
     */
    public static <T> void copySelective(Collection<? extends T> source,
        Collection<? super T> target, Predicate<T> filter)
    {
        for (T t : source)
        {
            if (filter.matches(t))
            {
                target.add(t);
            }
        }
    }
}
