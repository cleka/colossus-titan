package net.sf.colossus.util;


/**
 * A simple predicate interface.
 * 
 * @param <T> The type of object to be tested.
 */
public interface Predicate<T>
{
    boolean matches(T object);
}
