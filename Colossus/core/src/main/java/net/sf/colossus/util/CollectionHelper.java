package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


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

    /**
     * Retrieves all elements from a collection that match a predicate.
     * 
     * The result will be all elements of the source collection for which the predicate is true,
     * retrieved in normal iteration order.
     * 
     * @param <T> The type of elements we are interested in.
     * @param source A collection containing elements we are interested in. Not null. Can be empty.
     * @param filter The predicate determining if an element is to be retrieved. Not null.
     * @return A list containing all matching elements in iteration order. Never null. Can be empty.
     */
    public static <T> List<T> selectAsList(Collection<? extends T> source,
        Predicate<T> filter)
    {
        List<T> result = new ArrayList<T>();
        for (T t : source)
        {
            if (filter.matches(t))
            {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Retrieves the first element from a collection that matches a predicate.
     * 
     * @param <T> The type of elements we are interested in.
     * @param source A collection containing elements we are interested in. Not null. Can be empty.
     * @param filter The predicate determining if an element is to be retrieved. Not null.
     * @return The first matching element or null if no element matches.
     */
    public static <T> T selectFirst(Collection<? extends T> source,
        Predicate<T> filter)
    {
        for (T t : source)
        {
            if (filter.matches(t))
            {
                return t;
            }
        }
        return null;
    }
}
