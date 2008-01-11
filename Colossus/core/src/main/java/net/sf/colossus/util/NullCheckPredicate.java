package net.sf.colossus.util;


/**
 * A predicate checking objects on being the null object.
 * 
 * This class can be used on its own, returning the boolean
 * value given in its constructor for null values and its opposite for
 * non-null values.
 * 
 * In this case <code>new NullCheckPredicate(true)</code> creates a predicate
 * checking for null values, and <code>new NullCheckPredicate(false)</code>
 * creates one to check for values other than null.
 * 
 * Alternatively subclasses can overwrite {@link #matchesNonNullValue(Object)},
 * in which case that determines the value for the non-null objects.
 * 
 * @param <T> The type of objects to check.
 */
public class NullCheckPredicate<T> implements Predicate<T>
{
    private final boolean nullValue;

    /**
     * Creates a new predicate.
     * 
     * @param nullValue The value to return for null.
     */
    public NullCheckPredicate(boolean nullValue)
    {
        this.nullValue = nullValue;
    }

    public final boolean matches(T object)
    {
        if (object == null)
        {
            return nullValue;
        }
        else
        {
            return matchesNonNullValue(object);
        }
    }

    /**
     * Can be overwritten to apply further checks for values that are not null.
     * 
     * @param object The value to check. Not null.
     * @return True iff the object matches. In the default implementation the
     *     opposite of the null value.
     */
    protected boolean matchesNonNullValue(T object)
    {
        assert object != null;
        return !nullValue;
    }

}
