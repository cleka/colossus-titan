package net.sf.colossus.util;


import java.util.Arrays;


/**
 * A collection of static methods to help with using arrays of the Java language.
 * 
 * This is an addition to {@link Arrays}.
 */
public class ArrayHelper
{
    /**
     * Find the first element in the array that matches the predicate.
     * 
     * @param <T>       The type of element to use.
     * @param input     The array of candidates to match. Not null.
     * @param predicate The match condition. Not null.
     * @return The first match or null if there is none.
     */
    public static <T> T findFirstMatch(T[] input, Predicate<T> predicate)
    {
        assert input != null : "Illegal null value as parameter";
        assert predicate != null : "Illegal null value as parameter";

        for (T element : input)
        {
            if (predicate.matches(element))
            {
                return element;
            }
        }
        return null;
    }

    /**
     * Find the first element in the array that matches the predicate.
     * 
     * This is a two-dimensional version of {@link #findFirstMatch(T[], Predicate)},
     * iteration is right-to-left as usual in Java. 
     * 
     * @param <T>       The type of element to use.
     * @param input     The array of candidates to match. Not null.
     * @param predicate The match condition. Not null.
     * @return The first match or null if there is none.
     */
    public static <T> T findFirstMatch(T[][] input, Predicate<T> predicate)
    {
        assert input != null : "Illegal null value as parameter";
        assert predicate != null : "Illegal null value as parameter";

        for (T[] row : input)
        {
            for (T element : row)
            {
                if (predicate.matches(element))
                {
                    return element;
                }
            }
        }
        return null;
    }
}
