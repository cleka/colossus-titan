package net.sf.colossus.util;


/**
 * A simple predicate interface.
 * 
 * @param <T> The type of object to be tested.
 */
public interface Predicate<T>
{
    /**
     * Returns true iff the given object matches the predicate.
     * 
     * Note that null values are allowed and implementers are to return either
     * true or false for them instead of throwing exceptions.
     * 
     * @param object The object to test. May be null.
     * @return true iff the object matches.
     */
    boolean matches(T object);
}
